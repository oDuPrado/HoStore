// src/ui/venda/dialog/VendaDevolucaoDialog.java
package ui.venda.dialog;

import dao.ProdutoDAO;
import dao.VendaDevolucaoDAO;
import model.ProdutoModel;
import model.VendaItemModel;
import model.VendaDevolucaoModel;
import service.VendaDevolucaoService;
import util.AlertUtils;
import util.DB;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dialog para registrar devoluções de venda de forma mais completa:
 *  - Exibe nome do produto em vez de ID.
 *  - Mostra valor unitário e total de devolução.
 *  - Atualiza automaticamente o total de devolução quando o usuário altera a quantidade a devolver.
 */
public class VendaDevolucaoDialog extends JDialog {
    private final int vendaId;
    private final List<VendaItemModel> itens; // Lista de itens originais da venda (com produtoId, qtd e preco)

    private final JTable tabela;
    private final DefaultTableModel model;

    // Map temporário de produtoId -> nome do produto, para preenchimento da tabela
    private final Map<String, String> nomesProdutos = new HashMap<>();

    public VendaDevolucaoDialog(Window owner, int vendaId, List<VendaItemModel> itens) {
        super(owner, "Registrar Devolução", ModalityType.APPLICATION_MODAL);
        this.vendaId = vendaId;
        this.itens = itens;

        // 1) Configurações iniciais da janela
        setSize(650, 450);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // 2) Carrega todos os nomes de produto em um Map para evitar múltiplas queries
        preloadNomeProdutos();

        // 3) Define as colunas da tabela:
        //    0: Nome do Produto
        //    1: Qtd Vendida
        //    2: V.Unit. (valor unitário)
        //    3: Qtd Devolver (o usuário digita)
        //    4: Total Dev. (calculado automaticamente)
        //    5: Motivo
        model = new DefaultTableModel(new String[]{
            "Produto", "Qtd Vendida", "V.Unit.", "Qtd Devolver", "Total Dev.", "Motivo"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Apenas colunas 3 (Qtd Devolver) e 5 (Motivo) devem ser editáveis
                return column == 3 || column == 5;
            }
        };

        // 4) Popula cada linha com base na lista de itens da venda
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        for (VendaItemModel it : itens) {
            String produtoId = it.getProdutoId();
            String nome = nomesProdutos.getOrDefault(produtoId, produtoId);

            int qtdVendida = it.getQtd();
            double valorUnit = it.getPreco();

            // Coluna "Qtd Devolver" inicia em 0 e "Total Dev." em R$ 0,00
            model.addRow(new Object[]{
                nome,
                qtdVendida,
                nf.format(valorUnit),
                0,
                nf.format(0.0),
                "" // motivo vazio inicialmente
            });
        }

        // 5) Cria a JTable e adiciona um listener para recalcular "Total Dev." quando "Qtd Devolver" mudar
        tabela = new JTable(model);
        tabela.setRowHeight(24);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(100); // Ajusta largura do Total Dev.
        tabela.getColumnModel().getColumn(0).setPreferredWidth(180); // Ajusta largura do nome do produto

        // Adiciona TableModelListener para atualizar a coluna "Total Dev." sempre que "Qtd Devolver" for alterada
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() != TableModelEvent.UPDATE) return;

                int linha = e.getFirstRow();
                int coluna = e.getColumn();

                // Se a coluna alterada for "Qtd Devolver" (índice 3), recalcula o "Total Dev."
                if (coluna == 3) {
                    Object valorQtdObj = model.getValueAt(linha, 3);
                    int qtdDevolver = 0;
                    try {
                        qtdDevolver = Integer.parseInt(valorQtdObj.toString());
                    } catch (NumberFormatException ex) {
                        // Se não for número válido, mantemos 0 para não quebrar
                        qtdDevolver = 0;
                    }

                    // Recupera o item original correspondente pela posição na lista 'itens'
                    VendaItemModel itemOriginal = itens.get(linha);
                    double valorUnit = itemOriginal.getPreco();

                    // Calcula total = qtdDevolver * valorUnit
                    double totalDev = qtdDevolver * valorUnit;

                    // Atualiza a célula "Total Dev." (coluna 4) com o formato de moeda
                    model.setValueAt(nf.format(totalDev), linha, 4);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabela);
        add(scroll, BorderLayout.CENTER);

