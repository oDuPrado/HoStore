-- ============================================================================
-- SCHEMA DE CRIAÇÃO DO BANCO (Para novos clientes)
-- 
-- Este arquivo contém TODAS as tabelas necessárias para uma instalação
-- completa do HoStore com suporte total a NFCe
--
-- Use este arquivo para clientes SEM banco de dados existente
-- ============================================================================

-- ============================================================================
-- TABELAS BASE (Já existentes no sistema original)
-- ============================================================================

CREATE TABLE IF NOT EXISTS clientes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL,
  cpf_cnpj TEXT,
  email TEXT,
  telefone TEXT,
  endereco TEXT,
  numero TEXT,
  complemento TEXT,
  bairro TEXT,
  cidade TEXT,
  uf TEXT,
  cep TEXT,
  criado_em TEXT,
  atualizado_em TEXT
);

CREATE TABLE IF NOT EXISTS produtos (
  id TEXT PRIMARY KEY,
  codigo_barra TEXT UNIQUE,
  nome TEXT NOT NULL,
  descricao TEXT,
  preco_custo REAL,
  preco_venda REAL NOT NULL,
  estoque INTEGER DEFAULT 0,
  ativo INTEGER DEFAULT 1,
  
  -- Campos Fiscais para NFCe
  ncm TEXT,
  cfop TEXT,
  csosn TEXT,
  origem TEXT,
  unidade TEXT,
  
  criado_em TEXT,
  atualizado_em TEXT
);

