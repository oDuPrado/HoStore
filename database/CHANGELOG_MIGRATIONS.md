-- ============================================================================
-- CHANGELOG DE MIGRAÇÕES DO BANCO DE DADOS
-- HoStore - Sistema de Gestão de Vendas com suporte NFCe
--
-- Este arquivo documenta TODAS as alterações realizadas no banco de dados
-- ao longo do tempo, facilitando auditoria e rollback se necessário
-- ============================================================================

## VERSÃO 1.0.0 - BASE DO SISTEMA (Data: XX/XX/2025)
- ✅ Tabelas iniciais: vendas, vendas_itens, clientes, produtos, etc.
- ✅ Estrutura de usuários e permissões
- ✅ Configurações básicas da aplicação

## VERSÃO 1.1.0 - SUPORTE A NFCe (Data: 26/01/2026)

### 📌 RAZÃO DA ALTERAÇÃO:
Implementação de suporte completo para emissão de Notas Fiscais de Consumidor Eletrônicas (NFCe).
Isso permite que o HoStore gere, assine e transmita NFCes ao SEFAZ para autorização fiscal.

### ✨ MUDANÇAS REALIZADAS:

#### 1️⃣ ADIÇÃO DE CAMPOS EM TABELAS EXISTENTES

**Tabela: produtos**
- `ncm` TEXT - Nomenclatura Comum do Mercosul (identificação fiscal)
- `cfop` TEXT - Código Fiscal de Operações (tipo de operação)
- `csosn` TEXT - Código de Situação no Simples Nacional
- `origem` TEXT - Origem do produto (nacional/importado)
- `unidade` TEXT - Unidade de medida padrão

✅ Compatibilidade: Campos ADD com DEFAULT NULL (não quebram dados existentes)

---

#### 2️⃣ NOVAS TABELAS DE REFERÊNCIA

| Tabela | Propósito | Registros Iniciais |
|--------|-----------|-------------------|
| `ncm` | Codificação de mercadorias | Vazia (importar SEFAZ) |
| `cfop` | Tipos de operações fiscais | 4 registros básicos |
| `csosn` | Situações Simples Nacional | 6 registros básicos |
| `origem` | Origem de produtos | 9 registros SEFAZ |
| `unidades` | Medidas de quantidade | 9 padrões (UN, KG, L, etc) |
| `formas_pagamento` | Métodos de pagamento | 14 formas SEFAZ |

---

#### 3️⃣ TABELAS DE CONFIGURAÇÃO FISCAL

**config_nfce**
- Armazena certificado digital (caminho + senha)
- CSC (Código de Segurança do Contribuinte) para NFCe
- Séries e numeração para NFCe
- Ambiente (homologação/produção)
- Regime tributário (Simples Nacional, Lucro Presumido, etc)
- Dados da empresa (CNPJ, Razão Social, Endereço)

**config_fiscal_default**
- Padrões de NCM, CFOP, CSOSN para novos produtos
- Alíquotas default para cálculos de imposto

---

#### 4️⃣ TABELAS DE DOCUMENTOS FISCAIS

**documentos_fiscais** (Índices com chave única)
- Status: RASCUNHO → PENDENTE → AUTORIZADA → CANCELADA
- Armazenamento do XML da NFCe após geração
- Protocolo de autorização do SEFAZ
- Rastreamento de erros e rejeições

**documentos_fiscais_itens**
- Espelho de vendas_itens com dados fiscais específicos
- Inclui impostos calculados por item
- Informações de CFOP, NCM, CSOSN por linha

**documentos_fiscais_pagamentos**
- Formas de pagamento utilizadas na NFCe
- Segue padrão SEFAZ (dinheiro, débito, crédito, PIX, etc)

---

#### 5️⃣ TABELAS DE IMPOSTOS

**imposto_icms** (Imposto sobre Circulação de Mercadorias)
- Alíquotas por estado/estado_destino/NCM
- Base de cálculo reduzida
- Margem de valor agregado (MVA) para ST

**imposto_ipi** (Imposto sobre Produtos Industrializados)
- Alíquotas por NCM
- Validação de CNPJ produtor

**imposto_pis_cofins** (Contribuições Sociais)
- CST e alíquotas de PIS
- CST e alíquotas de COFINS
- Combinações válidas por NCM

---

### 🔒 DADOS PRESERVADOS

✅ **Nenhum dado foi alterado ou deletado**
- Registros de vendas permanecem intactos
- Histórico de clientes mantido
- Produtos existentes continuam funcionando
- Apenas NOVOS campos foram adicionados (nullable)

### 🚀 PRÓXIMAS AÇÕES PARA CLIENTE

