package ui.estoque.dialog;

import api.PokeTcgApi;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dialog para buscar cartas na API Pokémon TCG e selecionar uma para cadastro.
 * - Combo de Sets carregado do PokeTcgApi (cache/paginado/fallback).
 * - Busca de cartas em background (SwingWorker), sem travar UI.
 * - Ordenação na tabela habilitada.
 *
 * Visual: padronizado no UiKit (cards, tabela, botões).
 */
public class BuscarCartaDialog extends JDialog {

    private final JComboBox<ComboItem> cbSets = new JComboBox<>();
    private final JTextField tfBusca = new JTextField(18);
    private final JButton btnBuscar = UiKit.primary("🔎 Buscar");

    private final JTable tabela;
    private final DefaultTableModel model;

    private final List<Carta> resultados = new ArrayList<>();
    private Carta cartaSelecionada;

    private static final String API_URL = "https://api.pokemontcg.io/v2/cards";
    private static final String API_KEY = "8d293a2a-4949-4d04-a06c-c20672a7a12c";

    public static final double COTACAO_DOLAR = 5.0;

    // status visual
    private final JLabel lblStatus = UiKit.hint("Pronto.");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public BuscarCartaDialog(Frame owner) {
        super(owner, "Buscar Carta na API", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(860, 560));

        UiKit.applyDialogBase(this);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setOpaque(false);

        root.add(buildHeader(), BorderLayout.NORTH);

        // --- tabela ---
        model = new DefaultTableModel(new String[] {
                "Nome", "Set", "Série", "Número", "Preço (USD)", "Preço (R$)"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tabela = new JTable(model);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setAutoCreateRowSorter(true);

        UiKit.tableDefaults(tabela);

        // zebra em tudo
        DefaultTableCellRenderer zebra = UiKit.zebraRenderer();
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // alinhar colunas de dinheiro à direita (sem quebrar zebra)
        DefaultTableCellRenderer moneyRight = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(new EmptyBorder(0, 8, 0, 8));

                // aplica zebra (sem perder seleção)
                if (!isSelected) {
                    // reaproveita as cores do zebra renderer
                    Component z = zebra.getTableCellRendererComponent(table, value, false, hasFocus, row, column);
                    setBackground(z.getBackground());
                    setForeground(z.getForeground());
                }
                return c;
            }
        };
        tabela.getColumnModel().getColumn(4).setCellRenderer(moneyRight);
        tabela.getColumnModel().getColumn(5).setCellRenderer(moneyRight);

        // larguras decentes
        tabela.getColumnModel().getColumn(0).setPreferredWidth(260);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(220);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(160);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(110);

        JPanel centerCard = UiKit.card();
        centerCard.setLayout(new BorderLayout(8, 8));
        centerCard.add(buildFilters(), BorderLayout.NORTH);
        centerCard.add(UiKit.scroll(tabela), BorderLayout.CENTER);
        centerCard.add(buildStatusBar(), BorderLayout.SOUTH);

        root.add(centerCard, BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);

        setContentPane(root);

        // listeners (lógica igual)
        btnBuscar.addActionListener(e -> buscar());
        tfBusca.addActionListener(e -> buscar());

        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2)
                    selecionarAtualEFechar();
            }
        });

        // carrega sets na abertura
        carregarSets();

        pack();
        setLocationRelativeTo(owner);
    }

    private JComponent buildHeader() {
        JPanel header = UiKit.card();
        header.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        left.add(UiKit.title("🃏 Buscar Carta (Pokémon TCG API)"));
        left.add(UiKit.hint("Filtre por Set e/ou nome. Duplo clique seleciona."));
        header.add(left, BorderLayout.WEST);

        return header;
    }

    private JComponent buildFilters() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        // linha 0
        g.gridy = 0;

        g.gridx = 0;
        g.weightx = 0;
        p.add(new JLabel("Set:"), g);

        g.gridx = 1;
        g.weightx = 0.45;
        cbSets.setPrototypeDisplayValue(new ComboItem("",
                "Nome de set bem grande pra segurar layout"));
        p.add(cbSets, g);

        g.gridx = 2;
        g.weightx = 0;
        p.add(new JLabel("Nome:"), g);

        g.gridx = 3;
        g.weightx = 0.55;
        tfBusca.putClientProperty("JTextField.placeholderText", "ex: pikachu, charizard...");
        p.add(tfBusca, g);

        g.gridx = 4;
        g.weightx = 0;
        p.add(btnBuscar, g);

        return p;
    }

    private JComponent buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        lblStatus.setBorder(new EmptyBorder(4, 2, 0, 2));
        p.add(lblStatus, BorderLayout.WEST);

        JLabel hint = UiKit.hint("Enter busca | Duplo clique seleciona");
        p.add(hint, BorderLayout.EAST);

        return p;
    }

    private JComponent buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnCancel = UiKit.ghost("Cancelar");
        JButton btnOk = UiKit.primary("OK");

        btnOk.addActionListener(e -> selecionarAtualEFechar());
        btnCancel.addActionListener(e -> dispose());

        actions.add(btnCancel);
        actions.add(btnOk);

        bottom.add(actions, BorderLayout.EAST);
        return bottom;
    }

    private void setBusy(boolean busy, String msg) {
        btnBuscar.setEnabled(!busy);
        cbSets.setEnabled(!busy && cbSets.getItemCount() > 0);
        tfBusca.setEnabled(!busy);
        lblStatus.setText(msg != null ? msg : (busy ? "Carregando..." : "Pronto."));
    }

    private void selecionarAtualEFechar() {
        int row = tabela.getSelectedRow();
        if (row >= 0) {
            int modelRow = tabela.convertRowIndexToModel(row);
            if (modelRow >= 0 && modelRow < resultados.size()) {
                cartaSelecionada = resultados.get(modelRow);
                dispose();
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "Selecione uma carta.");
    }

    private void carregarSets() {
        cbSets.setEnabled(false);
        cbSets.removeAllItems();
        cbSets.addItem(new ComboItem("", "Carregando sets..."));
        setBusy(true, "Carregando sets...");

        new SwingWorker<List<SetBrief>, Void>() {
            @Override
            protected List<SetBrief> doInBackground() throws Exception {
                String json = PokeTcgApi.listarColecoes();
                if (json == null || json.isBlank())
                    return List.of();

                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonArray data = root.getAsJsonArray("data");
                if (data == null)
                    return List.of();

                java.lang.reflect.Type listType = new TypeToken<List<SetBrief>>() {
                }.getType();
                List<SetBrief> sets = new Gson().fromJson(data, listType);

                sets.sort(Comparator.comparing(s -> s.name == null ? "" : s.name.toLowerCase()));
                return sets;
            }

            @Override
            protected void done() {
                try {
                    List<SetBrief> sets = get();

                    cbSets.removeAllItems();
                    cbSets.addItem(new ComboItem("", "— Todos os Sets —"));

                    for (SetBrief s : sets) {
                        if (s != null && s.id != null && !s.id.isBlank()
                                && s.name != null && !s.name.isBlank()) {
                            cbSets.addItem(new ComboItem(s.id, s.name));
                        }
                    }

                    cbSets.setEnabled(true);
                    setBusy(false, "Sets carregados. Pronto pra buscar.");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    cbSets.removeAllItems();
                    cbSets.addItem(new ComboItem("", "— Todos os Sets —"));
                    cbSets.setEnabled(true);
                    setBusy(false, "Falha ao carregar sets.");

                    JOptionPane.showMessageDialog(BuscarCartaDialog.this,
                            "Erro ao carregar Sets:\n" + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void buscar() {
        String termo = tfBusca.getText().trim();
        ComboItem sel = (ComboItem) cbSets.getSelectedItem();
        String setId = sel != null ? sel.id : "";

        String filtro;
        if (!setId.isEmpty() && !termo.isEmpty()) {
            filtro = String.format("set.id:\"%s\" name:\"%s\"", setId, termo);
        } else if (!setId.isEmpty()) {
            filtro = String.format("set.id:\"%s\"", setId);
        } else if (!termo.isEmpty()) {
            filtro = String.format("name:\"%s\"", termo);
        } else {
            JOptionPane.showMessageDialog(this, "Informe set e/ou nome para buscar.");
            return;
        }

        setBusy(true, "Buscando cartas...");
        model.setRowCount(0);
        resultados.clear();

        new SwingWorker<List<Carta>, Void>() {
            @Override
            protected List<Carta> doInBackground() throws Exception {
                String q = URLEncoder.encode(filtro, StandardCharsets.UTF_8);
                String uri = API_URL + "?q=" + q + "&pageSize=250";

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(new URI(uri))
                        .timeout(Duration.ofSeconds(60))
                        .header("Accept", "application/json")
                        .header("User-Agent", "HoStore/1.0 (Java HttpClient)")
                        .header("X-Api-Key", API_KEY)
                        .GET()
                        .build();

                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() < 200 || res.statusCode() >= 300) {
                    throw new RuntimeException("HTTP " + res.statusCode() + " | " + safeSnippet(res.body()));
                }

                JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
                JsonArray data = root.getAsJsonArray("data");
                if (data == null)
                    return List.of();

                java.lang.reflect.Type listType = new TypeToken<List<Carta>>() {
                }.getType();
                List<Carta> cards = new Gson().fromJson(data, listType);
                return cards != null ? cards : List.of();
            }

            @Override
            protected void done() {
                try {
                    List<Carta> cards = get();

                    for (Carta c : cards) {
                        double usd = c != null ? c.getPrecoUSD() : 0.0;
                        double brl = usd * COTACAO_DOLAR;

                        model.addRow(new Object[] {
                                safe(c != null ? c.getName() : ""),
                                safe(c != null ? c.getSetName() : ""),
                                safe(c != null ? c.getSetSeries() : ""),
                                safe(c != null ? c.getNumber() : ""),
                                String.format("%.2f", usd),
                                String.format("%.2f", brl)
                        });

                        resultados.add(c);
                    }

                    if (cards.isEmpty()) {
                        setBusy(false, "Nenhuma carta encontrada.");
                        JOptionPane.showMessageDialog(BuscarCartaDialog.this, "Nenhuma carta encontrada.");
                    } else {
                        setBusy(false, "Encontradas: " + cards.size() + " cartas.");
                        if (model.getRowCount() > 0)
                            tabela.setRowSelectionInterval(0, 0);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    setBusy(false, "Erro na busca.");
                    JOptionPane.showMessageDialog(BuscarCartaDialog.this,
                            "Erro na busca:\n" + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String safeSnippet(String body) {
        if (body == null)
            return "";
        String s = body.replaceAll("\\s+", " ").trim();
        return s.length() > 240 ? s.substring(0, 240) + "..." : s;
    }

    /** Retorna a carta selecionada (ou null se cancelado) */
    public Carta getCartaSelecionada() {
        return cartaSelecionada;
    }

    /** combo simples id/label */
    private static class ComboItem {
        final String id, label;

        ComboItem(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** desserialização de Set (brief) */
    public static class SetBrief {
        String id;
        String name;
    }

    /** desserialização de Carta via Gson */
    public static class Carta {
        private String name, number;
        private SetObject set;
        private Tcgplayer tcgplayer;

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }

        public String getSetName() {
            return set != null ? set.name : "";
        }

        public String getSetId() {
            return set != null ? set.id : "";
        }

        public String getSetSeries() {
            return set != null ? set.series : "";
        }

        public double getPrecoUSD() {
            try {
                if (tcgplayer != null && tcgplayer.prices != null) {
                    PriceInfo pi = tcgplayer.prices.normal;
                    if (pi != null && pi.market != null)
                        return pi.market;
                }
            } catch (Exception ignored) {
            }
            return 0.0;
        }

        private static class SetObject {
            String id, name, series;
        }

        private static class Tcgplayer {
            Prices prices;
        }

        private static class Prices {
            PriceInfo normal;
        }

        private static class PriceInfo {
            Double market;
        }
    }
}
