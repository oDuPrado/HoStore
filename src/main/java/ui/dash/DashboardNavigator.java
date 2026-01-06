package ui.dash;

public interface DashboardNavigator {
    // Se no futuro você quiser navegar pra telas específicas (estoque/financeiro/fiscal),
    // implementa aqui. Por enquanto o dashboard abre diálogos com drill-down.
    void navegarPara(String destino);
}
