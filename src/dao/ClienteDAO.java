package dao;

import model.ClienteModel;
import util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    /* SELECT ALL */
    public List<ClienteModel> findAll() {
        List<ClienteModel> out = new ArrayList<>();
        try (Connection c = DB.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM clientes")) {

            while (rs.next()) out.add(map(rs));

        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    /* INSERT ou UPDATE */
    public void upsert(ClienteModel m) {
        String sql = 
        "INSERT INTO clientes (id,nome,telefone,cpf,data_nasc,tipo,endereco,cidade,estado,observacoes," +
        "criado_em,criado_por,alterado_em,alterado_por) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
        "ON CONFLICT(id) DO UPDATE SET " +
        "nome=excluded.nome, telefone=excluded.telefone, cpf=excluded.cpf, " +
        "data_nasc=excluded.data_nasc, tipo=excluded.tipo, endereco=excluded.endereco, " +
        "cidade=excluded.cidade, estado=excluded.estado, observacoes=excluded.observacoes, " +
        "alterado_em=excluded.alterado_em, alterado_por=excluded.alterado_por";

        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /* DELETE */
    public void delete(String id){
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM clientes WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e){ e.printStackTrace(); }
    }

    /* helpers */
    private ClienteModel map(ResultSet r) throws SQLException {
        ClienteModel m = new ClienteModel();
        m.setId(r.getString("id"));
        m.setNome(r.getString("nome"));
        m.setTelefone(r.getString("telefone"));
        m.setCpf(r.getString("cpf"));
        m.setDataNasc(r.getString("data_nasc"));
        m.setTipo(r.getString("tipo"));
        m.setEndereco(r.getString("endereco"));
        m.setCidade(r.getString("cidade"));
        m.setEstado(r.getString("estado"));
        m.setObservacoes(r.getString("observacoes"));
        m.setCriadoEm(r.getString("criado_em"));
        m.setCriadoPor(r.getString("criado_por"));
        m.setAlteradoEm(r.getString("alterado_em"));
        m.setAlteradoPor(r.getString("alterado_por"));
        return m;
    }
    private void bind(PreparedStatement ps, ClienteModel m) throws SQLException {
        ps.setString(1, m.getId());
        ps.setString(2, m.getNome());
        ps.setString(3, m.getTelefone());
        ps.setString(4, m.getCpf());
        ps.setString(5, m.getDataNasc());
        ps.setString(6, m.getTipo());
        ps.setString(7, m.getEndereco());
        ps.setString(8, m.getCidade());
        ps.setString(9, m.getEstado());
        ps.setString(10, m.getObservacoes());
        ps.setString(11, m.getCriadoEm());
        ps.setString(12, m.getCriadoPor());
        ps.setString(13, m.getAlteradoEm());
        ps.setString(14, m.getAlteradoPor());
    }
    /* NOVO – verifica se já existe CPF (pode ignorar um id ao editar) */
    public boolean cpfExists(String cpf, String ignoreId){
        String sql = "SELECT 1 FROM clientes WHERE cpf=? AND id <> ?";
        try(Connection c = DB.get();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cpf);
            ps.setString(2, ignoreId == null ? "" : ignoreId);
            try(ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e){ e.printStackTrace(); }
        return false;
    }
    public List<String> listarTodosNomes() {
        List<String> nomes = new ArrayList<>();
        try (PreparedStatement ps = DB.get().prepareStatement("SELECT nome FROM clientes ORDER BY nome")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nomes.add(rs.getString("nome"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nomes;
    }
    
    public String obterIdPorNome(String nome) {
        try (PreparedStatement ps = DB.get().prepareStatement("SELECT id FROM clientes WHERE nome = ? LIMIT 1")) {
            ps.setString(1, nome);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
}


