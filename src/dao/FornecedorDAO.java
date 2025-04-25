// src/dao/FornecedorDAO.java
package dao;

import model.FornecedorModel;
import util.DB;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FornecedorDAO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public void inserir(FornecedorModel f) throws Exception {
        String sql = "INSERT INTO fornecedores(" +
            "id,nome,telefone,email,cnpj,contato," +
            "endereco,cidade,estado,observacoes," +
            "pagamento_tipo,prazo,criado_em,alterado_em) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String now = LocalDateTime.now().format(FMT);
            ps.setString(1, f.getId());
            ps.setString(2, f.getNome());
            ps.setString(3, f.getTelefone());
            ps.setString(4, f.getEmail());
            ps.setString(5, f.getCnpj());
            ps.setString(6, f.getContato());
            ps.setString(7, f.getEndereco());
            ps.setString(8, f.getCidade());
            ps.setString(9, f.getEstado());
            ps.setString(10, f.getObservacoes());
            ps.setString(11, f.getPagamentoTipo());
            if (f.getPrazo() != null) ps.setInt(12, f.getPrazo());
            else ps.setNull(12, Types.INTEGER);
            ps.setString(13, now);
            ps.setString(14, now);
            ps.executeUpdate();
        }
    }

    public void atualizar(FornecedorModel f) throws Exception {
        String sql = "UPDATE fornecedores SET " +
            "nome=?,telefone=?,email=?,cnpj=?,contato=?,endereco=?,cidade=?,estado=?,observacoes=?, " +
            "pagamento_tipo=?,prazo=?,alterado_em=? WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String now = LocalDateTime.now().format(FMT);
            ps.setString(1, f.getNome());
            ps.setString(2, f.getTelefone());
            ps.setString(3, f.getEmail());
            ps.setString(4, f.getCnpj());
            ps.setString(5, f.getContato());
            ps.setString(6, f.getEndereco());
            ps.setString(7, f.getCidade());
            ps.setString(8, f.getEstado());
            ps.setString(9, f.getObservacoes());
            ps.setString(10, f.getPagamentoTipo());
            if (f.getPrazo() != null) ps.setInt(11, f.getPrazo());
            else ps.setNull(11, Types.INTEGER);
            ps.setString(12, now);
            ps.setString(13, f.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(String id) throws Exception {
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM fornecedores WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public FornecedorModel buscarPorId(String id) throws Exception {
        String sql = "SELECT * FROM fornecedores WHERE id=?";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return extract(rs);
            }
        }
    }

    public List<FornecedorModel> listar(String nomeFiltro,
                                        String cnpjFiltro,
                                        String tipoFiltro,
                                        Integer prazoFiltro) throws Exception {
        List<FornecedorModel> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM fornecedores WHERE 1=1");
        if (nomeFiltro != null && !nomeFiltro.isBlank()) sb.append(" AND nome LIKE ?");
        if (cnpjFiltro != null && !cnpjFiltro.isBlank()) sb.append(" AND cnpj LIKE ?");
        if (tipoFiltro != null && !tipoFiltro.isBlank()) sb.append(" AND pagamento_tipo = ?");
        if (prazoFiltro != null) sb.append(" AND prazo = ?");
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            int idx = 1;
            if (nomeFiltro != null && !nomeFiltro.isBlank()) ps.setString(idx++, "%" + nomeFiltro + "%");
            if (cnpjFiltro != null && !cnpjFiltro.isBlank()) ps.setString(idx++, "%" + cnpjFiltro + "%");
            if (tipoFiltro != null && !tipoFiltro.isBlank()) ps.setString(idx++, tipoFiltro);
            if (prazoFiltro != null) ps.setInt(idx++, prazoFiltro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(extract(rs));
            }
        }
        return out;
    }

    private FornecedorModel extract(ResultSet rs) throws SQLException {
        return new FornecedorModel(
            rs.getString("id"),
            rs.getString("nome"),
            rs.getString("telefone"),
            rs.getString("email"),
            rs.getString("cnpj"),
            rs.getString("contato"),
            rs.getString("endereco"),
            rs.getString("cidade"),
            rs.getString("estado"),
            rs.getString("observacoes"),
            rs.getString("pagamento_tipo"),
            rs.getObject("prazo") != null ? rs.getInt("prazo") : null,
            rs.getString("criado_em"),
            rs.getString("alterado_em")
        );
    }
    public String obterIdPorNome(String nome) {
        try (PreparedStatement ps = DB.get().prepareStatement("SELECT id FROM fornecedores WHERE nome = ? LIMIT 1")) {
            ps.setString(1, nome);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String obterNomePorId(String id) {
        try (PreparedStatement ps = DB.get().prepareStatement("SELECT nome FROM fornecedores WHERE id = ? LIMIT 1")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nome");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Desconhecido";
    }
    
    
}
