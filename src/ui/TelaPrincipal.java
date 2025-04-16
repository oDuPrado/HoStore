package ui;

import ui.PainelClientes;
import javax.swing.*;
import java.awt.*;

public class TelaPrincipal extends JFrame {

    private JPanel painelConteudo;

    // Cria um botão estilizado
private JButton criarBotao(String texto) {
    JButton botao = new JButton(texto);
    botao.setFocusPainted(false);
    botao.setBackground(new Color(60, 63, 65)); // cinza escuro
    botao.setForeground(Color.WHITE);
    botao.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
    botao.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
    
    // Muda a cor quando o mouse passa por cima
    botao.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseEntered(java.awt.event.MouseEvent evt) {
            botao.setBackground(new Color(80, 83, 85));
        }

        public void mouseExited(java.awt.event.MouseEvent evt) {
            botao.setBackground(new Color(60, 63, 65));
        }
    });

    return botao;
}

private void trocarPainel(JPanel novoPainel) {
    painelConteudo.removeAll();
    painelConteudo.add(novoPainel, BorderLayout.CENTER);
    painelConteudo.revalidate();
    painelConteudo.repaint();
}

    public TelaPrincipal() {
        setTitle("HoStore - ERP Pokémon TCG");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centraliza

        // Layout principal (menu lateral + conteúdo)
        getContentPane().setLayout(new BorderLayout());

        // Painel do menu lateral
        JPanel menuLateral = new JPanel();
        menuLateral.setLayout(new GridLayout(6, 1, 0, 10)); // 6 botões empilhados
        menuLateral.setBackground(new Color(40, 40, 40));
        menuLateral.setPreferredSize(new Dimension(180, 600));

        JButton btnEstoque = criarBotao("📦 Estoque");
        JButton btnClientes = criarBotao("🧍 Clientes");
        btnClientes.addActionListener(e -> trocarPainel(new PainelClientes()));
        JButton btnVendas = criarBotao("💰 Vendas");
        JButton btnRelatorios = criarBotao("📊 Relatórios");
        JButton btnConfig = criarBotao("⚙️ Ajustes");
        JButton btnBuscarCartas = criarBotao("🤖 Buscar Cartas"); // nome novo aqui

        menuLateral.add(btnEstoque);
        menuLateral.add(btnClientes);
        menuLateral.add(btnVendas);
        menuLateral.add(btnRelatorios);
        menuLateral.add(btnConfig);
        menuLateral.add(btnBuscarCartas);

        // Painel de conteúdo principal
        painelConteudo = new JPanel();
        painelConteudo.setLayout(new BorderLayout());
        painelConteudo.add(new DashboardPanel(), BorderLayout.CENTER);
        

        getContentPane().add(menuLateral, BorderLayout.WEST);
        getContentPane().add(painelConteudo, BorderLayout.CENTER);

        setVisible(true);
    }
}
