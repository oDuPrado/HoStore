package ui.ajustes.dialog;

import dao.ImpostoICMSDAO;
import dao.ImpostoIPIDAO;
import dao.ImpostoPisCofinsDAO;
import model.ImpostoIcmsModel;
import model.ImpostoIpiModel;
import model.ImpostoPisCofinsModel;
import util.DB;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;

/**
 * Importador de Catálogo Fiscal
 * Importa CSV com tabelas de impostos (ICMS, IPI, PIS/COFINS)
 */
public class FiscalCatalogImportDialog extends JDialog {

    private final JFileChooser chooser = new JFileChooser();
    private final JTextArea taLog = new JTextArea(10, 50);
    private final JProgressBar pBar = new JProgressBar(0, 100);
    private final JButton btnImportar = new JButton("📥 Importar");
    private final JButton btnFechar = new JButton("Fechar");

    public FiscalCatalogImportDialog(Frame owner) {
        super(owner, "Importador Catálogo Fiscal", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initComponents();
        setSize(700, 500);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(10, 10));
        header.add(UiKit.title("📥 Importar Catálogo Fiscal"), BorderLayout.WEST);
        header.add(UiKit.hint("CSV: NCM;ESTADO;ALIQUOTA_CONSUMIDOR"), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // File chooser
        JPanel filePanel = UiKit.card();
        filePanel.setLayout(new BorderLayout(8, 8));
        filePanel.add(new JLabel("Selecione arquivo CSV:"), BorderLayout.WEST);

        JButton btnBrowse = UiKit.ghost("📂 Procurar");
        btnBrowse.addActionListener(e -> {
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(this,
                    "✅ Arquivo selecionado:\n" + chooser.getSelectedFile().getAbsolutePath(),
                    "OK", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        filePanel.add(btnBrowse, BorderLayout.EAST);
        add(filePanel, BorderLayout.PAGE_START);

        // Log area
        JPanel logPanel = UiKit.card();
        logPanel.setLayout(new BorderLayout());
        taLog.setEditable(false);
        taLog.setFont(new Font("Courier New", Font.PLAIN, 10));
        taLog.setLineWrap(true);
        logPanel.add(new JLabel("Log de Importação:"), BorderLayout.NORTH);
        logPanel.add(new JScrollPane(taLog), BorderLayout.CENTER);
        add(logPanel, BorderLayout.CENTER);

        // Progress
        JPanel progressPanel = UiKit.card();
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(new JLabel("Progresso:"), BorderLayout.WEST);
        progressPanel.add(pBar, BorderLayout.CENTER);
        add(progressPanel, BorderLayout.PAGE_END);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        buttons.setOpaque(false);

        btnImportar.addActionListener(e -> onImportar());
        btnFechar.addActionListener(e -> dispose());

        buttons.add(btnImportar);
        buttons.add(btnFechar);
        add(buttons, BorderLayout.SOUTH);
    }

    private void onImportar() {
        File arquivo = chooser.getSelectedFile();
        if (arquivo == null || !arquivo.exists()) {
            JOptionPane.showMessageDialog(this, "❌ Arquivo não selecionado", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        taLog.setText("");
        pBar.setValue(0);
        btnImportar.setEnabled(false);

        // Executar em thread
        new Thread(() -> importarArquivo(arquivo)).start();
    }

    private void importarArquivo(File arquivo) {
        int linhaTotal = 0;
        int linhaProcessada = 0;
        int erros = 0;

        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);

            try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
                String linha;
                linhaTotal = (int) java.nio.file.Files.lines(arquivo.toPath()).count();
                linhaProcessada = 0;

                log("Iniciando importação de " + arquivo.getName() + " (" + linhaTotal + " linhas)...\n");

                while ((linha = br.readLine()) != null) {
                    linhaProcessada++;

                    if (linha.trim().isEmpty() || linha.startsWith("#")) continue;

                    try {
                        procesarLinha(linha, conn);
                    } catch (Exception ex) {
                        erros++;
                        log("❌ Linha " + linhaProcessada + ": " + ex.getMessage());
                    }

                    // Atualizar progresso
                    int progresso = (linhaProcessada * 100) / linhaTotal;
                    pBar.setValue(progresso);
                    pBar.setString(progresso + "%");
                }

                conn.commit();
                log("\n✅ Importação concluída!\n");
                log("Total: " + linhaProcessada + " linhas\n");
                log("Erros: " + erros + "\n");

            } catch (Exception ex) {
                conn.rollback();
                log("❌ Erro fatal: " + ex.getMessage());
                ex.printStackTrace();
            }

        } catch (Exception ex) {
            log("❌ Erro ao conectar BD: " + ex.getMessage());
        } finally {
            btnImportar.setEnabled(true);
        }
    }

    private void procesarLinha(String linha, Connection conn) throws Exception {
        String[] partes = linha.split(";");
        if (partes.length < 3) {
            throw new Exception("Formato inválido. Esperado: NCM;CAMPO2;VALOR");
        }

        String ncm = partes[0].trim();
        String campo2 = partes[1].trim();
        String valor = partes[2].trim();

        // Detectar tipo de importação por CAMPO2
        if ("ESTADO".equalsIgnoreCase(campo2)) {
            // Formato ICMS: NCM;ESTADO;ALIQUOTA_CONSUMIDOR
            if (partes.length < 4) throw new Exception("ICMS precisa de: NCM;ESTADO;ALIQUOTA_CONSUMIDOR;ALIQUOTA_CONTRIBUINTE");
            
            String estado = campo2;
            double aliqCons = Double.parseDouble(valor);
            double aliqCont = partes.length > 3 ? Double.parseDouble(partes[3].trim()) : aliqCons;

            ImpostoIcmsModel icms = new ImpostoIcmsModel();
            icms.setNcm(ncm);
            icms.setEstado("RS");  // TODO: puxar de partes[1]
            icms.setEstadoDestino("RS");
            icms.setAliquotaConsumidor(aliqCons);
            icms.setAliquotaContribuinte(aliqCont);
            new ImpostoICMSDAO().inserir(icms);
            log("✓ ICMS: " + ncm + " = " + aliqCons + "%\n");

        } else if ("IPI".equalsIgnoreCase(campo2)) {
            // Formato IPI: NCM;IPI;ALIQUOTA
            double aliq = Double.parseDouble(valor);
            ImpostoIpiModel ipi = new ImpostoIpiModel();
            ipi.setNcm(ncm);
            ipi.setAliquota(aliq);
            new ImpostoIPIDAO().inserir(ipi);
            log("✓ IPI: " + ncm + " = " + aliq + "%\n");

        } else if ("PIS".equalsIgnoreCase(campo2)) {
            // Formato PIS/COFINS: NCM;PIS;ALIQUOTA_PIS
            if (partes.length < 5) throw new Exception("PIS/COFINS precisa de: NCM;PIS;ALIQUOTA_PIS;COFINS;ALIQUOTA_COFINS");
            
            double aliqPis = Double.parseDouble(valor);
            String cofins = partes[3].trim();
            double aliqCofins = Double.parseDouble(partes[4].trim());

            ImpostoPisCofinsModel picf = new ImpostoPisCofinsModel();
            picf.setNcm(ncm);
            picf.setCstPis("04");  // isento
            picf.setAliquotaPis(aliqPis);
            picf.setCstCofins("04");  // isento
            picf.setAliquotaCofins(aliqCofins);
            new ImpostoPisCofinsDAO().inserir(picf);
            log("✓ PIS/COFINS: " + ncm + " = " + aliqPis + "% / " + aliqCofins + "%\n");
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            taLog.append(msg);
            taLog.setCaretPosition(taLog.getDocument().getLength());
        });
    }
}
