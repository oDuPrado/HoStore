package ui;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Calendar;
import java.util.Date;
import com.toedter.calendar.JCalendar; // Certifique-se de adicionar o jar do JCalendar ao projeto

public class DashboardPanel extends JPanel {

    // Botões que exibirão as datas selecionadas
    private JButton btnDataInicio;
    private JButton btnDataFim;

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245)); // fundo cinza claro

        // Título centralizado com fonte que suporta emojis
        JLabel titulo = new JLabel("Dashboard HoStore", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI Emoji", Font.BOLD, 24));
        titulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        // Cria o painel de filtro de datas customizado
        JPanel filtroDataPanel = criarFiltroData();

        // Agrupa o título e o filtro em um Box vertical
        Box boxTopo = Box.createVerticalBox();
        boxTopo.add(titulo);
        boxTopo.add(filtroDataPanel);
        add(boxTopo, BorderLayout.NORTH);

        // Painel com as métricas em grid 2x2
        JPanel painelMetricas = new JPanel(new GridLayout(2, 2, 20, 20));
        painelMetricas.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        painelMetricas.setBackground(getBackground());

        // Criação dos cards de métricas (os valores estão fixos por enquanto)
        painelMetricas.add(criarCard("📦 Cartas em Estoque", "184"));
        painelMetricas.add(criarCard("🧍 Clientes", "32"));
        painelMetricas.add(criarCard("📈 Vendas", "125"));  // Agora esse card só exibe o título e valor.
        painelMetricas.add(criarCard("💰 Valor Estimado", "R$ 2.340,00"));
        add(painelMetricas, BorderLayout.CENTER);

        // Exibe a última sincronização de forma discreta no rodapé
        JLabel lblSincronizacao = new JLabel("🕒 Última sincronização: Hoje, 09:30", SwingConstants.RIGHT);
        lblSincronizacao.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        lblSincronizacao.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 20));
        add(lblSincronizacao, BorderLayout.SOUTH);
    }

    /**
     * Cria o painel de filtro de datas com botões que, ao serem clicados,
     * abrem um calendário popup animado para seleção.
     */
    private JPanel criarFiltroData() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(getBackground());
        
        JLabel lblFiltro = new JLabel("Filtrar por Intervalo:");
        lblFiltro.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        
        // Define a data inicial: primeiro dia do mês atual
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date dataInicioPadrao = cal.getTime();
        
        // Data final: data atual
        Date dataFimPadrao = new Date();
        
        btnDataInicio = criarBotaoData("De: " + dataToString(dataInicioPadrao));
        btnDataFim = criarBotaoData("Até: " + dataToString(dataFimPadrao));
        
        // Ao clicar, abre o calendário popup animado para selecionar a data
        btnDataInicio.addActionListener(e -> {
            Date selecionada = mostrarCalendarioPopup(btnDataInicio, dataInicioPadrao);
            if (selecionada != null) {
                btnDataInicio.setText("De: " + dataToString(selecionada));
            }
        });
        btnDataFim.addActionListener(e -> {
            Date selecionada = mostrarCalendarioPopup(btnDataFim, dataFimPadrao);
            if (selecionada != null) {
                btnDataFim.setText("Até: " + dataToString(selecionada));
            }
        });
        
        panel.add(lblFiltro);
        panel.add(btnDataInicio);
        panel.add(btnDataFim);
        
        return panel;
    }
    
    // Cria um botão estilizado para exibir uma data
    private JButton criarBotaoData(String texto) {
        JButton botao = new JButton(texto);
        botao.setFocusPainted(false);
        botao.setBackground(new Color(60, 63, 65));
        botao.setForeground(Color.WHITE);
        botao.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        botao.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Efeito hover
        botao.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                botao.setBackground(new Color(80, 83, 85));
            }
            public void mouseExited(MouseEvent evt) {
                botao.setBackground(new Color(60, 63, 65));
            }
        });
        return botao;
    }
    
    // Converte uma data para string no formato dd/MM/yyyy
    private String dataToString(Date data) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(data);
    }
    
    /**
     * Exibe um popup modal com um JCalendar que aparece com um efeito de fade-in.
     * @param parent O componente a partir do qual o popup será centralizado.
     * @param dataInicial A data inicial para seleção.
     * @return A data selecionada ou null se cancelado.
     */
    private Date mostrarCalendarioPopup(Component parent, Date dataInicial) {
    // Cria o JDialog modal, sem decoração
    JDialog dialog = new JDialog((Frame) null, true);
    dialog.setUndecorated(true);
    dialog.setLayout(new BorderLayout());
    dialog.setSize(320, 340);
    dialog.setLocationRelativeTo(parent);
    dialog.setBackground(new Color(0, 0, 0, 0)); // fundo transparente para animação

    // Painel interno com padding e fundo branco
    JPanel painelPopup = new JPanel(new BorderLayout());
    painelPopup.setBackground(Color.WHITE);
    painelPopup.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
    ));

    // Label que mostra a data selecionada em tempo real
    JLabel lblPreviewData = new JLabel("Selecionada: " + dataToString(dataInicial), SwingConstants.CENTER);
    lblPreviewData.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
    lblPreviewData.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
    painelPopup.add(lblPreviewData, BorderLayout.NORTH);

    // Calendário
    JCalendar calendar = new JCalendar();
    calendar.setDate(dataInicial);
    calendar.setMaxSelectableDate(new Date()); // impede datas futuras
    painelPopup.add(calendar, BorderLayout.CENTER);

    // Botão de salvar
    JButton btnSalvar = new JButton("Salvar Data");
    btnSalvar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    btnSalvar.setFocusPainted(false);
    btnSalvar.setBackground(new Color(60, 63, 65));
    btnSalvar.setForeground(Color.WHITE);
    btnSalvar.setCursor(new Cursor(Cursor.HAND_CURSOR));

    btnSalvar.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            btnSalvar.setBackground(new Color(80, 83, 85));
        }
        public void mouseExited(MouseEvent e) {
            btnSalvar.setBackground(new Color(60, 63, 65));
        }
    });

    painelPopup.add(btnSalvar, BorderLayout.SOUTH);
    dialog.add(painelPopup, BorderLayout.CENTER);

    // Atualiza o label conforme o usuário mexe no calendário
    calendar.addPropertyChangeListener("calendar", evt -> {
        Date dataEscolhida = calendar.getDate();
        lblPreviewData.setText("Selecionada: " + dataToString(dataEscolhida));
    });

    // Fade-in do popup
    final float[] opacity = {0f};
    Timer fadeIn = new Timer(30, null);
    fadeIn.addActionListener(e -> {
        opacity[0] += 0.08f;
        if (opacity[0] >= 1f) {
            opacity[0] = 1f;
            fadeIn.stop();
        }
        dialog.setOpacity(opacity[0]);
    });
    dialog.setOpacity(0f);
    fadeIn.start();

    // Variável para guardar a data escolhida
    final Date[] dataSelecionada = {null};

    // Ao clicar em salvar, armazena a data e fecha
    btnSalvar.addActionListener(e -> {
        dataSelecionada[0] = calendar.getDate();
        dialog.dispose();
    });

    dialog.setVisible(true);
    return dataSelecionada[0]; // retorna a data escolhida ou null
}
    
    // Método para criar um card simples com título e valor centralizados
    private JPanel criarCard(String titulo, String valor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        JLabel lblValor = new JLabel(valor, SwingConstants.CENTER);
        lblValor.setFont(new Font("Segoe UI Emoji", Font.BOLD, 24));
        card.add(lblTitulo, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }
}
