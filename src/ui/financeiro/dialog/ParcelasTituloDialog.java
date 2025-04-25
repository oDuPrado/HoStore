// ParcelasTituloDialog – Financeiro Compacto
// Procure: "// ParcelasTituloDialog – Financeiro Compacto"
package ui.financeiro.dialog;

import dao.ParcelaContaPagarDAO;
import model.ParcelaContaPagarModel;

import javax.swing.*;
import java.text.DecimalFormatSymbols;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ParcelasTituloDialog extends JDialog {

    private final String tituloId;
    private final ParcelaContaPagarDAO parcelaDAO = new ParcelaContaPagarDAO();
    private List<ParcelaContaPagarModel> parcelas;

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[]{"ID","Parcela","Vencimento","Nominal","Juros","Acréscimo","Desconto","Pago","Status"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    // formatos
    private final SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat visFmt = new SimpleDateFormat("dd/MM/yyyy");
    private final NumberFormat moneyFmt = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    public ParcelasTituloDialog(Window owner, String tituloId) {
        super(owner, "Parcelas do Título", ModalityType.APPLICATION_MODAL);
        this.tituloId = tituloId;
        initComponents();
        loadParcelas();
        setSize(800, 400);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10,10));

        // tabela em scroll
        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // oculta coluna ID
        hideColumn(table, 0);

        // centraliza colunas de data e valores
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        // duplo-click paga
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    marcarPago();
                }
            }
        });

        // botões
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnFechar = new JButton("Fechar");
        JButton btnMarcar = new JButton("Marcar Pago");
        buttons.add(btnFechar);
        buttons.add(btnMarcar);
        add(buttons, BorderLayout.SOUTH);

        btnFechar.addActionListener(e -> dispose());
        btnMarcar.addActionListener(e -> marcarPago());
    }

    private void loadParcelas() {
        tableModel.setRowCount(0);
        try {
            parcelas = parcelaDAO.listarPorTitulo(tituloId);
            for (ParcelaContaPagarModel p : parcelas) {
                String dtVenc = p.getVencimento() != null
                    ? visFmt.format(isoFmt.parse(p.getVencimento()))
                    : "";
                tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getNumeroParcela(),
                    dtVenc,
                    moneyFmt.format(p.getValorNominal()),
                    moneyFmt.format(p.getValorJuros()),
                    moneyFmt.format(p.getValorAcrescimo()),
                    moneyFmt.format(p.getValorDesconto()),
                    moneyFmt.format(p.getValorPago()),
                    p.getStatus()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erro ao carregar parcelas:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void marcarPago() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma parcela.");
            return;
        }
        ParcelaContaPagarModel parcela = parcelas.get(row);
        // abre diálogo de pagamento
        new PagamentoContaPagarDialog(this, parcela).setVisible(true);
        // recarrega após fechar
        loadParcelas();
    }

    private void hideColumn(JTable t, int index) {
        TableColumn col = t.getColumnModel().getColumn(index);
        col.setMinWidth(0);
        col.setMaxWidth(0);
        col.setPreferredWidth(0);
    }
}
