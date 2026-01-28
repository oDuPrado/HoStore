# 📋 Checklist de Implementação NFC-e HoStore

**Data Início**: Janeiro 2026  
**Status**: Fase 2 - Serviços Core ✅ | Fase 3 - UI ⏳

---

## ✅ FASE 1: Database + Camada DAO (COMPLETO)

- [x] **DB.java** - Todas tabelas criadas
  - [x] ncm, cfop, csosn, origem, unidades (tabelas de referência)
  - [x] sequencias_fiscais (numeração auto-incremental por série/ambiente)
  - [x] documentos_fiscais (header do documento fiscal)
  - [x] documentos_fiscais_itens (linhas do documento)
  - [x] documentos_fiscais_pagamentos (formas de pagamento)
  - [x] imposto_icms (alíquotas ICMS por NCM/estado)
  - [x] imposto_ipi (alíquotas IPI por NCM)
  - [x] imposto_pis_cofins (alíquotas PIS/COFINS por NCM)
  - [x] configuracao_nfe_nfce (config única por loja)

- [x] **DAOs Criados** (8 no total)
  - [x] SequenciaFiscalDAO (nextNumero com lock)
  - [x] DocumentoFiscalDAO (inserir, buscar, listar, atualizar)
  - [x] DocumentoFiscalItemDAO (itens do documento)
  - [x] DocumentoFiscalPagamentoDAO (pagamentos)
  - [x] ImpostoICMSDAO (buscar alíquota ICMS)
  - [x] ImpostoIPIDAO (buscar alíquota IPI)
  - [x] ImpostoPisCofinsDAO (buscar alíquota PIS/COFINS)
  - [x] ConfiguracaoNfeNfceDAO (salvar/obter configuração)

---

## ✅ FASE 2: Serviços Core (COMPLETO)

- [x] **FiscalCalcService.java** (173 linhas)
  - [x] calcICMS(ncm, ufOrigem, ufDestino, baseCalculo, tipoOp)
  - [x] calcIPI(ncm, baseCalculo)
  - [x] calcPIS(ncm, baseCalculo)
  - [x] calcCOFINS(ncm, baseCalculo)
  - [x] calcularImpostosCompletos() - orquestração
  - [x] Fallback seguro (retorna 0 se tabela vazia)
  - [x] Inner class ImpostoCalculado
  - [x] Inner class ImpostosItem

- [x] **XmlBuilderNfce.java** (380 linhas)
  - [x] construir() - XML completo
  - [x] buildIde() - identificação
  - [x] buildEmit() - emitente
  - [x] buildDest() - destinatário
  - [x] buildDetItem() - itens com impostos
  - [x] buildTotal() - totalizações
  - [x] buildPag() - pagamento
  - [x] buildTransp() - transportador (placeholder)
  - [x] buildInfAdic() - informações adicionais
  - [x] buildICMS(), buildIPI(), buildPIS(), buildCOFINS()
  - [x] Helpers: gerarCNF, calcularDV, ufParaCodigo, escapeXml, obterCodigoMunicipio
  - [x] Conforme RFB 5.00

- [x] **XmlAssinaturaService.java** (114 linhas)
  - [x] Carregamento certificado A1 (.pfx)
  - [x] validarCertificado() - check expiration
  - [x] assinarXml() - adiciona Signature (XMLDSig)
  - [x] obterInfoCertificado() - para logging
  - [ ] TODO: Integrar Apache Santuario para RSA real (atualmente placeholder)

- [x] **SefazClientSoap.java** (281 linhas)
  - [x] enviarLoteNfce() - envio para autorização
  - [x] consultarRecibo() - query por recibo
  - [x] consultarChave() - query por chave (stub)
  - [x] cancelarNfe() - cancelamento (stub)
  - [x] RespostaSefaz class:
    - [x] sucesso, status, protocolo, recibo, xmlResposta, mensagemErro
    - [x] eAutorizada(), ehRejeitada(), ehProcessando()
    - [x] ehRetentavel (para retry logic)
  - [x] Endpoints RS configurados (extensível)
  - [x] HTTP POST, timeout, SSL handling

