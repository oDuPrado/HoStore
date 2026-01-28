package service;

import dao.RhCargoDAO;
import dao.RhComissaoDAO;
import dao.RhFolhaDAO;
import dao.RhFuncionarioDAO;
import dao.RhSalarioDAO;
import model.RhCargoModel;
import model.RhComissaoModel;
import model.RhFolhaModel;
import model.RhFuncionarioModel;
import model.RhSalarioModel;
import util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class RhService {

    private final RhFuncionarioDAO funcionarioDAO = new RhFuncionarioDAO();
    private final RhComissaoDAO comissaoDAO = new RhComissaoDAO();
    private final RhFolhaDAO folhaDAO = new RhFolhaDAO();
    private final RhSalarioDAO salarioDAO = new RhSalarioDAO();
    private final RhCargoDAO cargoDAO = new RhCargoDAO();

    public int gerarComissoesPeriodo(String dataIni, String dataFim) throws Exception {
        int count = 0;
        String sql = "SELECT id, data_venda, total_liquido, criado_por FROM vendas WHERE status <> 'cancelada' AND date(data_venda) >= date(?) AND date(data_venda) <= date(?)";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dataIni);
            ps.setString(2, dataFim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int vendaId = rs.getInt("id");
                    String data = rs.getString("data_venda");
                    double total = rs.getDouble("total_liquido");
                    String usuarioId = rs.getString("criado_por");
                    if (usuarioId == null || usuarioId.isBlank())
                        continue;

                    RhFuncionarioModel func = funcionarioDAO.buscarPorUsuarioId(usuarioId);
                    if (func == null)
                        continue;
                    double pct = func.getComissaoPct();
                    if (pct <= 0.0)
                        continue;

                    RhComissaoModel cm = new RhComissaoModel();
                    cm.setVendaId(vendaId);
                    cm.setFuncionarioId(func.getId());
                    cm.setPercentual(pct);
                    cm.setValor(total * pct / 100.0);
                    cm.setData(data != null ? data.substring(0, 10) : LocalDate.now().toString());
                    cm.setObservacoes("Comissao automatica");
                    comissaoDAO.inserir(cm);
                    count++;
                }
            }
        }
        return count;
    }

    public int gerarFolhaCompetencia(String competencia) throws Exception {
        int count = 0;
        YearMonth ym = YearMonth.parse(competencia);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<RhFuncionarioModel> funcs = funcionarioDAO.listar(true);
        for (RhFuncionarioModel f : funcs) {
            if (!funcionarioAtivoNoPeriodo(f, inicio, fim))
                continue;

            double salarioBase = resolveSalarioBase(f, inicio);
            double horas = somarHorasTrabalhadas(f.getId(), inicio.toString(), fim.toString());
            double comissao = comissaoDAO.somarPorFuncionarioPeriodo(f.getId(), inicio.toString(), fim.toString());

            RhFolhaModel folha = folhaDAO.buscarPorCompetenciaFuncionario(competencia, f.getId());
            if (folha == null) {
                folha = new RhFolhaModel();
                folha.setCompetencia(competencia);
                folha.setFuncionarioId(f.getId());
            }
            folha.setSalarioBase(salarioBase);
            folha.setHorasTrabalhadas(horas);
            folha.setHorasExtras(0.0);
            folha.setDescontos(0.0);
            folha.setComissao(comissao);
            double totalBruto = salarioBase + comissao;
            folha.setTotalBruto(totalBruto);
            folha.setTotalLiquido(totalBruto - folha.getDescontos());
            folha.setStatus("aberta");

            if (folha.getId() > 0) {
                folhaDAO.atualizar(folha);
            } else {
                folhaDAO.inserir(folha);
            }
            count++;
        }
        return count;
    }

    public void registrarEvolucaoSalarial(String funcionarioId, String cargoId, double salarioBase, String dataInicio, String motivo) throws Exception {
        RhSalarioModel sal = new RhSalarioModel();
        sal.setFuncionarioId(funcionarioId);
        sal.setCargoId(cargoId);
        sal.setSalarioBase(salarioBase);
        sal.setDataInicio(dataInicio);
        sal.setDataFim(null);
        sal.setMotivo(motivo);
        salarioDAO.inserir(sal);

        RhFuncionarioModel f = funcionarioDAO.buscarPorId(funcionarioId);
        if (f != null) {
            f.setCargoId(cargoId);
            f.setSalarioBase(salarioBase);
            funcionarioDAO.atualizar(f);
        }
    }

    private boolean funcionarioAtivoNoPeriodo(RhFuncionarioModel f, LocalDate inicio, LocalDate fim) {
        if (f.getAtivo() != 1)
            return false;
        if (f.getDataAdmissao() != null && !f.getDataAdmissao().isBlank()) {
            LocalDate adm = LocalDate.parse(f.getDataAdmissao());
            if (adm.isAfter(fim))
                return false;
        }
        if (f.getDataDemissao() != null && !f.getDataDemissao().isBlank()) {
            LocalDate dem = LocalDate.parse(f.getDataDemissao());
            if (dem.isBefore(inicio))
                return false;
        }
        return true;
    }

    private double resolveSalarioBase(RhFuncionarioModel f, LocalDate data) throws Exception {
        RhSalarioModel sal = salarioDAO.buscarVigente(f.getId(), data.toString());
        if (sal != null && sal.getSalarioBase() > 0)
            return sal.getSalarioBase();
        if (f.getSalarioBase() > 0)
            return f.getSalarioBase();
        if (f.getCargoId() != null) {
            RhCargoModel c = cargoDAO.buscarPorId(f.getCargoId());
            if (c != null) return c.getSalarioBase();
        }
        return 0.0;
    }

    private double somarHorasTrabalhadas(String funcionarioId, String dataIni, String dataFim) throws Exception {
        String sql = "SELECT COALESCE(SUM(horas_trabalhadas),0) AS total FROM rh_ponto WHERE funcionario_id=? AND date(data) >= date(?) AND date(data) <= date(?)";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, funcionarioId);
            ps.setString(2, dataIni);
            ps.setString(3, dataFim);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0.0;
    }
}
