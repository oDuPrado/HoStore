# 📊 Banco de Dados HoStore - Documentação de Migração para NFCe

## 📋 Estrutura de Arquivos

```
database/
├── ALTER_TABLES_NFCE_20260126.sql     ← Para clientes com banco existente
├── SCHEMA_FRESH_INSTALL.sql            ← Para novos clientes
├── CHANGELOG_MIGRATIONS.md              ← Histórico de alterações
└── README.md                           ← Este arquivo
```

---

## 🎯 Cenário 1: Cliente com Banco de Dados Existente

**Situação:** Cliente já usa o HoStore e possui dados históricos.

### ✅ Procedimento

1. **Fazer backup do banco:**
   ```bash
   cp hostore.db hostore.db.backup.20260126
   ```

2. **Executar script de alteração:**
   ```bash
   sqlite3 hostore.db < ALTER_TABLES_NFCE_20260126.sql
   ```

3. **Validar integridade:**
   ```sql
   -- No SQLite
   PRAGMA integrity_check;
   
   -- Verificar novos campos em produtos
   SELECT COUNT(*) FROM produtos WHERE ncm IS NOT NULL;
   
   -- Verificar se tabelas foram criadas
   SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'documento%';
   ```

### 📋 O que muda para o cliente

**Dados históricos:** ✅ **100% Preservados**
- Todas as vendas continuam intactas
- Todos os clientes continuam com seus dados
- Histórico de estoque mantido

**Novidades:**
- Campos fiscais adicionados em PRODUTOS (ncm, cfop, csosn, origem, unidade)
- Novas tabelas para gerenciar documentos fiscais
- Tabelas de cálculo de impostos
- Novo menu: Configuração > Fiscal

**Próximas ações do cliente:**
1. Acessar Sistema > Configuração > Fiscal
2. Preencher dados da empresa (CNPJ, Razão Social, etc)
3. Fazer upload do certificado digital
4. Atualizar todos os produtos com NCM/CFOP/CSOSN
5. Testar emissão em homologação

---

## 🎯 Cenário 2: Cliente Novo (Sem Banco)

**Situação:** Cliente está iniciando o uso do HoStore.

### ✅ Procedimento

1. **Criar banco com schema completo:**
   ```bash
   sqlite3 hostore.db < SCHEMA_FRESH_INSTALL.sql
   ```

2. **O banco já vem pronto com:**
   - ✅ Todas as tabelas necessárias
   - ✅ Dados de referência carregados (NCM, CFOP, CSOSN, Origem, Unidades)
   - ✅ Formas de pagamento configuradas
   - ✅ Tabelas fiscais criadas
   - ✅ Índices otimizados

3. **Cliente apenas configura:**
   - Dados da empresa (CNPJ, Razão Social)
   - Certificado digital
   - Começar a usar normalmente

---

## 🔄 Comparação dos Scripts

| Aspecto | ALTER_TABLES | SCHEMA_FRESH |
|---------|--------------|--------------|
| **Uso** | Clientes antigos | Clientes novos |
| **Preserva dados** | ✅ Sim | N/A (novo) |
| **Tabelas base** | Mantidas | Criadas |
| **Tabelas NFCe** | Criadas | Criadas |
| **Dados referência** | Inseridos | Inseridos |
| **Tempo execução** | ~5 segundos | ~10 segundos |
| **Risco** | Baixo | Nenhum |

---

## 📊 Estrutura Fiscal Criada

```
┌─────────────────────────────────────────────┐
│   config_nfce                     │
│   (Certificado, CSC, Série, Empresa)        │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│   SEQUENCIAS_FISCAIS                        │
│   (Última numeração emitida)                │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│   DOCUMENTOS_FISCAIS (NFCes)                │
│   ├─ documentos_fiscais_itens              │
│   └─ documentos_fiscais_pagamentos         │
└─────────────────────────────────────────────┘
                    ↓
            ┌───────────────┐
            │    SEFAZ      │
            │ (Autorização) │
            └───────────────┘

REFERÊNCIAS FISCAIS:
├─ ncm (Classificação de mercadorias)
├─ cfop (Código Fiscal de Operações)
├─ csosn (Situação Simples Nacional)
├─ origem (Nacional/Importado)
├─ unidades (Medidas)
├─ formas_pagamento (SEFAZ)
├─ imposto_icms (Cálculo ICMS)
├─ imposto_ipi (Cálculo IPI)
└─ imposto_pis_cofins (Cálculo PIS/COFINS)
```

---

## 🔐 Segurança & Backup

### Antes de Executar Alterações

1. **Sempre faça backup:**
   ```bash
   # Linux/Mac
   cp hostore.db hostore.db.backup.$(date +%Y%m%d_%H%M%S)
   
   # Windows PowerShell
   Copy-Item hostore.db "hostore.db.backup.$(Get-Date -Format 'yyyyMMdd_HHmmss')"
   ```

2. **Valide integridade:**
   ```sql
   PRAGMA integrity_check;
   PRAGMA foreign_key_check;
   ```

3. **Realize teste em homologação primeiro**

### Rollback (se necessário)

```bash
# Restaurar backup
cp hostore.db.backup hostore.db
```

---

## 📈 Tabelas Criadas