CREATE TABLE IF NOT EXISTS vendas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  numero_nfce TEXT,
  cliente_id INTEGER,
  data_venda TEXT NOT NULL,
  total_produtos REAL,
  total_desconto REAL,
  total_acrescimo REAL,
  total_final REAL,
  
  status_fiscal TEXT DEFAULT 'pendente',
  
  criado_em TEXT,
  atualizado_em TEXT,
  FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE IF NOT EXISTS vendas_itens (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  venda_id INTEGER NOT NULL,
  produto_id TEXT NOT NULL,
  quantidade INTEGER NOT NULL,
  valor_unit REAL NOT NULL,
  desconto REAL DEFAULT 0,
  acrescimo REAL DEFAULT 0,
  total_item REAL NOT NULL,
  
  criado_em TEXT,
  FOREIGN KEY (venda_id) REFERENCES vendas(id),
  FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE TABLE IF NOT EXISTS usuarios (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL,
  email TEXT UNIQUE NOT NULL,
  senha TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT
);

-- ============================================================================
-- TABELAS DE REFERÊNCIA FISCAL
-- ============================================================================

CREATE TABLE IF NOT EXISTS unidades (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

CREATE TABLE IF NOT EXISTS ncm (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

CREATE TABLE IF NOT EXISTS cfop (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

CREATE TABLE IF NOT EXISTS csosn (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

CREATE TABLE IF NOT EXISTS origem (
  codigo TEXT PRIMARY KEY,
  descricao TEXT NOT NULL,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT,
  alterado_em TEXT
);

CREATE TABLE IF NOT EXISTS formas_pagamento (
  id TEXT PRIMARY KEY,
  nome TEXT NOT NULL,
  codigo_sefaz TEXT,
  ativo INTEGER DEFAULT 1,
  criado_em TEXT
);

-- ============================================================================
-- TABELAS DE CONFIGURAÇÃO
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
-- TABELAS DE CONTROLE FISCAL
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

CREATE INDEX idx_seq_fiscal ON sequencias_fiscais (modelo, codigo_modelo, serie, ambiente);

-- ============================================================================
-- TABELAS DE DOCUMENTOS FISCAIS (NFCe)
-- ============================================================================

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

CREATE UNIQUE INDEX ux_doc_fiscal_unico 
  ON documentos_fiscais (modelo, codigo_modelo, serie, numero, ambiente);

CREATE INDEX idx_doc_fiscal_venda ON documentos_fiscais(venda_id);
CREATE INDEX idx_doc_fiscal_status ON documentos_fiscais(status);

-- ============================================================================

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

CREATE INDEX idx_doc_fiscal_itens_doc ON documentos_fiscais_itens(documento_id);

-- ============================================================================

CREATE TABLE IF NOT EXISTS documentos_fiscais_pagamentos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  documento_id TEXT NOT NULL,
  tipo TEXT NOT NULL,
  valor REAL NOT NULL,
  FOREIGN KEY (documento_id) REFERENCES documentos_fiscais(id) ON DELETE CASCADE
);

CREATE INDEX idx_doc_fiscal_pag_doc ON documentos_fiscais_pagamentos(documento_id);

-- ============================================================================
-- TABELAS DE CÁLCULO DE IMPOSTOS
-- ============================================================================

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

CREATE TABLE IF NOT EXISTS imposto_ipi (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  ncm TEXT NOT NULL,
  aliquota REAL,
  cnpj_produtor TEXT,
  ativo INTEGER DEFAULT 1,
  UNIQUE(ncm, cnpj_produtor)
);

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

-- ============================================================================
-- TABELAS DE COMANDAS
-- ============================================================================

CREATE TABLE IF NOT EXISTS comandas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  cliente_id TEXT,
  nome_cliente TEXT,
  mesa TEXT,
  status TEXT NOT NULL DEFAULT 'aberta',
  venda_id INTEGER,
  total_bruto REAL NOT NULL DEFAULT 0,
  desconto REAL NOT NULL DEFAULT 0,
  acrescimo REAL NOT NULL DEFAULT 0,
  total_liquido REAL NOT NULL DEFAULT 0,
  total_pago REAL NOT NULL DEFAULT 0,
  observacoes TEXT,
  criado_em TEXT NOT NULL,
  criado_por TEXT,
  fechado_em TEXT,
  fechado_por TEXT,
  tempo_permanencia_min INTEGER DEFAULT 0,
  cancelado_em TEXT,
  cancelado_por TEXT,
  FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  FOREIGN KEY (venda_id) REFERENCES vendas(id)
);

CREATE TABLE IF NOT EXISTS comandas_itens (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  comanda_id INTEGER NOT NULL,
  produto_id TEXT NOT NULL,
  qtd INTEGER NOT NULL,
  preco REAL NOT NULL,
  desconto REAL NOT NULL DEFAULT 0,
  acrescimo REAL NOT NULL DEFAULT 0,
  total_item REAL NOT NULL,
  observacoes TEXT,
  criado_em TEXT NOT NULL,
  criado_por TEXT,
  FOREIGN KEY (comanda_id) REFERENCES comandas(id) ON DELETE CASCADE,
  FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE TABLE IF NOT EXISTS comandas_pagamentos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  comanda_id INTEGER NOT NULL,
  tipo TEXT NOT NULL,
  valor REAL NOT NULL,
  data TEXT NOT NULL,
  usuario TEXT,
  FOREIGN KEY (comanda_id) REFERENCES comandas(id) ON DELETE CASCADE
);

-- INSERÇÃO DE DADOS INICIAIS
-- ============================================================================

-- Unidades de Medida Padrão
INSERT INTO unidades (codigo, descricao) VALUES 
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
INSERT INTO origem (codigo, descricao) VALUES 
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
INSERT INTO cfop (codigo, descricao) VALUES 
  ('5102', 'Venda para Consumidor Final'),
  ('5101', 'Venda ao Contribuinte'),
  ('6102', 'Devolução de Venda para Consumidor Final'),
  ('6101', 'Devolução de Venda ao Contribuinte');

-- CSOSN Padrão
INSERT INTO csosn (codigo, descricao) VALUES 
  ('102', 'Tributada pelo Simples Nacional sem Permissão de Crédito'),
  ('103', 'Isenção do ICMS no Simples Nacional'),
  ('300', 'Imunidade do ICMS'),
  ('400', 'Não Tributada pelo ICMS'),
  ('500', 'ICMS Cobrado Anteriormente por ST ou Substituição Tributária'),
  ('900', 'Outros');

-- Formas de Pagamento
INSERT INTO formas_pagamento (id, nome, codigo_sefaz) VALUES 
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
  ('deposito', 'Depósito Bancário', '16'),
  ('pix', 'PIX', '19'),
  ('sem_pagamento', 'Sem Pagamento', '90');

-- Configuração padrão
INSERT INTO config_nfce (id, emitir_nfce, ambiente, regime_tributario)
VALUES ('CONFIG_PADRAO', 1, 'homologacao', 'Simples Nacional');

INSERT INTO config_fiscal_default (id, cfop_padrao, csosn_padrao, origem_padrao, unidade_padrao)
VALUES ('DEFAULT', '5102', '102', '0', 'UN');

-- ============================================================================
-- FIM DO SCRIPT
-- ============================================================================
