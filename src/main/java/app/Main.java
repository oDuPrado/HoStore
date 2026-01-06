package app;

import com.formdev.flatlaf.FlatLightLaf;
import dao.CartaDAO;
import model.UsuarioModel;
import service.SessaoService;
import ui.TelaPrincipal;
import ui.ajustes.dialog.LoginDialog;
import ui.ajustes.painel.CategoriaProdutoPainel;
import util.BackupUtils;
import util.DB;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public class Main {

    public static void main(String[] args) {
        // 1) LookAndFeel antes de qualquer UI (inclui splash)
        applySavedThemeSafely();

        SwingUtilities.invokeLater(() -> {
            SplashUI splash = new SplashUI();
            splash.showSplash();

            new SwingWorker<Void, String>() {
                Exception initException = null;

                @Override
                protected Void doInBackground() {
                    publish("Verificando e criando banco de dados...");
                    try {
                        // 2) Inicialização única do banco: só aqui.
                        DB.prepararBancoSeNecessario();
                        publish("Banco OK.");
                    } catch (Exception e) {
                        initException = e;
                    }
                    return null;
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    if (chunks != null && !chunks.isEmpty()) {
                        splash.setStatus(chunks.get(chunks.size() - 1));
                    }
                }

                @Override
                protected void done() {
                    splash.close();

                    if (initException != null) {
                        initException.printStackTrace();
                        JOptionPane.showMessageDialog(
                                null,
                                "Erro ao inicializar o banco de dados:\n" + initException.getMessage(),
                                "Erro fatal",
                                JOptionPane.ERROR_MESSAGE
                        );
                        System.exit(1);
                        return;
                    }

                    // 3) Login
                    LoginDialog login = new LoginDialog(null);
                    login.setVisible(true);

                    UsuarioModel usuarioLogado = login.getUsuarioLogado();
                    if (usuarioLogado == null) {
                        System.exit(0);
                        return;
                    }

                    SessaoService.login(usuarioLogado);

                    // 4) Backup automático (não pode derrubar o app)
                    try {
                        BackupUtils.applyConfig(BackupUtils.loadConfig());
                    } catch (Exception e) {
                        System.err.println("Falha ao aplicar configuração de backup: " + e.getMessage());
                    }

                    // 5) Carta fake (se isso falhar, também não deveria derrubar o sistema)
                    try {
                        new CartaDAO().inserirCartaFake();
                    } catch (Exception e) {
                        System.err.println("Falha ao inserir carta fake: " + e.getMessage());
                    }

                    // 6) Abre janela principal
                    new TelaPrincipal();
                }
            }.execute();
        });
    }

    private static void applySavedThemeSafely() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(CategoriaProdutoPainel.class);
            String temaSalvo = prefs.get("temaSelecionado", "Claro (FlatLight)");
            LookAndFeel tema = CategoriaProdutoPainel.TEMAS.getOrDefault(temaSalvo, new FlatLightLaf());
            UIManager.setLookAndFeel(tema);
        } catch (Exception e) {
            // fallback absoluto
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception ignored) {}
            System.err.println("Falha ao aplicar tema: " + e.getMessage());
        }
    }

    // Splash simples mas decente
    private static class SplashUI {
        private final JFrame frame = new JFrame();
        private final JLabel statusLabel = new JLabel("Inicializando HoStore...");
        private final JProgressBar bar = new JProgressBar();

        SplashUI() {
            frame.setUndecorated(true);
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));

            bar.setIndeterminate(true);
            bar.setBorderPainted(false);

            panel.add(statusLabel, BorderLayout.NORTH);
            panel.add(bar, BorderLayout.SOUTH);

            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
        }

        void showSplash() {
            frame.setVisible(true);
        }

        void setStatus(String text) {
            statusLabel.setText(text);
        }

        void close() {
            frame.dispose();
        }
    }
}
