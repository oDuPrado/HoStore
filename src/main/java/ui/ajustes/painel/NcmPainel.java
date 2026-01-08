// src/main/java/ui/ajustes/painel/NcmPainel.java
package ui.ajustes.painel;

import model.CodigoDescricaoModel;
import service.FiscalCatalogService;
import service.FiscalCatalogService.CatalogType;
import ui.ajustes.dialog.FiscalCadastrosDialog;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

public class NcmPainel extends JPanel {

    private final FiscalCatalogService service = FiscalCatalogService.getInstance();

    private final JTabbedPane tabs = new JTabbedPane();

    private final DefaultTableModel modelNcm = new DefaultTableModel(new String[] { "Código", "Descrição" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final DefaultTableModel modelCfop = new DefaultTableModel(new String[] { "Código", "Descrição" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final DefaultTableModel modelCsosn = new DefaultTableModel(new String[] { "Código", "Descrição" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final DefaultTableModel modelOrigem = new DefaultTableModel(new String[] { "Código", "Descrição" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final DefaultTableModel modelUnidades = new DefaultTableModel(new String[] { "Código", "Descrição" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };

    private final JTable tblNcm = new JTable(modelNcm);
    private final JTable tblCfop = new JTable(modelCfop);
    private final JTable tblCsosn = new JTable(modelCsosn);
    private final JTable tblOrigem = new JTable(modelOrigem);
    private final JTable tblUnidades = new JTable(modelUnidades);

    public NcmPainel() {
        setLayout(new BorderLayout(10, 10));
        UiKit.applyPanelBase(this);

        add(buildHeader(), BorderLayout.NORTH);

        // Tabelas (abas)
        prepareTable(tblNcm);
        prepareTable(tblCfop);
        prepareTable(tblCsosn);
        prepareTable(tblOrigem);
        prepareTable(tblUnidades);

        // Renderer para NCM (formata ####.##.## sem alterar o valor armazenado)
        tblNcm.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Object v = value;
                if (v != null) {
                    String raw = v.toString().replaceAll("\\D", "");
                    v = formatNcm(raw);
                }
                return super.getTableCellRendererComponent(table, v, isSelected, hasFocus, row, column);
            }
        });

        tabs.addTab("NCM", wrapTab("NCMs cadastrados", tblNcm));
        tabs.addTab("CFOP", wrapTab("CFOPs cadastrados", tblCfop));
        tabs.addTab("CSOSN", wrapTab("CSOSNs cadastrados", tblCsosn));
        tabs.addTab("Origem", wrapTab("Origens cadastradas", tblOrigem));
        tabs.addTab("Unidades", wrapTab("Unidades cadastradas", tblUnidades));

        // Tabs dentro de um card (padrão do projeto)
        JPanel tabsCard = UiKit.card();
        tabsCard.setLayout(new BorderLayout());
        tabsCard.add(tabs, BorderLayout.CENTER);

        add(tabsCard, BorderLayout.CENTER);

        // Recarrega sempre que painel aparecer
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                carregarTudo();
            }
        });

        carregarTudo();
    }

    private JComponent buildHeader() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("🧾 Cadastros Fiscais"));
        left.add(UiKit.hint("Visualização rápida. Para editar, use o botão de configuração."));
        card.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton btnConfig = UiKit.primary("⚙️ Configurar Cadastros Fiscais");
        btnConfig.addActionListener(e -> abrirDialogCadastros());
        right.add(btnConfig);

        card.add(right, BorderLayout.EAST);

        return card;
    }

    private void prepareTable(JTable t) {
        UiKit.tableDefaults(t);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Zebra
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }
    }

    private JComponent wrapTab(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiKit.title(title), BorderLayout.WEST);
        top.add(UiKit.hint("Total: carregado do banco (somente leitura aqui)."), BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(UiKit.scroll(table), BorderLayout.CENTER);

        return panel;
    }

    private void abrirDialogCadastros() {
        Window win = SwingUtilities.getWindowAncestor(this);
        FiscalCadastrosDialog dlg = new FiscalCadastrosDialog(win);
        dlg.setVisible(true);
        // ao fechar, recarrega
        carregarTudo();
    }

    private void carregarTudo() {
        try {
            fill(modelNcm, service.findAll(CatalogType.NCM));
            fill(modelCfop, service.findAll(CatalogType.CFOP));
            fill(modelCsosn, service.findAll(CatalogType.CSOSN));
            fill(modelOrigem, service.findAll(CatalogType.ORIGEM));
            fill(modelUnidades, service.findAll(CatalogType.UNIDADES));
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro ao carregar cadastros fiscais:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fill(DefaultTableModel model, List<CodigoDescricaoModel> list) {
        model.setRowCount(0);
        for (CodigoDescricaoModel it : list) {
            model.addRow(new Object[] { it.getCodigo(), it.getDescricao() });
        }
    }

    private static String formatNcm(String raw8) {
        if (raw8 == null)
            return "";
        String r = raw8.replaceAll("\\D", "");
        if (r.length() != 8)
            return raw8;
        return r.substring(0, 4) + "." + r.substring(4, 6) + "." + r.substring(6);
    }

    /** Se quiser abrir em diálogo modal diretamente */
    public void abrir() {
        JDialog d = new JDialog((Window) null, "Cadastros Fiscais", Dialog.ModalityType.APPLICATION_MODAL);
        d.setContentPane(this);
        d.setSize(900, 600);
        d.setLocationRelativeTo(null);
        d.setVisible(true);
    }
}
