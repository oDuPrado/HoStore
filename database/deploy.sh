#!/bin/bash
# ============================================================================
# SCRIPT DE DEPLOYMENT - HoStore NFCe v1.1.0
# 
# Uso:
#   ./deploy.sh --tipo cliente --ambiente homologacao
#   ./deploy.sh --tipo novo --ambiente producao
#
# ============================================================================

set -e  # Exit on error

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função de log
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Variáveis padrão
TIPO_CLIENTE="cliente"
AMBIENTE="homologacao"
DB_PATH="hostore.db"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)

# Parse argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        --tipo)
            TIPO_CLIENTE="$2"
            shift 2
            ;;
        --ambiente)
            AMBIENTE="$2"
            shift 2
            ;;
        --db)
            DB_PATH="$2"
            shift 2
            ;;
        *)
            log_error "Argumento desconhecido: $1"
            exit 1
            ;;
    esac
done

# Validar tipo de cliente
if [[ "$TIPO_CLIENTE" != "cliente" && "$TIPO_CLIENTE" != "novo" ]]; then
    log_error "Tipo inválido: $TIPO_CLIENTE (use 'cliente' ou 'novo')"
    exit 1
fi

# Validar ambiente
if [[ "$AMBIENTE" != "homologacao" && "$AMBIENTE" != "producao" ]]; then
    log_error "Ambiente inválido: $AMBIENTE (use 'homologacao' ou 'producao')"
    exit 1
fi

echo "============================================================================"
echo "  HoStore - Deployment de NFCe v1.1.0"
echo "============================================================================"
echo ""
log_info "Tipo de Cliente: $TIPO_CLIENTE"
log_info "Ambiente: $AMBIENTE"
log_info "Banco de Dados: $DB_PATH"
echo ""

# ============================================================================
# CENÁRIO 1: CLIENTE COM BANCO EXISTENTE
# ============================================================================

if [[ "$TIPO_CLIENTE" == "cliente" ]]; then
    log_info "Iniciando migração de banco de dados existente..."
    echo ""

    # Verificar se banco existe
    if [ ! -f "$DB_PATH" ]; then
        log_error "Banco de dados não encontrado: $DB_PATH"
        exit 1
    fi

    log_info "Banco encontrado: $DB_PATH"

    # Fazer backup
    BACKUP_FILE="${DB_PATH}.backup.${BACKUP_DATE}"
    log_info "Criando backup: $BACKUP_FILE"
    cp "$DB_PATH" "$BACKUP_FILE"
    log_success "Backup criado com sucesso"
    echo ""

    # Validar integridade antes
    log_info "Validando integridade do banco antes de alterações..."
    INTEGRITY_CHECK=$(sqlite3 "$DB_PATH" "PRAGMA integrity_check;")
    if [[ "$INTEGRITY_CHECK" != "ok" ]]; then
        log_error "Integridade do banco comprometida!"
        log_error "Resultado: $INTEGRITY_CHECK"
        exit 1
    fi
    log_success "Integridade validada"
    echo ""

    # Executar script de alteração
    log_info "Executando script de migração: ALTER_TABLES_NFCE_20260126.sql"
    
    if [ ! -f "ALTER_TABLES_NFCE_20260126.sql" ]; then
        log_error "Script não encontrado: ALTER_TABLES_NFCE_20260126.sql"
        exit 1
    fi

    sqlite3 "$DB_PATH" < ALTER_TABLES_NFCE_20260126.sql 2>/dev/null
    
    if [ $? -eq 0 ]; then
        log_success "Script executado com sucesso"
    else
        log_error "Erro ao executar script"
        log_info "Restaurando backup..."
        cp "$BACKUP_FILE" "$DB_PATH"
        log_error "Banco restaurado. Tente novamente depois."
        exit 1
    fi
    echo ""

    # Validar integridade depois
    log_info "Validando integridade após alterações..."
    INTEGRITY_CHECK=$(sqlite3 "$DB_PATH" "PRAGMA integrity_check;")
    if [[ "$INTEGRITY_CHECK" != "ok" ]]; then
        log_error "Integridade comprometida após migração!"
        log_info "Restaurando backup..."
        cp "$BACKUP_FILE" "$DB_PATH"
        exit 1
    fi
    log_success "Integridade confirmada"
    echo ""

    # Verificações
    log_info "Verificando tabelas criadas..."
    TABLE_COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name LIKE 'documento%';")
    log_success "Tabelas fiscais encontradas: $TABLE_COUNT"

    UNIT_COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM unidades;")
    log_success "Unidades de medida carregadas: $UNIT_COUNT"

    CFOP_COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM cfop;")
    log_success "CFOPs carregados: $CFOP_COUNT"

    echo ""
    log_success "Migração concluída com sucesso!"
    echo ""

    # Próximas ações
    log_warning "PRÓXIMAS AÇÕES:"
    echo "  1. Acessar Sistema > Configuração > Fiscal"
    echo "  2. Preencher dados da empresa (CNPJ, Razão Social, Endereço)"
    echo "  3. Fazer upload do certificado digital"
    echo "  4. Definir ambiente como: $AMBIENTE"
    echo "  5. Atualizar produtos com NCM, CFOP, CSOSN"
    echo "  6. Importar tabelas de impostos (ICMS, IPI, PIS/COFINS)"
    echo "  7. Testar emissão de NFCe"
    echo ""

