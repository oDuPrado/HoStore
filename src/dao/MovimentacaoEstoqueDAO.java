// Procure no seu projeto (Ctrl+F) por "new DB().get()" e cole isto em src/dao/MovimentacaoEstoqueDAO.java
package dao;

import util.DB;
import model.MovimentacaoEstoqueModel;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MovimentacaoEstoqueDAO {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Insere nova movimentação e retorna o objeto com o ID gerado */
    public MovimentacaoEstoqueModel inserir(MovimentacaoEstoqueModel mov) throws SQLException {
        String sql = "INSERT INTO estoque_movimentacoes(produto_id, tipo_mov, quantidade, motivo, data, usuario) "
                   + "VALUES(?,?,?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            p.setString(1, mov.getProdutoId());
            p.setString(2, mov.getTipoMov());
            p.setInt(3, mov.getQuantidade());
            p.setString(4, mov.getMotivo());
            p.setString(5, mov.getData().format(FMT));
            p.setString(6, mov.getUsuario());
            p.executeUpdate();

            try (ResultSet rs = p.getGeneratedKeys()) {
                if (rs.next()) {
                    mov.setId(rs.getInt(1));
                }
            }
            return mov;
        }
    }

    /** Lista todas as movimentações */
    public List<MovimentacaoEstoqueModel> listarTodas() throws SQLException {
        String sql = "SELECT * FROM estoque_movimentacoes ORDER BY data DESC";
        List<MovimentacaoEstoqueModel> lista = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Lista movimentações de um produto específico */
    public List<MovimentacaoEstoqueModel> listarPorProduto(String produtoId) throws SQLException {
        String sql = "SELECT * FROM estoque_movimentacoes WHERE produto_id = ? ORDER BY data DESC";
        List<MovimentacaoEstoqueModel> lista = new ArrayList<>();
        try (Connection c = DB.get();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, produtoId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Constrói o objeto a partir do ResultSet */
    private MovimentacaoEstoqueModel mapear(ResultSet rs) throws SQLException {
        Integer id       = rs.getInt("id");
        String prodId    = rs.getString("produto_id");
        String tipoMov   = rs.getString("tipo_mov");
        int qtd          = rs.getInt("quantidade");
        String motivo    = rs.getString("motivo");
        LocalDateTime dt = LocalDateTime.parse(rs.getString("data"), FMT);
        String usuario   = rs.getString("usuario");

        return new MovimentacaoEstoqueModel(id, prodId, tipoMov, qtd, motivo, dt, usuario);
    }
}
