package ui.ajustes.painel;

public class UsuarioPainel extends AbstractCrudPainel {
    @Override
    protected String getTitulo() {
        return "Usuários do Sistema";
    }

    @Override
    protected String[] getColunas() {
        return new String[] {
            "Nome", "Usuário", "Tipo", "Status"
        };
    }

    @Override
    protected Object[] getValoresFake() {
        return new Object[] { "Admin", "admin123", "Administrador", "Ativo" };
    }
}
