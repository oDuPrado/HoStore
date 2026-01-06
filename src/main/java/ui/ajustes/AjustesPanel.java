package ui.ajustes;

import javax.swing.*;
import java.awt.*;

// Dialogs de configuração do sistema
import ui.ajustes.dialog.ConfigLojaDialog;
import ui.ajustes.dialog.ConfigImpressaoDialog;
import ui.ajustes.dialog.ConfigFinanceiroDialog;
import ui.ajustes.dialog.ConfigSistemaDialog;

// CRUDs de cadastros gerais
import ui.ajustes.painel.UsuarioPainel;
import ui.ajustes.painel.FornecedorPainel;
import ui.ajustes.painel.CategoriaProdutoPainel;
import ui.ajustes.painel.NcmPainel;
import ui.ajustes.painel.CondicaoPainel;
import ui.ajustes.painel.IdiomaPainel;
import ui.ajustes.painel.PlanoContaPainel;
import ui.ajustes.painel.TipoCartaPainel;
import ui.ajustes.painel.PromocaoPainel;
import ui.ajustes.painel.ClienteVipPainel;
import service.SessaoService;
import ui.ajustes.dialog.CartaAtributosDialog;
import ui.ajustes.dialog.TaxaCartaoDialog;

public class AjustesPanel extends JPanel {

    public AjustesPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(43, 43, 43)); // fundo dark

        JLabel titulo = new JLabel("⚙️ Ajustes do Sistema", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI Emoji", Font.BOLD, 22));
        titulo.setForeground(Color.WHITE);
        titulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titulo, BorderLayout.NORTH);

        JPanel container = new JPanel(new GridLayout(0, 2, 16, 16));
        container.setBorder(BorderFactory.createEmptyBorder(16, 32, 32, 32));
        container.setBackground(new Color(43, 43, 43));

        // ==== CONFIGURAÇÕES DO SISTEMA ====
        container.add(criarBotao("🛍 Dados da Loja", () -> new ConfigLojaDialog(null).setVisible(true)));
        container.add(criarBotao("🖨 Impressão e PDF", () -> new ConfigImpressaoDialog(null).setVisible(true)));
        container.add(criarBotao("🧾 Plano de Contas", () -> new PlanoContaPainel().abrir()));
        container.add(criarBotao("🗄 Backup e Sistema", () -> new ConfigSistemaDialog(null).setVisible(true)));
        if (SessaoService.isAdmin()) {
            container.add(criarBotao("👥 Usuários e Permissões", () -> new UsuarioPainel().abrir()));
        }

        // ==== CADASTROS GERAIS ====
        container.add(criarBotao("🚚 Fornecedores", () -> {
            // abre o painel de fornecedores
            new ui.ajustes.painel.FornecedorPainel().abrir();
        }));
        container.add(criarBotao("📦 Temas de interface", () -> new CategoriaProdutoPainel().abrir()));
        container.add(criarBotao("📑 Configuracao Fiscal", () -> new NcmPainel().abrir()));
        container.add(criarBotao("💳 Taxas do Cartão", () -> new TaxaCartaoDialog(null).setVisible(true)));
        container.add(criarBotao("🃏 Atributos da Carta", () -> new CartaAtributosDialog(null).setVisible(true)));
        container.add(criarBotao("🏷 Promoções e Descontos", () -> new PromocaoPainel().abrir()));
        container.add(criarBotao("⭐ Clientes VIP", () -> new ClienteVipPainel().abrir()));

        add(new JScrollPane(container), BorderLayout.CENTER);
    }

    private JButton criarBotao(String texto, Runnable acao) {
        JButton btn = new JButton(texto);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(60, 63, 65));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 40));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        btn.addActionListener(e -> acao.run());

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(75, 78, 80));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(60, 63, 65));
            }
        });

        return btn;
    }
}
