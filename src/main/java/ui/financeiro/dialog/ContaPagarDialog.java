package ui.financeiro.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.toedter.calendar.JDateChooser;
import dao.FornecedorDAO;
import dao.PlanoContaDAO;
import dao.ParcelaContaPagarDAO;
import dao.TituloContaPagarDAO;
import model.PlanoContaModel;
import model.ParcelaContaPagarModel;
import service.ContaPagarService;
import util.UiKit;
import util.FormatterFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

public class ContaPagarDialog extends JDialog {

    /* ───── Campos UI ─────────────────────────────────────────────────────── */
    private final JComboBox<String> cbFornecedor = new JComboBox<>();
    private final JFormattedTextField ftValorTotal = FormatterFactory.getMoneyField(0.0);

    private final JComboBox<String> cbTipoPagamento = new JComboBox<>(new String[] { "À Vista", "A Prazo" });
    private final JSpinner spQtdParcelas = new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));

    private final JComboBox<String> cbPlano = new JComboBox<>(new String[] { "Dias", "Intervalos" });
    private final JComboBox<String> cbDias = new JComboBox<>(new String[] {
            "5", "7", "12", "15", "25", "28", "30", "45", "60",
            "75", "90", "120", "Outros"
    });
    private final JFormattedTextField ftDiasCustom = new JFormattedTextField(NumberFormat.getIntegerInstance());
    private final JComboBox<String> cbIntervalo = new JComboBox<>(new String[] {
            "15,30,45,60",
            "7,15,30,45",
            "15,45,90,120"
    });

    private final JDateChooser dtBase = new JDateChooser(new Date());
    private final JTextArea taObs = new JTextArea(4, 28);

    private final JButton btnSelecionarConta = UiKit.ghost("Selecionar Conta Contábil…");
    private final JButton btnSelecionarPedidos = UiKit.ghost("Vincular Pedidos…");

    // IDs dos pedidos vinculados
    private final Set<String> pedidosVinculados = new HashSet<>();
    private PlanoContaModel contaSelecionada = null;

    /* ───── Serviço / estado ──────────────────────────────────────────────── */
    private final ContaPagarService service = new ContaPagarService();
    private final ParcelaContaPagarModel parcelaEdit; // null => novo título
    private final SimpleDateFormat fmtSQL = new SimpleDateFormat("yyyy-MM-dd");

    public ContaPagarDialog(Frame owner) {
        this(owner, null);
    }

    public ContaPagarDialog(Frame owner, ParcelaContaPagarModel parcela) {
        super(owner, parcela == null ? "Nova Conta a Pagar" : "Editar Parcela", true);
        this.parcelaEdit = parcela;

        UiKit.applyDialogBase(this);
        buildUI();

        pack();
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(owner);

        if (parcelaEdit != null)
            preencherEdicao(parcelaEdit);
    }

    /* ───── Construção visual ─────────────────────────────────────────────── */
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(UiKit.title(parcelaEdit == null ? "Nova Conta a Pagar" : "Editar Parcela"));
        header.add(UiKit.hint("Preencha os dados do título. Parcelas são geradas automaticamente quando for a prazo."));
        root.add(header, BorderLayout.NORTH);

        // Conteúdo: 2 cards
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 0;

        // Card 1: Dados principais
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        content.add(cardDadosPrincipais(), gc);

        // Card 2: Parcelamento / vínculos / obs
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 1;
        gc.weighty = 1;
        content.add(cardParcelamento(), gc);

        root.add(content, BorderLayout.CENTER);

        // Footer (ações)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton btnSalvar = UiKit.primary(parcelaEdit == null ? "Salvar" : "Atualizar");
        JButton btnCancelar = UiKit.ghost("Cancelar");
        btnSalvar.addActionListener(this::onSalvar);
        btnCancelar.addActionListener(e -> dispose());

        footer.add(btnCancelar);
        footer.add(btnSalvar);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);

        // Popula fornecedores
        carregarFornecedores();

        // Ajustes de componentes
        prepararCampos();

        // Listeners reativos
        cbTipoPagamento.addActionListener(e -> atualizarModoPagamento());
        cbPlano.addActionListener(e -> atualizarPlano());
        cbDias.addActionListener(e -> ftDiasCustom.setEnabled("Outros".equals(cbDias.getSelectedItem())));

        // botão de seleção de conta
        btnSelecionarConta.addActionListener(e -> selecionarContaContabil());

        // vincular pedidos
        btnSelecionarPedidos.addActionListener(e -> {
            VincularPedidosDialog dlg = new VincularPedidosDialog((Frame) SwingUtilities.getWindowAncestor(this));
            Set<String> selecionados = dlg.showDialog();
            if (selecionados != null && !selecionados.isEmpty()) {
                pedidosVinculados.clear();
                pedidosVinculados.addAll(selecionados);
                btnSelecionarPedidos.setText("🔗 " + pedidosVinculados.size() + " pedido(s) vinculado(s)");
            }
        });

        atualizarModoPagamento();
        atualizarPlano();
    }

    private JPanel cardDadosPrincipais() {
        JPanel card = UiKit.card();
        card.setLayout(new GridBagLayout());

        GridBagConstraints gc = baseGc();

        // Fornecedor
        addField(card, gc, 0, "Fornecedor", cbFornecedor);

        // Valor
        addField(card, gc, 1, "Valor Total (R$)", ftValorTotal);

        // Tipo pagamento
        addField(card, gc, 2, "Tipo Pagamento", cbTipoPagamento);

        // Conta contábil
        addField(card, gc, 3, "Conta Contábil", btnSelecionarConta);

        return card;
    }

    private JPanel cardParcelamento() {
        JPanel card = UiKit.card();
        card.setLayout(new GridBagLayout());

        GridBagConstraints gc = baseGc();

        // Qtd parcelas
        addField(card, gc, 0, "Qtd. Parcelas", spQtdParcelas);

        // Plano parcelas (linha com vários campos)
        JPanel linhaPlano = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linhaPlano.setOpaque(false);
        linhaPlano.add(cbPlano);
        linhaPlano.add(cbDias);
        linhaPlano.add(ftDiasCustom);
        linhaPlano.add(cbIntervalo);

        addField(card, gc, 1, "Plano de Parcelas", linhaPlano);

        // Vencimento base
        addField(card, gc, 2, "Vencimento Base", dtBase);

        // Pedidos
        addField(card, gc, 3, "Vincular Pedido", btnSelecionarPedidos);

        // Observações (ocupa o resto)
        taObs.setLineWrap(true);
        taObs.setWrapStyleWord(true);
        taObs.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Observações internas...");
        JScrollPane spObs = UiKit.scroll(taObs);
        spObs.setPreferredSize(new Dimension(520, 130));

        GridBagConstraints g2 = (GridBagConstraints) gc.clone();
        g2.gridy = 4;
        g2.weighty = 1;
        g2.fill = GridBagConstraints.BOTH;

        JPanel obsWrap = new JPanel(new BorderLayout(6, 6));
        obsWrap.setOpaque(false);
        obsWrap.add(new JLabel("Observações"), BorderLayout.NORTH);
        obsWrap.add(spObs, BorderLayout.CENTER);

        card.add(obsWrap, g2);

        return card;
    }

    private GridBagConstraints baseGc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.weighty = 0;
        return gc;
    }

    private void addField(JPanel parent, GridBagConstraints base, int row, String label, Component field) {
        GridBagConstraints gl = (GridBagConstraints) base.clone();
        gl.gridy = row;
        gl.gridx = 0;
        gl.weightx = 0;
        gl.fill = GridBagConstraints.NONE;
        gl.anchor = GridBagConstraints.WEST;

        JLabel l = new JLabel(label);
        parent.add(l, gl);

        GridBagConstraints gf = (GridBagConstraints) base.clone();
        gf.gridy = row;
        gf.gridx = 1;
        gf.weightx = 1;
        gf.fill = GridBagConstraints.HORIZONTAL;

        parent.add(field, gf);
    }

    private void prepararCampos() {
        // Fornecedor
        cbFornecedor.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        cbTipoPagamento.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        cbPlano.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        cbDias.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        cbIntervalo.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");

        // Valor total
        ftValorTotal.setColumns(10);
        ftValorTotal.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");

        // Parcelas
        ((JComponent) spQtdParcelas.getEditor()).putClientProperty(FlatClientProperties.STYLE, "arc: 10;");

        // Dias custom
        ftDiasCustom.setColumns(4);
        ftDiasCustom.setEnabled(false);
        ftDiasCustom.setValue(30);
        ftDiasCustom.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");

        // DateChooser
        dtBase.setDateFormatString("dd/MM/yyyy");
        prepararDateChooser(dtBase);

        // Botões ghost já vêm padronizados pelo UiKit, mas dá um arc extra se quiser
        btnSelecionarConta.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0;");
        btnSelecionarPedidos.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0;");
    }

    private void prepararDateChooser(JDateChooser dc) {
        dc.setPreferredSize(new Dimension(170, 30));
        if (dc.getDateEditor() != null) {
            JComponent editor = dc.getDateEditor().getUiComponent();
            if (editor instanceof JComponent) {
                editor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 1;");
            }
        }
        JButton calBtn = dc.getCalendarButton();
        if (calBtn != null) {
            calBtn.setText("📅");
            calBtn.setFocusPainted(false);
            calBtn.setMargin(new Insets(2, 8, 2, 8));
            calBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0; font: +1;");
            calBtn.setToolTipText("Selecionar data");
        }
    }

    private void carregarFornecedores() {
        cbFornecedor.removeAllItems();
        cbFornecedor.addItem("Selecione...");
        try {
            new FornecedorDAO().listar(null, null, null, null).forEach(f -> cbFornecedor.addItem(f.getNome()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void selecionarContaContabil() {
        try {
            PlanoContaDAO planoDao = new PlanoContaDAO();
            List<PlanoContaModel> contas = planoDao.listarTodos();

            PlanoContaModel sel = (PlanoContaModel) JOptionPane.showInputDialog(
                    this,
                    "Escolha a Conta Contábil:",
                    "Plano de Contas",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    contas.toArray(),
                    contaSelecionada);

            if (sel != null) {
                contaSelecionada = sel;
                btnSelecionarConta.setText(buildHierarchy(sel));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro ao carregar Plano de Contas:\n" + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ───── Construção da hierarquia para exibição no botão ─────────── */
    private String buildHierarchy(PlanoContaModel pc) throws SQLException {
        List<String> names = new ArrayList<>();
        PlanoContaDAO dao = new PlanoContaDAO();

        PlanoContaModel current = pc;
        while (current != null) {
            names.add(current.getDescricao());
            current = current.getParentId() != null ? dao.buscarPorId(current.getParentId()) : null;
        }

        Collections.reverse(names);
        return String.join(" > ", names);
    }

    /* ───── Handlers de UI ────────────────────────────────────────────────── */
    private void atualizarModoPagamento() {
        boolean aPrazo = "A Prazo".equals(cbTipoPagamento.getSelectedItem());
        spQtdParcelas.setEnabled(aPrazo);
        cbPlano.setEnabled(aPrazo);
        cbDias.setEnabled(aPrazo);
        cbIntervalo.setEnabled(aPrazo);

        if (!aPrazo)
            spQtdParcelas.setValue(1);
        atualizarPlano();
    }

    private void atualizarPlano() {
        boolean aPrazo = "A Prazo".equals(cbTipoPagamento.getSelectedItem());
        if (!aPrazo) {
            cbDias.setEnabled(false);
            cbIntervalo.setEnabled(false);
            ftDiasCustom.setEnabled(false);
            return;
        }

        boolean byDias = "Dias".equals(cbPlano.getSelectedItem());
        cbDias.setEnabled(byDias);
        cbIntervalo.setEnabled(!byDias);
        ftDiasCustom.setEnabled(byDias && "Outros".equals(cbDias.getSelectedItem()));
    }

    /* ───── Salvar / Atualizar ────────────────────────────────────────────── */
    private void onSalvar(ActionEvent evt) {
        try {
            if (cbFornecedor.getSelectedIndex() == 0) {
                JOptionPane.showMessageDialog(this, "Escolha o fornecedor.");
                return;
            }
            if (contaSelecionada == null) {
                JOptionPane.showMessageDialog(this, "Selecione a Conta Contábil.");
                return;
            }

            Number nValor = (Number) ftValorTotal.getValue();
            double valorTotal = (nValor == null) ? 0.0 : nValor.doubleValue();
            if (valorTotal <= 0) {
                JOptionPane.showMessageDialog(this, "Informe um valor total maior que zero.");
                return;
            }

            String fornecedorId = new FornecedorDAO().obterIdPorNome((String) cbFornecedor.getSelectedItem());
            String planoContaId = contaSelecionada.getId();

            boolean aPrazo = "A Prazo".equals(cbTipoPagamento.getSelectedItem());
            int numParcelas = aPrazo ? (Integer) spQtdParcelas.getValue() : 1;

            List<Date> datas = calcularVencimentos(numParcelas);

            // juros + preview (mantive sua lógica, só organizei)
            boolean jurosSimples = true;
            double taxa = 0;

            if (numParcelas >= 2) {
                boolean aplicarJuros = JOptionPane.showConfirmDialog(
                        this, "Aplicar juros por parcela?", "Juros",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

                if (aplicarJuros) {
                    String tipoSel = (String) JOptionPane.showInputDialog(
                            this, "Tipo de Juros:", "Juros",
                            JOptionPane.PLAIN_MESSAGE, null,
                            new String[] { "Simples", "Composto" }, "Simples");
                    if (tipoSel == null)
                        return;

                    jurosSimples = "Simples".equals(tipoSel);

                    String taxaStr = JOptionPane.showInputDialog(this, "Taxa de juros (%) por parcela:");
                    if (taxaStr == null)
                        return;

                    taxa = Double.parseDouble(taxaStr.replace(',', '.'));
                }
            }

            boolean preview = JOptionPane.showConfirmDialog(
                    this,
                    "Deseja pré-visualizar as parcelas geradas?",
                    "Pré-visualização",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

            if (parcelaEdit == null) {
                service.gerarTituloComDatas(
                        fornecedorId,
                        planoContaId,
                        valorTotal,
                        datas,
                        jurosSimples,
                        taxa,
                        preview,
                        this,
                        taObs.getText().trim());
            } else {
                // edição de parcela: só altera valor e vencimento
                parcelaEdit.setValorNominal(valorTotal);
                parcelaEdit.setVencimento(fmtSQL.format(datas.get(0)));
                new ParcelaContaPagarDAO().atualizar(parcelaEdit);
            }

            JOptionPane.showMessageDialog(this, "Dados salvos com sucesso!");
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Erro: " + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ───── Geração de vencimentos ───────────────────────────────────────── */
    private List<Date> calcularVencimentos(int parcelas) throws Exception {
        if (dtBase.getDate() == null)
            throw new IllegalStateException("Informe a data base.");

        LocalDate base = dtBase.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        List<Date> out = new ArrayList<>();

        if (parcelas <= 1) {
            out.add(dtBase.getDate());
            return out;
        }

        if ("Dias".equals(cbPlano.getSelectedItem())) {
            int intervalo;

            if ("Outros".equals(cbDias.getSelectedItem())) {
                Number n = (Number) ftDiasCustom.getValue();
                if (n == null || n.intValue() <= 0)
                    throw new IllegalArgumentException("Informe o intervalo de dias.");
                intervalo = n.intValue();
            } else {
                intervalo = Integer.parseInt((String) cbDias.getSelectedItem());
            }

            for (int p = 0; p < parcelas; p++) {
                out.add(java.sql.Date.valueOf(base.plusDays((long) intervalo * p)));
            }
        } else {
            String[] parts = ((String) cbIntervalo.getSelectedItem()).split(",");
            int[] seq = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();

            for (int p = 0; p < parcelas; p++) {
                int offset;
                if (p < seq.length) {
                    offset = seq[p];
                } else {
                    int last = seq[seq.length - 1];
                    int prev = seq[Math.max(0, seq.length - 2)];
                    int step = Math.max(1, last - prev);
                    offset = last + step * (p - seq.length + 1);
                }
                out.add(java.sql.Date.valueOf(base.plusDays(offset)));
            }
        }

        return out;
    }

    /* ───── Pré-preenchimento na edição ──────────────────────────────────── */
    private void preencherEdicao(ParcelaContaPagarModel p) {
        try {
            String fId = new TituloContaPagarDAO().buscarPorId(p.getTituloId()).getFornecedorId();
            String nome = new FornecedorDAO().buscarPorId(fId).getNome();
            cbFornecedor.setSelectedItem(nome);
        } catch (Exception ignored) {
        }

        ftValorTotal.setValue(p.getValorNominal());
        cbTipoPagamento.setSelectedItem("À Vista");
        spQtdParcelas.setEnabled(false);

        try {
            dtBase.setDate(java.sql.Date.valueOf(p.getVencimento()));
        } catch (Exception ignored) {
        }

        // teu código original tava jogando status dentro de observação (???)
        // mantive vazio pra não perpetuar esse crime.
        taObs.setText("");
    }
}
