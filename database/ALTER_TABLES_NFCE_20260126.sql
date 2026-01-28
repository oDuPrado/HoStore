-- ============================================================================
-- SCRIPT DE ALTERAÇÃO DE BANCO DE DADOS PARA IMPLEMENTAÇÃO DE NFCe
-- Data: 26/01/2026
-- Descrição: Adiciona suporte completo para emissão de NFCe no HoStore
-- 
-- ⚠️ IMPORTANTE: Este script adiciona campos e tabelas SEM alterar dados existentes
-- Clientes com banco de dados já existentes devem executar este script
-- Clientes novos receberão banco já atualizado
--
-- ESTRUTURA:
-- 1. Adição de campos fiscais aos produtos (se não existirem)
-- 2. Criação de tabelas de documentos fiscais
-- 3. Criação de tabelas de impostos (ICMS, IPI, PIS, COFINS)
-- 4. Criação de tabelas de configuração fiscal
-- 5. Criação de tabelas de sequências fiscais
-- ============================================================================
-- Nota: Em SQLite, "ALTER TABLE ... ADD COLUMN" falha se a coluna já existe.
-- Se isso ocorrer, apenas ignore o erro e continue o script.

-- ============================================================================
-- 1. VERIFICAR E ADICIONAR CAMPOS FISCAIS NA TABELA PRODUTOS
-- ============================================================================

-- Adicionar NCM se não existir
ALTER TABLE produtos ADD COLUMN ncm TEXT;

-- Adicionar CFOP se não existir
ALTER TABLE produtos ADD COLUMN cfop TEXT;

-- Adicionar CSOSN se não existir
ALTER TABLE produtos ADD COLUMN csosn TEXT;

-- Adicionar Origem se não existir
ALTER TABLE produtos ADD COLUMN origem TEXT;

-- Adicionar Unidade se não existir
ALTER TABLE produtos ADD COLUMN unidade TEXT;

-- ============================================================================
-- 2. TABELAS DE REFERÊNCIA FISCAL (se não existirem)
-- ============================================================================

