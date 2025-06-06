// src/ui/venda/dialog/VendaFinalizarDialog.java
package ui.venda.dialog;

import controller.VendaController;
import dao.ProdutoDAO;
import util.DB;
import util.AlertUtils;
import util.PDFGenerator;
import model.VendaItemModel;
import model.VendaModel;
import ui.venda.painel.PainelVendas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import model.ConfigLojaModel;
import dao.ConfigLojaDAO;

/**
 * Dialog de finalização de venda com:
 * - múltiplas formas de pagamento
 * - parcelamento (cartão) via ParcelamentoDialog
 * - baixa estoque e geração de comprovante/PDF
 */
public class VendaFinalizarDialog extends JDialog {

    /* ======= Dependências ======= */
    private final VendaController controller;
    private final PainelVendas painelPai;
    private final String clienteId;
    private final List<VendaItemModel> itens;

    /* ======= Resumo (labels) ======= */
    private final JLabel lblBruto = new JLabel();
    private final JLabel lblDesc = new JLabel();
    private final JLabel lblLiquido = new JLabel();

    /* ======= Pagamentos (UI) ======= */
    private JTable pagamentosTable;
    private DefaultTableModel pagamentosModel;
    private JComboBox<String> cboForma;
    private JFormattedTextField txtValor;

    /*
     * ---- Campos de cartão (exibem valores escolhidos no ParcelamentoDialog) ----
     */
    private final JTextField txtParcelas = new JTextField("1", 4);
    private final JFormattedTextField txtJuros;
    private final JComboBox<String> cboPeriodo = new JComboBox<>(new String[] { "15 dias", "30 dias" });

    // Configuração de parcelamento – populada quando o usuário fecha o
    // ParcelamentoDialog em OK
    private ParcelamentoDialog.ParcelamentoConfig parcelamentoConfig = new ParcelamentoDialog.ParcelamentoConfig();

    /* ---- Rodapé ---- */
    private final JLabel lblPago = new JLabel();
    private final JLabel lblTroco = new JLabel();

