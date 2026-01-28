# 🚀 PRÓXIMAS AÇÕES - IMPLEMENTAÇÃO NFC-e

## Fase de Teste (UAT)

### 1️⃣ Validação em HOMOLOGACAO

**Objetivo**: Testar fluxo completo em ambiente de homologação SEFAZ

**Passos**:
1. Obter certificado A1 teste (SEFAZ fornece)
2. Configurar em ConfigLojaDialog:
   - CNPJ loja
   - IE (ICMS)
   - UF: RS (ou seu estado)
   - Upload certificado
   - Botão "🔐 Testar Certificado" - deve retornar ✅
3. Escolher "HOMOLOGACAO"
4. Salvar

**Teste 1 - Automático via Worker**:
1. Fazer uma venda completa (min 1 item)
2. Finalizar (fechar mesa/comanda)
3. Abrir FiscalDocumentosPanel
4. Esperar 5 minutos (intervalo do FiscalWorker)
5. Verificar status: "pendente" → "xml_gerado" → "assinada" → "enviada" → "autorizada"
6. Botão "Detalhes" mostra XML gerado

**Teste 2 - Manual via UI**:
1. FiscalDocumentosPanel
2. Selecionar doc com status "pendente"
3. Botão "Gerar XML" - deve gerar imediatamente
4. Botão "Imprimir DANFE" - salva arquivo .txt
5. Acompanhar logs em cada etapa

**Teste 3 - Importar Tabelas de Impostos**:
1. Menu Ajustes → FiscalCatalogImportDialog
2. Preparar CSV com formato:
   ```
   # ICMS
   12345678901234;RS;18.0;18.0
   # IPI
   12345678901234;IPI;5.0
   # PIS/COFINS
   12345678901234;PIS;7.65;COFINS;7.60
   ```
3. Importar
4. Verificar BD se inseriu (usar SQLite client)

### 2️⃣ Monitoramento em Produção

Após validação em HOMOLOGACAO:

**Passos antes de PRODUCAO**:
1. Obter certificado A1 PRODUCAO
2. Atualizar em ConfigLojaDialog (PRODUCAO)
3. Re-testar certificado
4. Fazer venda teste
5. Acompanhar status completo até "autorizada"
6. Imprimir DANFE e validar QRcode

**Arquivos para backup/auditoria**:
- `data/hostore.db` - BD com histórico
- `data/logs/` - Logs da aplicação
- Tabela `logs_fiscal` - Auditoria de cada operação

---

## 📊 Fase de Relatórios

### Dashboards Recomendados

**Dashboard 1 - Status NFC-e**:
- Total de NFC-e por status (pendente, autorizada, rejeitada, erro)
- Timeline: últimos 7 dias
- Tempo médio de processamento

**Dashboard 2 - Erros e Reprocessamento**:
- Top 10 erros por tipo
- Taxa de sucesso (%)
- Documentos aguardando reprocessamento

**Dashboard 3 - Performance**:
- Tempo médio: calc → XML → assinar → enviar
- Pico de envios por hora
- Sucesso vs falha por hora do dia

---

## 🔧 Manutenção do Sistema

### Limpeza Periódica

**Script de limpeza de logs antigos** (executar mensalmente):
```java
LogFiscalDAO dao = new LogFiscalDAO();
dao.limparLogsAntigos(90);  // Remove logs > 90 dias
```

**Backup do BD**:
- Fazer backup diário de `data/hostore.db`
- Manter 3 meses de histórico

**Otimização de índices**:
```sql
REINDEX;
VACUUM;
```

### Monitoramento Contínuo

**Checklist Diário**:
- [ ] Verificar erros em logs_fiscal
- [ ] Confirmar processamento de docs (status "autorizada")
- [ ] Validar DANFE impressas

**Checklist Semanal**:
- [ ] Gerar relatório de erros
- [ ] Analisar performance (tempo médio)
- [ ] Revisar logs de certificado

**Checklist Mensal**:
- [ ] Limpar logs > 90 dias
- [ ] Backup e teste de restauração
- [ ] Atualizar tabelas de impostos (se houver novos)

---

## 🛠️ Troubleshooting

### Problema: "Certificado expirado"

**Solução**:
1. Obter novo certificado A1
2. ConfigLojaDialog → Fazer upload novo
3. Testar com botão "🔐 Testar Certificado"
4. Reprocessar docs pendentes

### Problema: "Erro ao conectar SEFAZ"

**Solução**:
1. Verificar internet
2. Verificar se SEFAZ está online (status.sefaz.rs.gov.br)
3. Verificar proxy (se ambiente corporativo)
4. Documentos ficarão com status "enviada" e reprocessarão em 5 min

### Problema: "Item com NCM inválido"

**Solução**:
1. Atualizar tabela NCM (FiscalCatalogImportDialog)
2. Reprocessar documento com botão "Forçar Processamento"
3. Verificar log em "Detalhes"

### Problema: "DANFE não gerou"

**Solução**:
1. Verificar permissões de escrita em `data/export/`
2. Tentar novamente com botão "Imprimir DANFE"
3. Verificar log em "Detalhes"

---

## 📈 Roadmap Futuro

### v1.1 - Melhorias Propostas

- [ ] **Integração com impressora térmica**
  - Print automático de DANFE
  - Fila de impressão

- [ ] **Envio de NFC-e por Email**
  - Enviar DANFE para cliente após autorização
  - Template customizável

- [ ] **Consulta de situação**
  - Botão "Consultar SEFAZ" para verificar status
  - Atualizar documentos já enviados

- [ ] **Integração com sistema de nota fiscal anterior**
  - Migração de histórico
  - Compatibilidade com emissão de NF-e modelo 55

- [ ] **Relatórios avançados**
  - Gráficos de performance
  - Análise de CFOP mais utilizados
  - Matriz de CST por período

- [ ] **Integração com MDFE** (Manifesto Eletrônico)
  - Complemento de transporte
  - Rastreamento de entregas

### v2.0 - Fiscalização

- [ ] **Integração com CNT (Contingência)**
  - Emissão em modo contingência
  - Retransmissão automática

- [ ] **SPED ECD/ECF**
  - Exportação de dados para SPED
  - Gênero de exportação automática

- [ ] **Integração com ERP**
  - Sincronização com contabilidade
  - API para terceiros

---

## 📞 Suporte e Contato

### Dúvidas Técnicas
- Revisar documentação em `DOCUMENTAÇÃO/MD/INDICE_NFCE.md`
- Consultar logs em `logs_fiscal` para rastrear problemas
- Testar em HOMOLOGACAO primeiro

### Problemas com SEFAZ
- Site: https://www.sefaz.rs.gov.br (ou seu estado)
- Manual: Baixar último manual de NFC-e (RFB 5.00)
- Teste: Usar ambiente de HOMOLOGACAO antes de PRODUCAO

### Certificado A1
- Fornecedor autorizado: Certisign, eTokens, etc
- Validade: tipicamente 1 ano
- Renovação: com 3 meses de antecedência

---

## ✅ Checklist Pré-Produção

- [ ] Testes em HOMOLOGACAO completados (7 dias)
- [ ] Documentação lida e entendida
- [ ] Certificado A1 PRODUCAO obtido e testado
- [ ] Tabelas de impostos atualizadas
- [ ] Backup do BD funcionando
- [ ] Monitoramento configurado
- [ ] Equipe treinada no uso
- [ ] Plano de rollback em caso de erro
- [ ] SLA de suporte definido

---

**Versão**: 1.0.0  
**Status**: Pronto para Produção ✅  
**Última Atualização**: [DATA]
