package dao;

import model.VendaDevolucaoModel;
import model.VendaItemModel;
import util.DB;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VendaDevolucaoDAO {

    /**
     * Insere um novo registro de devolução na tabela vendas_devolucoes.
     * A coluna correta para a quantidade é 'qtd', conforme o schema atual do banco.
     */
    public void inserir(VendaDevolucaoModel dev) throws SQLException {
        String sql = "INSERT INTO vendas_devolucoes(venda_id, produto_id, qtd, valor_unit, data, motivo) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection c = DB.get();
                PreparedStatement p = c.prepareStatement(sql)) {

            // Preenche os parâmetros da query com os dados do modelo
            p.setInt(1, dev.getVendaId());
            p.setString(2, dev.getProdutoId());
            p.setInt(3, dev.getQuantidade()); // corresponde à coluna 'qtd' na tabela
            p.setDouble(4, dev.getValor()); // corresponde à coluna 'valor_unit'
            p.setString(5, dev.getData().toString()); // data formatada como texto
            p.setString(6, dev.getMotivo());

            p.executeUpdate(); // executa o INSERT
        }
    }

    /**
     * Lista todas as devoluções vinculadas a uma venda específica.
     * Importante: usa 'qtd' como nome da coluna, conforme o banco.
     */
    public List<VendaDevolucaoModel> listarPorVenda(int vendaId) throws SQLException {
        List<VendaDevolucaoModel> lista = new ArrayList<>();
        String sql = "SELECT * FROM vendas_devolucoes WHERE venda_id = ?";

        try (Connection c = DB.get();
                PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, vendaId);

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    VendaDevolucaoModel d = new VendaDevolucaoModel();
                    d.setId(rs.getInt("id"));
                    d.setVendaId(rs.getInt("venda_id"));
                    d.setProdutoId(rs.getString("produto_id"));
                    d.setQuantidade(rs.getInt("qtd")); // <- aqui corrigido
                    d.setValor(rs.getDouble("valor_unit")); // <- aqui corrigido
                    d.setData(LocalDate.parse(rs.getString("data")));
                    d.setMotivo(rs.getString("motivo"));

                    lista.add(d);
                }
            }
        }

        return lista;
    }

    public void registrarDevolucaoCompleta(int vendaId, List<VendaItemModel> itens, Connection c) throws SQLException {
        String sql = "INSERT INTO vendas_devolucoes (venda_id, produto_id, qtd, valor_unit, data, motivo) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (VendaItemModel it : itens) {
                ps.setInt(1, vendaId);
                ps.setString(2, it.getProdutoId());
                ps.setInt(3, it.getQtd());
                ps.setDouble(4, it.getPreco()); // usa o valor de venda
                ps.setString(5, LocalDate.now().toString());
                ps.setString(6, "Cancelamento total");
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Reinsere no estoque
        new service.EstoqueService().entrarEstoqueEmLote(c, itens);
    }

    /**
 * Registra um estorno manual de item, semelhante à devolução.
 */
public void registrarEstornoParcial(int vendaId, VendaItemModel item, int qtdEstorno, Connection c) throws SQLException {
    String sql = "INSERT INTO vendas_devolucoes (venda_id, produto_id, qtd, valor_unit, data, motivo) " +
                 "VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setInt(1, vendaId);
        ps.setString(2, item.getProdutoId());
        ps.setInt(3, qtdEstorno);
        ps.setDouble(4, item.getPreco());
        ps.setString(5, LocalDate.now().toString());
        ps.setString(6, "Estorno parcial");
        ps.executeUpdate();
    }

    new service.EstoqueService().entrarEstoque(c, item.getProdutoId(), qtdEstorno);
}

/**
 * Registra um estorno total da venda (todos os itens).
 */
public void registrarEstornoCompleto(int vendaId, List<VendaItemModel> itens, Connection c) throws SQLException {
    String sql = "INSERT INTO vendas_devolucoes (venda_id, produto_id, qtd, valor_unit, data, motivo) " +
                 "VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
        for (VendaItemModel it : itens) {
            ps.setInt(1, vendaId);
            ps.setString(2, it.getProdutoId());
            ps.setInt(3, it.getQtd());
            ps.setDouble(4, it.getPreco());
            ps.setString(5, LocalDate.now().toString());
            ps.setString(6, "Estorno total");
            ps.addBatch();
        }
        ps.executeBatch();
    }

    new service.EstoqueService().entrarEstoqueEmLote(c, itens);
}


}
