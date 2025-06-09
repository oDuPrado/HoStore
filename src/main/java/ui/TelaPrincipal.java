package ui;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.formdev.flatlaf.FlatDarkLaf; // tema FlatLaf
import service.SessaoService; // controle de sessão
import model.UsuarioModel; // usuário logado
import util.DB; // verificação de conexão
import ui.estoque.painel.PainelEstoque;
import ui.clientes.painel.PainelClientes;
import ui.venda.painel.PainelVendas;
import ui.dash.painel.DashboardPanel;
import ui.ajustes.AjustesPanel;
import ui.financeiro.painel.PainelFinanceiro;
import ui.ajustes.dialog.LoginDialog;

public class TelaPrincipal extends JFrame {

    private static final String VERSION = "1.0.0";

    private JPanel painelHeader;
    private JPanel painelTabs;
    private JPanel painelConteudo;
    private JPanel painelFooter;

    public TelaPrincipal() {
        // 1️⃣ instala tema FlatLaf
        try {
            FlatDarkLaf.setup();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 2️⃣ controla sessão / login
        if (SessaoService.get() == null) {
            LoginDialog loginDialog = new LoginDialog(this);
            loginDialog.setVisible(true);
            UsuarioModel logado = loginDialog.getUsuarioLogado();
            if (logado == null) {
                JOptionPane.showMessageDialog(this,
                        "Login obrigatório para acessar o sistema.",
                        "Acesso negado", JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            } else {
                SessaoService.login(logado);
            }
        }
        UsuarioModel usuario = SessaoService.get();

        // 3️⃣ frame principal
        setTitle("HoStore - ERP TCG Card Game");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // abre maximizado
        setMinimumSize(new Dimension(1024, 600)); // tamanho mínimo

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());

        // 4️⃣ cria painéis
        painelHeader = createHeaderPanel(usuario);
        painelTabs = createTabBarPanel();
        painelConteudo = new JPanel(new BorderLayout());
        painelConteudo.setBackground(UIManager.getColor("Panel.background"));

        painelConteudo.add(new DashboardPanel(), BorderLayout.CENTER);
        painelFooter = createFooterPanel();

        // 5️⃣ monta layout
        getContentPane().add(painelHeader, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.add(painelTabs, BorderLayout.NORTH);
        center.add(painelConteudo, BorderLayout.CENTER);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(painelFooter, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createHeaderPanel(UsuarioModel usuario) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        header.setBackground(UIManager.getColor("Panel.background"));

        // esquerda: título + versão
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

        // direita: usuário + status
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel lblUser = new JLabel("Usuário: " + usuario.getNome());
        lblUser.setForeground(UIManager.getColor("Label.foreground"));
        String status = DB.isConnected() ? "Online" : "Offline";
        JLabel lblStatus = new JLabel("Status: " + status);
        lblStatus.setForeground(status.equals("Online")
                ? new Color(0, 200, 0)
                : UIManager.getColor("Label.foreground"));
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
        JButton tabRelatorios = criarTab("📊 Relatórios", new DashboardPanel());
        JButton tabAjustes = criarTab("⚙️ Ajustes", new AjustesPanel());
        JButton tabBuscar = criarTab("🤖 HoRadars", null);
        JButton tabFinanceiro = criarTab("🧾 Financeiro", new PainelFinanceiro());

        tabBuscar.addActionListener(e -> JOptionPane.showMessageDialog(this, "Em desenvolvimento…"));

        tabBar.add(tabEstoque);
        tabBar.add(tabClientes);
        tabBar.add(tabVendas);
        tabBar.add(tabRelatorios);
        tabBar.add(tabAjustes);
        tabBar.add(tabBuscar);
        tabBar.add(tabFinanceiro);

        return tabBar;
    }

    private JButton criarTab(String texto, JPanel painel) {
        JButton botao = new JButton(texto);
        botao.setFocusPainted(false);
        // remove cores forçadas, deixa o tema aplicar
        // botao.setOpaque(false);
        // botao.setContentAreaFilled(false);

        botao.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        botao.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (painel != null) {
            botao.addActionListener(e -> trocarPainel(painel));
        }
        return botao;
    }

    private void trocarPainel(JPanel novoPainel) {
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

        String hora = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        JLabel lblSync = new JLabel("Última sincronização: " + hora);
        lblSync.setForeground(UIManager.getColor("Label.foreground"));
        footer.add(lblSync, BorderLayout.EAST);

        return footer;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TelaPrincipal::new);
    }
}
