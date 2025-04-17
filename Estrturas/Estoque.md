# 📦 Documentação Oficial – Módulo de Estoque (HoStore)

---

## 🧠 **1. Visão Geral**

O **módulo de estoque** do ERP HoStore é projetado para:

- Gerenciar **estoques diversos** (Cartas, Boosters, Decks, ETBs, Acessórios, Produtos Gerais como comidas e bebidas).
- Cadastro modular e especializado para cada categoria.
- Consultas avançadas e rápidas do estoque.
- Alertas automáticos de estoque baixo.
- Integração futura com scrapper externo.

---

## 📑 **2. Categorias do Estoque**

| Categoria  | Exemplo                                      | Observação                                       |
|------------|----------------------------------------------|--------------------------------------------------|
| Cartas     | Pokémon individuais                          | Já implementado (precisa expansão)               |
| Boosters   | Pacotes fechados (booster, booster box)      | Cadastro especializado                           |
| Decks      | Decks prontos e pré-montados                 | Cadastro com lista completa de cartas            |
| ETBs       | Elite Trainer Boxes                          | Informações específicas (conteúdo etc.)          |
| Acessórios | Sleeves, Playmats, dados etc.                | Informações básicas (marca, tipo, quantidade)    |
| Produtos   | Comidas, bebidas e outros produtos variados  | Cadastro simples genérico                        |

---

## 🚦 **3. Fluxograma Completo (Geral)**

```plaintext
[Tela Principal] → [Painel Estoque]

  | Botão "Novo Item"
  ↓
[SelecionarCategoriaDialog]
  - Seleciona categoria do produto (Carta, Booster, Deck, ETB, etc.)

  | Categoria selecionada
  ↓
[Modal Cadastro Específico] 
  - CartaCadastroDialog, BoosterCadastroDialog, DeckCadastroDialog etc.
  - Preenche dados específicos por categoria
  - Validação obrigatória

  | Salva produto no banco
  ↓
✅ Produto cadastrado e listado no Painel Estoque
```

---

## 🎯 **4. Painel Estoque**

**Tela principal** do controle de estoque.

### 🔍 Filtros Disponíveis:

- **Categoria**
- **Quantidade (estoque baixo, estoque esgotado)**
- **Preço**
- **Pesquisa textual** por nome ou ID

### 🖥️ Colunas Principais (Tabela Geral):

- ID
- Nome
- Categoria
- Subcategoria (ex: tipo carta, sabor bebida etc.)
- Quantidade em estoque
- Preço Venda (unitário)
- Última atualização
- Ações (Editar, Remover, Detalhes)

### 📊 Dashboard Integrado (Topo do Painel):

- Estoque total em unidades e valor total estimado.
- Itens com estoque baixo (< quantidade mínima definida).
- Valor médio dos itens por categoria.

---

## 📋 **5. Cadastro Modular**

Cada categoria terá **seu próprio modal de cadastro** com campos específicos.

### 🃏 CartaCadastroDialog

- Nome, Coleção, Número, Condição, Raridade, Idioma, Quantidade, Preço, Custo.
- Validação obrigatória: nome, coleção, preço, quantidade.

### 📦 BoosterCadastroDialog

- Nome, Coleção, Quantidade Pacotes, Preço Pacote, Preço Box, Quantidade estoque.
- Campos opcionais: Idioma, Descrição curta.

### 🎴 DeckCadastroDialog

- Nome do Deck, Tipo (competitivo ou temático), Lista completa de cartas (nome, quantidade).
- Campos adicionais: Preço, Quantidade estoque.

### 📕 EtbCadastroDialog

- Nome, Coleção, Conteúdo completo (Boosters, dados, sleeves etc.), Preço, Quantidade estoque.

### 🎲 AcessorioCadastroDialog

- Tipo (Sleeve, Playmat etc.), Marca, Quantidade por pacote, Preço, Quantidade estoque.

### 🥤 ProdutoCadastroDialog (Genérico)

