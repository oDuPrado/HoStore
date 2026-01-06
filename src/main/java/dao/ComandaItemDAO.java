package dao;

import model.ComandaItemModel;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ComandaItemDAO {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public int inserir(ComandaItemModel it, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO comandas_itens
            (comanda_id, produto_id, qtd, preco, desconto, acrescimo, total_item, observacoes, criado_em, criado_por)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, it.getComandaId());
            ps.setString(2, it.getProdutoId());
            ps.setInt(3, it.getQtd());
            ps.setDouble(4, it.getPreco());
            ps.setDouble(5, it.getDesconto());
            ps.setDouble(6, it.getAcrescimo());
            ps.setDouble(7, it.getTotalItem());
            ps.setString(8, it.getObservacoes());
            ps.setString(9, it.getCriadoEm().format(FMT));
            ps.setString(10, it.getCriadoPor());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Falha ao gerar ID do item da comanda.");
    }

    public List<ComandaItemModel> listarPorComanda(int comandaId, Connection conn) throws SQLException {
        List<ComandaItemModel> out = new ArrayList<>();
        String sql = "SELECT * FROM comandas_itens WHERE comanda_id = ? ORDER BY id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, comandaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public ComandaItemModel buscarPorId(int itemId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM comandas_itens WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public void deletar(int itemId, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM comandas_itens WHERE id = ?")) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        }
    }

    private ComandaItemModel map(ResultSet rs) throws SQLException {
        ComandaItemModel it = new ComandaItemModel();
        it.setId(rs.getInt("id"));
        it.setComandaId(rs.getInt("comanda_id"));
        it.setProdutoId(rs.getString("produto_id"));
        it.setQtd(rs.getInt("qtd"));
        it.setPreco(rs.getDouble("preco"));
        it.setDesconto(rs.getDouble("desconto"));
        it.setAcrescimo(rs.getDouble("acrescimo"));
        it.setTotalItem(rs.getDouble("total_item"));
        it.setObservacoes(rs.getString("observacoes"));

        String criado = rs.getString("criado_em");
        it.setCriadoEm(criado == null ? null : LocalDateTime.parse(criado, FMT));
        it.setCriadoPor(rs.getString("criado_por"));
        return it;
    }
}