-- Tabela NCM (Nomenclatura Comum do Mercosul)
CREATE TABLE IF NOT EXISTS ncm (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

-- Tabela CFOP (Código Fiscal de Operações)
CREATE TABLE IF NOT EXISTS cfop (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

-- Tabela CSOSN (Código de Situação da Operação no Simples Nacional)
CREATE TABLE IF NOT EXISTS csosn (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

-- Tabela Origem de Produtos
CREATE TABLE IF NOT EXISTS origem (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

-- Tabela Unidades de Medida
CREATE TABLE IF NOT EXISTS unidades (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

-- Tabela Formas de Pagamento
CREATE TABLE IF NOT EXISTS formas_pagamento (
  id TEXT PRIMARY KEY,
  nome TEXT NOT NULL,
  codigo_sefaz TEXT,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT
);

-- ============================================================================
-- 3. TABELA DE CONFIGURAÇÃO FISCAL (NFe/NFCe)
-- ============================================================================

CREATE TABLE IF NOT EXISTS config_nfce (
  id TEXT PRIMARY KEY,
  emitir_nfce INTEGER DEFAULT 1,
  csc_nfce TEXT,
  id_csc_nfce INTEGER,
  cert_a1_path TEXT,
  cert_a1_senha TEXT,
  serie_nfce INTEGER DEFAULT 1,
  numero_inicial_nfce INTEGER DEFAULT 1,
  ambiente TEXT DEFAULT 'homologacao',
  regime_tributario TEXT DEFAULT 'Simples Nacional',
  
  -- Dados da empresa
  nome_empresa TEXT,
  cnpj TEXT,
  inscricao_estadual TEXT,
  uf TEXT,
  nome_fantasia TEXT,
  endereco_logradouro TEXT,
  endereco_numero TEXT,
  endereco_complemento TEXT,
  endereco_bairro TEXT,
  endereco_municipio TEXT,
  endereco_cep TEXT,
  
  -- Campos extras (A1/A3 e modo de emissao)
  modo_emissao TEXT DEFAULT 'OFFLINE_VALIDACAO',
  cert_a3_host TEXT,
  cert_a3_porta INTEGER,
  cert_a3_usuario TEXT,
  cert_a3_senha TEXT,
  usa_cert_laboratorio INTEGER DEFAULT 0,
  cert_lab_path TEXT,
  cert_lab_senha TEXT,
  xsd_versao TEXT DEFAULT '4.00',
  
  criado_em TEXT,
  alterado_em TEXT
);

-- ============================================================================
-- 4. TABELA DE CONFIGURAÇÃO FISCAL DEFAULT (para novos produtos)
-- ============================================================================

CREATE TABLE IF NOT EXISTS config_fiscal_default (
  id TEXT PRIMARY KEY,
  
  ncm_padrao TEXT,
  cfop_padrao TEXT,
  csosn_padrao TEXT,
  origem_padrao TEXT,
  unidade_padrao TEXT,
  
  aliquota_icms_padrao REAL DEFAULT 0,
  aliquota_pis_padrao REAL DEFAULT 0,
  aliquota_cofins_padrao REAL DEFAULT 0,
  
  criado_em TEXT,
  alterado_em TEXT,
  
  FOREIGN KEY(origem_padrao) REFERENCES origem(codigo),
  FOREIGN KEY(ncm_padrao) REFERENCES ncm(codigo),
  FOREIGN KEY(unidade_padrao) REFERENCES unidades(codigo)
);

-- ============================================================================
-- 12. CAMPOS EXTRAS (QUALIDADE DE VIDA)
-- ============================================================================

-- Persistir última pasta do certificado (.pfx/.p12)
ALTER TABLE config_loja ADD COLUMN certificado_dir TEXT;

-- ============================================================================
-- 5. TABELA DE SEQUÊNCIAS FISCAIS
-- ============================================================================

CREATE TABLE IF NOT EXISTS sequencias_fiscais (
  id TEXT PRIMARY KEY,
  modelo TEXT NOT NULL,
  codigo_modelo INTEGER NOT NULL,
  serie INTEGER NOT NULL,
  ambiente TEXT NOT NULL,
  ultimo_numero INTEGER NOT NULL DEFAULT 0,
  criado_em TEXT NOT NULL,
  alterado_em TEXT,
  UNIQUE(modelo, codigo_modelo, serie, ambiente)
);

CREATE INDEX IF NOT EXISTS ux_seq_fiscal 
  ON sequencias_fiscais (modelo, codigo_modelo, serie, ambiente);

-- ============================================================================
-- 6. TABELAS DE DOCUMENTOS FISCAIS (NFCe)
-- ============================================================================

-- Tabela Principal de Documentos Fiscais
CREATE TABLE IF NOT EXISTS documentos_fiscais (
  id TEXT PRIMARY KEY,
  venda_id INTEGER NOT NULL,
  modelo TEXT NOT NULL,
  codigo_modelo INTEGER NOT NULL,
  serie INTEGER NOT NULL,
  numero INTEGER NOT NULL,
  ambiente TEXT NOT NULL,
  status TEXT NOT NULL,

  chave_acesso TEXT,
  protocolo TEXT,
  recibo TEXT,
  xml TEXT,
  erro TEXT,

  total_produtos REAL,
  total_desconto REAL,
  total_acrescimo REAL,
  total_final REAL,

  criado_em TEXT NOT NULL,
  criado_por TEXT,
  atualizado_em TEXT,
  cancelado_em TEXT,
  cancelado_por TEXT,

  FOREIGN KEY (venda_id) REFERENCES vendas(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_doc_fiscal_unico
  ON documentos_fiscais (modelo, codigo_modelo, serie, numero, ambiente);

CREATE INDEX IF NOT EXISTS idx_doc_fiscal_venda 
  ON documentos_fiscais(venda_id);

CREATE INDEX IF NOT EXISTS idx_doc_fiscal_status 
  ON documentos_fiscais(status);

-- Tabela de Itens de Documentos Fiscais
CREATE TABLE IF NOT EXISTS documentos_fiscais_itens (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  documento_id TEXT NOT NULL,
  venda_item_id INTEGER,
  produto_id TEXT,
  descricao TEXT NOT NULL,

  ncm TEXT NOT NULL,
  cfop TEXT NOT NULL,
  csosn TEXT NOT NULL,
  origem TEXT NOT NULL,
  unidade TEXT NOT NULL,

  quantidade INTEGER NOT NULL,
  valor_unit REAL NOT NULL,
  desconto REAL NOT NULL DEFAULT 0,
  acrescimo REAL NOT NULL DEFAULT 0,
  total_item REAL NOT NULL,
  observacoes TEXT,

  FOREIGN KEY (documento_id) REFERENCES documentos_fiscais(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_doc_fiscal_itens_doc 
  ON documentos_fiscais_itens(documento_id);

-- Tabela de Pagamentos de Documentos Fiscais
CREATE TABLE IF NOT EXISTS documentos_fiscais_pagamentos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  documento_id TEXT NOT NULL,
  tipo TEXT NOT NULL,
  valor REAL NOT NULL,
  FOREIGN KEY (documento_id) REFERENCES documentos_fiscais(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_doc_fiscal_pag_doc 
  ON documentos_fiscais_pagamentos(documento_id);

-- ============================================================================
-- 7. TABELAS DE IMPOSTOS (ICMS, IPI, PIS, COFINS)
-- ============================================================================

-- ICMS (Imposto sobre Circulação de Mercadorias e Serviços)
CREATE TABLE IF NOT EXISTS imposto_icms (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  estado TEXT NOT NULL,
  estado_destino TEXT NOT NULL,
  ncm TEXT NOT NULL,
  aliquota_consumidor REAL,
  aliquota_contribuinte REAL,
  reducao_base REAL DEFAULT 0,
  mva_bc REAL DEFAULT 0,
  ativo INTEGER DEFAULT 1,
  UNIQUE(estado, estado_destino, ncm)
);

-- IPI (Imposto sobre Produtos Industrializados)
CREATE TABLE IF NOT EXISTS imposto_ipi (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  ncm TEXT NOT NULL,
  aliquota REAL,
  cnpj_produtor TEXT,
  ativo INTEGER DEFAULT 1,
  UNIQUE(ncm, cnpj_produtor)
);

-- PIS/COFINS
CREATE TABLE IF NOT EXISTS imposto_pis_cofins (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  ncm TEXT NOT NULL,
  cst_pis TEXT,
  aliquota_pis REAL,
  cst_cofins TEXT,
  aliquota_cofins REAL,
  ativo INTEGER DEFAULT 1,
  UNIQUE(ncm, cst_pis, cst_cofins)
);

-- ============================================================================
-- 8. ADICIONAR FOREIGN KEYS EM PRODUTOS (se estrutura existir)
-- ============================================================================

-- Nota: SQLite tem limitações com ALTER TABLE para adicionar FOREIGN KEYS
-- Se necessário adicionar essas constraints, será feito via aplicação

-- ============================================================================
-- 9. DADOS INICIAIS (Referências Fiscais Básicas)
-- ============================================================================

-- Unidades de Medida Padrão
INSERT OR IGNORE INTO unidades (codigo, descricao) VALUES 
  ('UN', 'Unidade'),
  ('KG', 'Quilograma'),
  ('L', 'Litro'),
  ('M', 'Metro'),
  ('M2', 'Metro Quadrado'),
  ('CX', 'Caixa'),
  ('DZ', 'Dúzia'),
  ('PCT', 'Pacote'),
  ('HR', 'Hora');

-- Origem de Produtos
INSERT OR IGNORE INTO origem (codigo, descricao) VALUES 
  ('0', 'Nacional'),
  ('1', 'Importado'),
  ('2', 'Nacional com conteúdo importado'),
  ('3', 'Nacional, com fração de importado'),
  ('4', 'Nacional, conforme lei complementar'),
  ('5', 'Importado, com fração de nacional'),
  ('6', 'Importado, conforme lei complementar'),
  ('7', 'Armazenado nacional'),
  ('8', 'Armazenado importado');

-- CFOP Padrão (Operações com Consumidor Final)
INSERT OR IGNORE INTO cfop (codigo, descricao) VALUES 
  ('5102', 'Venda para Consumidor Final'),
  ('5101', 'Venda ao Contribuinte'),
  ('6102', 'Devolução de Venda para Consumidor Final'),
  ('6101', 'Devolução de Venda ao Contribuinte');

-- CSOSN Padrão (Simples Nacional)
INSERT OR IGNORE INTO csosn (codigo, descricao) VALUES 
  ('102', 'Tributada pelo Simples Nacional sem Permissão de Crédito'),
  ('103', 'Isenção do ICMS no Simples Nacional'),
  ('300', 'Imunidade do ICMS'),
  ('400', 'Não Tributada pelo ICMS'),
  ('500', 'ICMS Cobrado Anteriormente por ST ou Substituição Tributária'),
  ('900', 'Outros');

-- Formas de Pagamento SEFAZ
INSERT OR IGNORE INTO formas_pagamento (id, nome, codigo_sefaz) VALUES 
  ('dinheiro', 'Dinheiro', '01'),
  ('cheque', 'Cheque', '02'),
  ('cartao_credito', 'Cartão de Crédito', '03'),
  ('cartao_debito', 'Cartão de Débito', '04'),
  ('credito_loja', 'Crédito Loja', '05'),
  ('vale_alimentacao', 'Vale Alimentação', '10'),
  ('vale_refeicao', 'Vale Refeição', '11'),
  ('vale_presente', 'Vale Presente', '12'),
  ('vale_combustivel', 'Vale Combustível', '13'),
  ('boleto', 'Boleto Bancário', '15'),
  ('depósito', 'Depósito Bancário', '16'),
  ('pagamento_instancia', 'Pagamento por Instância de Crédito', '17'),
  ('transferência', 'Transferência Bancária', '18'),
  ('pix', 'PIX', '19'),
  ('sem_pagamento', 'Sem Pagamento', '90');

-- ============================================================================
-- 10. REGISTROS DE AUDITORIA
-- ============================================================================

-- Inserir configuração padrão se não existir
INSERT OR IGNORE INTO config_nfce (id, emitir_nfce, ambiente, regime_tributario)
VALUES ('CONFIG_PADRAO', 1, 'homologacao', 'Simples Nacional');

INSERT OR IGNORE INTO config_fiscal_default (id, cfop_padrao, csosn_padrao, origem_padrao, unidade_padrao)
VALUES ('DEFAULT', '5102', '102', '0', 'UN');

-- ============================================================================
-- FIM DO SCRIPT
-- ============================================================================

-- ============================================================================
-- V007: PEDIDOS DE COMPRA (CUSTO/PRECO/FORNECEDOR POR ITEM)
-- ============================================================================
ALTER TABLE pedido_produtos ADD COLUMN fornecedor_id TEXT;
ALTER TABLE pedido_produtos ADD COLUMN custo_unit REAL;
ALTER TABLE pedido_produtos ADD COLUMN preco_venda_unit REAL;
-- 
-- ✅ DADOS PRESERVADOS: Este script apenas ADICIONA campos e tabelas
-- ✅ COMPATIBILIDADE: Usa "IF NOT EXISTS" para evitar erros em bancos atualizados
-- ✅ SEGURANÇA: Nenhum dado existente foi alterado
--
-- Próximas ações:
-- 1. Configurar dados fiscais em Configuração > Fiscal
-- 2. Atualizar Produtos com NCM, CFOP, CSOSN, Origem e Unidade
-- 3. Importar/configurar tabelas de impostos (ICMS, IPI, PIS, COFINS)
-- 4. Emitir primeira NFCe em modo homologação
--
-- Para suporte, consulte: DOCUMENTAÇÃO/MD/CHECKLIST_IMPLEMENTACAO_NFCE.md
-- ============================================================================
-- Comandas: tempo de permanencia (cap 8h)
ALTER TABLE comandas ADD COLUMN tempo_permanencia_min INTEGER DEFAULT 0;

-- ============================================================================
-- V008: Estoque por lote - marcação de legado
-- ============================================================================
ALTER TABLE estoque_lotes ADD COLUMN origem TEXT DEFAULT 'NORMAL';
ALTER TABLE estoque_lotes ADD COLUMN legado INTEGER DEFAULT 0;

-- ============================================================================
-- V009: MODULO RH (FUNCIONARIOS, CARGOS, PONTO, ESCALA, FERIAS, COMISSOES, FOLHA)
-- ============================================================================
CREATE TABLE IF NOT EXISTS rh_cargos (
  id TEXT PRIMARY KEY,
  nome TEXT NOT NULL,
  descricao TEXT,
  salario_base REAL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

CREATE TABLE IF NOT EXISTS rh_funcionarios (
  id TEXT PRIMARY KEY,
  nome TEXT NOT NULL,
  tipo_contrato TEXT NOT NULL,
  cpf TEXT,
  cnpj TEXT,
  rg TEXT,
  pis TEXT,
  data_admissao TEXT,
  data_demissao TEXT,
  cargo_id TEXT,
  salario_base REAL,
  comissao_pct REAL DEFAULT 0,
  usuario_id TEXT,
  email TEXT,
  telefone TEXT,
  endereco TEXT,
  ativo INTEGER DEFAULT 1,
  observacoes TEXT,
  criado_em TEXT,
  alterado_em TEXT
);

CREATE TABLE IF NOT EXISTS rh_salarios (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  funcionario_id TEXT NOT NULL,
  cargo_id TEXT,
  salario_base REAL NOT NULL,
  data_inicio TEXT NOT NULL,
  data_fim TEXT,
  motivo TEXT,
  criado_em TEXT
);

CREATE TABLE IF NOT EXISTS rh_ponto (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  funcionario_id TEXT NOT NULL,
  data TEXT NOT NULL,
  entrada TEXT,
  saida TEXT,
  intervalo_inicio TEXT,
  intervalo_fim TEXT,
  horas_trabalhadas REAL,
  origem TEXT,
  criado_em TEXT
);

CREATE TABLE IF NOT EXISTS rh_escala (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  funcionario_id TEXT NOT NULL,
  data TEXT NOT NULL,
  inicio TEXT,
  fim TEXT,
  observacoes TEXT,
  criado_em TEXT
);

CREATE TABLE IF NOT EXISTS rh_ferias (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  funcionario_id TEXT NOT NULL,
  data_inicio TEXT NOT NULL,
  data_fim TEXT NOT NULL,
  abono INTEGER DEFAULT 0,
  status TEXT DEFAULT 'programada',
  observacoes TEXT,
  criado_em TEXT
);

CREATE TABLE IF NOT EXISTS rh_comissoes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  venda_id INTEGER,
  funcionario_id TEXT NOT NULL,
  percentual REAL,
  valor REAL,
  data TEXT,
  observacoes TEXT,
  UNIQUE(venda_id, funcionario_id)
);

CREATE TABLE IF NOT EXISTS rh_folha (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  competencia TEXT NOT NULL,
  funcionario_id TEXT NOT NULL,
  salario_base REAL,
  horas_trabalhadas REAL,
  horas_extras REAL,
  descontos REAL,
  comissao REAL,
  total_bruto REAL,
  total_liquido REAL,
  status TEXT DEFAULT 'aberta',
  criado_em TEXT
);

-- ============================================================================
-- V010: VENDAS - PARCELAMENTO / TAXAS DE CARTAO (METADADOS)
-- ============================================================================
ALTER TABLE vendas ADD COLUMN acrescimo REAL DEFAULT 0;
ALTER TABLE vendas ADD COLUMN juros REAL;
ALTER TABLE vendas ADD COLUMN intervalo_dias INTEGER;

ALTER TABLE vendas_pagamentos ADD COLUMN bandeira TEXT;
ALTER TABLE vendas_pagamentos ADD COLUMN tipo_cartao TEXT;
ALTER TABLE vendas_pagamentos ADD COLUMN parcelas INTEGER;
ALTER TABLE vendas_pagamentos ADD COLUMN intervalo_dias INTEGER;
ALTER TABLE vendas_pagamentos ADD COLUMN taxa_pct REAL;
ALTER TABLE vendas_pagamentos ADD COLUMN taxa_valor REAL;
ALTER TABLE vendas_pagamentos ADD COLUMN taxa_quem TEXT;

-- ============================================================================
-- V011: ESTORNOS - SEPARACAO TAXA
-- ============================================================================
ALTER TABLE vendas_estornos_pagamentos ADD COLUMN tipo_estorno TEXT;
ALTER TABLE vendas_estornos_pagamentos ADD COLUMN taxa_quem TEXT;


-- ============================================================================
-- V012: PROMOCOES COMPLETAS (categoria, hist?rico e itens)
-- ============================================================================
ALTER TABLE produtos ADD COLUMN categoria TEXT;
ALTER TABLE promocoes ADD COLUMN categoria TEXT;
ALTER TABLE promocoes ADD COLUMN ativo INTEGER DEFAULT 1;
ALTER TABLE promocoes ADD COLUMN prioridade INTEGER DEFAULT 0;

ALTER TABLE vendas_itens ADD COLUMN promocao_id TEXT;
ALTER TABLE vendas_itens ADD COLUMN desconto_origem TEXT;
ALTER TABLE vendas_itens ADD COLUMN desconto_valor REAL;
ALTER TABLE vendas_itens ADD COLUMN desconto_tipo TEXT;

CREATE TABLE IF NOT EXISTS promocoes_aplicacoes (
  id TEXT PRIMARY KEY,
  promocao_id TEXT,
  venda_id INTEGER,
  venda_item_id INTEGER,
  produto_id TEXT,
  cliente_id TEXT,
  qtd INTEGER,
  preco_original REAL,
  desconto_valor REAL,
  preco_final REAL,
  desconto_tipo TEXT,
  data_aplicacao TEXT
);
