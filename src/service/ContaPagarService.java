package service;

import dao.ParcelaContaPagarDAO;
import dao.TituloContaPagarDAO;
import model.ParcelaContaPagarModel;
import model.TituloContaPagarModel;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Regras de negócio de contas a pagar.
 * Agora gera títulos vinculados a uma Conta do Plano de Contas.
 */
public class ContaPagarService {

    private final TituloContaPagarDAO tituloDAO   = new TituloContaPagarDAO();
    private final ParcelaContaPagarDAO parcelaDAO = new ParcelaContaPagarDAO();
    private final SimpleDateFormat fmtSQL         = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat fmtBR          = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Gera um novo título a pagar usando uma lista de datas de vencimento.
     *
     * @param fornecedorId   ID do fornecedor
     * @param planoContaId   ID da conta contábil do plano de contas
     * @param valorTotal     valor total do título
     * @param datasVenc      lista de datas de vencimento (uma por parcela)
     * @param jurosSimples   aplica juros simples se true
     * @param taxa           percentual de juros por parcela
     * @param preview        mostra preview antes de gravar
     * @param parent         componente para JOptionPane (preview)
     * @param obs            observações do título
     */
    public void gerarTituloComDatas(
            String fornecedorId,
            String planoContaId,
            double valorTotal,
            List<Date> datasVenc,
            boolean jurosSimples,
            double taxa,
            boolean preview,
            Component parent,
            String obs
    ) throws SQLException {
        int nParcelas = datasVenc.size();
        double base = valorTotal / nParcelas;

        // calcula valores com juros
        java.util.List<Double> valores = new java.util.ArrayList<>();
        for (int i = 1; i <= nParcelas; i++) {
            double acr = 0;
            if (taxa > 0) {
                acr = jurosSimples
                    ? base * (taxa/100.0) * i
                    : base * (Math.pow(1+taxa/100.0,i)-1);
            }
            valores.add(base + acr);
        }

        // preview HTML
        if (preview) {
            StringBuilder html = new StringBuilder("<html><table border='1'>")
                .append("<tr><th>Parcela</th><th>Vencimento</th><th>Valor</th></tr>");
            for (int i = 0; i < nParcelas; i++) {
                html.append("<tr><td>")
                    .append(i+1).append("</td><td>")
                    .append(fmtBR.format(datasVenc.get(i))).append("</td><td>")
                    .append(String.format(Locale.US,"R$ %,.2f", valores.get(i)))
                    .append("</td></tr>");
            }
            html.append("</table></html>");
            JOptionPane.showMessageDialog(parent, html.toString(),
                "Pré-visualização", JOptionPane.PLAIN_MESSAGE);
        }

        // grava título
        String tituloId   = UUID.randomUUID().toString();
        String dataGeracao= fmtSQL.format(new Date());
        TituloContaPagarModel titulo = new TituloContaPagarModel(
    tituloId,
    fornecedorId,
    planoContaId,
    tituloId,
    dataGeracao,
    valorTotal,
    "aberto",
    null, // ou pedidoId se você for passar depois
    obs
);

        tituloDAO.inserir(titulo);

        // grava parcelas
        for (int i = 0; i < nParcelas; i++) {
            Date dt       = datasVenc.get(i);
            double valor  = valores.get(i);
            double acr    = valor - base;
            ParcelaContaPagarModel p = new ParcelaContaPagarModel(
                null,
                tituloId,
                i+1,
                fmtSQL.format(dt),
                base,
                (taxa>0 && jurosSimples) ? base*(taxa/100.0) : 0,
                acr,
                0,
                0,
                null,
                null,
                false,
                null,
                "aberto"
            );
            parcelaDAO.inserir(p);
        }
    }

    /**
     * Registra um pagamento de uma parcela específica.
     * Atualiza o status da parcela e não do título (pode ser complementado).
     */
    public void registrarPagamento(int parcelaId, double valorPago, Date dataPagamento, String formaPagamento)
            throws SQLException {
        ParcelaContaPagarModel parcela = parcelaDAO.buscarPorId(parcelaId);
        if (parcela == null) {
            throw new SQLException("Parcela não encontrada para pagamento.");
        }

        double novoPago = parcela.getValorPago() + valorPago;
        parcela.setValorPago(novoPago);

        String dataStr = fmtSQL.format(dataPagamento);
        parcela.setDataPagamento(dataStr);
        parcela.setDataCompensacao(dataStr);

        if (novoPago >= (parcela.getValorNominal() + parcela.getValorAcrescimo())) {
            parcela.setStatus("pago");
            parcela.setPagoComDesconto(novoPago < (parcela.getValorNominal() + parcela.getValorAcrescimo()));
        }

        parcelaDAO.atualizar(parcela);
    }
}
