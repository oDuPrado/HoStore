package util;

import dao.ConfigLojaDAO;
import dao.ProdutoDAO;
import model.ConfigLojaModel;
import model.VendaItemModel;

import javax.swing.table.TableModel;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fonte ÚNICA do layout do cupom fiscal (texto).
 * - VendaFinalizarDialog usa pra mostrar em tela
 * - PDFGenerator usa pra gerar PDF e imprimir na térmica
 *
 * Regra: qualquer ajuste visual do cupom acontece aqui.
 */
public class CupomFiscalFormatter {

    // 48 colunas costuma encaixar bem em 80mm (térmica) e fica mais “nota”
    public static final int RECEIPT_WIDTH = 48;

    private static final Locale LOCALE_BR = new Locale("pt", "BR");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(LOCALE_BR);

    private CupomFiscalFormatter() {
    }

    /** Linha simples de pagamento (pra não depender de UI/TableModel no core) */
    public static class PagamentoLinha {
        public final String tipo;
        public final double valor;

        public PagamentoLinha(String tipo, double valor) {
            this.tipo = tipo;
            this.valor = valor;
        }
    }

    /** Parcelamento (opcional) */
    public static class ParcelamentoInfo {
        public final int parcelas;
        public final double jurosPct;
        public final int intervaloDias;

        public ParcelamentoInfo(int parcelas, double jurosPct, int intervaloDias) {
            this.parcelas = parcelas;
            this.jurosPct = jurosPct;
            this.intervaloDias = intervaloDias;
        }
    }