# ============================================================================
# CENÁRIO 2: CLIENTE NOVO
# ============================================================================

elif [[ "$TIPO_CLIENTE" == "novo" ]]; then
    log_info "Criando novo banco de dados HoStore com suporte NFCe..."
    echo ""

    # Verificar se já existe
    if [ -f "$DB_PATH" ]; then
        log_warning "Banco já existe: $DB_PATH"
        read -p "Deseja sobrescrever? (s/n) " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Ss]$ ]]; then
            log_info "Operação cancelada"
            exit 0
        fi
        rm "$DB_PATH"
        log_info "Banco anterior removido"
    fi

    # Criar schema
    log_info "Executando schema completo: SCHEMA_FRESH_INSTALL.sql"

    if [ ! -f "SCHEMA_FRESH_INSTALL.sql" ]; then
        log_error "Schema não encontrado: SCHEMA_FRESH_INSTALL.sql"
        exit 1
    fi

    sqlite3 "$DB_PATH" < SCHEMA_FRESH_INSTALL.sql 2>/dev/null

    if [ $? -eq 0 ]; then
        log_success "Banco criado com sucesso"
    else
        log_error "Erro ao criar banco"
        exit 1
    fi
    echo ""

    # Validar integridade
    log_info "Validando integridade..."
    INTEGRITY_CHECK=$(sqlite3 "$DB_PATH" "PRAGMA integrity_check;")
    if [[ "$INTEGRITY_CHECK" != "ok" ]]; then
        log_error "Integridade comprometida: $INTEGRITY_CHECK"
        exit 1
    fi
    log_success "Integridade validada"
    echo ""

    # Verificações
    log_info "Verificando dados carregados..."
    TABLE_COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM sqlite_master WHERE type='table';")
    log_success "Total de tabelas: $TABLE_COUNT"

    UNIT_COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM unidades;")
    log_success "Unidades de medida: $UNIT_COUNT"

    PAYMENT_COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM formas_pagamento;")
    log_success "Formas de pagamento: $PAYMENT_COUNT"

    CSOSN_COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM csosn;")
    log_success "CSOSN carregados: $CSOSN_COUNT"

    echo ""
    log_success "Banco criado e inicializado com sucesso!"
    echo ""

    # Informações de configuração
    log_warning "CONFIGURAÇÃO NECESSÁRIA:"
    echo "  1. Definir dados da empresa"
    echo "  2. Fazer upload do certificado digital"
    echo "  3. Definir ambiente como: $AMBIENTE"
    echo "  4. Configurar alíquotas de impostos"
    echo "  5. Começar a usar o sistema"
    echo ""

fi

# ============================================================================
# RESUMO FINAL
# ============================================================================

echo "============================================================================"
echo "  Status: ✓ OPERAÇÃO CONCLUÍDA COM SUCESSO"
echo "============================================================================"
echo ""
log_info "Banco de dados: $DB_PATH"
log_info "Tipo de cliente: $TIPO_CLIENTE"
log_info "Ambiente: $AMBIENTE"

if [[ "$TIPO_CLIENTE" == "cliente" ]]; then
    log_info "Backup realizado: $BACKUP_FILE"
fi

echo ""
log_success "HoStore está pronto para emissão de NFCe!"
echo ""
