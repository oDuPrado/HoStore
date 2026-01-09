package util;

import dao.ConfigLojaDAO;
import dao.ProdutoDAO;
import model.ConfigLojaModel;
import model.VendaItemModel;
import model.VendaModel; // @FIX: import para VendaModel
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import java.awt.*;
import java.text.NumberFormat; // @FIX: import para NumberFormat
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import util.CupomFiscalFormatter;
import javax.swing.table.TableModel;

// Imports necessários do PDFBox
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

// Imports para impressão
import javax.print.attribute.standard.MediaSizeName;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import model.PrinterConfigModel; // caso use um model
import dao.PrinterConfigDAO; // caso use persistência

/**
 * Responsável por gerar comprovantes em tela e PDF.
 * (Layout preservado, com a implementação definitiva para salvar em
 * data/export)
 */
public class PDFGenerator {

    // Pasta onde os PDFs serão exportados (relativo ao diretório raiz da aplicação)
    private static final String EXPORT_DIR = "data/export";
    // Pasta onde ficam as fontes (é preciso ter Roboto-Regular.ttf dentro de
    // data/fonts)
    private static final String FONT_DIR = "data/fonts";
    // Largura do cupom em caracteres (para formatação monoespaçada)
    private static final int RECEIPT_WIDTH = 44;

    /*
     * ───────────────────── Comprovante Fiscal (Cupom Eletrônico)
     * ─────────────────────
     */
    public static class ComprovanteDialog extends JDialog {

        public ComprovanteDialog(
                Dialog owner,
                int vendaId,
                List<VendaItemModel> itens,
                String formaFinal,
                int parcelas,
                double juros,
                String periodo,
                TableModel pagamentos) {

            super(owner, "Comprovante Fiscal - Venda #" + vendaId, true);
            setSize(480, 650);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(8, 8));
            ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

            /* Área de texto do cupom */
            JTextArea ta = new JTextArea();
            ta.setEditable(false);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            StringBuilder sb = new StringBuilder();