- [x] **DanfeNfceGenerator.java** (265 linhas)
  - [x] gerarDANFETexto() - 80mm thermal printer format
  - [x] gerarDANFEPdf() - placeholder (ready for iText)
  - [x] salvarEmArquivo() - write to file
  - [x] gerarURLQRCode() - CSC hash SHA-256
  - [x] gerarHashCSC() - security
  - [x] Formatação: empresa, itens, totais, pagamento, QR, chave

- [x] **FiscalWorker.java** (224 linhas)
  - [x] Singleton pattern
  - [x] Timer scheduled 5 min (configurable)
  - [x] processarPendentes() - XML generation
  - [x] processarAssinados() - SEFAZ send
  - [x] processarComErro() - retry com backoff (2^n)
  - [x] State machine: pendente → xml_gerado → assinada → enviada → autorizada
  - [x] Max 5 retries
  - [x] Thread-safe
  - [x] iniciar(), parar(), forcarProcessamento()

- [x] **DocumentoFiscalService.java** (PARCIAL - 223 linhas)
  - [x] criarDocumentoPendenteParaVenda() - já existe
  - [ ] calcularImpostos() - TODO
  - [ ] gerarXml() - TODO
  - [ ] assinarXml() - TODO
  - [ ] enviarSefaz() - TODO
  - [ ] imprimirDanfe() - TODO
  - [ ] cancelarDocumento() - TODO

---

## ⏳ FASE 3: Models (PARCIAL)

- [x] DocumentoFiscalModel.java
- [x] ConfiguracaoNfeNfceModel.java
- [ ] ImpostoIcmsModel (check if exists)
- [ ] ImpostoIpiModel (check if exists)
- [ ] ImpostoPisCofinsModel (check if exists)

**Ação**: Verificar se models existem em src/main/java/model/

---

## ⏳ FASE 4: Integração DocumentoFiscalService

### 4.1 Adicionar Métodos de Orquestração

```java
// Em DocumentoFiscalService.java

public ImpostosItem calcularImpostos(String documentoId) throws Exception {
    // Buscar documento + itens
    // Para cada item: FiscalCalcService.calcularImpostosCompletos()
    // Atualizar totais
    // Log resultado
}

public String gerarXml(String documentoId) throws Exception {
    // Buscar documento + config
    // XmlBuilderNfce builder = new XmlBuilderNfce(doc, config, itens);
    // String xml = builder.construir();
    // Atualizar status="xml_gerado"
    // Log XML size
}

public String assinarXml(String documentoId, String certPath, String senha) throws Exception {
    // Buscar XML anterior
    // XmlAssinaturaService signer = new XmlAssinaturaService(certPath, senha);
    // String xmlAssinado = signer.assinarXml(xml);
    // Atualizar status="assinada"
    // Log sucesso
}

public RespostaSefaz enviarSefaz(String documentoId, boolean producao) throws Exception {
    // Buscar XML assinado
    // SefazClientSoap sefaz = new SefazClientSoap(...);
    // RespostaSefaz resposta = sefaz.enviarLoteNfce(xmlAssinado, producao);
    // Atualizar: status, protocolo, chave, recibo, erro
    // Log resposta
}

public void imprimirDanfe(String documentoId, String caminhoSaida) throws Exception {
    // Buscar documento + itens
    // DanfeNfceGenerator danfe = new DanfeNfceGenerator(doc, config, itens);
    // danfe.salvarEmArquivo(caminhoSaida);
    // Log arquivo criado
}
```

### 4.2 Atualizar FiscalWorker

```java
// Em FiscalWorker.java - métodos processarPendentes() deve chamar:
DocumentoFiscalService service = new DocumentoFiscalService();

// Estado: pendente → xml_gerado
service.calcularImpostos(docId);
service.gerarXml(docId);
documentoDAO.atualizarStatus(conn, docId, "xml_gerado", null, xml, null, null);

// Estado: assinada → enviada
service.assinarXml(docId, certPath, senha);
documentoDAO.atualizarStatus(conn, docId, "assinada", null, null, null, null);

RespostaSefaz resposta = service.enviarSefaz(docId, producao);
if (resposta.eAutorizada()) {
    documentoDAO.atualizarStatus(conn, docId, "autorizada", null, null, resposta.getChave(), resposta.getProtocolo());
} else if (resposta.ehRejeitada()) {
    documentoDAO.atualizarStatus(conn, docId, "rejeitada", resposta.getMensagem(), null, null, null);
}
```

