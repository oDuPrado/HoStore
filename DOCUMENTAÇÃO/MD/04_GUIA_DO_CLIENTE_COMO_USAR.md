# 📘 04 - Guia Definitivo do Cliente (Como usar o HoStore)

Este é o manual definitivo para operação do HoStore após assinatura.  
Ele foi feito para você conseguir:

- treinar equipe
- padronizar rotina
- reduzir erro (estoque/caixa/financeiro)
- operar com previsibilidade

---

## 0) Onboarding pós-assinatura (o que fazer no Dia 1)

### 0.1 Checklist de setup (15–30 min)
- [ ] Fazer login como admin
- [ ] Trocar senha padrão do admin
- [ ] Cadastrar dados da loja (nome, contato, etc.)
- [ ] Criar usuários por função
- [ ] Revisar configurações principais (impressão, backups, etc.)
- [ ] Fazer 1 venda de teste (item qualquer)
- [ ] Fazer 1 devolução parcial de teste
- [ ] Validar se estoque voltou corretamente
- [ ] Conferir onde ficam exportações/comprovantes (PDF, etc.)
- [ ] Fazer 1 backup manual e guardar

### 0.2 Modelo de funções (recomendado)
| Perfil | O que pode | O que NÃO pode |
|---|---|---|
| Vendedor | criar venda, finalizar, consultar cliente/produto | ajustes grandes, estorno sem autorização, mexer em config |
| Estoque | entrada/ajuste com motivo, pedidos de compra | estorno/devolução sem processo |
| Gerente | tudo operacional + auditoria | mexer no banco manualmente |
| Admin | configurações e manutenção | usar no dia a dia (evitar) |

---

## 1) Cadastro mínimo para começar

### 1.1 Clientes
Cadastre cliente quando:
- você quer histórico de compras
- você vai parcelar / contas a receber
- você oferece crédito de loja

Campos recomendados:
- Nome
- Telefone
- Documento (CPF/CNPJ se aplicável)
- Observações (VIP, regra de desconto, etc.)

Boas práticas:
- padronizar nome (sem “Joãozinho do bairro” misturado com “João Silva”)
- evitar duplicidade (sempre buscar antes de criar)

---

### 1.2 Produtos e categorias (TCG)
Produtos comuns em loja TCG:
- cartas (unitárias)
- boosters, decks, ETB, selados
- acessórios (sleeves, playmat, deckbox)
- itens gerais (quando aplicável)

Campos recomendados:
- Nome claro e pesquisável
- Categoria correta
- Preço custo / venda
- Quantidade
- Código de barras (se existir)
- Observações

Boas práticas de nome:
- **[Categoria] Nome - Variante/Condição**
Ex:
- Carta: “Charizard ex - SV - NM”
- Selado: “Booster Box - Scarlet & Violet”
- Acessório: “Sleeve Dragon Shield Matte - Black”

---

## 2) Fluxo de venda (do balcão ao fechamento)

### 2.1 Antes de vender (rotina de balcão)
- [ ] confirmar o item e a condição (cartas)
- [ ] conferir estoque na tela
- [ ] definir regra de preço (sem inventar)
- [ ] confirmar forma de pagamento

### 2.2 Criando a venda
Passos:
1. abrir “Nova Venda”
2. selecionar cliente (ou consumidor padrão)
3. adicionar itens (quantidade correta)
4. revisar carrinho
5. aplicar desconto se permitido
6. escolher forma de pagamento
7. confirmar pagamento
8. finalizar

### 2.3 Regras essenciais
- venda aberta não é venda finalizada
- só finalize quando o pagamento estiver confirmado
- desconto deve seguir regra comercial
- evite editar preço “no susto” sem motivo

---

## 3) Pagamentos (padrões que evitam erro)

### 3.1 Dinheiro
- conferir troco antes de confirmar
- usar desconto apenas se política permitir

### 3.2 PIX
- confirmar recebimento antes de finalizar
- evitar “depois eu pago” (vira inadimplência informal)

