package ui.financeiro.dialog;

import javax.swing.*;
import com.toedter.calendar.JDateChooser;

import service.ContaReceberService;
import dao.ClienteDAO;
import model.ClienteModel;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @CR Dialog: cadastra manualmente um Título + parcelas.
 */
public class ContaReceberDialog extends JDialog {

    private final ClienteDAO clienteDAO     = new ClienteDAO();
    private final ContaReceberService crSvc = new ContaReceberService();

    private JComboBox<String> cbCliente;
    private JTextField txtTotal, txtParcelas, txtIntervalo, txtObs;
    private JDateChooser dtPrimeiroVenc;

    public ContaReceberDialog(Frame owner) {
        super(owner, "Novo Título (Receber)", true);
        setSize(450, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(criarForm(), BorderLayout.CENTER);
        add(criarBotoes(), BorderLayout.SOUTH);
    }

    private JPanel criarForm() {
        JPanel p = new JPanel(new GridLayout(0,2,10,6));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        p.add(new JLabel("Cliente:"));
        cbCliente = new JComboBox<>();
        try { clienteDAO.findAll().forEach(c -> cbCliente.addItem(c.getNome())); }
        catch (Exception ex) { ex.printStackTrace(); }
        p.add(cbCliente);

        p.add(new JLabel("Valor Total:"));
        txtTotal = new JTextField("0");
        p.add(txtTotal);

        p.add(new JLabel("Parcelas:"));
        txtParcelas = new JTextField("1");
        p.add(txtParcelas);

        p.add(new JLabel("Intervalo dias:"));
        txtIntervalo = new JTextField("30");
        p.add(txtIntervalo);

        p.add(new JLabel("Primeiro venc.:"));
        dtPrimeiroVenc = new com.toedter.calendar.JDateChooser(new Date());
        dtPrimeiroVenc.setDateFormatString("dd/MM/yyyy");
        p.add(dtPrimeiroVenc);

        p.add(new JLabel("Obs.:"));
        txtObs = new JTextField();
        p.add(txtObs);

        return p;
    }

    private JPanel criarBotoes() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btSalvar = new JButton("Salvar");
        JButton btCancelar = new JButton("Cancelar");
        p.add(btCancelar); p.add(btSalvar);

        btCancelar.addActionListener(e -> dispose());

        btSalvar.addActionListener(e -> {
            try {
                String cliNome = (String) cbCliente.getSelectedItem();
                ClienteModel cli = clienteDAO.buscarPorNome(cliNome);
                double total   = Double.parseDouble(txtTotal.getText().replace(",", "."));
                int parcelas   = Integer.parseInt(txtParcelas.getText());
                int intervalo  = Integer.parseInt(txtIntervalo.getText());
                java.text.SimpleDateFormat iso =
                    new java.text.SimpleDateFormat("yyyy-MM-dd");
                String venc = iso.format(dtPrimeiroVenc.getDate());

                crSvc.criarTituloParcelado(
                        cli.getId(), total, parcelas, venc, intervalo, txtObs.getText());

                JOptionPane.showMessageDialog(this,"Título criado!");
                dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Erro: "+ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
        return p;
    }
}
