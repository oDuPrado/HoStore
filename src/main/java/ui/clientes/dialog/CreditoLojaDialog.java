package ui.clientes.dialog;

import model.ClienteModel;
import service.CreditoLojaService;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog para consultar e movimentar crédito de loja de um cliente.
 */
public class CreditoLojaDialog extends JDialog {
    private final CreditoLojaService service = new CreditoLojaService();
    private final ClienteModel cliente;

    private JLabel lblCliente;       // Exibe nome/CPF do cliente
    private JLabel lblSaldo;         // Exibe o saldo atual de crédito
    private JRadioButton rbEntrada;  // Selecionar operação ENTRADA
    private JRadioButton rbUso;      // Selecionar operação USO
    private JTextField txtValor;     // Digitar valor da operação
    private JTextField txtReferencia;// Motivo ou ID da venda/devolução
    private JButton btnSalvar;       // Confirma a operação
    private JButton btnCancelar;     // Fecha o diálogo

    /**
     * Construtor: recebe a janela pai e o cliente que irá movimentar.
     */
    public CreditoLojaDialog(Window parent, ClienteModel cliente) {
        super(parent, "Crédito de Loja – " + cliente.getNome(), ModalityType.APPLICATION_MODAL);
        this.cliente = cliente;
        initComponents();   // Monta a UI
        loadSaldo();        // Consulta e exibe saldo inicial
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Monta os componentes visuais do diálogo.
     */
    private void initComponents() {
        // Linha com nome e CPF do cliente
        lblCliente = new JLabel("Cliente: " + cliente.getNome() + " (CPF: " + cliente.getCpf() + ")");

        // Linha com saldo atual (atualizado em loadSaldo())
        lblSaldo = new JLabel();

        // Radios para escolher tipo de movimentação
        rbEntrada = new JRadioButton("Entrada", true);
        rbUso     = new JRadioButton("Uso");
        ButtonGroup grupoTipo = new ButtonGroup();
        grupoTipo.add(rbEntrada);
        grupoTipo.add(rbUso);
        JPanel painelTipo = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        painelTipo.add(rbEntrada);
        painelTipo.add(rbUso);

        // Campo de valor e placeholder
        txtValor = new JTextField(10);
        txtValor.putClientProperty("JTextField.placeholderText", "Valor R$");

        // Campo de referência opcional
        txtReferencia = new JTextField(15);
        txtReferencia.putClientProperty("JTextField.placeholderText", "Referência");

        // Botões Salvar e Cancelar
        btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> onSalvar());
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());

        // Layout de campos em grid 4x2
        JPanel painelCampos = new JPanel(new GridLayout(4, 2, 8, 8));
        painelCampos.add(new JLabel("Tipo:"));
        painelCampos.add(painelTipo);
        painelCampos.add(new JLabel("Valor:"));
        painelCampos.add(txtValor);
        painelCampos.add(new JLabel("Referência:"));
        painelCampos.add(txtReferencia);
        painelCampos.add(btnSalvar);
        painelCampos.add(btnCancelar);

        // Organiza tudo no diálogo
        setLayout(new BorderLayout(10, 10));
        add(lblCliente, BorderLayout.NORTH);
        add(lblSaldo,   BorderLayout.CENTER);
        add(painelCampos, BorderLayout.SOUTH);
    }

    /**
     * Consulta e exibe o saldo atual do cliente.
     */
    private void loadSaldo() {
        double saldo = service.consultarSaldo(cliente.getId());
        lblSaldo.setText(String.format("Saldo atual: R$ %.2f", saldo));
    }

    /**
     * Lógica executada ao clicar em Salvar.
     * Valida entrada, chama o service e atualiza o saldo.
     */
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
            loadSaldo();  // Recarrega saldo após operação
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor inválido: informe um número.", 
                                          "Erro de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), 
                                          "Saldo Insuficiente", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Falha inesperada: " + ex.getMessage(),
                                          "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