    public VendaFinalizarDialog(Dialog owner,
            VendaController controller,
            String clienteId,
            PainelVendas painelPai) {

        super(owner, "Finalizar Venda", true);
        this.controller = controller;
        this.painelPai = painelPai;
        this.clienteId = clienteId;
        this.itens = new ArrayList<>(controller.getCarrinho());

        /* === Formatter para juros % === */
        NumberFormatter pctFmt = new NumberFormatter(NumberFormat.getNumberInstance(new Locale("pt", "BR")));
        pctFmt.setValueClass(Double.class);
        pctFmt.setMinimum(0.0);
        pctFmt.setMaximum(999.0);
        pctFmt.setAllowsInvalid(false);
        txtJuros = new JFormattedTextField(pctFmt);
        txtJuros.setColumns(4);
        txtJuros.setValue(0.0);

        setSize(800, 540);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        add(createResumoPanel(), BorderLayout.NORTH);
        add(createPagamentosPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        atualizarValores();
    }

    /* ======= Painel Resumo ======= */
    private JPanel createResumoPanel() {
        JPanel p = new JPanel(new GridLayout(0, 2, 4, 4));
        p.setBorder(BorderFactory.createTitledBorder("Resumo da Venda"));

        double bruto = 0, descV = 0;
        for (VendaItemModel it : itens) {
            double b = it.getQtd() * it.getPreco();
            bruto += b;
            descV += b * it.getDesconto() / 100.0;
        }
        double liquido = bruto - descV;
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        p.add(new JLabel("Itens:"));
        p.add(new JLabel(String.valueOf(itens.size())));
        p.add(new JLabel("Valor Bruto:"));
        p.add(lblBruto);
        p.add(new JLabel("Desconto:"));
        p.add(lblDesc);
        p.add(new JLabel("Total Líquido:"));
        p.add(lblLiquido);

        lblBruto.setText(nf.format(bruto));
        lblDesc.setText(String.format("%,.2f%%  (%s)", descV / bruto * 100, nf.format(descV)));
        lblLiquido.setText(nf.format(liquido));
        return p;
    }

    /* ======= Painel Pagamentos ======= */
    private JPanel createPagamentosPanel() {

        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createTitledBorder("Pagamentos"));

        /* -------- Layout vertical: duas linhas -------- */
        JPanel entrada = new JPanel();
        entrada.setLayout(new BoxLayout(entrada, BoxLayout.Y_AXIS));

        /* ---- Linha 1: forma + valor ---- */
        JPanel linha1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        cboForma = new JComboBox<>(new String[] { "DINHEIRO", "PIX", "CARTAO", "VALE-PRESENTE", "OUTROS" });
        linha1.add(new JLabel("Forma:"));
        linha1.add(cboForma);

        NumberFormat moneyFmt = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        moneyFmt.setMinimumFractionDigits(2);
        moneyFmt.setMaximumFractionDigits(2);
        NumberFormatter moneyFormatter = new NumberFormatter(moneyFmt);
        moneyFormatter.setValueClass(Double.class);
        moneyFormatter.setMinimum(0.0);
        moneyFormatter.setAllowsInvalid(true);
        moneyFormatter.setOverwriteMode(false);
        txtValor = new JFormattedTextField(moneyFormatter);
        txtValor.setColumns(8);
        linha1.add(new JLabel("Valor:"));
        linha1.add(txtValor);
        entrada.add(linha1);

        /* ---- Linha 2: detalhes cartão + botão ---- */
        JPanel linha2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        linha2.add(new JLabel("Parcelas:"));
        linha2.add(txtParcelas);
        linha2.add(new JLabel("Juros%:"));
        linha2.add(txtJuros);
        linha2.add(new JLabel("Período:"));
        linha2.add(cboPeriodo);
        txtParcelas.setEditable(false);
        txtJuros.setEditable(false);
        cboPeriodo.setEnabled(false);

        JButton btnParcelamento = new JButton("Configurar Parcelamento");
        linha2.add(btnParcelamento);
        entrada.add(linha2);

        // escondidos até escolher CARTAO
        txtParcelas.setVisible(false);
        txtJuros.setVisible(false);
        cboPeriodo.setVisible(false);
        btnParcelamento.setVisible(false);

        /* ---- alterna visibilidade quando muda a forma ----- */
        cboForma.addActionListener(e -> {
            boolean card = "CARTAO".equalsIgnoreCase((String) cboForma.getSelectedItem());
            txtParcelas.setVisible(card);
            txtJuros.setVisible(card);
            cboPeriodo.setVisible(card);
            btnParcelamento.setVisible(card);
            entrada.revalidate();
            entrada.repaint();
        });

        btnParcelamento.addActionListener(e -> {
            // total líquido atual para pré-cálculo
            // Calcula total da venda
            double totalVenda = 0;
            for (VendaItemModel it : itens)
                totalVenda += it.getQtd() * it.getPreco() * (1 - it.getDesconto() / 100.0);

            // Subtrai pagamentos já adicionados
            double totalPagos = 0;
            for (int i = 0; i < pagamentosModel.getRowCount(); i++) {
                totalPagos += (Double) pagamentosModel.getValueAt(i, 1);
            }

            // Valor restante a pagar (usado como base do parcelamento)
            double valorRestante = Math.max(0, totalVenda - totalPagos);

            // Abre o diálogo de parcelamento
            ParcelamentoDialog dlg = new ParcelamentoDialog(
                    SwingUtilities.getWindowAncestor(this),
                    parcelamentoConfig,
                    valorRestante);

            dlg.setVisible(true);

            if (dlg.isOk()) {
                // Atualiza a configuração
                parcelamentoConfig = dlg.getConfig();

                // Preenche os campos visuais (informativos)
                txtParcelas.setText(String.valueOf(parcelamentoConfig.parcelas));
                txtJuros.setValue(parcelamentoConfig.juros);
                cboPeriodo.setSelectedItem(parcelamentoConfig.intervaloDias + " dias");

                // Preenche o campo de valor com o valor restante (ajustado)
                txtValor.setValue(valorRestante);
            }
        });

        /* ---- Botão adicionar pagamento ---- */
        JButton btnAdd = criarBotao("Adicionar");
        btnAdd.addActionListener(e -> onAddPagamento());
        linha2.add(btnAdd);

        painel.add(entrada, BorderLayout.NORTH);

        /* ---- tabela pagamentos ---- */
        pagamentosModel = new DefaultTableModel(new String[] { "Forma", "Valor", "" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 2;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == 1 ? Double.class : String.class;
            }
        };
        pagamentosTable = new JTable(pagamentosModel);
        pagamentosTable.getColumnModel().getColumn(2)
                .setCellRenderer(new ButtonRenderer("🗑"));
        pagamentosTable.getColumnModel().getColumn(2)
                .setCellEditor(new ButtonEditor(evt -> {
                    int r = pagamentosTable.getSelectedRow();
                    if (r >= 0)
                        pagamentosModel.removeRow(r);
                    atualizarValores();
                }));
        painel.add(new JScrollPane(pagamentosTable), BorderLayout.CENTER);
        return painel;
    }

