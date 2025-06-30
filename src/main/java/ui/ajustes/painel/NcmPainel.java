// src/main/java/ui/ajustes/painel/NcmPainel.java
package ui.ajustes.painel;

import dao.NcmDAO;
import model.NcmModel;
import ui.ajustes.dialog.NcmDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.SQLException;
import java.util.List;

public class NcmPainel extends JPanel {

    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final NcmDAO dao = new NcmDAO();

    public NcmPainel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Tabela central
        modelo = new DefaultTableModel(new String[]{"Código", "Descrição"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setRowHeight(24);
        tabela.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createTitledBorder("NCMs Cadastrados"));
        add(scroll, BorderLayout.CENTER);

        // Botões no topo
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton btnConfig = new JButton("⚙️ Configurar NCMs");
        btnConfig.addActionListener(e -> {
            Window win = SwingUtilities.getWindowAncestor(this);
            NcmDialog dlg = new NcmDialog(win);
            dlg.setVisible(true);
        });
        topo.add(btnConfig);
        add(topo, BorderLayout.NORTH);

        // Recarrega sempre que o painel for exibido
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) {
                carregarTabela();
            }
        });

        // Carrega imediatamente ao criar o painel
        carregarTabela();
    }

    private void carregarTabela() {
        modelo.setRowCount(0);
        try {
            List<NcmModel> lista = dao.findAll();
            for (NcmModel n : lista) {
                // formatação bruta em formato ####.##.##
                String c = n.getCodigo();
                String mask = c.substring(0, 4) + "." + c.substring(4, 6) + "." + c.substring(6);
                modelo.addRow(new Object[]{ mask, n.getDescricao() });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar NCMs:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /** Se quiser abrir em diálogo modal diretamente */
    public void abrir() {
        JDialog d = new JDialog((Window)null, "NCMs", Dialog.ModalityType.APPLICATION_MODAL);
        d.setContentPane(this);
        d.setSize(700, 500);
        d.setLocationRelativeTo(null);
        d.setVisible(true);
    }
}