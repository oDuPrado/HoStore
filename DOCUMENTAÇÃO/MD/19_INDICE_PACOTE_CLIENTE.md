# 📦 Pacote de Documentação do Cliente - HoStore

**Produto**: HoStore - ERP para lojas TCG (Pokémon, Yu-Gi-Oh!, Magic, Digimon e afins)  
**Versão do pacote**: 1.0.0  
**Atualização**: Janeiro 2026  
**Status**: ✅ Pronto para operação

---

## Como usar este pacote (ordem recomendada)

1. **[01 - Por que o HoStore existe (comercial)](01_POR_QUE_HOSTORE_EXISTE.md)** (8–12 min)  
   Entenda o valor, o diferencial e como o HoStore paga a conta (margem, controle e previsibilidade).

2. **[04 - Guia definitivo de uso (cliente)](04_GUIA_DO_CLIENTE_COMO_USAR.md)** (40–90 min)  
   Manual completo de operação, rotinas, boas práticas e “como fazer sem quebrar o estoque”.

3. **[02 - Guia de decisão (operacional)](02_GUIA_DE_DECISAO.md)** (25–45 min)  
   Quando usar devolução vs estorno vs cancelamento; comanda vs venda direta; desconto; parcelamento; crédito de loja; ajustes.

4. **[03 - Riscos técnicos e operacionais](03_RISCOS_TECNICOS_E_OPERACAO.md)** (20–35 min)  
   O que dá prejuízo quando usado errado, o que não mexer, e como manter integridade (dados/estoque/financeiro/backup).

---

## O objetivo (sem romantização)

Este pacote existe para:

- Reduzir suporte (menos “me ajuda aqui rapidinho” todo dia)
- Padronizar operação (vendedor A não pode inventar regra diferente do vendedor B)
- Evitar prejuízo (estoque fantasma + caixa incoerente = dor de cabeça)
- Acelerar onboarding (cliente assina hoje e opera amanhã)

---

## Navegação rápida por perfil

### Dono / Gestor
- Comece por **01** e **04** (valor + operação)
- Use **02** para padronizar a equipe
- Leia **03** antes de “mexer no banco”

### Vendedor (balcão)
- Leia **04** (capítulos 1 a 5)
- Use **02** quando surgir dúvida do “o que eu faço agora?”

### Estoquista / Compras
- Leia **04** (capítulos 6 e 8)
- Use **02** (entrada/ajuste/pedido de compra)
- Leia **03** (riscos de inventário)

### TI / Dev (manutenção)
- Leia **03** inteiro
- Depois consulte a documentação técnica do repositório (README/Arquitetura/etc.)

---

## Checklist de “assinou, começa a operar” (10 minutos)

- [ ] Trocar senha do usuário admin
- [ ] Criar usuários por função (vendedor / gerente / estoque)
- [ ] Conferir dados da loja e configurações principais
- [ ] Realizar 1 venda de teste + gerar comprovante
- [ ] Fazer 1 devolução de teste (parcial) e validar estoque
- [ ] Garantir que **backup** esteja habilitado e testado

---

## Arquivos deste pacote

- **INDICE_PACOTE_CLIENTE.md** (este arquivo)
- **01_POR_QUE_HOSTORE_EXISTE.md**
- **02_GUIA_DE_DECISAO.md**
- **03_RISCOS_TECNICOS_E_OPERACAO.md**
- **04_GUIA_DO_CLIENTE_COMO_USAR.md**

---

## Glossário rápido (termos que você vai ver)

- **Venda aberta**: rascunho (itens podem mudar, ainda não deve impactar estoque/caixa).
- **Venda fechada**: finalizada (impacta estoque e caixa conforme pagamento).
- **Estorno**: reversão total (desfaz venda, reverte impactos).
- **Devolução**: reversão parcial (cliente devolveu itens específicos).
- **Cancelamento**: desfaz antes de finalizar (uso típico em vendas abertas).
- **Ajuste de estoque**: correção manual controlada (sempre com motivo).
- **Crédito de loja**: saldo do cliente na loja (vira “dívida” da loja com o cliente).

---

✅ **Próximo passo**: vá para **01_POR_QUE_HOSTORE_EXISTE.md**
