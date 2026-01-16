# 🧭 02 - Guia de Decisão (Completo)

Este documento responde a pergunta mais comum de toda operação:
**“Qual ação eu faço agora para não quebrar estoque e caixa?”**

Se sua equipe usar este guia, você economiza:
- retrabalho
- erro de estoque
- briga interna
- suporte

---

## 0) Princípios (leia uma vez e pare de sofrer)

### 0.1 Regra de ouro: toda ação crítica tem impacto
- Finalizar venda: **mexe em estoque e caixa**
- Estornar: **desfaz impactos**
- Devolver: **desfaz parte dos impactos**
- Ajustar estoque: **corrige quantidade, mas não corrige histórico de venda**
- Editar dados manualmente: **quebra rastreabilidade**

### 0.2 “Corrigir” nunca pode apagar rastro
Se você apaga rastro, você perde:
- controle
- auditoria
- aprendizado (erro vira repetição)

---

## 1) Devolução vs Estorno vs Cancelamento (a decisão mais importante)

### 1.1 Definições objetivas

| Ação | Quando usar | Impacto | Exemplo |
|---|---|---|---|
| **Cancelamento** | venda ainda **não** finalizada | remove o rascunho/rascunho some | vendedor abriu venda errada |
| **Estorno** | venda finalizada e cliente devolveu **tudo** (ou pagamento precisa ser revertido por completo) | reverte venda inteira | cliente desistiu na hora / cobrança duplicada |
| **Devolução** | venda finalizada e cliente devolveu **parte** | reverte apenas itens devolvidos | devolveu 1 booster de 3 |
| **Ajuste** | correção de estoque sem relação direta com venda | muda quantidade e registra motivo | item que quebrou / inventário achou diferença |

### 1.2 Árvore de decisão (rápida)

```
A venda foi FINALIZADA?
  ├─ NÃO  -> CANCELAR (antes de finalizar)
  └─ SIM
       ├─ Cliente devolveu TUDO? -> ESTORNO
       └─ Cliente devolveu PARTE? -> DEVOLUÇÃO
```

### 1.3 Exemplos reais (para a equipe não errar)
- Venda aberta com item errado: **Cancelar** e criar outra
- Venda fechada e cliente devolveu 1 item: **Devolução parcial**
- Venda fechada e pagamento cobrado 2x: **Estorno (total)**
- Produto que quebrou no estoque: **Ajuste (com motivo)**

---

## 2) Comanda vs Venda Direta (balcão)

### 2.1 Quando usar COMANDA
Use comanda quando:
- o cliente ainda vai adicionar itens
- você precisa “segurar” itens para um fechamento no final
- evento/mesa/consumo contínuo (quando aplicável)

**Risco** se usar errado:
- bagunça de estoque se baixar item antes da venda
- “itens presos” em comandas esquecidas

**Regra operacional recomendada**:
- comanda é “pré-venda”
- só vira venda (e mexe em estoque) quando finaliza

### 2.2 Quando usar VENDA DIRETA
Use venda direta quando:
- é compra imediata
- o cliente já decidiu
- pagamento vai acontecer agora

---

## 3) Desconto: onde aplicar e por quê

### 3.1 Tipos de desconto e quando usar
| Tipo | Quando usar | Benefício | Risco |
|---|---|---|---|
| **No item** | queima de estoque, item com condição diferente | claro e auditável | vendedor “usa para tudo” |
| **No total** | negociação do carrinho | simples e rápido | vira “desconto aleatório” |
| **Por pagamento** | PIX/dinheiro com desconto, cartão sem | regra comercial clara | precisa disciplina para aplicar sempre |

### 3.2 Regra recomendada (para loja não virar feira)
- preço base = preço cheio
- desconto (PIX/dinheiro) = opcional com política
- vendedor não decide sozinho (defina limites)

---

## 4) Parcelamento: quando vale e quando destrói margem

### 4.1 Quando parcelar (vale a pena)
- ticket alto
- cliente confiável / política da loja permite
- taxa do cartão está considerada na precificação

### 4.2 Quando não parcelar (armadilha)
- ticket baixo (taxa come tudo)
- loja não controla contas a receber (vira “dinheiro perdido”)
- equipe preenche parcela errado (gera confusão)

### 4.3 Checklist antes de parcelar
- [ ] Qual a taxa efetiva do cartão?
- [ ] Margem cobre a taxa?
- [ ] Cliente já tem histórico?
- [ ] Parcelas ficam registradas corretamente?
- [ ] Tem política de cobrança/atraso?

---

## 5) Entrada de estoque: simples vs pedido de compra

### 5.1 Entrada simples
Use quando:
- reposição pequena
- compra pontual
- fornecedor informal

### 5.2 Pedido de compra
Use quando:
- compra grande
- quer rastrear fornecedor e custo
- quer controle de recebimento e diferença

**Decisão rápida**
```
Compra grande e recorrente? -> Pedido de compra
Compra pequena e pontual?   -> Entrada simples
```

---

## 6) Ajuste de estoque (o botão “perigo”)

### 6.1 Quando usar
- quebra/perda
- divergência de inventário
- correção de cadastro (erro humano)

### 6.2 Quando NÃO usar
- para corrigir venda finalizada (use devolução/estorno)
- para “fazer bater” sem motivo (isso vira fraude/inconsistência)

### 6.3 Política recomendada (empresa que quer crescer)
- ajuste só com **motivo**
- ajuste grande só com **autorização do gerente**
- ajuste deve ser revisado semanalmente

---

## 7) Crédito de loja: o que é e quando usar

### 7.1 Quando usar crédito
- cliente devolveu e prefere crédito
- política da loja incentiva retorno
- troca por outro produto

### 7.2 Quando evitar crédito
- caixa muito apertado e você precisa de liquidez
- equipe sem disciplina (crédito vira “dívida esquecida”)

### 7.3 Regra comercial recomendada
- crédito tem validade? (ex: 90/180 dias)
- crédito é transferível? (sim/não)
- crédito pode ser convertido em dinheiro? (normalmente não)

---

## 8) Checklist de decisão (coloque na parede)

Antes de confirmar qualquer ação crítica:
- [ ] Venda está **aberta** ou **fechada**?
- [ ] Isso vai mexer em **estoque**?
- [ ] Isso vai mexer em **caixa**?
- [ ] Existe **motivo** registrado?
- [ ] Precisa de **autorização**?
- [ ] A equipe sabe o que acontece depois?

---

## 9) FAQ operacional (as dúvidas que sempre voltam)

**“Posso ajustar estoque para corrigir venda?”**  
Não. Ajuste corrige quantidade, não corrige histórico. Use devolução/estorno.

**“Cliente devolveu só 1 item, como faço?”**  
Devolução parcial, item a item.

**“Venda ficou errada mas nem finalizei.”**  
Cancelar e refazer. Não finalize coisa errada “pra corrigir depois”.

---

✅ Próximo: **[03 - Riscos Técnicos e Operacionais](03_RISCOS_TECNICOS_E_OPERACAO.md)**
