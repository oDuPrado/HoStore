package service;

import dao.ImpostoICMSDAO;
import dao.ImpostoIPIDAO;
import dao.ImpostoPisCofinsDAO;
import model.*;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Cálculo de impostos por item fiscal.
 * Usa tabelas imposto_icms, imposto_ipi, imposto_pis_cofins com fallback para 0/default.
 */
public class FiscalCalcService {

    private final ImpostoICMSDAO imICMS = new ImpostoICMSDAO();
    private final ImpostoIPIDAO imIPI = new ImpostoIPIDAO();
    private final ImpostoPisCofinsDAO imPisCofins = new ImpostoPisCofinsDAO();

    /**
     * Calcula ICMS para item.
     * Busca em imposto_icms(estado, estado_destino, ncm, aliquota_consumidor)
     * Se não encontrar: retorna 0 (fallback safe para não quebrar emissão)
     */
    public ImpostoCalculado calcICMS(String ncm, String ufOrigem, String ufDestino,
                                     double baseCalculo, String tipoOperacao) throws SQLException {
        ImpostoCalculado resultado = new ImpostoCalculado();
        resultado.tipo = "ICMS";
        resultado.cst = "00";  // Default

        try {
            ImpostoIcmsModel icms = imICMS.buscarPorNcmEUf(ncm, ufOrigem, ufDestino);
            if (icms != null) {
                // NFCe usa aliquota_consumidor
                Double aliq = icms.getAliquotaConsumidor();
                if (aliq != null && aliq > 0) {
                    // Aplicar redução de base se existir
                    double base = baseCalculo * (1 - icms.getReducaoBase() / 100.0);
                    resultado.valor = base * aliq / 100.0;
                    resultado.aliquota = aliq;
                    resultado.cst = "00";  // Tributável
                }
            }
        } catch (Exception e) {
            // Log warning e fallback
            System.err.println("⚠️ Erro ao buscar ICMS para NCM=" + ncm + ", UF=" + ufDestino + ": " + e.getMessage());
        }

        if (resultado.valor == 0) {
            resultado.aliquota = 0.0;
        }
        return resultado;
    }