```
1. Executar este script: ALTER_TABLES_NFCE_20260126.sql
2. Acessar Sistema > Configuração > Fiscal
3. Preencher dados da empresa (CNPJ, Razão Social, etc)
4. Fazer upload do certificado digital (A1 ou eToken)
5. Definir série e ambiente (homologação)
6. Atualizar todos os produtos com NCM/CFOP/CSOSN
7. Importar tabela de impostos (ICMS, IPI, PIS)
8. Realizar teste de emissão em homologação
9. Mudar para produção
```

### ⚠️ ROLLBACK (se necessário)

Se precisar reverter esta versão:

```sql
-- Remover novas tabelas (em ordem de foreign keys)
DROP TABLE IF EXISTS documentos_fiscais_pagamentos;
DROP TABLE IF EXISTS documentos_fiscais_itens;
DROP TABLE IF EXISTS documentos_fiscais;
DROP TABLE IF EXISTS imposto_pis_cofins;
DROP TABLE IF EXISTS imposto_ipi;
DROP TABLE IF EXISTS imposto_icms;
DROP TABLE IF EXISTS sequencias_fiscais;
DROP TABLE IF EXISTS config_fiscal_default;
DROP TABLE IF EXISTS config_nfce;
DROP TABLE IF EXISTS formas_pagamento;
DROP TABLE IF EXISTS csosn;
DROP TABLE IF EXISTS cfop;
DROP TABLE IF EXISTS origem;
DROP TABLE IF EXISTS ncm;
DROP TABLE IF EXISTS unidades;

-- Remover campos adicionados em produtos
-- Nota: SQLite não suporta DROP COLUMN facilmente
-- Alternativa: Criar nova tabela sem os campos e copiar dados
```

---

## ESTRUTURA COMPLETA DE FISCAL

```
┌─ config_nfce ─────────────────┐
│ Certificado, CSC, Série, Regime         │
└──────────────────────────────────────────┘
           ↓
┌─ sequencias_fiscais ────────────────────┐
│ Último número emitido por série         │
└──────────────────────────────────────────┘
           ↓
┌─ documentos_fiscais ────────────────────┐
│ NFCe com status, XML, protocolo         │
├─ documentos_fiscais_itens              │
│ Linhas com NCM, CFOP, CSOSN, impostos  │
├─ documentos_fiscais_pagamentos         │
│ Formas de pagamento utilizadas          │
└──────────────────────────────────────────┘
           ↓
    ┌─ SEFAZ ─────┐
    │ Autorização │
    └─────────────┘
```

---

## REFERÊNCIA RÁPIDA DE TABELAS

| Tabela | Tipo | Descrição |
|--------|------|-----------|
| `sequencias_fiscais` | Controle | Numeração sequencial de NFCe |
| `documentos_fiscais` | Transacional | Documentos gerados e autorizados |
| `documentos_fiscais_itens` | Transacional | Itens de cada NFCe com fiscal |
| `documentos_fiscais_pagamentos` | Transacional | Formas de pagamento por nota |
| `config_nfce` | Configuração | Parâmetros e certificado |
| `config_fiscal_default` | Configuração | Padrões para novos produtos |
| `imposto_icms` | Referência | Alíquotas ICMS por estado/NCM |
| `imposto_ipi` | Referência | Alíquotas IPI por NCM |
| `imposto_pis_cofins` | Referência | Alíquotas PIS/COFINS por NCM |
| `ncm` | Referência | Classificação de produtos |
| `cfop` | Referência | Tipos de operação fiscal |
| `csosn` | Referência | Situações Simples Nacional |
| `origem` | Referência | Origem (nacional/importado) |
| `unidades` | Referência | Unidades de medida |
| `formas_pagamento` | Referência | Métodos de pagamento SEFAZ |

---

## HISTÓRICO DE VERSÕES

| Versão | Data | Descrição | Status |
|--------|------|-----------|--------|
| 1.0.0 | XX/XX/2025 | Sistema base | ✅ Estável |
| 1.1.0 | 26/01/2026 | Suporte NFCe | ✅ Em Produção |

---

## SUPORTE E DOCUMENTAÇÃO

📄 Documentação completa: `/DOCUMENTAÇÃO/MD/`
🔧 Guia técnico: `IMPLEMENTACAO_NFCE_STATUS.md`
✅ Checklist: `CHECKLIST_IMPLEMENTACAO_NFCE.md`
🚀 Quick start: `QUICK_START_NFCE_TESTES.md`

---

*Última atualização: 26/01/2026*
*Gerado automaticamente pelo sistema HoStore*
