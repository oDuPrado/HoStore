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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

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
            int vendaId = controller.finalizar(clienteId, formaFinal, parcelas);

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

    private JButton criarBotao(String txt) {
        JButton b = new JButton(txt);
        b.setBackground(new Color(60, 63, 65));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    /* ─────────────────── Comprovante ─────────────────── */
    public static class ComprovanteDialog extends JDialog {
        public ComprovanteDialog(Dialog owner,
                int vendaId,
                List<VendaItemModel> itens,
                String formaFinal,
                int parcelas,
                double juros,
                String periodo,
                TableModel pagamentos) {

            super(owner, "Comprovante Venda #" + vendaId, true);
            setSize(480, 650);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(8, 8));
            ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

            JTextArea ta = new JTextArea();
            ta.setEditable(false);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            NumberFormat cf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            StringBuilder sb = new StringBuilder();

            /* Cabeçalho Loja */
            sb.append("      HoStore - Sistema de Vendas\n")
                    .append("    CNPJ: 12.345.678/0001-99\n")
                    .append("    Rua Exemplo, 123 - Centro\n\n");

            sb.append(String.format("Venda #: %-5d  Data: %s\n",
                    vendaId,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))))
                    .append("\n");

            /* Itens */
            sb.append(String.format("%-20s %4s %10s %6s %10s\n",
                    "Produto", "Qtd", "V.Unit", "Desc%", "Total"));
            sb.append("-----------------------------------------------------------\n");

            ProdutoDAO pdao = new ProdutoDAO();
            double totBruto = 0, totDesc = 0;
            final double[] totLiquido = { 0 };

            for (VendaItemModel it : itens) {
                String nome;
                try {
                    nome = pdao.findById(it.getProdutoId()).getNome();
                } catch (Exception e) {
                    nome = it.getProdutoId();
                }
                int qtd = it.getQtd();
                double unit = it.getPreco();
                double descV = unit * qtd * it.getDesconto() / 100.0;
                double linha = unit * qtd - descV;

                sb.append(String.format("%-20.20s %4d %10s %5.0f%% %10s\n",
                        nome, qtd, cf.format(unit), it.getDesconto(), cf.format(linha)));

                totBruto += unit * qtd;
                totDesc += descV;
                totLiquido[0] += linha;
            }
            sb.append("-----------------------------------------------------------\n");
            sb.append(String.format("%-36s %10s\n", "Total bruto:", cf.format(totBruto)));
            sb.append(String.format("%-36s %10s\n", "Desconto:", cf.format(totDesc)));
            // Soma o valor total pago (com juros incluído)
            double totalPago = 0;
            for (int i = 0; i < pagamentos.getRowCount(); i++) {
                totalPago += (Double) pagamentos.getValueAt(i, 1);
            }
            sb.append(String.format("%-36s %10s\n", "Total produtos:", cf.format(totLiquido[0])));
            sb.append(String.format("%-36s %10s\n", "Total líquido:", cf.format(totalPago)))

                    .append("\n");

            /* Pagamentos */
            sb.append("Pagamentos:\n");
            for (int i = 0; i < pagamentos.getRowCount(); i++) {
                sb.append(String.format("  %-12s %10s\n",
                        pagamentos.getValueAt(i, 0),
                        cf.format((Double) pagamentos.getValueAt(i, 1))));
            }
            sb.append("\n");

            /* Parcelas cartão (se houver) */
            if (parcelas > 1 && pagamentos.getRowCount() > 0) {
                for (int i = 0; i < pagamentos.getRowCount(); i++) {
                    if ("CARTAO".equalsIgnoreCase((String) pagamentos.getValueAt(i, 0))) {
                        double valorCartao = (Double) pagamentos.getValueAt(i, 1);
                        double valorParc = valorCartao / parcelas;
                        int dias = "15 dias".equals(periodo) ? 15 : 30;
                        sb.append("Parcelamento cartão: ")
                                .append(parcelas).append("x de ").append(cf.format(valorParc))
                                .append(juros > 0 ? ("  Juros: " + juros + "%\n") : "\n");
                        LocalDate data = LocalDate.now().plusDays(dias);
                        for (int p = 1; p <= parcelas; p++) {
                            sb.append(String.format("  %2d/%d  %s  %s\n",
                                    p, parcelas,
                                    data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    cf.format(valorParc)));
                            data = data.plusDays(dias);
                        }
                        sb.append("\n");
                        break;
                    }
                }
            }

            sb.append("Forma de pagamento: ").append(formaFinal).append("\n");
            sb.append("Obrigado pela preferência!\nVolte sempre à HoStore.\n");

            ta.setText(sb.toString());
            add(new JScrollPane(ta), BorderLayout.CENTER);

            /* Botões */
            JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnPdf = botao("Imprimir PDF");
            btnPdf.addActionListener(ev -> {
                try {
                    VendaModel vm = new VendaModel(
                            String.valueOf(vendaId), null, 0, 0, totLiquido[0], null, parcelas, null);
                    vm.setItens(itens);
                    PDFGenerator.gerarComprovanteVenda(vm, itens);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            JButton btnClose = botao("Fechar");
            btnClose.addActionListener(ev -> dispose());
            b.add(btnPdf);
            b.add(btnClose);
            add(b, BorderLayout.SOUTH);
        }

        private JButton botao(String t) {
            JButton b = new JButton(t);
            b.setBackground(new Color(60, 63, 65));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            return b;
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
