// src/ui/venda/dialog/VendaDevolucaoDialog.java
package ui.venda.dialog;

import dao.VendaDevolucaoDAO;
import model.VendaItemModel;
import service.VendaDevolucaoService;
import model.VendaDevolucaoModel;
import util.AlertUtils;
import util.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public class VendaDevolucaoDialog extends JDialog {
    private final int vendaId;
    private final List<VendaItemModel> itens;

    private final JTable tabela;
    private final DefaultTableModel model;

    public VendaDevolucaoDialog(Window owner, int vendaId, List<VendaItemModel> itens) {
        super(owner, "Registrar Devolução", ModalityType.APPLICATION_MODAL);
        this.vendaId = vendaId;
        this.itens = itens;

        setSize(600, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        model = new DefaultTableModel(new String[] {
                "Produto", "Qtd Vendida", "Qtd Devolver", "Motivo"
        }, 0);

        for (VendaItemModel it : itens) {
            model.addRow(new Object[] {
                    it.getProdutoId(),
                    it.getQtd(),
                    0, // inicial: nenhuma devolução
                    "" // motivo vazio
            });
        }

        tabela = new JTable(model);
        JScrollPane scroll = new JScrollPane(tabela);

        add(scroll, BorderLayout.CENTER);

        JButton btnSalvar = new JButton("Confirmar Devolução");
        btnSalvar.addActionListener(this::salvar);
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(btnCancelar);
        rodape.add(btnSalvar);

        add(rodape, BorderLayout.SOUTH);
    }

    private void salvar(ActionEvent evt) {
        try (Connection c = DB.get()) {
            VendaDevolucaoDAO dao = new VendaDevolucaoDAO();

            for (int i = 0; i < model.getRowCount(); i++) {
                int qtdDevolver = Integer.parseInt(model.getValueAt(i, 2).toString());
                String motivo = (String) model.getValueAt(i, 3);
                String produtoId = (String) model.getValueAt(i, 0);
                int qtdVendida = (Integer) model.getValueAt(i, 1);

                if (qtdDevolver <= 0)
                    continue;

                if (qtdDevolver > qtdVendida) {
                    AlertUtils.error("A quantidade devolvida não pode ser maior que a vendida (linha " + (i + 1) + ")");
                    return;
                }

                VendaDevolucaoModel dev = new VendaDevolucaoModel();
                dev.setVendaId(vendaId);
                dev.setProdutoId(produtoId);
                dev.setQuantidade(qtdDevolver);
                dev.setMotivo(motivo);
                dev.setData(LocalDate.now());

                VendaDevolucaoService service = new VendaDevolucaoService();
                service.registrarDevolucao(dev);

            }

            AlertUtils.info("Devoluções registradas com sucesso.");
            dispose();

        } catch (Exception e) {
            AlertUtils.error("Erro ao registrar devoluções:\n" + e.getMessage());
        }
    }
}