            /* =========== 1) Cabeçalho Fiscal =========== */
            ConfigLojaModel config = null;
            try {
                config = new ConfigLojaDAO().buscar();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Erro ao carregar dados da loja:\n" + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }

            String nomeLoja = (config != null && config.getNome() != null) ? config.getNome() : "Loja Padrão";
            String cnpjLoja = (config != null && config.getCnpj() != null) ? config.getCnpj() : "00.000.000/0000-00";
            String telefoneLoja = (config != null && config.getTelefone() != null) ? config.getTelefone() : "";
            String textoRodape = (config != null && config.getTextoRodapeNota() != null)
                    ? config.getTextoRodapeNota()
                    : "Obrigado pela preferência!";
            String modeloNota = (config != null && config.getModeloNota() != null) ? config.getModeloNota() : "65";
            String serieNota = (config != null && config.getSerieNota() != null) ? config.getSerieNota() : "001";

            int numeroInicial = (config != null) ? config.getNumeroInicialNota() : 1;
            int numeroCupom = numeroInicial + vendaId;

            sb.append(center(nomeLoja.toUpperCase())).append('\n');
            if (!cnpjLoja.isEmpty())
                sb.append(center("CNPJ: " + cnpjLoja)).append('\n');
            if (!telefoneLoja.isEmpty())
                sb.append(center("Tel: " + telefoneLoja)).append('\n');
            sb.append('\n');
            sb.append(center("CUPOM FISCAL ELETRÔNICO")).append('\n');
            sb.append(center("SÉRIE: " + serieNota + "    Nº: " + numeroCupom)).append('\n');
            sb.append(center("MODELO: " + modeloNota)).append('\n');
            sb.append(center(formatDateTime(LocalDateTime.now()))).append('\n');
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append('\n');

            /* =========== 2) Itens da Venda =========== */
            // Cabeçalho da tabela de itens
            sb.append(String.format("%-20s %4s %7s %6s %7s\n",
                    padRight("PRODUTO", 20),
                    padLeft("QTD", 4),
                    padLeft("UN", 7),
                    padLeft("VL.UNI", 6),
                    padLeft("TOTAL", 7)));
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");

            // Acumuladores
            double totBruto = 0.0;
            double totDesconto = 0.0;
            final double[] totLiquidoFinal = { 0.0 }; // wrapper para alterações em lambdas

            ProdutoDAO pdao = new ProdutoDAO();
            for (VendaItemModel it : itens) {
                String nomeProduto;
                try {
                    nomeProduto = pdao.findById(it.getProdutoId()).getNome();
                } catch (Exception e) {
                    nomeProduto = it.getProdutoId(); // fallback: mostra o ID se falhar
                }
                if (nomeProduto.length() > 20) {
                    nomeProduto = nomeProduto.substring(0, 20);
                }

                int qtd = it.getQtd();
                double unit = it.getPreco();
                double descV = unit * qtd * it.getDesconto() / 100.0;
                double linhaVl = unit * qtd - descV;

                String unidadeFormatada = String.format(Locale.US, "%.3f", qtd * 1.0).replace('.', ',');

                sb.append(String.format("%-20s %4s %7s %6s %7s\n",
                        padRight(nomeProduto, 20),
                        padLeft(String.valueOf(qtd), 4),
                        padLeft(unidadeFormatada, 7),
                        padLeft(cf.format(unit), 6),
                        padLeft(cf.format(linhaVl), 7)));

                totBruto += unit * qtd;
                totDesconto += descV;
                totLiquidoFinal[0] += linhaVl;
            }
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");

            /* =========== 3) Totais =========== */
            sb.append(padRight("Total Bruto:", receiptLabelWidth()))
                    .append(padLeft(cf.format(totBruto), 8)).append('\n');
            sb.append(padRight("Descontos:", receiptLabelWidth()))
                    .append(padLeft(cf.format(totDesconto), 8)).append('\n');
            sb.append(padRight("Total Líquido:", receiptLabelWidth()))
                    .append(padLeft(cf.format(totLiquidoFinal[0]), 8)).append('\n');
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append('\n');

            /* =========== 4) Pagamentos =========== */
            sb.append(center("PAGAMENTOS")).append('\n');
            for (int i = 0; i < pagamentos.getRowCount(); i++) {
                String tipoPagamento = String.valueOf(pagamentos.getValueAt(i, 0));
                double valorPagto = ((Number) pagamentos.getValueAt(i, 1)).doubleValue();
                sb.append(String.format("%-22s %20s%n",
                        padRight(tipoPagamento, 22),
                        padLeft(cf.format(valorPagto), 20)));
            }
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append('\n');

            /* =========== 5) Parcelamento Cartão (se houver) =========== */
            if (parcelas > 1) {
                double valorLiquido = totLiquidoFinal[0];
                sb.append("Parcelamento Cartão: ").append(parcelas).append("x de ")
                        .append(cf.format((valorLiquido / parcelas) * (1 + juros / 100.0))).append("\n");
                sb.append("Juros: ").append(String.format(Locale.US, "%.2f", juros)).append("%\n");
                sb.append("Primeira Parcela em: ")
                        .append(formatDate(LocalDateTime.now().plusDays(parsePeriodo(periodo)))).append("\n");
                sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");
            }

            /* =========== 6) Informações Fiscais Complementares =========== */
            sb.append(center("CHAVE DE ACESSO NFC-e")).append('\n');
            sb.append(center("0000 0000 0000 0000 0000 0000 0000 0000 0000")).append('\n');
            sb.append(center("DANFE NFC-e em http://www.sefa.gov.br")).append('\n');
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append('\n');

            /* =========== 7) Rodapé =========== */
            sb.append(center(textoRodape)).append('\n');
            sb.append(center("SEM VALOR FISCAL")).append('\n');
            sb.append('\n');
            sb.append(center("Obrigado pela preferência!")).append('\n');
            sb.append(center("Volte sempre!")).append('\n');

            /* ==== Monta a UI ==== */
            ta.setText(sb.toString()); // preenche o cupom
            add(new JScrollPane(ta), BorderLayout.CENTER); // exibe com scroll
        }

        /* ────────────── Métodos auxiliares de formatação ────────────── */

        private JButton botao(String t) {
            JButton b = new JButton(t);
            b.setBackground(new Color(60, 63, 65));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            return b;
        }

        private String center(String text) {
            return center(text, RECEIPT_WIDTH);
        }

        private String center(String text, int width) {
            if (text == null)
                text = "";
            if (text.length() >= width)
                return text.substring(0, width);
            int pad = (width - text.length()) / 2;
            return repeatChar(' ', pad) + text;
        }

        private String repeatChar(char c, int count) {
            return String.valueOf(c).repeat(Math.max(0, count));
        }

        private String padRight(String text, int len) {
            if (text == null)
                text = "";
            if (text.length() >= len)
                return text.substring(0, len);
            return text + " ".repeat(len - text.length());
        }