    /**
     * Versão principal: gera texto do cupom.
     */
    public static String gerarTextoCupom(
            int vendaId,
            List<VendaItemModel> itens,
            List<PagamentoLinha> pagamentos,
            ParcelamentoInfo parcelamento,
            double acrescimo) {
        ConfigLojaModel cfg = carregarConfigLojaSilencioso();

        String nomeLoja = safe(cfg != null ? cfg.getNome() : null, "Loja");
        String nomeFantasia = safe(cfg != null ? cfg.getNomeFantasia() : null, "");
        String cnpj = safe(cfg != null ? cfg.getCnpj() : null, "");
        String ie = safe(cfg != null ? cfg.getInscricaoEstadual() : null, "");
        String tel = safe(cfg != null ? cfg.getTelefone() : null, "");
        String email = safe(cfg != null ? cfg.getEmail() : null, "");

        String logradouro = safe(cfg != null ? cfg.getEnderecoLogradouro() : null, "");
        String numero = safe(cfg != null ? cfg.getEnderecoNumero() : null, "");
        String bairro = safe(cfg != null ? cfg.getEnderecoBairro() : null, "");
        String municipio = safe(cfg != null ? cfg.getEnderecoMunicipio() : null, "");
        String uf = safe(cfg != null ? cfg.getEnderecoUf() : null, "");
        String cep = safe(cfg != null ? cfg.getEnderecoCep() : null, "");

        String modeloNota = safe(cfg != null ? cfg.getModeloNota() : null, "65");
        String serieNota = safe(cfg != null ? cfg.getSerieNota() : null, "001");
        int numeroInicial = (cfg != null ? cfg.getNumeroInicialNota() : 1);
        int numeroCupom = numeroInicial + vendaId;

        String rodape = safe(cfg != null ? cfg.getTextoRodapeNota() : null, "Obrigado pela preferência!");

        StringBuilder sb = new StringBuilder();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        // Cabeçalho mais “nota”
        sb.append(line('=')).append('\n');
        sb.append(center(nomeLoja.toUpperCase())).append('\n');
        if (!nomeFantasia.isBlank() && !nomeFantasia.equalsIgnoreCase(nomeLoja)) {
            sb.append(center(nomeFantasia)).append('\n');
        }
        if (!cnpj.isBlank())
            sb.append(center("CNPJ: " + formatCnpj(cnpj))).append('\n');
        if (!ie.isBlank())
            sb.append(center("IE: " + ie)).append('\n');

        String endereco = joinEnd(logradouro, numero, bairro);
        String cidadeUf = joinCity(municipio, uf, cep);

        if (!endereco.isBlank())
            sb.append(center(endereco)).append('\n');
        if (!cidadeUf.isBlank())
            sb.append(center(cidadeUf)).append('\n');

        if (!tel.isBlank())
            sb.append(center("Tel: " + tel)).append('\n');
        if (!email.isBlank())
            sb.append(center(email)).append('\n');

        sb.append(line('-')).append('\n');
        sb.append(center("CUPOM FISCAL ELETRÔNICO")).append('\n');
        sb.append(center("NFC-e  Modelo: " + modeloNota + "  Série: " + serieNota)).append('\n');
        sb.append(center("Número: " + numeroCupom + "   Venda: #" + vendaId)).append('\n');
        sb.append(center(now)).append('\n');
        sb.append(line('-')).append('\n');

        // Itens
        // Deixa a “cara de nota”: item / descrição / qtd x vl un / total
        // ✅ Otimização: Cachear produtos em HashMap para evitar N+1 queries
        ProdutoDAO pdao = new ProdutoDAO();
        java.util.Map<String, String> produtoCache = new java.util.HashMap<>();
        for (VendaItemModel it : itens) {
            if (!produtoCache.containsKey(it.getProdutoId())) {
                try {
                    var prod = pdao.findById(it.getProdutoId());
                    String nome = (prod != null && prod.getNome() != null && !prod.getNome().isBlank())
                            ? prod.getNome()
                            : it.getProdutoId();
                    produtoCache.put(it.getProdutoId(), nome);
                } catch (Exception e) {
                    produtoCache.put(it.getProdutoId(), it.getProdutoId());
                }
            }
        }

        double totBruto = 0.0;
        double totDesconto = 0.0;
        double totLiquido = 0.0;

        int itemIdx = 1;
        for (VendaItemModel it : itens) {
            int qtd = it.getQtd();
            double unit = it.getPreco();
            double brutoLinha = unit * qtd;
            double descLinha = brutoLinha * (it.getDesconto() / 100.0);
            double totalLinha = brutoLinha - descLinha;

            // Usar cache ao invés de chamada individual de DB
            String nomeProduto = produtoCache.getOrDefault(it.getProdutoId(), it.getProdutoId());
            nomeProduto = limitar(nomeProduto, RECEIPT_WIDTH);

            // Linha 1: "001 DESCRICAO..."
            sb.append(String.format("%03d %s%n", itemIdx++, nomeProduto));

            // Linha 2: "QTD x VLUN = TOTAL"
            // Ex: " 2 x 9,90 19,80"
            String qtdStr = padLeft(String.valueOf(qtd), 3);
            String unitStr = padLeft(MOEDA.format(unit), 12);
            String totalStr = padLeft(MOEDA.format(totalLinha), 12);

            sb.append(String.format("   %s x %s %s%n",
                    qtdStr,
                    unitStr,
                    totalStr));

            // Linha 3: desconto (se houver)
            if (descLinha > 0.0) {
                sb.append(String.format("   Desconto: %s%n", MOEDA.format(descLinha)));
            }

            sb.append(line('.')).append('\n');

            totBruto += brutoLinha;
            totDesconto += descLinha;
            totLiquido += totalLinha;
        }

        sb.append(line('-')).append('\n');

        // Totais
        sb.append(totalLine("TOTAL BRUTO", totBruto)).append('\n');
        if (totDesconto > 0)
            sb.append(totalLine("DESCONTOS", totDesconto)).append('\n');
        sb.append(line('-')).append('\n');
        if (acrescimo > 0) {
            sb.append(totalLine("ACRESCIMO", acrescimo)).append('\n');
            totLiquido += acrescimo;
        }
        sb.append(totalLineBold("TOTAL A PAGAR", totLiquido)).append('\n');
        sb.append(line('-')).append('\n');

        // Pagamentos
        sb.append(center("PAGAMENTOS")).append('\n');
        double pago = 0.0;
        if (pagamentos != null) {
            for (PagamentoLinha p : pagamentos) {
                String tipo = safe(p.tipo, "OUTROS");
                double v = p.valor;
                pago += v;
                sb.append(pagamentoLine(tipo, v)).append('\n');
            }
        }
        sb.append(line('-')).append('\n');

        double diff = pago - totLiquido;
        if (diff > 0) {
            sb.append(totalLine("TROCO", diff)).append('\n');
        }

        // Parcelamento (se houver)
        if (parcelamento != null && parcelamento.parcelas > 1) {
            sb.append(line('-')).append('\n');
            sb.append(center("PARCELAMENTO")).append('\n');

            double totalComJuros = totLiquido * (1 + (parcelamento.jurosPct / 100.0));
            double valorParcela = totalComJuros / parcelamento.parcelas;

            sb.append(String.format("Parcelas: %dx%n", parcelamento.parcelas));
            sb.append(String.format("Juros: %.2f%%%n", parcelamento.jurosPct));
            sb.append(String.format("Intervalo: %d dias%n", parcelamento.intervaloDias));
            sb.append(String.format("Total c/ juros: %s%n", MOEDA.format(totalComJuros)));
            sb.append(String.format("Valor parcela:  %s%n", MOEDA.format(valorParcela)));
        }

        // Área “fiscal” fake por enquanto (seu sistema não emite NFC-e real ainda)
        sb.append(line('-')).append('\n');
        sb.append(center("INFORMAÇÕES FISCAIS")).append('\n');
        sb.append(center("SEM VALOR FISCAL (MODO DEMO)")).append('\n');
        sb.append(center("Chave NFC-e: " + "0000 0000 0000 0000 0000 0000 0000 0000 0000")).append('\n');

        sb.append(line('=')).append('\n');
        sb.append(center(rodape)).append('\n');
        sb.append(center("Volte sempre!")).append('\n');
        sb.append(line('=')).append('\n');

        return sb.toString();
    }

