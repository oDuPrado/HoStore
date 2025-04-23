package service;

import model.UsuarioModel;

public class SessaoService {
    private static UsuarioModel usuarioAtual;

    public static void login(UsuarioModel u) {
        usuarioAtual = u;
    }

    public static UsuarioModel get() {
        return usuarioAtual;
    }

    public static void logout() {
        usuarioAtual = null;
    }

    public static boolean isAdmin() {
        return usuarioAtual != null && "Admin".equalsIgnoreCase(usuarioAtual.getTipo());
    }
}
