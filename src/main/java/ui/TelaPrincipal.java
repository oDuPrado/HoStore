package ui;

import javax.swing.*;
import java.awt.*;

import ui.ajustes.AjustesPanel;
import ui.clientes.painel.PainelClientes;
import ui.dash.painel.DashboardPanel;
import ui.estoque.painel.PainelEstoque;
import ui.venda.painel.PainelVendas;
// ➡️ Novo import para o módulo Financeiro
import ui.financeiro.painel.PainelFinanceiro;

public class TelaPrincipal extends JFrame {

    private JPanel painelConteudo;

    // Cria um botão estilizado usado em todo o menu
    private JButton criarBotao(String texto) {
        JButton botao = new JButton(texto);
        botao.setFocusPainted(false);
        botao.setBackground(new Color(60, 63, 65)); // cinza escuro
        botao.setForeground(Color.WHITE);
        botao.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        botao.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Efeito hover
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

    // Método para trocar o painel de conteúdo
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
        setLocationRelativeTo(null); // Centraliza na tela

        getContentPane().setLayout(new BorderLayout());

        // ─── Menu lateral ─────────────────────────────────────────────────────────
        JPanel menuLateral = new JPanel();
        // Alterado para 7 linhas (antes era 6)
        menuLateral.setLayout(new GridLayout(7, 1, 0, 10));
        menuLateral.setBackground(new Color(40, 40, 40));
        menuLateral.setPreferredSize(new Dimension(180, 600));

        // Botões existentes
        JButton btnEstoque       = criarBotao("📦 Estoque");
        JButton btnClientes      = criarBotao("🧍 Clientes");
        JButton btnVendas        = criarBotao("💰 Vendas");
        JButton btnRelatorios    = criarBotao("📊 Relatórios");
        JButton btnConfig        = criarBotao("⚙️ Ajustes");
        JButton btnBuscarCartas  = criarBotao("🤖 Buscar Cartas");
        // ➡️ Novo botão Financeiro
        JButton btnFinanceiro    = criarBotao("🧾 Financeiro");

        // Define ação de cada botão
        btnEstoque      .addActionListener(e -> trocarPainel(new PainelEstoque()));
        btnClientes     .addActionListener(e -> trocarPainel(new PainelClientes()));
        btnVendas       .addActionListener(e -> trocarPainel(new PainelVendas(this)));
        btnRelatorios   .addActionListener(e -> trocarPainel(new DashboardPanel())); // ajuste conforme desejar
        btnConfig       .addActionListener(e -> trocarPainel(new AjustesPanel()));
        btnBuscarCartas .addActionListener(e -> {/* abrir dialog de busca de cartas */});
        btnFinanceiro   .addActionListener(e -> trocarPainel(new PainelFinanceiro()));

        // Adiciona ao menu
        menuLateral.add(btnEstoque);
        menuLateral.add(btnClientes);
        menuLateral.add(btnVendas);
        menuLateral.add(btnRelatorios);
        menuLateral.add(btnConfig);
        menuLateral.add(btnBuscarCartas);
        menuLateral.add(btnFinanceiro);

        // ─── Painel de conteúdo ───────────────────────────────────────────────────
        painelConteudo = new JPanel(new BorderLayout());
        painelConteudo.add(new DashboardPanel(), BorderLayout.CENTER);

        // Adiciona tudo à janela principal
        getContentPane().add(menuLateral, BorderLayout.WEST);
        getContentPane().add(painelConteudo, BorderLayout.CENTER);

        setVisible(true);
    }
}
