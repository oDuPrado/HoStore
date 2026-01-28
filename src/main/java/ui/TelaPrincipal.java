// src/ui/TelaPrincipal.java
package ui;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.formdev.flatlaf.FlatDarkLaf;

import service.SessaoService;
import model.UsuarioModel;
import util.DB;
import util.SyncStatusUtil;

import ui.estoque.painel.PainelEstoque;
import ui.clientes.painel.PainelClientes;
import ui.venda.painel.PainelVendas;
import ui.dash.painel.DashboardPanel;
import ui.ajustes.AjustesPanel;
import ui.financeiro.painel.PainelFinanceiro;
import ui.ajustes.dialog.LoginDialog;
import ui.eventos.painel.PainelEventos;

// ✅ NOVO
import ui.comandas.painel.PainelComandas;
import ui.rh.painel.PainelRH;

public class TelaPrincipal extends JFrame {

    private static final String VERSION = "1.1.23";

    private JPanel painelHeader;
    private JPanel painelTabs;
    private JPanel painelConteudo;
    private JPanel painelFooter;

    // Footer labels
    private JLabel lblDbStatus;
    private JLabel lblSync;
    private JLabel lblClock;

    public TelaPrincipal() {
        try {
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // ====== Frame base ======
        setTitle("HoStore - ERP TCG Card Game");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1024, 600));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        // Etapa 9: Handler para parar FiscalWorker ao fechar
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onWindowClosing();
            }
        });

        // ✅ Monta a estrutura ANTES do login (para não existir painelConteudo null)
        painelConteudo = new JPanel(new BorderLayout());
        painelConteudo.setBackground(UIManager.getColor("Panel.background"));

        // Header inicial (placeholder null-safe)
        painelHeader = createHeaderPanel(SessaoService.get());
        painelTabs = createTabBarPanel();
        painelFooter = createFooterPanel();

        getContentPane().add(painelHeader, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.add(painelTabs, BorderLayout.NORTH);
        center.add(painelConteudo, BorderLayout.CENTER);

        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(painelFooter, BorderLayout.SOUTH);

        startFooterTimers();

        setVisible(true);

        // ✅ Agora sim: login e carregar home real
        SwingUtilities.invokeLater(this::garantirLoginECarregarHome);
    }

    /** Garante login e depois carrega a home/dashboard (agora com UI pronta). */
    private void garantirLoginECarregarHome() {
        if (SessaoService.get() == null) {
            LoginDialog loginDialog = new LoginDialog(this);
            loginDialog.setVisible(true);

            UsuarioModel logado = loginDialog.getUsuarioLogado();
            if (logado == null) {
                JOptionPane.showMessageDialog(this,
                        "Login obrigatório para acessar o sistema.",
                        "Acesso negado", JOptionPane.WARNING_MESSAGE);
                System.exit(0);
                return;
            }
            SessaoService.login(logado);
        }

        // Atualiza header com usuário real
        refreshHeader();

        // Carrega dashboard/home real
        trocarPainel(new ui.dash.painel.DashboardPanel(this, destino -> {
            // teu roteamento de destino aqui
            // exemplo:
            // if ("VENDAS".equals(destino)) trocarPainel(new PainelVendas(this));
        }));
    }

    private void refreshHeader() {
        UsuarioModel usuario = SessaoService.get();

        // remove e recria o header
        getContentPane().remove(painelHeader);
        painelHeader = createHeaderPanel(usuario);
        getContentPane().add(painelHeader, BorderLayout.NORTH);

        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private JPanel createHeaderPanel(UsuarioModel usuario) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        header.setBackground(UIManager.getColor("Panel.background"));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel lblTitle = new JLabel("HoStore");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(UIManager.getColor("Label.foreground"));

        JLabel lblVersion = new JLabel("v" + VERSION);
        lblVersion.setForeground(UIManager.getColor("Label.foreground"));

        left.add(lblTitle);
        left.add(lblVersion);
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        String nome = (usuario != null && usuario.getNome() != null) ? usuario.getNome() : "—";
        JLabel lblUser = new JLabel("Usuário: " + nome);
        lblUser.setForeground(UIManager.getColor("Label.foreground"));

        boolean ok = DB.isConnected();
        JLabel lblStatus = new JLabel(ok ? "Banco: OK" : "Banco: ERRO");
        lblStatus.setForeground(ok ? new Color(0, 200, 0) : new Color(200, 70, 70));

        right.add(lblUser);
        right.add(lblStatus);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createTabBarPanel() {
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setBackground(UIManager.getColor("Panel.background"));

        JButton tabEstoque = criarTab("📦 Estoque", new PainelEstoque());
        JButton tabClientes = criarTab("🧍 Clientes", new PainelClientes());
        JButton tabVendas = criarTab("💰 Vendas", new PainelVendas(this));
        JButton tabComandas = criarTab("🧾 Comandas", new PainelComandas());
        JButton tabEventos = criarTab("🏆 Eventos/Liga", new PainelEventos());
        JButton tabRelatorios = criarTab("📊 Relatórios", new DashboardPanel());
        JButton tabAjustes = criarTab("⚙️ Ajustes", new AjustesPanel());
        JButton tabRh = criarTab("👥 RH", new PainelRH());
        JButton tabFinanceiro = criarTab("🧾 Financeiro", new PainelFinanceiro());

        // RH agora ativo

        tabBar.add(tabEstoque);
        tabBar.add(tabClientes);
        tabBar.add(tabVendas);
        tabBar.add(tabComandas);
        tabBar.add(tabEventos);
        tabBar.add(tabRelatorios);
        tabBar.add(tabAjustes);
        tabBar.add(tabRh);
        tabBar.add(tabFinanceiro);

        return tabBar;
    }

    private JButton criarTab(String texto, JPanel painel) {
        JButton botao = new JButton(texto);
        botao.setFocusPainted(false);
        botao.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        botao.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (painel != null) {
            botao.addActionListener(e -> trocarPainel(painel));
        }
        return botao;
    }

    private void trocarPainel(JPanel novoPainel) {
        // ✅ blindagem extra (pra não quebrar nunca mais)
        if (painelConteudo == null) {
            System.err.println("painelConteudo ainda não inicializado. Ignorando troca.");
            return;
        }
        painelConteudo.removeAll();
        painelConteudo.add(novoPainel, BorderLayout.CENTER);
        painelConteudo.revalidate();
        painelConteudo.repaint();
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        footer.setBackground(UIManager.getColor("Panel.background"));

        JLabel lblCopy = new JLabel("© Ho Systems");
        lblCopy.setForeground(UIManager.getColor("Label.foreground"));
        footer.add(lblCopy, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        lblDbStatus = new JLabel("Banco: ...");
        lblDbStatus.setForeground(UIManager.getColor("Label.foreground"));

        lblSync = new JLabel("Última sincronização: ...");
        lblSync.setForeground(UIManager.getColor("Label.foreground"));

        lblClock = new JLabel("Agora: ...");
        lblClock.setForeground(UIManager.getColor("Label.foreground"));

        right.add(lblDbStatus);
        right.add(lblSync);
        right.add(lblClock);

        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void startFooterTimers() {
        Timer clockTimer = new Timer(1000, e -> {
            String agora = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            lblClock.setText("Agora: " + agora);
        });
        clockTimer.start();

        Timer statusTimer = new Timer(15000, e -> {
            boolean ok = DB.isConnected();
            lblDbStatus.setText(ok ? "Banco: OK" : "Banco: ERRO");
            lblSync.setText("Última sincronização: " + SyncStatusUtil.getUltimaSincronizacaoFormatada());
        });
        statusTimer.start();

        lblDbStatus.setText(DB.isConnected() ? "Banco: OK" : "Banco: ERRO");
        lblSync.setText("Última sincronização: " + SyncStatusUtil.getUltimaSincronizacaoFormatada());
        lblClock.setText("Agora: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
    }

    /**
     * Etapa 9: Handler para encerramento seguro do FiscalWorker
     * Chamado quando a janela é fechada
     */
    private void onWindowClosing() {
        int opcao = JOptionPane.showConfirmDialog(this,
                "Deseja realmente sair da aplicação?",
                "Confirmar saída",
                JOptionPane.YES_NO_OPTION);

        if (opcao == JOptionPane.YES_OPTION) {
            try {
                // Parar worker fiscal de forma segura
                service.FiscalWorker.getInstance().parar();
                util.LogService.audit("FISCAL_WORKER_PARADO", "sistema", null, "worker fiscal parado");
            } catch (Exception e) {
                util.LogService.auditError("FISCAL_WORKER_ERRO_SHUTDOWN", "sistema", null,
                        "erro ao parar fiscal worker", e);
            }

            // Executar logout
            util.LogService.audit("APP_LOGOUT", "sistema", null, "aplicação encerrada");
            SessaoService.logout();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TelaPrincipal::new);
    }
}