    /** Helper: converte TableModel em lista simples */
    public static List<PagamentoLinha> fromTableModel(TableModel tm) {
        List<PagamentoLinha> list = new ArrayList<>();
        if (tm == null)
            return list;

        for (int i = 0; i < tm.getRowCount(); i++) {
            Object a = tm.getValueAt(i, 0);
            Object b = tm.getValueAt(i, 1);
            String tipo = (a == null) ? "OUTROS" : a.toString();
            double valor = 0.0;
            if (b instanceof Number n)
                valor = n.doubleValue();
            else if (b != null) {
                try {
                    valor = Double.parseDouble(b.toString().replace(",", "."));
                } catch (Exception ignored) {
                }
            }
            list.add(new PagamentoLinha(tipo, valor));
        }
        return list;
    }

    // -------------------- Layout helpers --------------------

    private static String line(char c) {
        return String.valueOf(c).repeat(RECEIPT_WIDTH);
    }

    private static String center(String text) {
        if (text == null)
            text = "";
        text = text.trim();
        if (text.length() >= RECEIPT_WIDTH)
            return text.substring(0, RECEIPT_WIDTH);

        int pad = (RECEIPT_WIDTH - text.length()) / 2;
        return " ".repeat(Math.max(0, pad)) + text;
    }

    private static String padLeft(String text, int len) {
        if (text == null)
            text = "";
        if (text.length() >= len)
            return text.substring(0, len);
        return " ".repeat(len - text.length()) + text;
    }

    private static String limitar(String text, int len) {
        if (text == null)
            return "";
        text = text.trim();
        if (text.length() <= len)
            return text;
        return text.substring(0, len);
    }

    private static String safe(String v, String def) {
        if (v == null)
            return def;
        String s = v.trim();
        return s.isEmpty() ? def : s;
    }

    private static String totalLine(String label, double valor) {
        String l = label.toUpperCase();
        String v = MOEDA.format(valor);
        // label à esquerda, valor à direita
        int espaço = RECEIPT_WIDTH - l.length() - v.length();
        if (espaço < 1)
            espaço = 1;
        return l + " ".repeat(espaço) + v;
    }

    private static String totalLineBold(String label, double valor) {
        // “bold” em texto: coloca marcador visual
        return totalLine("** " + label + " **", valor);
    }

    private static String pagamentoLine(String tipo, double valor) {
        String t = safe(tipo, "OUTROS").toUpperCase();
        String v = MOEDA.format(valor);
        int espaço = RECEIPT_WIDTH - t.length() - v.length();
        if (espaço < 1)
            espaço = 1;
        return t + " ".repeat(espaço) + v;
    }

    private static String resolverNomeProduto(ProdutoDAO pdao, String produtoId) {
        try {
            var p = pdao.findById(produtoId);
            if (p != null && p.getNome() != null && !p.getNome().isBlank())
                return p.getNome();
        } catch (Exception ignored) {
        }
        return produtoId == null ? "" : produtoId;
    }

    private static ConfigLojaModel carregarConfigLojaSilencioso() {
        try {
            return new ConfigLojaDAO().buscar();
        } catch (SQLException e) {
            return null;
        }
    }

    private static String joinEnd(String logradouro, String numero, String bairro) {
        String a = safe(logradouro, "");
        String n = safe(numero, "");
        String b = safe(bairro, "");

        String s = a;
        if (!n.isBlank())
            s += ", " + n;
        if (!b.isBlank())
            s += " - " + b;
        return s.trim();
    }

    private static String joinCity(String municipio, String uf, String cep) {
        String m = safe(municipio, "");
        String u = safe(uf, "");
        String c = safe(cep, "");
        String s = "";

        if (!m.isBlank() && !u.isBlank())
            s = m + " - " + u;
        else if (!m.isBlank())
            s = m;
        else if (!u.isBlank())
            s = u;

        if (!c.isBlank())
            s = (s.isBlank() ? "" : s + "  ") + "CEP: " + c;
        return s.trim();
    }

    private static String formatCnpj(String cnpj) {
        String raw = cnpj.replaceAll("\\D", "");
        if (raw.length() != 14)
            return cnpj;
        return raw.substring(0, 2) + "." + raw.substring(2, 5) + "." + raw.substring(5, 8) + "/" +
                raw.substring(8, 12) + "-" + raw.substring(12);
    }
}
