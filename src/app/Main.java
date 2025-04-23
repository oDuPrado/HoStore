package app;

import dao.CartaDAO;
import model.UsuarioModel;
import service.SessaoService;
import ui.TelaPrincipal;
import ui.ajustes.dialog.LoginDialog;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
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