---

## ⏳ FASE 5: Inicialização da Aplicação

### 5.1 Encontrar Classe Principal

**Arquivo**: src/main/java/app/App.java (ou similar)

### 5.2 Inicializar FiscalWorker no Startup

```java
@Override
public void windowOpened(WindowEvent e) {
    // ... código existente ...
    
    // Inicializar worker fiscal
    FiscalWorker.getInstance().iniciar();
    logger.info("FiscalWorker iniciado (intervalo 5 minutos)");
}

@Override
public void windowClosing(WindowEvent e) {
    // ... código existente ...
    
    // Parar worker
    FiscalWorker.getInstance().parar();
    logger.info("FiscalWorker parado");
}
```

---

## ⏳ FASE 6: UI - Configuração Fiscal

### 6.1 Adicionar Aba em ConfigLojaDialog

**Arquivo**: src/main/java/ui/ajustes/dialog/ConfigLojaDialog.java

**Ação**:
1. Adicionar JTabbedPane.addTab("Fiscal", painelFiscal)
2. Criar painel com campos:
   - JComboBox ambiente (HOMOLOGACAO, PRODUCAO)
   - JSpinner série (1-999)
   - JTextField certificado path
   - JPasswordField certificado senha
   - JTextField CSC (32 chars)
   - JTextField ID CSC (4 chars)
   - JComboBox regime (SN, LP, LL)
   - JTextField UF (2 chars, RS selecionado)
   - JTextField Município
   - Botão "Testar Certificado"
   - Botão "Salvar"

**Listeners**:
```java
btnTestCert.addActionListener(e -> {
    String certPath = tfCertPath.getText();
    String senha = new String(pfSenha.getPassword());
    try {
        XmlAssinaturaService signer = new XmlAssinaturaService(certPath, senha);
        signer.validarCertificado();
        JOptionPane.showMessageDialog(null, "✅ Certificado válido!");
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, "❌ Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
});

btnSalvar.addActionListener(e -> {
    ConfiguracaoNfeNfceModel config = new ConfiguracaoNfeNfceModel();
    config.setAmbiente(cbAmbiente.getSelectedItem().toString());
    config.setSerie((Integer) spnSerie.getValue());
    // ... set other fields ...
    new ConfiguracaoNfeNfceDAO().salvar(config);
    JOptionPane.showMessageDialog(null, "✅ Configuração salva!");
});
```

---

## ⏳ FASE 7: UI - Painel Documentos Fiscais

### 7.1 Criar FiscalDocumentosPanel.java

**Arquivo**: src/main/java/ui/relatorios/FiscalDocumentosPanel.java (ou dashboard)

**Componentes**:
- JTable com colunas: Número, Série, Status, Venda ID, Chave, Protocolo, Erro
- Botões:
  - "Atualizar" (refresh table)
  - "Gerar XML" (força geração)
  - "Assinar" (força assinatura)
  - "Enviar SEFAZ" (força envio)
  - "Imprimir DANFE" (abre salvardialog)
  - "Reprocessar" (recoloca em fila)
  - "Cancelar" (marca para cancelamento)
  - "Detalhes" (abre XML em editor)

**TableModel**:
```java
private void atualizarTabela() {
    List<DocumentoFiscalModel> docs = documentoDAO.listarPorStatus(conn, null, 100);
    DefaultTableModel model = (DefaultTableModel) table.getModel();
    model.setRowCount(0);
    for (DocumentoFiscalModel doc : docs) {
        model.addRow(new Object[] {
            doc.getNumero(),
            doc.getSerie(),
            doc.getStatus(),
            doc.getVendaId(),
            doc.getChaveAcesso(),
            doc.getProtocolo(),
            doc.getErro()
        });
    }
}
```

