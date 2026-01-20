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

    /** Insere nova movimentação e retorna o objeto com o ID gerado (com nova conexão) */
    public MovimentacaoEstoqueModel inserir(MovimentacaoEstoqueModel mov) throws SQLException {
        try (Connection c = DB.get()) {
            return inserir(mov, c);
        }
    }

    /** Insere nova movimentação usando uma conexão existente (ideal para transações) */
    public MovimentacaoEstoqueModel inserir(MovimentacaoEstoqueModel mov, Connection c) throws SQLException {
        String sql = "INSERT INTO estoque_movimentacoes(produto_id, lote_id, tipo_mov, quantidade, motivo, data, usuario) "
                   + "VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, mov.getProdutoId());
            if (mov.getLoteId() == null) {
                p.setNull(2, Types.INTEGER);
            } else {
                p.setInt(2, mov.getLoteId());
            }
            p.setString(3, mov.getTipoMov());
            p.setInt(4, mov.getQuantidade());
            p.setString(5, mov.getMotivo());
            p.setString(6, mov.getData().format(FMT));
            p.setString(7, mov.getUsuario());
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
        Integer loteId   = (Integer) rs.getObject("lote_id");
        String tipoMov   = rs.getString("tipo_mov");
        int qtd          = rs.getInt("quantidade");
        String motivo    = rs.getString("motivo");
        LocalDateTime dt = LocalDateTime.parse(rs.getString("data"), FMT);
        String usuario   = rs.getString("usuario");

        return new MovimentacaoEstoqueModel(id, prodId, loteId, tipoMov, qtd, motivo, dt, usuario);
    }
}
