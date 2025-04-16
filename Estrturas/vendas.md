# 🛒 Documentação Oficial: Sistema de Vendas HoStore

---

## 🧠 1. Visão Geral do Sistema

O **módulo de vendas** é responsável por gerenciar o processo completo de vendas no ERP HoStore, permitindo:

- Criar, editar e finalizar vendas;
- Adicionar e remover itens ao carrinho;
- Vincular clientes à venda;
- Aplicar descontos;
- Gerar comprovantes;
- Realizar baixa automática no estoque;
- Consultar e gerenciar vendas anteriores;
- Manter histórico de alterações (auditoria);
- Controlar pagamentos e parcelas;
- Permitir estornos e reabertura mediante autorização.

---

## 🗺️ 2. Fluxograma Completo de Vendas

```plaintext
[TelaPrincipal] → [PainelVendas]

  | (Botão "Nova Venda")
  ↓

[VendaNovaDialog] (Carrinho aberto)
  - Selecionar cliente
  - Adicionar/Remover itens
  - Editar quantidade/preço
  - Aplicar descontos (% ou fixo)
  - Consultar subtotal atualizado em tempo real

  | (Botão "Finalizar")
  ↓

[VendaFinalizarDialog]
  - Confirmação final
  - Selecionar forma de pagamento e parcelamento
  - Revisão dos dados completos (cliente, itens, total)
  - Opção de gerar comprovante (PDF/Imprimir)

  | (Botão "Confirmar")
  ↓

✅ Venda Finalizada:
  - Salvar venda no banco de dados
  - Atualizar estoque automaticamente
  - Enviar notificações (estoque baixo, alterações)

[Retorno ao PainelVendas]
```

---

## 🧩 3. Responsabilidades das Classes e Telas

### 📌 PainelVendas.java
- Lista todas as vendas realizadas;
- Aplicar filtros avançados por cliente, data, valor total, status;
- Permite acessar detalhes específicos de cada venda;
- Acesso rápido para criar nova venda.

### 📌 VendaNovaDialog.java
- Criação da venda com carrinho aberto;
- Seleciona o cliente da venda;
- Busca avançada de cartas pelo nome/número;
- Aplica descontos por item ou totais;
- Edita/remover itens antes de finalizar a venda;
- Verifica automaticamente disponibilidade em estoque.

### 📌 VendaFinalizarDialog.java
- Exibe resumo detalhado antes de finalizar;
- Seleciona método de pagamento (dinheiro, cartão, PIX, etc.);
- Configuração de parcelamento;
- Geração do comprovante da venda em PDF ou impressão direta.

### 📌 VendaService.java
- Gerencia operações de venda no banco de dados;
- Realiza baixa automática do estoque;
- Mantém histórico completo das ações realizadas (logs);
- Gerencia sistema de estornos e reabertura da venda (mediante autenticação).

### 📌 VendaDAO.java e VendaItemDAO.java
- Operações CRUD nas tabelas de `vendas` e `vendas_itens`.

---

## 💬 4. Regras de Negócio Essenciais

- 🚫 Não permitir vendas sem cliente associado.
- 🚫 Não finalizar venda sem itens adicionados.
- ⚠️ Verificar estoque disponível antes de confirmar.
- 📉 Realizar baixa imediata do estoque após confirmação.
- 🔒 Preços registrados na venda são sempre do momento da venda.
- 🔐 Após confirmada, venda é marcada como fechada, não permitindo alterações diretas.
- ♻️ Vendas fechadas só podem ser editadas por meio de estorno ou reabertura autenticada (admin).

---

## 📊 5. Estrutura de Dados e Estado das Vendas

**Estados da venda:**

| Estado      | Editar Itens | Excluir Itens | Estoque |
|-------------|--------------|---------------|---------|
| Aberta      | ✅ Sim       | ✅ Sim        | ❌ Não  |
| Fechada     | ❌ Não       | ❌ Não        | ✅ Sim  |
| Estornada   | ❌ Não       | ❌ Não        | 🔄 Reverte |

### Estrutura SQL (simplificada)

```sql
clientes (
  id TEXT PRIMARY KEY,
  nome TEXT, telefone TEXT, cpf TEXT, ...
)

vendas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  data_venda TEXT,
  cliente_id TEXT,
  total REAL,
  desconto REAL,
  forma_pagamento TEXT,
  parcelas INTEGER,
  status TEXT DEFAULT 'aberta',
  criado_em TEXT,
  criado_por TEXT,
  alterado_em TEXT,
  alterado_por TEXT,
  FOREIGN KEY (cliente_id) REFERENCES clientes(id)
)

vendas_itens (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  venda_id INTEGER,
  carta_id TEXT,
  qtd INTEGER,
  preco REAL,
  desconto REAL,
  FOREIGN KEY (venda_id) REFERENCES vendas(id)
)

cartas (
  id TEXT PRIMARY KEY,
  nome TEXT, colecao TEXT, numero TEXT,
  qtd INTEGER, preco REAL
)
```

---

## 🚀 6. Funcionalidades Adicionais (Recomendadas)

### 🎟️ Aplicação de Descontos
- Descontos por item ou totais;
- Percentual (%) ou valor fixo.

### 🧾 Geração de Comprovantes
- PDF ou impressão direta com detalhes completos da venda;
- Opção de customização para loja física.

### 🕵️ Histórico Completo (Auditoria)
- Registra ações: criação, alteração, finalização, estorno;
- Campos de auditoria: `criado_em`, `criado_por`, `alterado_em`, `alterado_por`.

### 🔍 Consulta Avançada
- Filtragem por cliente, período, valor total, método de pagamento, status.

### 💳 Controle de Pagamento e Parcelamento
- Suporte completo a pagamento via cartão, dinheiro, PIX, transferência;
- Gestão automática das parcelas com datas e notificações.

### 📦 Sistema de Reservas
- Permite reservar itens do estoque temporariamente;
- Reserva expira após período configurado se venda não for concluída.

### 🔔 Notificações Automáticas
- Alertas ao usuário/admin de estoque baixo;
- Notificações de alterações importantes na venda (finalização, estorno, reabertura).

### 🛠️ Modo Venda Rápida
- Interface simplificada para transações rápidas;
- Reduz etapas para momentos de alta demanda (lançamentos e eventos).

---

## ♻️ 7. Sistema de Estorno e Reabertura

### 🔁 Estorno
- Gera automaticamente uma venda negativa para corrigir erros;
- Devolve produtos ao estoque;
- Registro claro para auditoria;
- Requer autorização do administrador.

### 🔓 Reabertura
- Venda pode ser reaberta para edição (estado volta para aberta);
- Permite correções diretamente;
- Apenas administradores podem realizar reabertura;
- Requer autenticação adicional (senha admin).

---

## 🗃️ 8. Auditoria e Logs

Cada venda e suas ações são monitoradas. Campos importantes:

| Campo         | Descrição                                 | Exemplo            |
|---------------|-------------------------------------------|--------------------|
| criado_em     | Data/hora da criação                      | 2025-04-16 14:10   |
| criado_por    | Usuário que criou a venda                 | admin              |
| alterado_em   | Data/hora da última alteração             | 2025-04-16 14:15   |
| alterado_por  | Usuário que fez a última alteração        | admin              |
| status        | Situação atual (aberta, fechada, estornada)| fechada           |

---

## ✅ **Conclusão**

Esta documentação oferece uma visão completa e detalhada sobre o módulo de vendas do ERP **HoStore**, permitindo uma compreensão ampla das funcionalidades, estrutura e fluxos necessários para desenvolvimento e manutenção do sistema.

✅ **Documentação completa e pronta para implementação.**