- Nome, Categoria (Comida, Bebida etc.), Preço, Validade (se aplicável), Quantidade estoque.

---

## 🔔 **6. Regras de Negócio**

### 🔴 Validação de Dados:

- **Obrigatórios**: Nome, preço e quantidade.
- Validade obrigatória para produtos perecíveis.

### ⚠️ Alertas Automáticos:

- Estoque baixo (<5 unidades padrão).
- Produtos próximos da validade (15 dias antecedência).

### 📉 Atualizações do Estoque:

- Redução automática após vendas.
- Incremento manual via modais específicos (entrada estoque).

---

## 📂 **7. Estrutura de Dados**

**Exemplo Estrutura SQL Completa (simplificada):**

```sql
cartas (
  id TEXT PRIMARY KEY,
  nome TEXT,
  colecao TEXT,
  numero TEXT,
  qtd INTEGER,
  preco REAL,
  custo REAL,
  condicao TEXT,
  raridade TEXT,
  idioma TEXT,
  alterado_em TEXT,
  alterado_por TEXT
)

boosters (
  id TEXT PRIMARY KEY,
  nome TEXT,
  colecao TEXT,
  qtd_pacotes INTEGER,
  preco_pacote REAL,
  preco_box REAL,
  qtd INTEGER,
  alterado_em TEXT,
  alterado_por TEXT
)

decks (
  id TEXT PRIMARY KEY,
  nome TEXT,
  tipo TEXT,
  preco REAL,
  qtd INTEGER,
  alterado_em TEXT,
  alterado_por TEXT
)

deck_cartas (
  deck_id TEXT,
  carta_id TEXT,
  quantidade INTEGER,
  PRIMARY KEY(deck_id, carta_id),
  FOREIGN KEY(deck_id) REFERENCES decks(id),
  FOREIGN KEY(carta_id) REFERENCES cartas(id)
)

etbs (
  id TEXT PRIMARY KEY,
  nome TEXT,
  colecao TEXT,
  conteudo TEXT,
  preco REAL,
  qtd INTEGER,
  alterado_em TEXT,
  alterado_por TEXT
)

acessorios (
  id TEXT PRIMARY KEY,
  tipo TEXT,
  marca TEXT,
  qtd_pacote INTEGER,
  preco REAL,
  qtd INTEGER,
  alterado_em TEXT,
  alterado_por TEXT
)

produtos (
  id TEXT PRIMARY KEY,
  nome TEXT,
  categoria TEXT,
  preco REAL,
  validade DATE,
  qtd INTEGER,
  alterado_em TEXT,
  alterado_por TEXT
)
```

---

## 🛠️ **8. Consultas Avançadas e Dashboards**

- Busca integrada por **categoria, texto e faixa de preço**.
- Dashboard dinâmico mostrando resumo visual do estoque.
- Relatórios automáticos em PDF (estoque completo ou filtrado).

---

## ♻️ **9. Exclusão e Histórico (Auditoria)**

- Exclusão com confirmação obrigatória.
- Manter log de exclusão no banco para auditoria futura.

**Campos padrão para auditoria**:

| Campo          | Descrição                      | Exemplo                |
|----------------|--------------------------------|------------------------|
| criado_em      | Data/hora criação              | 2025-04-17 08:30       |
| criado_por     | Usuário criação                | admin                  |
| alterado_em    | Data/hora última alteração     | 2025-04-17 09:15       |
| alterado_por   | Usuário alteração              | admin                  |

---

## 🚀 **10. Implementações Futuras (previstas)**

- Integração automática com scrapper (Liga Pokémon, etc.)
- Sistema de reservas automáticas.
- Gestão avançada de validade (produtos perecíveis).

---

✅ **Conclusão**

Esta documentação detalha claramente **todos os aspectos** necessários para desenvolver e manter um módulo de estoque robusto, escalável e preparado para atender uma loja física especializada em Pokémon TCG e demais produtos.

✅ **Documentação Completa e pronta para implementação.**