# ⚠️ 03 - Riscos Técnicos e Operacionais (com mitigação)

Este documento existe para prevenir o que mais destrói ERP na prática:
**uso errado + alteração manual + falta de backup**.

Se você ler e aplicar, você evita:
- estoque inconsistente
- financeiro incoerente
- perda de dados
- “sistema não presta” (quando na real foi operação sem processo)

---

## 1) Riscos operacionais (usuário)

### 1.1 Finalizar venda sem revisar itens
**Risco**: estoque baixa errado e depois ninguém sabe corrigir sem bagunçar.  
**Mitigação**:
- obrigar revisão do carrinho
- limitar edição de preço por perfil (se aplicável)
- usar o Guia de Decisão para correções

**Checklist de finalização (mínimo)**
- [ ] Itens e quantidades ok
- [ ] Preço/desconto ok
- [ ] Pagamento confirmado (PIX/dinheiro/cartão)
- [ ] Cliente correto (ou consumidor padrão)
- [ ] Confirmar e gerar comprovante

---

### 1.2 Devolução/estorno sem motivo (fraude e auditoria impossível)
**Risco**: prejuízo sem rastreio, disputa com cliente, “sumiu dinheiro”.  
**Mitigação**:
- motivo obrigatório
- revisão semanal por gerente
- log/registro de usuário da ação

---

### 1.3 Ajuste de estoque como “borracheiro” do sistema
**Risco**: você “conserta” hoje e destrói seu histórico amanhã.  
**Mitigação**:
- ajuste só com motivo
- ajuste grande com autorização
- relatório semanal de ajustes

---

## 2) Riscos de dados (integridade, backup, perda)

### 2.1 Sem backup = sem negócio
**Risco**: perder histórico e recomeçar do zero.  
**Mitigação**:
- backup automático diário
- backup manual antes de mudanças grandes
- cópia externa (drive/pendrive/nuvem)

**Teste de backup (uma vez por mês)**
- [ ] localizar backup mais recente
- [ ] restaurar em ambiente de teste
- [ ] abrir sistema e validar dados

---

### 2.2 Mexer no banco na mão
**Risco**: quebra vínculo de tabelas, relatórios, histórico e consistência.  
**Mitigação**:
- alteração manual só por TI
- sempre com backup antes e depois
- registrar o que foi feito (data + motivo + responsável)

---

## 3) Riscos de segurança (usuário e permissões)

### 3.1 Credenciais padrão
**Risco**: acesso total não autorizado.  
**Mitigação**:
- trocar senha do admin no primeiro acesso
- criar usuários por função
- aplicar política de senha (mínimo 12-16 caracteres)

### 3.2 Estação compartilhada sem sessão
**Risco**: ações em nome do usuário errado.  
**Mitigação**:
- login individual
- bloquear estação quando sair
- encerrar sessão no fim do turno

---

## 4) Riscos de performance (quando o volume cresce)

### 4.1 Relatórios com períodos gigantes (“desde 2018”)
**Risco**: consultas ficam lentas e travam UI.  
**Mitigação**:
- usar janelas de 30/90 dias
- separar análises por período
- arquivar dados antigos (quando a operação permitir)

### 4.2 Crescimento sem disciplina de cadastro
**Risco**: duplicidade de produtos, nomes inconsistentes, busca ruim.  
**Mitigação**:
- padrão de nomenclatura
- uso de código de barras quando disponível
- treinamento de cadastro para equipe

---

## 5) Riscos de fiscal (quando aplicável)

> Fiscal é onde a realidade dá tapa. Se você vai emitir documentos fiscais, a disciplina tem que ser maior.

Riscos comuns:
- NCM/CFOP/CSOSN incorretos
- ambiente de homologação vs produção confuso
- certificado vencido ou mal configurado

Mitigação:
- validação de campos obrigatórios
- checklist de configuração fiscal
- separar usuário “fiscal” com permissões

---

## 6) “Não mexa nisso sem saber” (regras de ouro)

- Não apague registros no banco para “sumir problema”
- Não use ajuste para corrigir venda
- Não finalize venda sem confirmar pagamento
- Não faça estorno/devolução sem motivo
- Não opere sem backup

---

## 7) Checklist de auditoria semanal (15 minutos)

- [ ] Ajustes de estoque: quantidade e motivo fazem sentido?
- [ ] Estornos: foram autorizados?
- [ ] Devoluções: batem com itens devolvidos?
- [ ] Contas a receber: parcelas vencidas existem?
- [ ] Backup: existe backup recente e acessível?

---

✅ Próximo: **[04 - Guia definitivo de uso](04_GUIA_DO_CLIENTE_COMO_USAR.md)**
