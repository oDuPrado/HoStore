package ui.ajustes;

import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Dialogs de configuração do sistema
import ui.ajustes.dialog.ConfigLojaDialog;
import ui.ajustes.dialog.ConfigImpressaoDialog;
import ui.ajustes.dialog.ConfigSistemaDialog;

// CRUDs e painéis
import ui.ajustes.painel.UsuarioPainel;
import ui.ajustes.painel.FornecedorPainel;
import ui.ajustes.painel.CategoriaProdutoPainel;
import ui.ajustes.painel.NcmPainel;
import ui.ajustes.painel.PlanoContaPainel;
import ui.ajustes.painel.PromocaoPainel;
import ui.ajustes.painel.ClienteVipPainel;

import service.SessaoService;
import ui.ajustes.dialog.CartaAtributosDialog;
import ui.ajustes.dialog.TaxaCartaoDialog;

public class AjustesPanel extends JPanel {

    public AjustesPanel() {
        setLayout(new BorderLayout(10, 10));
        UiKit.applyPanelBase(this);

        // ===== Header =====
        JPanel headerCard = UiKit.card();
        headerCard.setLayout(new BorderLayout(8, 4));

        JLabel titulo = UiKit.title("⚙️ Ajustes do Sistema");
        JLabel subtitulo = UiKit.hint("Configurações gerais, fiscal, impressão, usuários e cadastros.");

        JPanel headerText = new JPanel(new GridLayout(0, 1, 0, 2));
        headerText.setOpaque(false);
        headerText.add(titulo);
        headerText.add(subtitulo);

        headerCard.add(headerText, BorderLayout.WEST);

        add(headerCard, BorderLayout.NORTH);

        // ===== Grid de ações =====
        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(2, 2, 2, 2));

        // ==== CONFIGURAÇÕES DO SISTEMA ====
        grid.add(tile("🛍 Dados da Loja", "Cadastro fiscal, endereço, regime tributário",
                () -> new ConfigLojaDialog(null).setVisible(true)));

        grid.add(tile("🧾 NFC-e e Emissão", "Certificado A1, CSC e modo de emissão",
                () -> new ui.ajustes.dialog.ConfigNfceDialog(null).setVisible(true)));

        grid.add(tile("🖨 Impressão e PDF", "Impressoras, cupom/PDF, preferências",
                () -> new ConfigImpressaoDialog(null).setVisible(true)));

        grid.add(tile("🧾 Plano de Contas", "Categorias financeiras e estrutura contábil",
                () -> new PlanoContaPainel().abrir()));

        grid.add(tile("🗄 Backup e Sistema", "Backup, preferências e parâmetros do sistema",
                () -> new ConfigSistemaDialog(null).setVisible(true)));

        if (SessaoService.isAdmin()) {
            grid.add(tile("👥 Usuários e Permissões", "Acesso, permissões e administração",
                    () -> new UsuarioPainel().abrir()));
        } else {
            // mantém a grade “cheia” e evita buraco visual quando não-admin
            grid.add(tileDisabled("👥 Usuários e Permissões", "Disponível apenas para admin"));
        }

        // ==== CADASTROS GERAIS ====
        grid.add(tile("🚚 Fornecedores", "Cadastro e gestão de fornecedores",
                () -> new FornecedorPainel().abrir()));

        grid.add(tile("🎨 Categorias / Interface", "Categorias e organização visual",
                () -> new CategoriaProdutoPainel().abrir()));

        grid.add(tile("📑 Configuração Fiscal", "NCM/CFOP/CSOSN e cadastros fiscais",
                () -> new NcmPainel().abrir()));

        grid.add(tile("💳 Taxas do Cartão", "Taxas por bandeira, parcelas e mês",
                () -> new TaxaCartaoDialog(null).setVisible(true)));

        grid.add(tile("🃏 Atributos da Carta", "Tipos, raridades e atributos do catálogo",
                () -> new CartaAtributosDialog(null).setVisible(true)));

        grid.add(tile("🏷 Promoções e Descontos", "Regras de preço e promoções",
                () -> new PromocaoPainel().abrir()));

        grid.add(tile("⭐ Clientes VIP", "Regras e benefícios para clientes especiais",
                () -> new ClienteVipPainel().abrir()));

        // ===== Scroll do grid =====
        JScrollPane sp = UiKit.scroll(grid);
        sp.setBorder(null); // o card já “molda” o visual
        add(sp, BorderLayout.CENTER);
    }

    private JComponent tile(String title, String desc, Runnable action) {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel t = new JLabel(title);
        t.putClientProperty("FlatLaf.style", "font: +1;");

        JLabel d = UiKit.hint(desc);

        JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
        text.setOpaque(false);
        text.add(t);
        text.add(d);

        JButton abrir = UiKit.primary("Abrir");
        abrir.addActionListener(e -> action.run());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(abrir);

        card.add(text, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        // Clicar no card também abre
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                action.run();
            }
        });

        return card;
    }

    private JComponent tileDisabled(String title, String desc) {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(8, 8));

        JLabel t = new JLabel(title);
        t.putClientProperty("FlatLaf.style", "font: +1;");

        JLabel d = UiKit.hint(desc);

        JButton abrir = UiKit.ghost("Bloqueado");
        abrir.setEnabled(false);

        JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
        text.setOpaque(false);
        text.add(t);
        text.add(d);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(abrir);

        card.setEnabled(false);
        card.add(text, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }
}
