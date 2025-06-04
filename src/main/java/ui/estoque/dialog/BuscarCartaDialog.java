package ui.estoque.dialog;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Dialog para buscar cartas na API Pokémon TCG e selecionar uma para cadastro.
 * Filtros por Set, busca por nome, e ordenação de colunas ativados.
 */
public class BuscarCartaDialog extends JDialog {
    private final JComboBox<ComboItem> cbSets       = new JComboBox<>();
    private final JTextField          tfBusca      = new JTextField(15);
    private final JButton             btnBuscar    = new JButton("Buscar");
    private final JTable              tabela;
    private final DefaultTableModel   model;
    private final List<Carta>         resultados   = new ArrayList<>();
    private Carta                      cartaSelecionada;

    private static final String API_URL         = "https://api.pokemontcg.io/v2/cards";
    private static final String API_KEY         = "8d293a2a-4949-4d04-a06c-c20672a7a12c";
    public  static final double   COTACAO_DOLAR  = 5.0;

    public BuscarCartaDialog(Frame owner) {
        super(owner, "Buscar Carta na API", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8,8));
        setPreferredSize(new Dimension(700, 450));

        // --- topo: filtro de Set + busca por nome ---
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        pnlFiltros.add(new JLabel("Set:"));
        pnlFiltros.add(cbSets);
        pnlFiltros.add(new JLabel("Nome:"));
        pnlFiltros.add(tfBusca);
        pnlFiltros.add(btnBuscar);
        add(pnlFiltros, BorderLayout.NORTH);

        // carrega lista de sets
        carregarSets();

        // --- centro: tabela de resultados ---
        model = new DefaultTableModel(new String[]{
            "Nome", "Set", "Número", "Preço (USD)", "Preço (R$)"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(model);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setAutoCreateRowSorter(true);  // habilita ordenação clicando no cabeçalho
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        // --- rodapé: OK / Cancelar ---
        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8,8));
        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancelar");
        pnlBotoes.add(btnCancel);
        pnlBotoes.add(btnOk);
        add(pnlBotoes, BorderLayout.SOUTH);

        // listeners
        btnBuscar.addActionListener(e -> buscar());
        btnOk.addActionListener(e -> {
            int row = tabela.getSelectedRow();
            if (row >= 0) {
                // converter índice de view para model se estiver ordenado
                int modelRow = tabela.convertRowIndexToModel(row);
                cartaSelecionada = resultados.get(modelRow);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Selecione uma carta.");
            }
        });
        btnCancel.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(owner);
    }

    private void carregarSets() {
        SwingUtilities.invokeLater(() -> {
            try {
                String uri = "https://api.pokemontcg.io/v2/sets?pageSize=500";
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .header("X-Api-Key", API_KEY)
                    .GET().build();
                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                JsonArray data = JsonParser.parseString(res.body())
                                           .getAsJsonObject()
                                           .getAsJsonArray("data");

                // usa TypeToken e fully-qualified Type
                java.lang.reflect.Type listType =
                    new TypeToken<List<SetBrief>>() {}.getType();
                List<SetBrief> sets = new Gson().fromJson(data.toString(), listType);

                Collections.sort(sets, Comparator.comparing(s -> s.name));
                cbSets.addItem(new ComboItem("", "— Todos os Sets —"));
                for (SetBrief s : sets) {
                    cbSets.addItem(new ComboItem(s.id, s.name));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao carregar Sets: "+ex.getMessage());
            }
        });
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

        btnBuscar.setEnabled(false);
        model.setRowCount(0);
        resultados.clear();

        SwingUtilities.invokeLater(() -> {
            try {
                String q = URLEncoder.encode(filtro, StandardCharsets.UTF_8);
                String uri = API_URL + "?q=" + q + "&pageSize=250";
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .header("X-Api-Key", API_KEY)
                    .GET().build();
                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                JsonArray data = JsonParser.parseString(res.body())
                                           .getAsJsonObject()
                                           .getAsJsonArray("data");

                // desserializa lista de Carta
                java.lang.reflect.Type listType =
                    new TypeToken<List<Carta>>() {}.getType();
                List<Carta> cards = new Gson().fromJson(data.toString(), listType);

                for (Carta c : cards) {
                    double usd = c.getPrecoUSD();
                    double brl = usd * COTACAO_DOLAR;
                    model.addRow(new Object[]{
                        c.getName(),
                        c.getSetName(),
                        c.getNumber(),
                        String.format("%.2f", usd),
                        String.format("%.2f", brl)
                    });
                    resultados.add(c);
                }

                if (cards.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Nenhuma carta encontrada.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro na busca: " + ex.getMessage());
            } finally {
                btnBuscar.setEnabled(true);
            }
        });
    }

    /** Retorna a carta selecionada (ou null se cancelado) */
    public Carta getCartaSelecionada() {
        return cartaSelecionada;
    }

    /** combo simples id/label */
    private static class ComboItem {
        final String id, label;
        ComboItem(String id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    /** desserialização de Carta via Gson */
    public static class Carta {
        private String name, number;
        private SetObject set;
        private Tcgplayer tcgplayer;

        public String getName()     { return name; }
        public String getNumber()   { return number; }
        public String getSetName()  { return set   != null ? set.name : ""; }
        public String getSetId()    { return set   != null ? set.id   : ""; }
        public String getSetSeries() {
            return set != null ? set.series : "";
        }
        
        public double getPrecoUSD() {
            if (tcgplayer != null && tcgplayer.prices != null) {
                PriceInfo pi = tcgplayer.prices.normal;
                if (pi != null && pi.market != null) return pi.market;
            }
            return 0.0;
        }

        private static class SetObject {
            String id, name, series;  // ✅ adiciona o campo series
        }        
        private static class Tcgplayer  { Prices prices; }
        private static class Prices     { PriceInfo normal; }
        private static class PriceInfo  { Double market; }
    }

    /** desserialização de Set (brief) */
    public static class SetBrief {
        String id;
        String name;
    }
}
