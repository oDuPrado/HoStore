package model;

public class ComparativoModel {
    public double valorAtual;
    public double valorAnterior;
    public double deltaAbs;
    public double deltaPct; // ex: 0.12 = 12%

    public static ComparativoModel of(double atual, double anterior) {
        ComparativoModel c = new ComparativoModel();
        c.valorAtual = atual;
        c.valorAnterior = anterior;
        c.deltaAbs = atual - anterior;
        if (Math.abs(anterior) < 0.0000001) c.deltaPct = (Math.abs(atual) < 0.0000001) ? 0 : 1;
        else c.deltaPct = (atual - anterior) / anterior;
        return c;
    }
}
