package ui.dialog;

import dao.CartaDAO;
import model.Carta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Modal para escolher 1+ cartas do estoque */
public class SelectCartaDialog extends JDialog {

    private final CartaDAO cartaDAO = new CartaDAO();
    private final DefaultTableModel modelo;
    private final JTable tabela;

    private final JTextField buscaField   = new JTextField(15);
    private final JComboBox<String> colecaoBox;
    private final JComboBox<String> ordemBox = new JComboBox<>(
            new String[]{"Mais antigo", "Mais novo", "Nome", "Número"});

    public SelectCartaDialog(JFrame owner) {
        super(owner, "Selecionar cartas", true);
        setSize(800, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        // Filtros topo
        colecaoBox = new JComboBox<>();
        colecaoBox.addItem("Todas");
        cartaDAO.listarColecoes().forEach(colecaoBox::addItem);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Buscar:"));   filtros.add(buscaField);
        filtros.add(new JLabel("Coleção:")); filtros.add(colecaoBox);
        filtros.add(new JLabel("Ordenar:")); filtros.add(ordemBox);

        JButton filtrarBtn = new JButton("Filtrar");
        filtrarBtn.addActionListener(e -> carregar());
        filtros.add(filtrarBtn);
        add(filtros, BorderLayout.NORTH);

        // Tabela
        modelo = new DefaultTableModel(
                new String[]{"UID", "Nome", "Coleção", "Número", "Qtd", "R$ Unit."}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        // Botões
        JButton ok = new JButton("Adicionar selecionadas");
        ok.addActionListener(e -> dispose());
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> { modelo.setRowCount(0); dispose(); });

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(cancelar);  rodape.add(ok);
        add(rodape, BorderLayout.SOUTH);

        colecaoBox.setSelectedItem("Todas");
        buscaField.setText(""); // limpa a busca
        ordemBox.setSelectedItem("Mais antigo");
        carregar();

    }

    /** Recarrega tabela com filtros atuais */
    private void carregar() {
        modelo.setRowCount(0);
        List<Carta> cartas = cartaDAO.listarCartas(
                buscaField.getText(),
                (String) colecaoBox.getSelectedItem(),
                (String) ordemBox.getSelectedItem());

        for (Carta c : cartas) {
            modelo.addRow(new Object[]{
                    c.getId(), c.getNome(), c.getColecao(),
                    c.getNumero(), c.getQtd(), c.getPreco()
            });
        }
    }

    /** Retorna as cartas escolhidas */
   public List<Carta> getSelecionadas() {
    int[] rows = tabela.getSelectedRows();
    List<Carta> selecionadas = new ArrayList<>();
    for (int r : rows) {
        selecionadas.add(new Carta(
            (String) modelo.getValueAt(r, 0),
            (String) modelo.getValueAt(r, 1),
            (String) modelo.getValueAt(r, 2),
            (String) modelo.getValueAt(r, 3),
            (int)    modelo.getValueAt(r, 4),
            (double) modelo.getValueAt(r, 5)
        ));
    }
    return selecionadas;
}

}
