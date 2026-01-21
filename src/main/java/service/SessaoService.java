package service;

import model.UsuarioModel;
import util.LogService;

public class SessaoService {
    private static UsuarioModel usuarioAtual;

    public static void login(UsuarioModel u) {
        usuarioAtual = u;
        String user = (u != null && u.getUsuario() != null) ? u.getUsuario() : "desconhecido";
        LogService.audit("LOGIN", "usuario", (u != null ? u.getId() : null), "login ok user=" + user);
    }

    public static UsuarioModel get() {
        return usuarioAtual;
    }

    public static void logout() {
        UsuarioModel u = usuarioAtual;
        usuarioAtual = null;
        String user = (u != null && u.getUsuario() != null) ? u.getUsuario() : "desconhecido";
        LogService.audit("LOGOUT", "usuario", (u != null ? u.getId() : null), "logout user=" + user);
    }

    public static boolean isAdmin() {
        return usuarioAtual != null && "Admin".equalsIgnoreCase(usuarioAtual.getTipo());
    }
}