### Tabelas Principais de Documentos
- `documentos_fiscais` - NFCes emitidas
- `documentos_fiscais_itens` - Linhas de cada NFCe
- `documentos_fiscais_pagamentos` - Formas de pagamento

### Tabelas de Cálculo de Impostos
- `imposto_icms` - ICMS por estado/NCM
- `imposto_ipi` - IPI por NCM
- `imposto_pis_cofins` - PIS/COFINS por NCM

### Tabelas de Configuração
- `config_nfce` - Parâmetros fiscais e certificado
- `config_fiscal_default` - Padrões para novos produtos
- `sequencias_fiscais` - Controle de numeração

### Tabelas de Referência
- `ncm` - Nomenclatura de Produtos
- `cfop` - Operações Fiscais
- `csosn` - Situações Simples Nacional
- `origem` - Origem de Produtos
- `unidades` - Unidades de Medida
- `formas_pagamento` - Métodos SEFAZ

---

## 🧪 Testes Pós-Migração

### Teste 1: Integridade do Banco
```sql
-- Verificar se todas as tabelas foram criadas
SELECT COUNT(*) as total_tabelas 
FROM sqlite_master 
WHERE type='table';
-- Resultado esperado: 20+ tabelas
```

### Teste 2: Dados de Referência
```sql
-- Verificar unidades carregadas
SELECT COUNT(*) FROM unidades;
-- Resultado esperado: 9

-- Verificar formas de pagamento
SELECT COUNT(*) FROM formas_pagamento;
-- Resultado esperado: 13+

-- Verificar CFOP
SELECT COUNT(*) FROM cfop;
-- Resultado esperado: 4
```

### Teste 3: Índices
```sql
-- Listar índices criados
SELECT name, tbl_name FROM sqlite_master 
WHERE type='index' AND sql IS NOT NULL;
```

### Teste 4: Dados Históricos (se alteração)
```sql
-- Para clientes com migração
SELECT COUNT(*) FROM vendas;
-- Resultado esperado: mesmo número de antes

SELECT COUNT(*) FROM vendas_itens;
-- Resultado esperado: mesmo número de antes

-- Verificar se produtos mantiveram dados
SELECT COUNT(*) FROM produtos WHERE preco_venda > 0;
```

---

## ⚙️ Configuração Pós-Migração

### Para Clientes com Banco Existente

1. **Acesse Configuração > Fiscal**
2. **Preencha dados da empresa:**
   - CNPJ
   - Razão Social / Nome Fantasia
   - Inscrição Estadual
   - Endereço completo
   - UF

3. **Configure certificado:**
   - Envie certificado digital (A1 ou eToken)
   - Defina senha
   - Defina série da NFCe
   - Defina ambiente (homologação/produção)

4. **Atualize produtos:**
   - Para cada produto, preencha:
     - NCM (Nomenclatura do Produto)
     - CFOP (Tipo de Operação)
     - CSOSN (Situação Fiscal)
     - Origem (Nacional/Importado)
     - Unidade

5. **Importe tabelas de impostos:**
   - ICMS (por estado)
   - IPI (se aplicável)
   - PIS/COFINS

6. **Teste em homologação:**
   - Emita primeira NFCe de teste
   - Valide XML gerado
   - Solicite autorização SEFAZ
   - Confirme recebimento de protocolo

7. **Mude para produção:**
   - Altere ambiente de "homologacao" para "producao"
   - Recomece numeração se necessário
   - Comece a emitir NFCes reais

---

## 📝 Changelog

Veja [CHANGELOG_MIGRATIONS.md](CHANGELOG_MIGRATIONS.md) para histórico completo de versões e alterações.

---

## 🆘 Troubleshooting

### Erro: "database is locked"
- Feche todas conexões ao banco
- Aguarde 30 segundos
- Tente novamente

### Erro: "table already exists"
- Seu banco já foi atualizado
- Nenhuma ação necessária
- Scripts usam `CREATE TABLE IF NOT EXISTS`

### Erro: "FOREIGN KEY constraint failed"
- Não ocorre em scripts originais
- Valide integridade com `PRAGMA integrity_check`
- Restaure backup se necessário

### Campos aparecem NULL nos produtos
- Normal! Novos campos começam vazios
- Cliente preenche conforme necessário
- Não afeta dados existentes

---

## 📚 Documentação Relacionada

- [CHECKLIST_IMPLEMENTACAO_NFCE.md](../DOCUMENTAÇÃO/MD/CHECKLIST_IMPLEMENTACAO_NFCE.md)
- [QUICK_START_NFCE_TESTES.md](../DOCUMENTAÇÃO/MD/QUICK_START_NFCE_TESTES.md)
- [IMPLEMENTACAO_NFCE_STATUS.md](../DOCUMENTAÇÃO/MD/IMPLEMENTACAO_NFCE_STATUS.md)

---

## 📞 Suporte

Para dúvidas sobre migração, consulte:
- Documentação: `/DOCUMENTAÇÃO/MD/`
- Status de implementação: `IMPLEMENTACAO_NFCE_STATUS.md`
- Guia técnico: `GUIA_TECNICO_CORRECOES.md`

---

**Última atualização:** 26/01/2026  
**Versão:** 1.1.0 (Suporte NFCe)  
**Status:** ✅ Pronto para Produção
