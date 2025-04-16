package app;

import dao.CartaDAO;
import ui.TelaPrincipal;

public class Main {
    public static void main(String[] args) {
        // inserir carta fake para teste
        CartaDAO dao = new CartaDAO();
        dao.inserirCartaFake();

        // abrir app
        new TelaPrincipal();
    }
}
