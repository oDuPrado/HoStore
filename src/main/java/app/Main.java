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
import util.LogService;

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
                        LogService.audit("APP_DB_OK", "sistema", null, "banco pronto");
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
                        LogService.auditError("APP_DB_ERRO", "sistema", null,
                                "falha ao inicializar banco", initException);
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
                        LogService.audit("LOGIN_CANCEL", "usuario", null, "login cancelado");
                        System.exit(0);
                        return;
                    }

                    SessaoService.login(usuarioLogado);

                    // 4) Backup automatico (nao pode derrubar o app)
                    try {
                        BackupUtils.applyConfig(BackupUtils.loadConfig());
                    } catch (Exception e) {
                        LogService.auditError("BACKUP_CONFIG_ERRO", "sistema", null,
                                "falha ao aplicar config de backup", e);
                    }

                    // 5) Carta fake (se isso falhar, nao deveria derrubar o sistema)
                    try {
                        new CartaDAO().inserirCartaFake();
                    } catch (Exception e) {
                        LogService.auditError("CARTA_FAKE_ERRO", "sistema", null,
                                "falha ao inserir carta fake", e);
                    }

                    // 6) Inicializa worker de documentos fiscais (Etapa 9)
                    try {
                        service.FiscalWorker.getInstance().iniciar();
                        LogService.audit("FISCAL_WORKER_INICIADO", "sistema", null, "worker de NFC-e iniciado");
                    } catch (Exception e) {
                        LogService.auditError("FISCAL_WORKER_ERRO", "sistema", null,
                                "falha ao iniciar worker fiscal", e);
                    }

                    // 7) Abre janela principal
                    LogService.audit("APP_START", "sistema", null, "app iniciado");
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
            LogService.auditError("APP_TEMA_ERRO", "sistema", null, "falha ao aplicar tema", e);
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