    /* ======= Rodapé ======= */
    private JPanel createFooterPanel() {
        JPanel rod = new JPanel(new BorderLayout());
        lblPago.setFont(lblPago.getFont().deriveFont(Font.BOLD, 14f));
        lblTroco.setFont(lblTroco.getFont().deriveFont(Font.BOLD, 14f));
        JPanel val = new JPanel(new GridLayout(1, 2, 4, 4));
        val.add(lblPago);
        val.add(lblTroco);
        rod.add(val, BorderLayout.WEST);

        JButton btnConf = criarBotao("Confirmar");
        btnConf.addActionListener(e -> onConfirm());
        JButton btnCan = criarBotao("Cancelar");
        btnCan.addActionListener(e -> dispose());
        JPanel bts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bts.add(btnCan);
        bts.add(btnConf);
        rod.add(bts, BorderLayout.EAST);
        return rod;
    }

    /* ======= Ações ======= */
    private void onAddPagamento() {
        String forma = (String) cboForma.getSelectedItem();
        double valor;

        if ("CARTAO".equalsIgnoreCase(forma)) {
            // 1. Calcula valor total da venda
            double totalVenda = 0;
            for (VendaItemModel it : itens)
                totalVenda += it.getQtd() * it.getPreco() * (1 - it.getDesconto() / 100.0);

            // 2. Soma valores já pagos nas outras formas
            double totalPagos = 0;
            for (int i = 0; i < pagamentosModel.getRowCount(); i++) {
                String tipo = (String) pagamentosModel.getValueAt(i, 0);
                if (!"CARTAO".equalsIgnoreCase(tipo)) { // ignora cartão já adicionado
                    totalPagos += (Double) pagamentosModel.getValueAt(i, 1);
                }
            }

            // 3. Calcula valor base restante para o cartão (sem juros)
            double valorBaseCartao = Math.max(0, totalVenda - totalPagos);

            if (valorBaseCartao <= 0) {
                AlertUtils.error("Nada restante a pagar com cartão.");
                return;
            }

            // 4. Aplica juros somente sobre esse valor
            double juros = parcelamentoConfig.juros;
            valor = valorBaseCartao * (1 + juros / 100.0);

        } else {
            try {
                txtValor.commitEdit();
            } catch (Exception ignored) {
            }

            Number valorRaw = (Number) txtValor.getValue();

            if (valorRaw == null || valorRaw.doubleValue() <= 0.0) {
                AlertUtils.error("Informe um valor válido!");
                return;
            }

            valor = valorRaw.doubleValue();
        }

        pagamentosModel.addRow(new Object[] { forma, valor, "" });
        txtValor.setValue(null);
        atualizarValores();
    }

