package ui.clientes.dialog;

import model.ClienteModel;
import service.CreditoLojaService;
import util.UiKit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class CreditoLojaDialog extends JDialog {
    private final CreditoLojaService service = new CreditoLojaService();
    private final ClienteModel cliente;

    private JLabel lblCliente;
    private JLabel lblSaldo;

    private JRadioButton rbEntrada;
    private JRadioButton rbUso;

    private JTextField txtValor;
    private JTextField txtReferencia;

    public CreditoLojaDialog(Window parent, ClienteModel cliente) {
        super(parent, "Crédito de Loja – " + cliente.getNome(), ModalityType.APPLICATION_MODAL);
        this.cliente = cliente;

        UiKit.applyDialogBase(this);

        setSize(760, 420);
        setResizable(true);
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        loadSaldo();
        setLocationRelativeTo(parent);

        bindKeys();
        SwingUtilities.invokeLater(() -> txtValor.requestFocusInWindow());
    }

    private JComponent buildHeader() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(UiKit.title("💰 Crédito de Loja"));
        left.add(UiKit.hint("Entrada adiciona saldo | Uso consome saldo"));
        card.add(left, BorderLayout.WEST);

        lblCliente = UiKit.hint("Cliente: " + cliente.getNome() + " (CPF: " + cliente.getCpf() + ")");
        card.add(lblCliente, BorderLayout.EAST);

        return card;
    }

    private JComponent buildCenter() {
        JPanel wrap = new JPanel(new BorderLayout(10, 10));
        wrap.setOpaque(false);

        // ===== Card de saldo =====
        JPanel saldoCard = UiKit.card();
        saldoCard.setLayout(new BorderLayout(10, 6));

        lblSaldo = new JLabel("Saldo atual: —");
        lblSaldo.putClientProperty("FlatLaf.style", "font: +2");
        saldoCard.add(lblSaldo, BorderLayout.WEST);

        saldoCard.add(UiKit.hint("Dica: referência pode ser motivo ou ID da venda/devolução."), BorderLayout.SOUTH);

        wrap.add(saldoCard, BorderLayout.NORTH);

        // ===== Card de movimentação (agora bem explícito) =====
        JPanel movCard = UiKit.card();
        movCard.setLayout(new BorderLayout(8, 8));

        JPanel movHeader = new JPanel(new BorderLayout());
        movHeader.setOpaque(false);
        movHeader.add(UiKit.title("Movimentação"), BorderLayout.WEST);
        movHeader.add(UiKit.hint("Preencha e clique em Salvar"), BorderLayout.EAST);
        movCard.add(movHeader, BorderLayout.NORTH);

        rbEntrada = new JRadioButton("➕ Entrada (adicionar)");
        rbUso = new JRadioButton("➖ Uso (consumir)");
        rbEntrada.setSelected(true);

        ButtonGroup grupoTipo = new ButtonGroup();
        grupoTipo.add(rbEntrada);
        grupoTipo.add(rbUso);

        JPanel painelTipo = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        painelTipo.setOpaque(false);
        painelTipo.add(rbEntrada);
        painelTipo.add(rbUso);

        txtValor = new JTextField(14);
        txtValor.putClientProperty("JTextField.placeholderText", "Valor (ex: 25,50)");

        txtReferencia = new JTextField(28);
        txtReferencia.putClientProperty("JTextField.placeholderText", "Motivo / referência (opcional)");

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        // Linha: Tipo
        g.gridy = 0;
        g.gridx = 0; g.weightx = 0;
        form.add(new JLabel("Tipo:"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(painelTipo, g);

        // Linha: Valor
        g.gridy = 1;
        g.gridx = 0; g.weightx = 0;
        form.add(new JLabel("Valor:"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(txtValor, g);

        // Linha: Referência
        g.gridy = 2;
        g.gridx = 0; g.weightx = 0;
        form.add(new JLabel("Referência:"), g);
        g.gridx = 1; g.weightx = 1;
        form.add(txtReferencia, g);

        movCard.add(form, BorderLayout.CENTER);

        wrap.add(movCard, BorderLayout.CENTER);

        return wrap;
    }

    private JComponent buildFooter() {
        JPanel card = UiKit.card();
        card.setLayout(new BorderLayout(10, 10));

        JLabel hint = UiKit.hint("Enter salva | Esc fecha");
        card.add(hint, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        actions.setOpaque(false);

        JButton btnCancelar = UiKit.ghost("Cancelar (ESC)");
        btnCancelar.addActionListener(e -> dispose());

        JButton btnSalvar = UiKit.primary("Salvar (ENTER)");
        btnSalvar.addActionListener(e -> onSalvar());

        actions.add(btnCancelar);
        actions.add(btnSalvar);

        card.add(actions, BorderLayout.EAST);
        getRootPane().setDefaultButton(btnSalvar);

        return card;
    }

    private void bindKeys() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
        am.put("cancelar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    private void loadSaldo() {
        double saldo = service.consultarSaldo(cliente.getId());
        lblSaldo.setText(String.format("Saldo atual: R$ %.2f", saldo));
    }

    private void onSalvar() {
        try {
            double valor = Double.parseDouble(txtValor.getText().replace(",", "."));
            String referencia = txtReferencia.getText().trim();

            if (rbEntrada.isSelected()) {
                service.adicionarCredito(cliente.getId(), valor, referencia);
            } else {
                service.usarCredito(cliente.getId(), valor, referencia);
            }

            JOptionPane.showMessageDialog(this, "Operação realizada com sucesso!");
            loadSaldo();

            txtValor.selectAll();
            txtValor.requestFocusInWindow();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Valor inválido: informe um número.",
                    "Erro de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Saldo Insuficiente", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Falha inesperada: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