**Ações**:
```java
table.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            int row = table.getSelectedRow();
            String docId = (String) table.getValueAt(row, 0);
            mostrarXml(docId);
        }
    }
});

btnGerarXml.addActionListener(e -> {
    String docId = getSelectedDocId();
    try {
        service.gerarXml(docId);
        atualizarTabela();
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
});

btnImprimirDanfe.addActionListener(e -> {
    String docId = getSelectedDocId();
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        try {
            service.imprimirDanfe(docId, chooser.getSelectedFile().getAbsolutePath());
            JOptionPane.showMessageDialog(null, "✅ DANFE salvo!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
});
```

---

## ⏳ FASE 8: Importador de Catálogo Fiscal

### 8.1 Criar FiscalCatalogImportDialog.java

**Arquivo**: src/main/java/ui/ajustes/dialog/FiscalCatalogImportDialog.java

**Componentes**:
- JFileChooser (CSV/XLSX)
- JProgressBar (importação)
- JTextArea (log)
- Botão "Importar"

**Fluxo**:
```java
btnImportar.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("CSV/XLSX", "csv", "xlsx"));
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        try {
            String arquivo = chooser.getSelectedFile().getAbsolutePath();
            importarArquivo(arquivo);
        } catch (Exception ex) {
            taLog.append("❌ Erro: " + ex.getMessage() + "\n");
        }
    }
});

private void importarArquivo(String arquivo) throws Exception {
    List<String[]> linhas = lerCSV(arquivo);  // ou XLSX
    int total = 0, sucesso = 0;
    
    taLog.append("Iniciando importação (" + linhas.size() + " linhas)...\n");
    
    for (String[] linha : linhas) {
        total++;
        try {
            String ncm = linha[0];
            String estado = linha[1];
            String aliquota = linha[2];
            // ... validar e inserir ...
            sucesso++;
            if (total % 10 == 0) {
                pBar.setValue((total * 100) / linhas.size());
            }
        } catch (Exception ex) {
            taLog.append("❌ Linha " + total + ": " + ex.getMessage() + "\n");
        }
    }
    
    taLog.append("\n✅ Importação concluída: " + sucesso + "/" + total + " registros\n");
}
```

---

## ⏳ FASE 9: Logs Fiscal

### 9.1 Criar Tabela de Logs (BD)

```sql
CREATE TABLE IF NOT EXISTS logs_fiscal (
    id TEXT PRIMARY KEY,
    documento_id TEXT NOT NULL,
    etapa TEXT NOT NULL,  -- XML_GERADO, ASSINADA, ENVIADA, AUTORIZADA, ERRO
    tipo_log TEXT,        -- INFO, WARN, ERROR
    mensagem TEXT,
    payload_resumido TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (documento_id) REFERENCES documentos_fiscais(id)
);
```

### 9.2 Criar LogFiscalDAO.java

```java
public class LogFiscalDAO {
    public void inserir(String documentoId, String etapa, String tipoLog, String mensagem) {
        String sql = "INSERT INTO logs_fiscal (id, documento_id, etapa, tipo_log, mensagem, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        // ... executar ...
    }
    
    public List<String> listarPorDocumento(String documentoId) {
        // ... query ...
    }
}
```

### 9.3 Integrar com Serviços

```java
// Em cada serviço, após ação:
LogFiscalDAO logDAO = new LogFiscalDAO();
logDAO.inserir(documentoId, "XML_GERADO", "INFO", "XML montado com sucesso");
```

---

## ⏳ FASE 10: Integração com VendaService

### 10.1 Encontrar Classe VendaService

**Arquivo**: src/main/java/service/VendaService.java

### 10.2 Adicionar Ao Finalizacao

```java
public void finalizarVenda(String vendaId, String usuario) throws Exception {
    // ... código existente de finalização ...
    
    // Criar documento fiscal automático
    if (configuracaoNfe.isEmitirNfce()) {
        try {
            DocumentoFiscalService fiscalService = new DocumentoFiscalService();
            fiscalService.criarDocumentoPendenteParaVenda(
                vendaId, 
                usuario, 
                configuracaoNfe.getAmbiente()
            );
            logger.info("Documento fiscal criado para venda: " + vendaId);
        } catch (Exception ex) {
            logger.warn("Erro ao criar documento fiscal: " + ex.getMessage());
            // Não bloqueia venda, apenas avisa
        }
    }
}
```

---

