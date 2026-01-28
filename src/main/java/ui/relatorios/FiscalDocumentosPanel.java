package ui.relatorios;

import dao.DocumentoFiscalDAO;
import model.DocumentoFiscalModel;
import service.DocumentoFiscalService;
import service.FiscalWorker;
import util.DB;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.List;

/**
 * Painel de Documentos Fiscais (NFC-e)
 * Lista documentos, status e ações
 */
public class FiscalDocumentosPanel extends JPanel {

    private final JTable table = new JTable();
    private final DefaultTableModel tableModel;
    private final DocumentoFiscalDAO docDAO = new DocumentoFiscalDAO();
    private final DocumentoFiscalService docService = new DocumentoFiscalService();

    public FiscalDocumentosPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        add(buildHeader(), BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new Object[][] {},
                new String[] { "Número", "Série", "Venda ID", "Status", "Chave", "Protocolo", "Erro" }) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table.setModel(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    mostrarXml();
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        add(sp, BorderLayout.CENTER);

        // Footer with actions
        add(buildFooter(), BorderLayout.SOUTH);

        // Carregar dados
        atualizarTabela();
    }

    private JComponent buildHeader() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JLabel title = UiKit.title("📋 Documentos Fiscais (NFC-e)");
        card.add(title, BorderLayout.WEST);

        JLabel hint = UiKit.hint("Clique 2x para ver XML. Status: pendente → xml_gerado → assinada → enviada → autorizada");
        card.add(hint, BorderLayout.CENTER);

        return card;
    }

    private JComponent buildFooter() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        actions.setOpaque(false);

        JButton btnAtualizar = UiKit.ghost("🔄 Atualizar");
        btnAtualizar.addActionListener(e -> atualizarTabela());
        actions.add(btnAtualizar);

        JButton btnForcarProcessamento = UiKit.ghost("⚡ Forçar Processamento (Job)");
        btnForcarProcessamento.addActionListener(e -> forcarProcessamento());
        actions.add(btnForcarProcessamento);

        JButton btnGerarXml = UiKit.ghost("📄 Gerar XML");
        btnGerarXml.addActionListener(e -> gerarXml());
        actions.add(btnGerarXml);

        JButton btnImprimirDanfe = UiKit.ghost("🖨️ Imprimir DANFE");
        btnImprimirDanfe.addActionListener(e -> imprimirDanfe());
        actions.add(btnImprimirDanfe);

        JButton btnDetalhes = UiKit.ghost("🔍 Detalhes");
        btnDetalhes.addActionListener(e -> mostrarXml());
        actions.add(btnDetalhes);

        card.add(actions, BorderLayout.WEST);
        return card;
    }

    private void atualizarTabela() {
        try (Connection conn = DB.get()) {
            // Lista últimos 50 documentos
            List<DocumentoFiscalModel> docs = docDAO.listarPorStatus(conn, null, 50);
            
            tableModel.setRowCount(0);
            for (DocumentoFiscalModel doc : docs) {
                tableModel.addRow(new Object[] {
                    doc.numero,
                    doc.serie,
                    doc.vendaId,
                    doc.status,
                    doc.chaveAcesso != null ? doc.chaveAcesso.substring(0, Math.min(10, doc.chaveAcesso.length())) + "..." : "-",
                    doc.protocolo != null ? doc.protocolo : "-",
                    doc.erro != null ? doc.erro.substring(0, Math.min(30, doc.erro.length())) + "..." : "-"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Erro ao carregar documentos: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DocumentoFiscalModel getSelectedDoc() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Selecione um documento", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        int numero = (int) tableModel.getValueAt(row, 0);
        int serie = (int) tableModel.getValueAt(row, 1);
        int vendaId = (int) tableModel.getValueAt(row, 2);

        try (Connection conn = DB.get()) {
            DocumentoFiscalModel doc = docDAO.buscarPorVenda(conn, vendaId);
            if (doc != null && doc.numero == numero && doc.serie == serie) {
                return doc;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private void gerarXml() {
        DocumentoFiscalModel doc = getSelectedDoc();
        if (doc == null) return;

        try {
            docService.gerarXml(doc.id);
            JOptionPane.showMessageDialog(this, "✅ XML gerado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            atualizarTabela();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimirDanfe() {
        DocumentoFiscalModel doc = getSelectedDoc();
        if (doc == null) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(new java.io.File(
                "DANFE_venda-" + doc.vendaId + "_nfce-" + doc.numero + "_serie-" + doc.serie + ".txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String caminhoSaida = chooser.getSelectedFile().getAbsolutePath();
                docService.imprimirDanfe(doc.id, caminhoSaida);
                JOptionPane.showMessageDialog(this, "✅ DANFE salvo:\n" + caminhoSaida, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "❌ Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void mostrarXml() {
        DocumentoFiscalModel doc = getSelectedDoc();
        if (doc == null) return;

        if (doc.xml == null || doc.xml.isBlank()) {
            JOptionPane.showMessageDialog(this, "❌ Documento ainda não tem XML", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Exibe em dialog com textarea
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "XML - Documento " + doc.numero, true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);

        JTextArea ta = new JTextArea(doc.xml);
        ta.setEditable(false);
        ta.setFont(new Font("Courier New", Font.PLAIN, 10));
        ta.setLineWrap(true);

        JScrollPane sp = new JScrollPane(ta);
        dialog.add(sp, BorderLayout.CENTER);

        JButton btnClose = new JButton("Fechar");
        btnClose.addActionListener(e -> dialog.dispose());
        JPanel footer = new JPanel();
        footer.add(btnClose);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void forcarProcessamento() {
        try {
            FiscalWorker.getInstance().forcarProcessamento();
            JOptionPane.showMessageDialog(this, "✅ Job fiscal executado!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            atualizarTabela();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
