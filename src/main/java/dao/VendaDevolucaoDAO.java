package dao;

import model.VendaDevolucaoModel;
import model.VendaItemModel;
import util.DB;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VendaDevolucaoDAO {

    private static final String SQL_INSERT =
            "INSERT INTO vendas_devolucoes(venda_id, produto_id, qtd, valor_unit, data, motivo) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    /**
     * Mantido por compatibilidade (abre conexão própria).
     * Prefira inserir(dev, c) para ficar transacional.
     */
    public void inserir(VendaDevolucaoModel dev) throws SQLException {
        try (Connection c = DB.get()) {
            inserir(dev, c);
        }
    }

    /**
     * Insere devolução usando a MESMA conexão do caller (transacional).
     */
    public void inserir(VendaDevolucaoModel dev, Connection c) throws SQLException {
        if (dev == null) throw new SQLException("Devolução nula");
        if (c == null) throw new SQLException("Connection nula");

        try (PreparedStatement p = c.prepareStatement(SQL_INSERT)) {
            p.setInt(1, dev.getVendaId());
            p.setString(2, dev.getProdutoId());
            p.setInt(3, dev.getQuantidade());
            p.setDouble(4, dev.getValor());
            p.setString(5, (dev.getData() == null ? LocalDate.now() : dev.getData()).toString());
            p.setString(6, dev.getMotivo());
            p.executeUpdate();
        }
    }

    public List<VendaDevolucaoModel> listarPorVenda(int vendaId) throws SQLException {
        List<VendaDevolucaoModel> lista = new ArrayList<>();
        String sql = "SELECT id, venda_id, produto_id, qtd, valor_unit, data, motivo " +
                     "FROM vendas_devolucoes WHERE venda_id = ? ORDER BY id ASC";

        try (Connection c = DB.get();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, vendaId);

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    VendaDevolucaoModel d = new VendaDevolucaoModel();
                    d.setId(rs.getInt("id"));
                    d.setVendaId(rs.getInt("venda_id"));
                    d.setProdutoId(rs.getString("produto_id"));
                    d.setQuantidade(rs.getInt("qtd"));
                    d.setValor(rs.getDouble("valor_unit"));

                    String dataStr = rs.getString("data");
                    d.setData((dataStr == null || dataStr.isBlank()) ? LocalDate.now() : LocalDate.parse(dataStr.trim()));

                    d.setMotivo(rs.getString("motivo"));
                    // usuario é opcional e NÃO vem do banco se você não adicionou coluna
                    lista.add(d);
                }
            }
        }

        return lista;
    }

    // Mantive seus métodos de batch (sem mexer em estoque aqui, porque agora o Service manda)
    public void registrarDevolucaoCompleta(int vendaId, List<VendaItemModel> itens, Connection c) throws SQLException {
        if (c == null) throw new SQLException("Connection nula");
        if (itens == null || itens.isEmpty()) return;

        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            for (VendaItemModel it : itens) {
                ps.setInt(1, vendaId);
                ps.setString(2, it.getProdutoId());
                ps.setInt(3, it.getQtd());
                ps.setDouble(4, it.getPreco());
                ps.setString(5, LocalDate.now().toString());
                ps.setString(6, "Cancelamento total");
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void registrarEstornoParcial(int vendaId, VendaItemModel item, int qtdEstorno, Connection c) throws SQLException {
        if (c == null) throw new SQLException("Connection nula");
        if (item == null) throw new SQLException("Item nulo");
        if (qtdEstorno <= 0) throw new SQLException("qtdEstorno inválida: " + qtdEstorno);

        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, vendaId);
            ps.setString(2, item.getProdutoId());
            ps.setInt(3, qtdEstorno);
            ps.setDouble(4, item.getPreco());
            ps.setString(5, LocalDate.now().toString());
            ps.setString(6, "Estorno parcial");
            ps.executeUpdate();
        }
    }

    public void registrarEstornoCompleto(int vendaId, List<VendaItemModel> itens, Connection c) throws SQLException {
        if (c == null) throw new SQLException("Connection nula");
        if (itens == null || itens.isEmpty()) return;

        try (PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
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
    }
}