    /**
     * Calcula IPI para item.
     * Busca em imposto_ipi(ncm, aliquota)
     * Se não encontrar: retorna 0
     */
    public ImpostoCalculado calcIPI(String ncm, double baseCalculo) throws SQLException {
        ImpostoCalculado resultado = new ImpostoCalculado();
        resultado.tipo = "IPI";

        try {
            ImpostoIpiModel ipi = imIPI.buscarPorNcm(ncm);
            if (ipi != null && ipi.getAliquota() != null && ipi.getAliquota() > 0) {
                resultado.valor = baseCalculo * ipi.getAliquota() / 100.0;
                resultado.aliquota = ipi.getAliquota();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao buscar IPI para NCM=" + ncm + ": " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Calcula PIS para item.
     * Busca em imposto_pis_cofins(ncm, cst_pis, aliquota_pis)
     * Se não encontrar: retorna 0
     */
    public ImpostoCalculado calcPIS(String ncm, double baseCalculo) throws SQLException {
        ImpostoCalculado resultado = new ImpostoCalculado();
        resultado.tipo = "PIS";
        resultado.cst = "04";  // Isento (default para Simples Nacional)

        try {
            ImpostoPisCofinsModel pc = imPisCofins.buscarPorNcm(ncm);
            if (pc != null && pc.getAliquotaPis() != null && pc.getAliquotaPis() > 0) {
                resultado.valor = baseCalculo * pc.getAliquotaPis() / 100.0;
                resultado.aliquota = pc.getAliquotaPis();
                resultado.cst = pc.getCstPis() != null ? pc.getCstPis() : "04";
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao buscar PIS para NCM=" + ncm + ": " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Calcula COFINS para item.
     * Busca em imposto_pis_cofins(ncm, cst_cofins, aliquota_cofins)
     * Se não encontrar: retorna 0
     */
    public ImpostoCalculado calcCOFINS(String ncm, double baseCalculo) throws SQLException {
        ImpostoCalculado resultado = new ImpostoCalculado();
        resultado.tipo = "COFINS";
        resultado.cst = "04";  // Isento (default para Simples Nacional)

        try {
            ImpostoPisCofinsModel pc = imPisCofins.buscarPorNcm(ncm);
            if (pc != null && pc.getAliquotaCofins() != null && pc.getAliquotaCofins() > 0) {
                resultado.valor = baseCalculo * pc.getAliquotaCofins() / 100.0;
                resultado.aliquota = pc.getAliquotaCofins();
                resultado.cst = pc.getCstCofins() != null ? pc.getCstCofins() : "04";
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao buscar COFINS para NCM=" + ncm + ": " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Cálculo completo de todos os impostos para um item.
     * Retorna estrutura pronta para XML.
     */
    public ImpostosItem calcularImpostosCompletos(String ncm, String ufOrigem, String ufDestino,
                                                   double valorItem) throws SQLException {
        ImpostosItem impostos = new ImpostosItem();
        impostos.ncm = ncm;
        impostos.baseCalculo = valorItem;

        impostos.icms = calcICMS(ncm, ufOrigem, ufDestino, valorItem, "venda");
        impostos.ipi = calcIPI(ncm, valorItem);
        impostos.pis = calcPIS(ncm, valorItem);
        impostos.cofins = calcCOFINS(ncm, valorItem);

        // Total de impostos
        impostos.totalImpostos = impostos.icms.valor + impostos.ipi.valor +
                                 impostos.pis.valor + impostos.cofins.valor;

        return impostos;
    }

    /**
     * Cálculo simplificado para NFC-e (Simples Nacional):
     * - ICMS = 0 (apenas CSOSN declarado)
     * - PIS/COFINS = 0
     * - IPI pode ser calculado por NCM (se existir)
     */
    public ImpostosItem calcularImpostosSimples(String ncm, String csosn, String origem,
                                                double valorItem) throws SQLException {
        ImpostosItem impostos = new ImpostosItem();
        impostos.ncm = ncm;
        impostos.baseCalculo = valorItem;

        ImpostoCalculado icms = new ImpostoCalculado();
        icms.tipo = "ICMS";
        icms.cst = (csosn == null || csosn.isBlank()) ? "102" : csosn;
        icms.aliquota = 0.0;
        icms.valor = 0.0;
        impostos.icms = icms;

        impostos.ipi = calcIPI(ncm, valorItem);

        ImpostoCalculado pis = new ImpostoCalculado();
        pis.tipo = "PIS";
        pis.cst = "04";
        pis.aliquota = 0.0;
        pis.valor = 0.0;
        impostos.pis = pis;

        ImpostoCalculado cofins = new ImpostoCalculado();
        cofins.tipo = "COFINS";
        cofins.cst = "04";
        cofins.aliquota = 0.0;
        cofins.valor = 0.0;
        impostos.cofins = cofins;

        impostos.totalImpostos = impostos.icms.valor + impostos.ipi.valor +
                                 impostos.pis.valor + impostos.cofins.valor;
        return impostos;
    }

    /**
     * Modelo para resultado de cálculo de um imposto
     */
    public static class ImpostoCalculado {
        public String tipo;        // "ICMS", "IPI", "PIS", "COFINS"
        public String cst;         // CST/CSOSN
        public double aliquota = 0.0;
        public double valor = 0.0;

        @Override
        public String toString() {
            return tipo + ": " + String.format("%.2f", valor) + " (" + aliquota + "%)";
        }
    }

    /**
     * Modelo consolidado de impostos para um item
     */
    public static class ImpostosItem {
        public String ncm;
        public double baseCalculo;
        public ImpostoCalculado icms;
        public ImpostoCalculado ipi;
        public ImpostoCalculado pis;
        public ImpostoCalculado cofins;
        public double totalImpostos;
    }
}
