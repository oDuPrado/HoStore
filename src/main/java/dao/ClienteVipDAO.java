package dao;

import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClienteVipDAO {

    public boolean isVipPorCpf(String cpf) {
        if (cpf == null || cpf.isBlank())
            return false;
        String sql = "SELECT 1 FROM clientes_vip WHERE cpf = ? LIMIT 1";
        try (Connection c = DB.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cpf);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