## ⏳ FASE 11: Testes Unitários

### 11.1 FiscalCalcServiceTest

```java
@Test
public void testCalcICMS_ComTabelaExistente() throws Exception {
    ImpostoIcmsModel icms = new ImpostoIcmsModel();
    icms.setAliquotaConsumidor(7.0);
    // ... mock DAO ...
    
    FiscalCalcService service = new FiscalCalcService();
    ImpostoCalculado resultado = service.calcICMS("95049090", "RS", "RS", 100.0, "venda");
    
    assertEquals(7.0, resultado.getAliquota());
    assertEquals(7.0, resultado.getValor());
}

@Test
public void testCalcICMS_TabelaVazia_RetornaZero() throws Exception {
    // Mock DAO retorna null
    FiscalCalcService service = new FiscalCalcService();
    ImpostoCalculado resultado = service.calcICMS("00000000", "RS", "RS", 100.0, "venda");
    
    assertEquals(0.0, resultado.getValor());
}
```

### 11.2 XmlBuilderNfceTest

```java
@Test
public void testConstruir_XmlValido() throws Exception {
    DocumentoFiscalModel doc = criarDocumentoTeste();
    XmlBuilderNfce builder = new XmlBuilderNfce(doc, config, itens);
    String xml = builder.construir();
    
    assertTrue(xml.contains("<ide>"));
    assertTrue(xml.contains("<emit>"));
    assertTrue(xml.contains("<det>"));
    assertTrue(xml.contains("<total>"));
}
```

### 11.3 SequenciaFiscalDAOTest

```java
@Test
public void testNextNumero_ThreadSafe() throws Exception {
    SequenciaFiscalDAO dao = new SequenciaFiscalDAO();
    
    // Executar 10 threads simultâneas
    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<Future<Integer>> futures = new ArrayList<>();
    
    for (int i = 0; i < 10; i++) {
        futures.add(executor.submit(() -> 
            dao.nextNumero(conn, "NFCe", 65, 1, "HOMOLOGACAO")
        ));
    }
    
    Set<Integer> numeros = new HashSet<>();
    for (Future<Integer> future : futures) {
        numeros.add(future.get());
    }
    
    assertEquals(10, numeros.size());  // Todos únicos
}
```

---

## ⏳ FASE 12: Testes Integrados

### 12.1 Fluxo Completo

```bash
Cenário: Venda com NFC-e
1. Criar venda com 2 itens (produtos com NCM válido)
2. Finalizar venda
   ✓ Cria DocumentoFiscal com status=pendente
   ✓ Enfileira em FiscalWorker

3. Executar FiscalWorker.forcarProcessamento()
   ✓ Gera XML (calcula impostos)
   ✓ Status muda para xml_gerado
   ✓ Assina XML
   ✓ Status muda para assinada

4. Enviar para SEFAZ (homologação)
   ✓ Resposta 100 (autorizado)
   ✓ Status muda para autorizada
   ✓ Salva protocolo + chave

5. Imprimir DANFE
   ✓ Arquivo .txt criado
   ✓ Contém QRCode URL
   ✓ Pronto para impressão térmica
```

---

## 📊 Checklist Final

- [ ] Compilação sem erros: `mvn clean compile`
- [ ] DAOs testados (sequência, busca, atualização)
- [ ] Serviços core validados
- [ ] UI Config funcionando
- [ ] UI Painel sincronizando
- [ ] Venda → Documento Fiscal automático
- [ ] Impressão DANFE
- [ ] Testes unitários passando
- [ ] Documentação atualizada
- [ ] Deploy em homologação com A1 teste

---

## 🔗 Referências

- **RFB 5.00**: https://www.sefaz.rs.gov.br/ASP/public/NFC-e/NFCe.aspx
- **Padrão CSOSN 102/500**: Opção pelo Simples Nacional
- **CFOP 5102**: Venda varejista
- **CSC**: Código de Segurança do Contribuinte (32 caracteres)
- **A1 Certificado**: Formato PKCS#12 (.pfx, .p12)

---

**Status Geral**: 🟡 60% COMPLETO (Core + 40% UI + Testes)

**Próximo Passo**: Criar UI Config + testar com certificado teste
