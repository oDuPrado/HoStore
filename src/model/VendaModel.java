
package model;

public class VendaModel {
    private int id;
    private String dataVenda;
    private String clienteId;
    private double total;
    private double desconto;
    private String formaPagamento;
    private int parcelas;
    private String status;

    public VendaModel(int id,String data,String cliente,double total,double desconto,String forma,int parcelas,String status){
        this.id=id;this.dataVenda=data;this.clienteId=cliente;this.total=total;this.desconto=desconto;this.formaPagamento=forma;this.parcelas=parcelas;this.status=status;
    }
    public VendaModel(String data,String cliente,double total,double desconto,String forma,int parcelas,String status){
        this(-1,data,cliente,total,desconto,forma,parcelas,status);
    }
    public int getId(){return id;}
    public void setId(int id){this.id=id;}
    public String getDataVenda(){return dataVenda;}
    public String getClienteId(){return clienteId;}
    public double getTotal(){return total;}
    public double getDesconto(){return desconto;}
    public String getFormaPagamento() {
        return formaPagamento;
    }
    
    public int getParcelas() {
        return parcelas;
    }
    
}