        // 6) Painel de botões na parte inferior
        JButton btnSalvar = new JButton("Confirmar Devolução");
        btnSalvar.addActionListener(this::salvar);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(btnCancelar);
        rodape.add(btnSalvar);
        add(rodape, BorderLayout.SOUTH);
    }

    /**
     * Pré-carrega em um Map o nome de cada produto (produtoId -> nome),
     * evitando múltiplas consultas ao banco durante a montagem da tabela.
     */
    private void preloadNomeProdutos() {
        ProdutoDAO produtoDAO = new ProdutoDAO();
        for (VendaItemModel it : itens) {
            String produtoId = it.getProdutoId();
            if (!nomesProdutos.containsKey(produtoId)) {
                try {
                    ProdutoModel pm = produtoDAO.findById(produtoId);
                    if (pm != null) {
                        nomesProdutos.put(produtoId, pm.getNome());
                    } else {
                        nomesProdutos.put(produtoId, produtoId);
                    }
                } catch (Exception ex) {
                    // Em caso de falha ao buscar, deixamos o próprio ID como fallback
                    nomesProdutos.put(produtoId, produtoId);
                }
            }
        }
    }

    /**
     * Método acionado ao clicar em "Confirmar Devolução".
     * Percorre cada linha da tabela, valida e cria um VendaDevolucaoModel
     * para chamar o service.registrarDevolucao().
     */
    private void salvar(ActionEvent evt) {
        try (Connection c = DB.get()) {
            VendaDevolucaoDAO dao = new VendaDevolucaoDAO();

            for (int i = 0; i < model.getRowCount(); i++) {
                // 1) Lê a quantidade a devolver da coluna 3
                Object qtdObj = model.getValueAt(i, 3);
                int qtdDevolver = 0;
                try {
                    qtdDevolver = Integer.parseInt(qtdObj.toString());
                } catch (NumberFormatException ex) {
                    AlertUtils.error("Quantidade inválida na linha " + (i + 1));
                    return;
                }

                // 2) Se qtdDevolver <= 0, ignora esta linha
                if (qtdDevolver <= 0) {
                    continue;
                }

                // 3) Lê o motivo (coluna 5)
                String motivo = (String) model.getValueAt(i, 5);

                // 4) Recupera o item original da venda pela posição na lista
                VendaItemModel itemOriginal = itens.get(i);
                String produtoId = itemOriginal.getProdutoId();
                int qtdVendida = itemOriginal.getQtd();
                double valorUnit = itemOriginal.getPreco();

                // 5) Valida se a qtdDevolver não ultrapassa qtdVendida
                if (qtdDevolver > qtdVendida) {
                    AlertUtils.error("A quantidade devolvida não pode ser maior que a vendida (linha " + (i + 1) + ")");
                    return;
                }

                // 6) Monta o modelo de devolução com todos os campos preenchidos
                VendaDevolucaoModel dev = new VendaDevolucaoModel();
                dev.setVendaId(vendaId);
                dev.setProdutoId(produtoId);
                dev.setQuantidade(qtdDevolver);
                dev.setMotivo(motivo);
                dev.setData(LocalDate.now());
                dev.setValor(valorUnit); // valor unitário correto

                // 7) Chama o service que fará o INSERT e atualizará o estoque
                VendaDevolucaoService service = new VendaDevolucaoService();
                service.registrarDevolucao(dev);
            }

            AlertUtils.info("Devoluções registradas com sucesso.");
            dispose();

        } catch (Exception e) {
            AlertUtils.error("Erro ao registrar devoluções:\n" + e.getMessage());
        }
    }
}
