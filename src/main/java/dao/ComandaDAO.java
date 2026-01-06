package dao;

import model.ComandaModel;
import model.ComandaResumoModel;
import util.DB;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ComandaDAO {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Insere comanda e retorna ID (INTEGER AUTOINCREMENT).
     * Regras:
     * - cliente_id nunca pode ser "" ou null -> vira "AVULSO"
     * - status default: "aberta"
     * - totais zerados na criação
     */
    public int inserir(ComandaModel c, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO comandas
            (cliente_id, nome_cliente, mesa, status, total_bruto, desconto, acrescimo, total_liquido, total_pago,
             observacoes, criado_em, criado_por)
            VALUES (?, ?, ?, ?, 0, 0, 0, 0, 0, ?, ?, ?)
        """;

        String cid = sanitizeClienteId(c.getClienteId());
        String status = sanitizeStatus(c.getStatus());

        if (c.getCriadoEm() == null) {
            c.setCriadoEm(LocalDateTime.now());
        }

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cid);
            ps.setString(2, blankToNull(c.getNomeCliente()));
            ps.setString(3, blankToNull(c.getMesa()));
            ps.setString(4, status);
            ps.setString(5, blankToNull(c.getObservacoes()));
            ps.setString(6, c.getCriadoEm().format(FMT));
            ps.setString(7, blankToNull(c.getCriadoPor()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        throw new SQLException("Falha ao gerar ID da comanda.");
    }

    public ComandaModel buscarPorId(int id) throws SQLException {
        try (Connection conn = DB.get()) {
            return buscarPorId(id, conn);
        }
    }

    public ComandaModel buscarPorId(int id, Connection conn) throws SQLException {
        String sql = "SELECT * FROM comandas WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    /**
     * Atualiza totais/status e timestamps de fechamento/cancelamento.
     * Observação: não mexe em cliente_id, mesa, nome_cliente etc.
     */
    public void atualizarTotaisEStatus(ComandaModel c, Connection conn) throws SQLException {
        String sql = """
            UPDATE comandas SET
                status=?,
                total_bruto=?,
                desconto=?,
                acrescimo=?,
                total_liquido=?,
                total_pago=?,
                fechado_em=?,
                fechado_por=?,
                cancelado_em=?,
                cancelado_por=?
            WHERE id=?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sanitizeStatus(c.getStatus()));
            ps.setDouble(2, c.getTotalBruto());
            ps.setDouble(3, c.getDesconto());
            ps.setDouble(4, c.getAcrescimo());
            ps.setDouble(5, c.getTotalLiquido());
            ps.setDouble(6, c.getTotalPago());

            ps.setString(7, c.getFechadoEm() == null ? null : c.getFechadoEm().format(FMT));
            ps.setString(8, blankToNull(c.getFechadoPor()));

            ps.setString(9, c.getCanceladoEm() == null ? null : c.getCanceladoEm().format(FMT));
            ps.setString(10, blankToNull(c.getCanceladoPor()));

            ps.setInt(11, c.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Lista resumo com saldo = total_liquido - total_pago
     */
    public List<ComandaResumoModel> listarResumo(String filtroStatus) throws SQLException {
        List<ComandaResumoModel> out = new ArrayList<>();

        boolean filtra = !(filtroStatus == null || filtroStatus.isBlank() || filtroStatus.equalsIgnoreCase("todas"));
        String where = filtra ? "WHERE c.status = ?" : "";

        String sql = """
            SELECT
                c.id,
                COALESCE(cl.nome, c.nome_cliente, '—') AS cliente,
                COALESCE(c.mesa, '—') AS mesa,
                c.status,
                c.criado_em,
                c.total_liquido,
                c.total_pago,
                (c.total_liquido - c.total_pago) AS saldo
            FROM comandas c
            LEFT JOIN clientes cl ON cl.id = c.cliente_id
        """ + " " + where + " ORDER BY c.id DESC";

        try (Connection conn = DB.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (filtra) ps.setString(1, filtroStatus);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComandaResumoModel r = new ComandaResumoModel();
                    r.setId(rs.getInt("id"));
                    r.setCliente(rs.getString("cliente"));
                    r.setMesa(rs.getString("mesa"));
                    r.setStatus(rs.getString("status"));
                    r.setCriadoEm(rs.getString("criado_em"));
                    r.setTotalLiquido(rs.getDouble("total_liquido"));
                    r.setTotalPago(rs.getDouble("total_pago"));
                    r.setSaldo(rs.getDouble("saldo"));
                    out.add(r);
                }
            }
        }

        return out;
    }

    /* ===================== helpers ===================== */

    private ComandaModel map(ResultSet rs) throws SQLException {
        ComandaModel c = new ComandaModel();
        c.setId(rs.getInt("id"));
        c.setClienteId(rs.getString("cliente_id"));
        c.setNomeCliente(rs.getString("nome_cliente"));
        c.setMesa(rs.getString("mesa"));
        c.setStatus(rs.getString("status"));

        c.setTotalBruto(rs.getDouble("total_bruto"));
        c.setDesconto(rs.getDouble("desconto"));
        c.setAcrescimo(rs.getDouble("acrescimo"));
        c.setTotalLiquido(rs.getDouble("total_liquido"));
        c.setTotalPago(rs.getDouble("total_pago"));

        c.setObservacoes(rs.getString("observacoes"));

        String criado = rs.getString("criado_em");
        c.setCriadoEm(criado == null ? null : LocalDateTime.parse(criado, FMT));
        c.setCriadoPor(rs.getString("criado_por"));

        String fechado = rs.getString("fechado_em");
        c.setFechadoEm(fechado == null ? null : LocalDateTime.parse(fechado, FMT));
        c.setFechadoPor(rs.getString("fechado_por"));

        String cancel = rs.getString("cancelado_em");
        c.setCanceladoEm(cancel == null ? null : LocalDateTime.parse(cancel, FMT));
        c.setCanceladoPor(rs.getString("cancelado_por"));

        return c;
    }

    private static String sanitizeClienteId(String clienteId) {
        if (clienteId == null) return "AVULSO";
        String v = clienteId.trim();
        return v.isEmpty() ? "AVULSO" : v;
    }

    private static String sanitizeStatus(String status) {
        if (status == null) return "aberta";
        String s = status.trim();
        return s.isEmpty() ? "aberta" : s.toLowerCase();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }
}
