package service;

import dao.ParcelaContaPagarDAO;
import dao.TituloContaPagarDAO;
import model.ParcelaContaPagarModel;
import model.TituloContaPagarModel;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;


import javax.swing.*;
import java.util.List;
import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Regras de negócio de contas a pagar.
 * Permite gerar títulos com lista arbitrária de vencimentos,
 * com ou sem aplicação de juros simples / composto,
 * além de pré-visualização opcional.
 */
public class ContaPagarService {

    private final TituloContaPagarDAO tituloDAO   = new TituloContaPagarDAO();
    private final ParcelaContaPagarDAO parcelaDAO = new ParcelaContaPagarDAO();
    private final SimpleDateFormat fmtSQL         = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat fmtBR          = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Gera um novo título a pagar usando uma lista de datas de vencimento.
     * @param fornecedorId   ID do fornecedor
     * @param valorTotal     valor total do título
     * @param datasVenc      lista de objetos java.util.Date (uma por parcela)
     * @param aplicarJuros   true = aplica juros
     * @param jurosSimples   válido se aplicarJuros=true
     * @param taxa           percentual
     * @param preview        mostra preview HTML se true
     * @param parent         componente para JOptionPane
     * @param obs            observações
     */
    public void gerarTituloComDatas(String fornecedorId,
                                    double valorTotal,
                                    List<Date> datasVenc,
                                    boolean jurosSimples,
                                    double taxa,
                                    boolean preview,
                                    Component parent,
                                    String obs) throws SQLException {

        int nParcelas = datasVenc.size();
        double base = valorTotal / nParcelas;
        List<Double> valores = new ArrayList<>();

        for (int i=1;i<=nParcelas;i++){
            double acr = 0;
            if (taxa>0){
                acr = jurosSimples
                    ? base * (taxa/100.0) * i
                    : base * (Math.pow(1+taxa/100.0,i)-1);
            }
            valores.add(base+acr);
        }

        if (preview){
            StringBuilder html=new StringBuilder("<html><table border='1'>");
            html.append("<tr><th>Parcela</th><th>Vencimento</th><th>Valor (R$)</th></tr>");
            for (int i=0;i<nParcelas;i++){
                html.append("<tr><td>")
                    .append(i+1).append("</td><td>")
                    .append(fmtBR.format(datasVenc.get(i))).append("</td><td>")
                    .append(String.format(Locale.US,"R$ %,.2f",valores.get(i)))
                    .append("</td></tr>");
            }
            html.append("</table></html>");
            JOptionPane.showMessageDialog(parent,html.toString(),
                "Pré-visualização",JOptionPane.PLAIN_MESSAGE);
        }

        /* grava título */
        String tituloId = UUID.randomUUID().toString();
        String dataGeracao = fmtSQL.format(new Date());
        TituloContaPagarModel titulo = new TituloContaPagarModel(
            tituloId,fornecedorId,tituloId,dataGeracao,
            valorTotal,"aberto",obs
        );
        tituloDAO.inserir(titulo);

        /* grava parcelas */
        for (int i=0;i<nParcelas;i++){
            Date dt = datasVenc.get(i);
            double valorFinal = valores.get(i);
            double acr = valorFinal - base;

            ParcelaContaPagarModel p = new ParcelaContaPagarModel(
                null,tituloId,i+1,fmtSQL.format(dt),
                base,                                          // valor nominal
                (taxa>0 && jurosSimples) ? base*(taxa/100.0) : 0,
                acr,                                           // acréscimo (juros)
                0,0,null,null,false,null,"aberto"
            );
            parcelaDAO.inserir(p);
        }

    }

    /**
 * Registra um pagamento de uma parcela específica.
 */
public void registrarPagamento(int parcelaId, double valorPago, Date dataPagamento, String formaPagamento) throws SQLException {
    ParcelaContaPagarModel parcela = parcelaDAO.buscarPorId(parcelaId);

    if (parcela == null) {
        throw new SQLException("Parcela não encontrada para pagamento.");
    }

    double novoValorPago = parcela.getValorPago() + valorPago;
    parcela.setValorPago(novoValorPago);

    SimpleDateFormat fmtSQL = new SimpleDateFormat("yyyy-MM-dd");
    String dataPagStr = fmtSQL.format(dataPagamento);

    parcela.setDataPagamento(dataPagStr);
    parcela.setDataCompensacao(dataPagStr);

    if (novoValorPago >= (parcela.getValorNominal() + parcela.getValorAcrescimo())) {
        parcela.setStatus("pago");
        parcela.setPagoComDesconto(novoValorPago < (parcela.getValorNominal() + parcela.getValorAcrescimo()));
    }

    parcelaDAO.atualizar(parcela);
}

}
