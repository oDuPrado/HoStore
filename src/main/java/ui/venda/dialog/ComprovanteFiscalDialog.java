package ui.venda.dialog;

import util.UiKit;
import model.VendaItemModel;
import model.VendaModel;
import util.CupomFiscalFormatter;
import util.PDFGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ComprovanteFiscalDialog extends JDialog {

    public ComprovanteFiscalDialog(
            Window owner,
            int vendaId,
            List<VendaItemModel> itens,
            TableModel pagamentos,
            CupomFiscalFormatter.ParcelamentoInfo parcelamento,
            double acrescimo) {
        super(owner, "Comprovante Fiscal - Venda #" + vendaId, ModalityType.APPLICATION_MODAL);
        UiKit.applyDialogBase(this);

        setSize(520, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ta.setLineWrap(false);

        // Texto vem do formatter único
        String texto = CupomFiscalFormatter.gerarTextoCupom(
                vendaId,
                itens,
                CupomFiscalFormatter.fromTableModel(pagamentos),
                parcelamento,
                acrescimo);

        ta.setText(texto);
        ta.setCaretPosition(0);

        add(UiKit.scroll(ta), BorderLayout.CENTER);

        JPanel bts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        JButton btnPdf = new JButton("Gerar PDF");
        btnPdf.addActionListener(ev -> {
            try {
                // Monta VendaModel mínimo só pra PDFGenerator
                VendaModel vm = new VendaModel(
                        String.valueOf(vendaId),
                        null, 0, 0, 0.0, null,
                        parcelamento != null ? parcelamento.parcelas : 1,
                        null);
                vm.setItens(itens);

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String nomeArquivo = "comprovante_" + vendaId + "_" + timestamp + ".pdf";
                java.io.File pasta = new java.io.File("data/export");
                if (!pasta.exists())
                    pasta.mkdirs();
                java.io.File destino = new java.io.File(pasta, nomeArquivo);

                PDFGenerator.gerarComprovanteVenda(vm, itens, pagamentos, parcelamento, acrescimo, destino.getAbsolutePath());

                JOptionPane.showMessageDialog(this,
                        "PDF gerado:\n" + destino.getPath(),
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Erro ao gerar PDF:\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnTermica = new JButton("Imprimir Térmica");
        btnTermica.addActionListener(ev -> {
            try {
                VendaModel vm = new VendaModel(
                        String.valueOf(vendaId),
                        null, 0, 0, 0.0, null,
                        parcelamento != null ? parcelamento.parcelas : 1,
                        null);
                vm.setItens(itens);

                PDFGenerator.imprimirCupomFiscal(vm, itens, pagamentos, parcelamento, acrescimo);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Erro ao imprimir:\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnClose = new JButton("Fechar");
        btnClose.addActionListener(ev -> dispose());

        bts.add(btnPdf);
        bts.add(btnTermica);
        bts.add(btnClose);

        add(bts, BorderLayout.SOUTH);
    }
}
