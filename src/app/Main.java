package app;

import com.formdev.flatlaf.FlatLightLaf; // fallback
import dao.CartaDAO;
import model.UsuarioModel;
import service.SessaoService;
import ui.TelaPrincipal;
import ui.ajustes.dialog.LoginDialog;
import ui.ajustes.painel.CategoriaProdutoPainel;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.prefs.Preferences;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // aplica o tema salvo pelo usuário
            try {
                Preferences prefs = Preferences.userNodeForPackage(CategoriaProdutoPainel.class);
                String temaSalvo = prefs.get("temaSelecionado", "Claro (FlatLight)");
                LookAndFeel tema = CategoriaProdutoPainel.TEMAS.getOrDefault(temaSalvo, new FlatLightLaf());
                UIManager.setLookAndFeel(tema);
            } catch (Exception e) {
                System.err.println("Falha ao aplicar o tema: " + e.getMessage());
            }

            // login
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);

            UsuarioModel usuarioLogado = login.getUsuarioLogado();
            if (usuarioLogado == null) {
                System.exit(0); // cancelou
            }

            SessaoService.login(usuarioLogado);

            // carta fake (depois do login pra garantir contexto)
            new CartaDAO().inserirCartaFake();

            // abre janela principal
            new TelaPrincipal();
        });
    }
}