        private String padLeft(String text, int len) {
            if (text == null)
                text = "";
            if (text.length() >= len)
                return text.substring(0, len);
            return " ".repeat(len - text.length()) + text;
        }

        private String formatDateTime(LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }

        private String formatDate(LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        private int parsePeriodo(String periodo) {
            try {
                return Integer.parseInt(periodo.replaceAll("\\D+", ""));
            } catch (Exception e) {
                return 0;
            }
        }

        private int receiptLabelWidth() {
            return RECEIPT_WIDTH - 8; // deixa espaço para o valor monetário (8 colunas)
        }
    }

    /*
     * ─────────────────── Sobrecarga para não quebrar código legado
     * ───────────────────
     * Se alguém chamar apenas com (venda, itens), redireciona para o método
     * completo,
     * gerando um PDF em data/export com nome e timestamp automático.
     */
    public static void gerarComprovanteVenda(VendaModel venda, List<VendaItemModel> itens) {
        try {
            // monta timestamp para o nome do arquivo
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String nomeArquivo = "comprovante_" + venda.getId() + "_" + timestamp + ".pdf";

            // garante que a pasta existe
            java.io.File pasta = new java.io.File(EXPORT_DIR);
            if (!pasta.exists())
                pasta.mkdirs();

            // caminho completo do PDF
            java.io.File destino = new java.io.File(pasta, nomeArquivo);

            // chama a versão completa
            gerarComprovanteVenda(venda, itens, destino.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ─────────────────── Método principal que gera o PDF no caminho dado
     * ───────────────────
     * 
     * @param venda           modelo da venda (contém ID, lista de itens, total,
     *                        parcelas, etc.)
     * @param itens           lista de itens vendidos
     * @param caminhoCompleto caminho absoluto (ou relativo) onde salvar o PDF
     *                        (inclui nome do arquivo)
     * @throws Exception se falhar em criar o PDF ou salvar
     */
    public static void gerarComprovanteVenda(VendaModel venda, List<VendaItemModel> itens, String caminhoCompleto)
            throws Exception {
        // 1) Reconstrói o texto do cupom (mesmo layout do ComprovanteDialog).
        // Reutilizamos o dialog em memória, passando juros=0.0 e periodo="".
        ComprovanteDialog dialog = new ComprovanteDialog(
                null,
                venda.getId(), // já é int
                itens,
                venda.getFormaPagamento(), // string que vem do VendaModel
                venda.getParcelas(), // int que vem do VendaModel
                0.0, // juros fixo 0.0, pois não temos getJuros()
                "", // período vazio, pois não temos getPeriodo()
                new javax.swing.table.DefaultTableModel(
                        new Object[][] {
                                { venda.getFormaPagamento(), venda.getTotalLiquido() }
                        },
                        new String[] { "Forma", "Valor" }));
        // Recupera o JTextArea que está dentro do JScrollPane do dialog
        JScrollPane scroll = (JScrollPane) dialog.getContentPane().getComponent(0);
        JTextArea ta = (JTextArea) scroll.getViewport().getView();
        String texto = ta.getText();

        // 2) Cria documento PDF
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        // 3) Carrega fonte monoespaçada (Roboto-Regular.ttf deve estar em data/fonts)
        PDType0Font font = PDType0Font.load(doc, new java.io.File(FONT_DIR, "Roboto-Regular.ttf"));

        // 4) Escreve texto no PDF
        PDPageContentStream content = new PDPageContentStream(doc, page);
        content.setFont(font, 10);
        content.beginText();
        content.setLeading(14f); // espaço entre linhas
        // margens: 40pt da esquerda, 50pt do topo
        content.newLineAtOffset(40, page.getMediaBox().getHeight() - 50);

        for (String line : texto.split("\n")) {
            content.showText(line);
            content.newLine();
        }
        content.endText();
        content.close();

        // 5) Salva o PDF no caminho especificado
        doc.save(caminhoCompleto);
        doc.close();
    }

    /**
     * Imprime o cupom diretamente na impressora térmica configurada.
     * 
     * @param venda VendaModel com formaPagamento, parcelas, etc.
     * @param itens Itens da venda
     */
    public static void imprimirCupomFiscal(VendaModel venda, List<VendaItemModel> itens) {
        try {
            // 1) Gera o texto do cupom reutilizando o ComprovanteDialog
            ComprovanteDialog dialog = new ComprovanteDialog(
                    null,
                    venda.getId(),
                    itens,
                    venda.getFormaPagamento(),
                    venda.getParcelas(),
                    0.0,
                    "",
                    new javax.swing.table.DefaultTableModel(
                            new Object[][] { { venda.getFormaPagamento(), venda.getTotalLiquido() } },
                            new String[] { "Forma", "Valor" }));

            JScrollPane scroll = (JScrollPane) dialog.getContentPane().getComponent(0);
            JTextArea ta = (JTextArea) scroll.getViewport().getView();
            String texto = ta.getText();

            // 2) Converte para stream de bytes
            ByteArrayInputStream stream = new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8));
            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            Doc doc = new SimpleDoc(stream, flavor, null);

            // 3) Lê impressora configurada (usando DAO)
            PrinterConfigModel config = new PrinterConfigDAO().loadConfig();
            String nomeImpressora = config != null ? config.getDefaultPrinterName() : null;
            if (nomeImpressora == null || nomeImpressora.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nenhuma impressora configurada.",
                        "Erro de Impressão", JOptionPane.ERROR_MESSAGE);
                return;
            }
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService selecionada = null;

            for (PrintService ps : services) {
                if (ps.getName().equalsIgnoreCase(nomeImpressora)) {
                    selecionada = ps;
                    break;
                }
            }

            if (selecionada == null) {
                JOptionPane.showMessageDialog(null, "Impressora \"" + nomeImpressora + "\" não encontrada.",
                        "Erro de Impressão", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 4) Imprime
            DocPrintJob job = selecionada.createPrintJob();
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new JobName("Comprovante Venda #" + venda.getId(), null));
            attrs.add(new Copies(1));
            job.print(doc, attrs);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Erro ao imprimir cupom fiscal:\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper pra normalizar o id (String/int/Integer)
    private static int getVendaIdAsInt(VendaModel venda) {
        Object id = venda.getId();
        if (id == null)
            return 0;
        if (id instanceof Number n)
            return n.intValue();
        try {
            return Integer.parseInt(id.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public static void gerarComprovanteVenda(
            VendaModel venda,
            List<VendaItemModel> itens,
            TableModel pagamentos,
            CupomFiscalFormatter.ParcelamentoInfo parcelamento,
            String caminhoCompleto) throws Exception {

        int vendaId = getVendaIdAsInt(venda);

        String texto = CupomFiscalFormatter.gerarTextoCupom(
                vendaId,
                itens,
                CupomFiscalFormatter.fromTableModel(pagamentos),
                parcelamento);

        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        PDType0Font font = PDType0Font.load(doc, new java.io.File(FONT_DIR, "Roboto-Regular.ttf"));

        PDPageContentStream content = new PDPageContentStream(doc, page);
        content.setFont(font, 10);
        content.beginText();
        content.setLeading(14f);
        content.newLineAtOffset(40, page.getMediaBox().getHeight() - 50);

        for (String line : texto.split("\n")) {
            content.showText(line);
            content.newLine();
        }

        content.endText();
        content.close();

        doc.save(caminhoCompleto);
        doc.close();
    }

public static void imprimirCupomFiscal(
        VendaModel venda,
        List<VendaItemModel> itens,
        TableModel pagamentos,
        CupomFiscalFormatter.ParcelamentoInfo parcelamento) {
    try {
        int vendaId = getVendaIdAsInt(venda);

        String texto = CupomFiscalFormatter.gerarTextoCupom(
                vendaId,
                itens,
                CupomFiscalFormatter.fromTableModel(pagamentos),
                parcelamento
        );

        ByteArrayInputStream stream = new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8));
        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        Doc doc = new SimpleDoc(stream, flavor, null);

        PrinterConfigModel config = new PrinterConfigDAO().loadConfig();
        String nomeImpressora = config != null ? config.getDefaultPrinterName() : null;
        if (nomeImpressora == null || nomeImpressora.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nenhuma impressora configurada.",
                    "Erro de Impressão", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService selecionada = null;
        for (PrintService ps : services) {
            if (ps.getName().equalsIgnoreCase(nomeImpressora)) {
                selecionada = ps;
                break;
            }
        }
        if (selecionada == null) {
            JOptionPane.showMessageDialog(null, "Impressora \"" + nomeImpressora + "\" não encontrada.",
                    "Erro de Impressão", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DocPrintJob job = selecionada.createPrintJob();
        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(new JobName("Cupom Venda #" + vendaId, null));
        attrs.add(new Copies(1));
        job.print(doc, attrs);

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null,
                "Erro ao imprimir cupom:\n" + e.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
}