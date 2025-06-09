package ui.dash.painel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.toedter.calendar.JCalendar;

/**
 * DashboardPanel redesenhado para respeitar temas FlatLaf (claro/escuro).
 * - Usa cores do UIManager em vez de valores fixos.
 * - Remove backgrounds brancos hardcoded.
 * - Reliquia no estilo padrão de botões e painéis.
 */
public class DashboardPanel extends JPanel {
    private JButton btnDataInicio;
    private JButton btnDataFim;

    public DashboardPanel() {
        // usa o layout principal e cor de fundo do tema
        super(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        // ─── TÍTULO ───────────────────────────────────────────────────────
        JLabel lblTitle = new JLabel("Dashboard HoStore", SwingConstants.CENTER);
        lblTitle.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 24f));
        lblTitle.setForeground(UIManager.getColor("Label.foreground"));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);

        // ─── FILTRO DE DATAS ───────────────────────────────────────────────
        JPanel filtroData = criarFiltroData();
        add(filtroData, BorderLayout.NORTH);

        // ─── MÉTRICAS (grid 2x2) ────────────────────────────────────────────
        JPanel painelMetricas = new JPanel(new GridLayout(2, 2, 20, 20));
        painelMetricas.setOpaque(false); // deixa transparência para herdar fundo
        painelMetricas.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        painelMetricas.add(criarCard("📦 Cartas em Estoque", "184"));
        painelMetricas.add(criarCard("🧍 Clientes",          "32"));
        painelMetricas.add(criarCard("📈 Vendas",           "125"));
        painelMetricas.add(criarCard("💰 Valor Estimado",    "R$ 2.340,00"));
        add(painelMetricas, BorderLayout.CENTER);

        // ─── RODAPÉ ─────────────────────────────────────────────────────────
        String hora = new SimpleDateFormat("HH:mm").format(new Date());
        JLabel lblSync = new JLabel("🕒 Última sincronização: Hoje, " + hora, SwingConstants.RIGHT);
        lblSync.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 12f));
        lblSync.setForeground(UIManager.getColor("Label.foreground"));
        lblSync.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 20));
        add(lblSync, BorderLayout.SOUTH);
    }

    private JPanel criarFiltroData() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setOpaque(false);

        JLabel lblFiltro = new JLabel("Filtrar por Intervalo:");
        lblFiltro.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 14f));
        lblFiltro.setForeground(UIManager.getColor("Label.foreground"));
        panel.add(lblFiltro);

        // data inicial: primeiro dia do mês
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date dtInicio = cal.getTime();
        Date dtFim    = new Date();

        btnDataInicio = criarBotaoData("De: " + format(dtInicio));
        btnDataFim    = criarBotaoData("Até: " + format(dtFim));

        btnDataInicio.addActionListener(e -> {
            Date sel = mostrarCalendarioPopup(btnDataInicio, dtInicio);
            if (sel != null) btnDataInicio.setText("De: " + format(sel));
        });
        btnDataFim.addActionListener(e -> {
            Date sel = mostrarCalendarioPopup(btnDataFim, dtFim);
            if (sel != null) btnDataFim.setText("Até: " + format(sel));
        });

        panel.add(btnDataInicio);
        panel.add(btnDataFim);
        return panel;
    }

    private JButton criarBotaoData(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(UIManager.getFont("Button.font").deriveFont(Font.PLAIN, 14f));
        // herda cores do tema
        btn.setForeground(UIManager.getColor("Button.foreground"));
        btn.setBackground(UIManager.getColor("Button.background"));
        btn.setFocusPainted(false);
        return btn;
    }

    private String format(Date d) {
        return new SimpleDateFormat("dd/MM/yyyy").format(d);
    }

    private Date mostrarCalendarioPopup(Component parent, Date dataInicial) {
        // cria diálogo modal sem decoração
        JDialog dlg = new JDialog((Frame) null, true);
        dlg.setUndecorated(true);
        dlg.setBackground(new Color(0,0,0,0));
        dlg.setLayout(new BorderLayout());
        dlg.setSize(320, 340);
        dlg.setLocationRelativeTo(parent);

        // painel interno com tema
        JPanel pane = new JPanel(new BorderLayout());
        pane.setBackground(UIManager.getColor("Panel.background"));
        pane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(10,10,10,10)
        ));

        JLabel preview = new JLabel("Selecionada: " + format(dataInicial), SwingConstants.CENTER);
        preview.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 14f));
        preview.setForeground(UIManager.getColor("Label.foreground"));
        pane.add(preview, BorderLayout.NORTH);

        JCalendar calendar = new JCalendar();
        calendar.setDate(dataInicial);
        calendar.setMaxSelectableDate(new Date());
        pane.add(calendar, BorderLayout.CENTER);

        JButton btnSalvar = new JButton("Salvar Data");
        btnSalvar.setFont(UIManager.getFont("Button.font").deriveFont(Font.PLAIN, 13f));
        btnSalvar.setForeground(UIManager.getColor("Button.foreground"));
        btnSalvar.setBackground(UIManager.getColor("Button.background"));
        btnSalvar.setFocusPainted(false);
        pane.add(btnSalvar, BorderLayout.SOUTH);

        calendar.addPropertyChangeListener("calendar", evt -> {
            preview.setText("Selecionada: " + format(calendar.getDate()));
        });

        dlg.add(pane, BorderLayout.CENTER);

        final Date[] selecionada = {null};
        btnSalvar.addActionListener(e -> {
            selecionada[0] = calendar.getDate();
            dlg.dispose();
        });

        // fade-in
        new Timer(30, new ActionListener() {
            float op = 0;
            public void actionPerformed(ActionEvent e) {
                op += 0.1f;
                if (op >= 1f) { op = 1f; ((Timer)e.getSource()).stop(); }
                dlg.setOpacity(op);
            }
        }).start();

        dlg.setOpacity(0f);
        dlg.setVisible(true);
        return selecionada[0];
    }

    private JPanel criarCard(String titulo, String valor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(UIManager.getColor("Panel.background"));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(20,20,20,20)
        ));

        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 14f));
        lblTit.setForeground(UIManager.getColor("Label.foreground"));

        JLabel lblVal = new JLabel(valor, SwingConstants.CENTER);
        lblVal.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 24f));
        lblVal.setForeground(UIManager.getColor("Label.foreground"));

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);
        return card;
    }
}
