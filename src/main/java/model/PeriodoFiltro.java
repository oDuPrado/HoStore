package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class PeriodoFiltro {
    private final LocalDate inicio; // inclusivo
    private final LocalDate fim;    // inclusivo
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public PeriodoFiltro(LocalDate inicio, LocalDate fim) {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("fim não pode ser antes de inicio");
        this.inicio = inicio;
        this.fim = fim;
    }

    public LocalDate getInicio() { return inicio; }
    public LocalDate getFim() { return fim; }

    public String getInicioIso() { return inicio.format(ISO); }
    public String getFimIso() { return fim.format(ISO); }

    public static PeriodoFiltro hoje() {
        LocalDate d = LocalDate.now();
        return new PeriodoFiltro(d, d);
    }

    public static PeriodoFiltro ontem() {
        LocalDate d = LocalDate.now().minusDays(1);
        return new PeriodoFiltro(d, d);
    }

    public static PeriodoFiltro mesAtual() {
        LocalDate now = LocalDate.now();
        return new PeriodoFiltro(now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
    }

    public static PeriodoFiltro mesPassado() {
        LocalDate now = LocalDate.now().minusMonths(1);
        return new PeriodoFiltro(now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
    }

    public static PeriodoFiltro ultimosDias(int dias) {
        if (dias <= 0) throw new IllegalArgumentException("dias deve ser > 0");
        LocalDate fim = LocalDate.now();
        LocalDate ini = fim.minusDays(dias - 1L);
        return new PeriodoFiltro(ini, fim);
    }

    public PeriodoFiltro periodoAnteriorMesmoTamanho() {
        long len = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1;
        LocalDate fimAnt = inicio.minusDays(1);
        LocalDate iniAnt = fimAnt.minusDays(len - 1);
        return new PeriodoFiltro(iniAnt, fimAnt);
    }

    @Override
    public String toString() {
        return getInicioIso() + " até " + getFimIso();
    }
}