### 3.3 Cartão
- atenção às taxas (margem)
- parcelamento deve ser registrado corretamente

---

## 4) Correções pós-venda (sem quebrar o mundo)

### 4.1 Venda ainda não foi finalizada
Ação correta:
- **Cancelar** e refazer

Por quê:
- você evita histórico errado
- evita ajustes desnecessários

### 4.2 Venda foi finalizada e houve erro
Use o **Guia de Decisão** para escolher:
- estorno total
- devolução parcial
- nunca “ajeitar no estoque” para esconder erro

---

## 5) Devoluções, Estornos e Crédito de loja

### 5.1 Política recomendada (exemplo)
- devolução aceita em até X dias
- item lacrado precisa estar lacrado
- carta depende de condição (não aceitar se danificada depois)
- crédito de loja pode ter validade

### 5.2 Crédito de loja
Quando usar:
- devolução onde o cliente prefere crédito
- trocas
- fidelização

Risco:
- crédito sem controle vira dívida oculta

Boa prática:
- revisar crédito semanalmente
- definir política clara

---

## 6) Estoque (entrada, inventário e ajuste)

### 6.1 Entrada de estoque
Use quando:
- recebeu mercadoria
- fez compra de reposição

Boas práticas:
- registrar custo para margem
- registrar fornecedor (quando aplicável)
- conferir quantidade antes de lançar

### 6.2 Inventário (rotina recomendada)
Não espere “o ano acabar”.
Faça inventário por partes:
- semanal: categoria pequena (acessórios)
- mensal: selados
- trimestral: cartas ou itens de maior volume

### 6.3 Ajuste (com motivo)
Use ajuste para:
- quebra/perda
- divergência de inventário
- vencimento

Nunca use ajuste para:
- corrigir venda (use devolução/estorno)

---

## 7) Financeiro (o mínimo para operar com controle)

### 7.1 Contas a pagar
- cadastrar título (valor total, vencimento, fornecedor)
- registrar parcelas se existirem
- dar baixa ao pagar

### 7.2 Contas a receber (parcelas)
- toda venda parcelada precisa gerar parcelas corretamente
- rotina semanal: verificar vencidas

### 7.3 Rotina financeira (recomendada)
Diário:
- conferir total vendido e pagamentos

Semanal:
- contas a pagar nos próximos 7 dias
- parcelas vencidas e a vencer

Mensal:
- fechamento por categoria (margem e giro)

---

## 8) Relatórios (como usar para decidir compra)

### 8.1 Relatórios que importam
- vendas por período
- top produtos
- estoque baixo
- estoque parado (baixo giro)

### 8.2 Regra de ouro de performance
- relatórios em janelas de 30/90 dias
- evitar “desde sempre”

---

## 9) Backup, atualização e suporte (evite catástrofe)

### 9.1 Backup
- manter backup diário
- fazer backup manual antes de atualizar
- guardar cópia externa (drive/pendrive)

### 9.2 Atualização do sistema
Antes de atualizar:
- [ ] fazer backup manual
- [ ] fechar sistema corretamente
- [ ] aplicar atualização
- [ ] abrir e validar 1 venda de teste

### 9.3 Suporte (melhor forma de acionar)
Quando reportar problema, enviar:
- print da tela
- passos para reproduzir
- data/hora aproximada
- o que era esperado vs o que aconteceu

---

## 10) Treinamento rápido da equipe (roteiro pronto)

### Treino 1 (30 min): vendedor
- criar venda
- adicionar item
- aplicar desconto (quando permitido)
- finalizar com dinheiro/PIX
- imprimir/gerar comprovante
- simular erro: cancelar antes de finalizar

### Treino 2 (30 min): correções
- devolução parcial
- estorno total
- crédito de loja (quando usar)

### Treino 3 (30 min): estoque
- entrada de estoque
- ajuste com motivo
- inventário simples

---

✅ Próximo: **[02 - Guia de Decisão](02_GUIA_DE_DECISAO.md)** (para fixar)
