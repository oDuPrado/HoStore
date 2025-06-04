package util;

import dao.ConfigLojaDAO;
import dao.ProdutoDAO;
import model.ConfigLojaModel;
import model.VendaItemModel;
import model.VendaModel; // @FIX: novo import
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import java.awt.*;
import java.text.NumberFormat; // @FIX: novo import
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Responsável por gerar comprovantes em tela e PDF.
 * (Layout preservado, apenas correções de compile-time.)
 */
public class PDFGenerator {

    private static final String EXPORT_DIR = "data/export";
    private static final String FONT_DIR = "data/fonts";
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
            String textoRodape = (config != null && config.getTextoRodapeNota() != null) ? config.getTextoRodapeNota()
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

            // =========== 2) Itens da Venda ===========
            // Cabeçalho da tabela de itens
            sb.append(String.format("%-20s %4s %7s %6s %7s\n",
                    padRight("PRODUTO", 20),
                    padLeft("QTD", 4),
                    padLeft("UN", 7),
                    padLeft("VL.UNI", 6),
                    padLeft("TOTAL", 7)));
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");

            // Inicializa acumuladores
            double totBruto = 0.0;
            double totDesconto = 0.0;

            // Wrapper final para permitir uso em lambdas (ex: geração de PDF)
            final double[] totLiquidoFinal = { 0.0 };

            ProdutoDAO pdao = new ProdutoDAO();

            for (VendaItemModel it : itens) {
                String nomeProduto;
                try {
                    nomeProduto = pdao.findById(it.getProdutoId()).getNome();
                } catch (Exception e) {
                    nomeProduto = it.getProdutoId(); // fallback: mostra o ID se falhar
                }

                // Trunca nome do produto se exceder 20 caracteres
                if (nomeProduto.length() > 20) {
                    nomeProduto = nomeProduto.substring(0, 20);
                }

                int qtd = it.getQtd();
                double unit = it.getPreco();
                double descV = unit * qtd * it.getDesconto() / 100.0;
                double linhaVl = unit * qtd - descV;

                // Formata quantidade com três casas decimais
                String unidadeFormatada = String.format(Locale.US, "%.3f", qtd * 1.0).replace('.', ',');

                // Linha formatada: PRODUTO QTD UN VL.UNI TOTAL
                sb.append(String.format("%-20s %4s %7s %6s %7s\n",
                        padRight(nomeProduto, 20),
                        padLeft(String.valueOf(qtd), 4),
                        padLeft(unidadeFormatada, 7),
                        padLeft(cf.format(unit), 6),
                        padLeft(cf.format(linhaVl), 7)));

                // Acumula totais
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
                    .append(padLeft(cf.format(totLiquidoFinal), 8)).append('\n');
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

            // =========== 5) Parcelamento Cartão (se houver) ===========
            if (parcelas > 1) {
                // Ajusta para usar o valor dentro do array final
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

            /* ==== monta a UI ==== */
            ta.setText(sb.toString()); // @FIX: exibe texto
            add(new JScrollPane(ta), BorderLayout.CENTER); // @FIX: scroll

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
            return RECEIPT_WIDTH - 8;
        }
    }

    /*
     * ─────────────────── SOBRECARGA para não quebrar código legado
     * ───────────────────
     */
    public static void gerarComprovanteVenda(VendaModel venda, List<VendaItemModel> itens) {
      
        // Mantido vazio para compilar enquanto você decide como gerar.
        System.out.println("[DEBUG] Gerar PDF para venda " + venda.getId() + " com " + itens.size() + " itens.");
    }

    
}