    private void atualizarValores() {
        double liquido = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                .parse(lblLiquido.getText(), new java.text.ParsePosition(0)).doubleValue();
        double pago = 0;
        for (int i = 0; i < pagamentosModel.getRowCount(); i++)
            pago += (Double) pagamentosModel.getValueAt(i, 1);
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        lblPago.setText("Pago:  " + nf.format(pago));
        lblTroco.setText("Troco: " + nf.format(pago - liquido));
    }

    private void onConfirm() {
        try {
            /* --- formaPagamento + parcelas --- */
            String formaFinal;
            int parcelas = parcelamentoConfig.parcelas;

            if (pagamentosModel.getRowCount() == 1) {
                formaFinal = (String) pagamentosModel.getValueAt(0, 0);
            } else {
                formaFinal = "MULTI";
            }

            /* 1) Grava venda */
            int vendaId = controller.finalizar(
                    clienteId,
                    formaFinal,
                    parcelas,
                    parcelamentoConfig.intervaloDias,
                    calcularDataPrimeiroVencimentoISO());

            /* 2) Grava pagamentos */
            try (Connection c = DB.get()) {
                for (int i = 0; i < pagamentosModel.getRowCount(); i++) {
                    String forma = (String) pagamentosModel.getValueAt(i, 0);
                    Double valor = (Double) pagamentosModel.getValueAt(i, 1);
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO vendas_pagamentos(venda_id,tipo,valor) VALUES (?,?,?)")) {
                        ps.setInt(1, vendaId);
                        ps.setString(2, forma);
                        ps.setDouble(3, valor);
                        ps.executeUpdate();
                    }
                }
            }

            /* 3) Atualiza UI principal */
            painelPai.carregarVendas(null, null, "Todos", "Todos");
            dispose();

            /* 4) Comprovante */
            double juros = parcelamentoConfig.juros;
            String periodo = parcelamentoConfig.intervaloDias + " dias";

            new ComprovanteDialog((Dialog) getOwner(), vendaId, itens,
                    formaFinal, parcelas, juros, periodo,
                    pagamentosModel).setVisible(true);

        } catch (Exception ex) {
            AlertUtils.error("Erro ao finalizar venda:\n" + ex.getMessage());
        }
    }

    private String calcularDataPrimeiroVencimentoISO() {
        int dias = parcelamentoConfig.intervaloDias;
        LocalDate venc = LocalDate.now().plusDays(dias);
        return venc.format(DateTimeFormatter.ISO_DATE); // retorna yyyy-MM-dd
    }

    private JButton criarBotao(String txt) {
        JButton b = new JButton(txt);
        b.setBackground(new Color(60, 63, 65));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    /*
     * ─────────────────── Comprovante Fiscal (Cupom Eletrônico) ───────────────────
     */
    public static class ComprovanteDialog extends JDialog {
        private static final int RECEIPT_WIDTH = 44; // largura padrão para formatação (44 colunas)

        public ComprovanteDialog(Dialog owner,
                int vendaId,
                List<VendaItemModel> itens,
                String formaFinal,
                int parcelas,
                double juros,
                String periodo,
                TableModel pagamentos) {
            super(owner, "Comprovante Fiscal - Venda #" + vendaId, true);
            setSize(480, 650);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(8, 8));
            ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

            // Text area para exibir o cupom
            JTextArea ta = new JTextArea();
            ta.setEditable(false);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            // ⚠️ Adiciona o componente na tela
            add(new JScrollPane(ta), BorderLayout.CENTER);

            // Formatação de moeda em Real
            NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            StringBuilder sb = new StringBuilder();

            // =========== 1) Cabeçalho Fiscal ===========
            ConfigLojaModel config = null;
            try {
                config = new ConfigLojaDAO().buscar();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Erro ao carregar dados da loja:\n" + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }

            // Proteções contra valores nulos
            String nomeLoja = (config != null && config.getNome() != null) ? config.getNome() : "Loja Padrão";
            String cnpjLoja = (config != null && config.getCnpj() != null) ? config.getCnpj() : "00.000.000/0000-00";
            String telefoneLoja = (config != null && config.getTelefone() != null) ? config.getTelefone() : "";
            String textoRodape = (config != null && config.getTextoRodapeNota() != null)
                    ? config.getTextoRodapeNota()
                    : "";
            String modeloNota = (config != null && config.getModeloNota() != null) ? config.getModeloNota() : "65";
            String serieNota = (config != null && config.getSerieNota() != null) ? config.getSerieNota() : "001";

            // Evita erro de null ao somar com vendaId
            int numeroInicial = (config != null) ? config.getNumeroInicialNota() : 1;
            int numeroCupom = numeroInicial + vendaId;

            // Linhas de cabeçalho: centralizar dentro de RECEIPT_WIDTH
            sb.append(center(nomeLoja.toUpperCase(), RECEIPT_WIDTH)).append("\n");
            if (!cnpjLoja.isEmpty()) {
                sb.append(center("CNPJ: " + cnpjLoja, RECEIPT_WIDTH)).append("\n");
            }
            if (!telefoneLoja.isEmpty()) {
                sb.append(center("Tel: " + telefoneLoja, RECEIPT_WIDTH)).append("\n");
            }
            sb.append("\n");

            sb.append(center("CUPOM FISCAL ELETRÔNICO", RECEIPT_WIDTH)).append("\n");
            sb.append(center("SÉRIE: " + serieNota + "    Nº: " + numeroCupom, RECEIPT_WIDTH)).append("\n");
            sb.append(center("MODELO: " + modeloNota, RECEIPT_WIDTH)).append("\n");
            sb.append(center(formatDateTime(LocalDateTime.now()), RECEIPT_WIDTH)).append("\n");
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");

            // =========== 2) Itens da Venda ===========
            // Cabeçalho da tabela de itens
            sb.append(String.format("%-20s %4s %7s %6s %7s\n",
                    padRight("PRODUTO", 20),
                    padLeft("QTD", 4),
                    padLeft("UN", 7),
                    padLeft("VL.UNI", 6),
                    padLeft("TOTAL", 7)));
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");

            // Inicializa acumuladores
            double totBruto = 0.0;
            double totDesconto = 0.0;

            // Wrapper final para permitir uso em lambdas (ex: geração de PDF)
            final double[] totLiquidoFinal = { 0.0 };

            ProdutoDAO pdao = new ProdutoDAO();

            for (VendaItemModel it : itens) {
                String nomeProduto;
                try {
                    nomeProduto = pdao.findById(it.getProdutoId()).getNome();
                } catch (Exception e) {
                    nomeProduto = it.getProdutoId(); // fallback: mostra o ID se falhar
                }

                // Trunca nome do produto se exceder 20 caracteres
                if (nomeProduto.length() > 20) {
                    nomeProduto = nomeProduto.substring(0, 20);
                }

                int qtd = it.getQtd();
                double unit = it.getPreco();
                double descV = unit * qtd * it.getDesconto() / 100.0;
                double linhaVl = unit * qtd - descV;

                // Formata quantidade com três casas decimais
                String unidadeFormatada = String.format(Locale.US, "%.3f", qtd * 1.0).replace('.', ',');

                // Linha formatada: PRODUTO QTD UN VL.UNI TOTAL
                sb.append(String.format("%-20s %4s %7s %6s %7s\n",
                        padRight(nomeProduto, 20),
                        padLeft(String.valueOf(qtd), 4),
                        padLeft(unidadeFormatada, 7),
                        padLeft(cf.format(unit), 6),
                        padLeft(cf.format(linhaVl), 7)));

                // Acumula totais
                totBruto += unit * qtd;
                totDesconto += descV;
                totLiquidoFinal[0] += linhaVl;
            }

            sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");

            // =========== 3) Totais ===========
            sb.append(padRight("Total Bruto:", ThirtySixChars(RECEIPT_WIDTH))).append(padLeft(cf.format(totBruto), 8))
                    .append("\n");
            sb.append(padRight("Descontos:", ThirtySixChars(RECEIPT_WIDTH))).append(padLeft(cf.format(totDesconto), 8))
                    .append("\n");
            sb.append(padRight("Total Líquido:", ThirtySixChars(RECEIPT_WIDTH)))
                    .append(padLeft(cf.format(totLiquidoFinal[0]), 8)).append("\n");

            sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");

            // =========== 4) Pagamentos ===========
            sb.append(center("PAGAMENTOS", RECEIPT_WIDTH)).append("\n");
            for (int i = 0; i < pagamentos.getRowCount(); i++) {
                String tipoPagamento = pagamentos.getValueAt(i, 0).toString();
                double valorPagto = (Double) pagamentos.getValueAt(i, 1);
                sb.append(String.format("%-22s %20s\n",
                        padRight(tipoPagamento, 22),
                        padLeft(cf.format(valorPagto), 20)));
            }
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");

            // =========== 5) Parcelamento Cartão (se houver) ===========
            if (parcelas > 1) {
                // Ajusta para usar o valor dentro do array final
                double valorLiquido = totLiquidoFinal[0];

                sb.append("Parcelamento Cartão: ").append(parcelas).append("x de ")
                        .append(cf.format((valorLiquido / parcelas) * (1 + juros / 100.0))).append("\n");
                sb.append("Juros: ").append(String.format(Locale.US, "%.2f", juros)).append("%\n");
                sb.append("Primeira Parcela em: ")
                        .append(formatDate(LocalDateTime.now().plusDays(parsePeriodo(periodo)))).append("\n");
                sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");
            }

            // =========== 6) Informações Fiscais Complementares ===========
            sb.append(center("CHAVE DE ACESSO NFC-e", RECEIPT_WIDTH)).append("\n");
            sb.append(center("0000 0000 0000 0000 0000 0000 0000 0000 0000", RECEIPT_WIDTH)).append("\n");
            sb.append(center("DANFE NFC-e em http://www.sefa.gov.br", RECEIPT_WIDTH)).append("\n");
            sb.append(repeatChar('-', RECEIPT_WIDTH)).append("\n");

            // =========== 7) Rodapé ===========
            sb.append(center(textoRodape, RECEIPT_WIDTH)).append("\n");

            // Aviso legal: sem valor fiscal
            sb.append(center("SEM VALOR FISCAL", RECEIPT_WIDTH)).append("\n");
            sb.append("\n");
            sb.append(center("Obrigado pela preferência!", RECEIPT_WIDTH)).append("\n");
            sb.append(center("Volte sempre!", RECEIPT_WIDTH)).append("\n");

            ta.setText(sb.toString());
            ta.setCaretPosition(0);

            // =========== 8) Botões de ação ===========
JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
JButton btnPdf = botao("Imprimir PDF");
btnPdf.addActionListener(ev -> {
    try {
        // Cria model temporário com dados da venda
        VendaModel vm = new VendaModel(
                String.valueOf(vendaId),
                null, 0, 0, totLiquidoFinal[0], null, parcelas, null);
        vm.setItens(itens);

        // Gera nome do arquivo com timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nomeArquivo = "comprovante_" + vendaId + "_" + timestamp + ".pdf";

        // Cria pasta se não existir
        java.io.File pasta = new java.io.File("data/export");
        if (!pasta.exists()) pasta.mkdirs();

        // Caminho final do PDF
        java.io.File destino = new java.io.File(pasta, nomeArquivo);

        // Gera o PDF usando o novo método
        PDFGenerator.gerarComprovanteVenda(vm, itens, destino.getAbsolutePath());

        JOptionPane.showMessageDialog(this,
            "Comprovante gerado com sucesso:\n" + destino.getPath(),
            "Sucesso", JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,
            "Erro ao gerar PDF:\n" + ex.getMessage(),
            "Erro", JOptionPane.ERROR_MESSAGE);
    }
});
JButton btnClose = botao("Fechar");
btnClose.addActionListener(ev -> dispose());
b.add(btnPdf);
b.add(btnClose);
add(b, BorderLayout.SOUTH);
}

        /**
         * Cria um botão estilizado.
         */
        private JButton botao(String t) {
            JButton b = new JButton(t);
            b.setBackground(new Color(60, 63, 65));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            return b;
        }

        // ─────────── Métodos auxiliares de formatação ───────────

        /**
         * Centraliza o texto em uma largura fixa de caracteres.
         */
        private String center(String text, int width) {
            if (text == null) {
                text = "";
            }
            if (text.length() >= width) {
                return text.substring(0, width);
            }
            int padding = (width - text.length()) / 2;
            return repeatChar(' ', padding) + text;
        }

        /**
         * Repete um caractere 'count' vezes e retorna como String.
         */
        private String repeatChar(char c, int count) {
            return String.valueOf(c).repeat(Math.max(0, count));
        }

        /**
         * Preenche a direita com espaços até atingir 'length' caracteres.
         */
        private String padRight(String text, int length) {
            if (text == null) {
                text = "";
            }
            if (text.length() >= length) {
                return text.substring(0, length);
            }
            return text + " ".repeat(length - text.length());
        }

        /**
         * Preenche a esquerda com espaços até atingir 'length' caracteres.
         */
        private String padLeft(String text, int length) {
            if (text == null) {
                text = "";
            }
            if (text.length() >= length) {
                return text.substring(0, length);
            }
            int pad = length - text.length();
            return " ".repeat(pad) + text;
        }

        /**
         * Retorna a representação de data/hora no formato dd/MM/yyyy HH:mm:ss.
         */
        private String formatDateTime(LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }

        /**
         * Retorna apenas data no formato dd/MM/yyyy a partir de LocalDateTime.
         */
        private String formatDate(LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        /**
         * Converte o período (por exemplo, "15 dias" ou "30 dias") para número de dias.
         */
        private int parsePeriodo(String periodo) {
            try {
                return Integer.parseInt(periodo.replaceAll("\\D+", ""));
            } catch (Exception e) {
                return 0;
            }
        }

        /**
         * Calcula quantos espaços usar à direita em linhas de total (36 = RECEIPT_WIDTH
         * - 8).
         * Ajusta para alinhar o valor na coluna certa.
         */
        private int ThirtySixChars(int totalWidth) {
            return totalWidth - 8; // reserva 8 colunas para formatação de valor (ex.: "R$ 999,99")
        }
    }

    /* ─────────── Button Renderer/Editor (remover linha) ─────────── */
    class ButtonRenderer extends JButton implements TableCellRenderer {
        ButtonRenderer(String txt) {
            setText(txt);
            setOpaque(true);
            setFocusPainted(false);
        }

        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean s, boolean f, int r, int c) {
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private final JButton btn = new JButton("🗑");
        private final java.util.function.Consumer<Void> action;
        private boolean clicked;

        ButtonEditor(java.util.function.Consumer<Void> action) {
            super(new JCheckBox());
            this.action = action;
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                clicked = true;
                fireEditingStopped();
            });
        }

        public Component getTableCellEditorComponent(JTable tbl, Object v, boolean s, int r, int c) {
            clicked = false;
            return btn;
        }

        public Object getCellEditorValue() {
            if (clicked && action != null)
                action.accept(null);
            return "";
        }
    }
}
