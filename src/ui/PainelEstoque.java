package ui;

import model.Carta;
import service.EstoqueService;
import ui.dialog.CartaCadastroDialog;
import util.AlertUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

public class PainelEstoque extends JPanel {

    private final EstoqueService estoqueService = new EstoqueService();
    private final JTable tabela;
    private final DefaultTableModel modelo;
    private final JComboBox<String> colecaoFilter = new JComboBox<>();

    public PainelEstoque(JFrame owner) {
        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(10,10,10,10));

        modelo = new DefaultTableModel(new String[]{
            "ID","Nome","Coleção","Número","Qtd","Preço (R$)","Editar","Del"
        },0){
            @Override public boolean isCellEditable(int r,int c){ return c==6||c==7; }
            @Override public Class<?> getColumnClass(int col){
                if(col==4) return Integer.class;
                if(col==5) return Double.class;
                return String.class;
            }
        };
        tabela = new JTable(modelo);
        personalizarTabela(owner);

        add(toolbar(owner),BorderLayout.NORTH);
        add(new JScrollPane(tabela),BorderLayout.CENTER);

        atualizarTabela(null,"Nome");
    }

    private JComponent toolbar(JFrame owner){
        JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        JTextField busca=new JTextField(15);
        colecaoFilter.addItem("Todas");
        estoqueService.listarCartas(null,null,"Nome")
                       .stream().map(Carta::getColecao).distinct()
                       .forEach(colecaoFilter::addItem);

        JButton btnBuscar=new JButton("Buscar");
        JButton btnNovo  =new JButton("Nova Carta");

        btnBuscar.addActionListener(e-> atualizarTabela(
            busca.getText(), (String) colecaoFilter.getSelectedItem() ));

        btnNovo.addActionListener(e->{
            new CartaCadastroDialog(owner,null).setVisible(true);
            atualizarTabela(null,"Nome");
        });

        bar.add(new JLabel("Nome:"));bar.add(busca);
        bar.add(new JLabel("Coleção:"));bar.add(colecaoFilter);
        bar.add(btnBuscar);bar.add(btnNovo);
        return bar;
    }

    private void atualizarTabela(String termo,String colecao){
        modelo.setRowCount(0);
        for(Carta c: estoqueService.listarCartas(termo,colecao,"Nome")){
            modelo.addRow(new Object[]{
                c.getId(),c.getNome(),c.getColecao(),c.getNumero(),
                c.getQtd(),c.getPreco(),"Editar","X"
            });
        }
    }

    private void personalizarTabela(JFrame owner){
        tabela.getColumnModel().getColumn(5).setCellRenderer((tbl,val,sel,foc,row,col)->{
            JLabel l=new JLabel(NumberFormat.getCurrencyInstance(new Locale("pt","BR"))
                    .format((Double)val));
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            return l;
        });

        // botões
        tabela.getColumnModel().getColumn(6).setCellRenderer(new ButtonCell("Editar"));
        tabela.getColumnModel().getColumn(6).setCellEditor(new ButtonCellEditor("Editar", row->{
            String id=(String) modelo.getValueAt(row,0);
            Carta ct=estoqueService.listarCartas(id,null,"Nome").stream()
                                   .filter(c->c.getId().equals(id)).findFirst().orElse(null);
            new CartaCadastroDialog(owner,ct).setVisible(true);
            atualizarTabela(null,"Nome");
        }));

        tabela.getColumnModel().getColumn(7).setCellRenderer(new ButtonCell("Del"));
        tabela.getColumnModel().getColumn(7).setCellEditor(new ButtonCellEditor("Del", row->{
            if(JOptionPane.showConfirmDialog(this,"Excluir carta?","Confirma",
                    JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                try{
                    estoqueService.excluirCarta((String)modelo.getValueAt(row,0));
                    atualizarTabela(null,"Nome");
                }catch(Exception ex){ AlertUtils.error("Erro: "+ex.getMessage()); }
            }
        }));
    }

    /* ---------- helpers ---------- */
    private static class ButtonCell extends JButton implements javax.swing.table.TableCellRenderer{
        ButtonCell(String txt){ setText(txt); setFocusPainted(false);}
        @Override public Component getTableCellRendererComponent(
            JTable t,Object v,boolean s,boolean h,int r,int c){ return this; }
    }
    private static class ButtonCellEditor extends DefaultCellEditor{
        private final JButton btn=new JButton(); private final java.util.function.IntConsumer action; private int row;
        ButtonCellEditor(String txt,java.util.function.IntConsumer act){
            super(new JCheckBox()); action=act; btn.setText(txt); btn.addActionListener(e->{action.accept(row); fireEditingStopped();});
        }
        @Override public Component getTableCellEditorComponent(JTable t,Object v,boolean s,int r,int c){ row=r; return btn; }
    }
}
