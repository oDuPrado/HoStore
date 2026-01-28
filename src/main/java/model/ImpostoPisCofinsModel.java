package model;

/**
 * Model para configuração de alíquotas de PIS e COFINS por NCM
 * Utilizado para cálculo automático de impostos em documentos fiscais
 */
public class ImpostoPisCofinsModel {
    private int id;
    private String ncm;                    // 8 dígitos (ex: 95049090)
    private String cstPis;                 // Código de Situação Tributária PIS (ex: 04=isento)
    private Double aliquotaPis;            // Alíquota PIS em percentual (ex: 1.25)
    private String cstCofins;              // Código de Situação Tributária COFINS (ex: 04=isento)
    private Double aliquotaCofins;         // Alíquota COFINS em percentual (ex: 5.75)
    private boolean ativo = true;          // Registro ativo para busca

    // Constructors

    public ImpostoPisCofinsModel() {
    }

    public ImpostoPisCofinsModel(String ncm, String cstPis, Double aliquotaPis, 
                                  String cstCofins, Double aliquotaCofins) {
        this.ncm = ncm;
        this.cstPis = cstPis;
        this.aliquotaPis = aliquotaPis;
        this.cstCofins = cstCofins;
        this.aliquotaCofins = aliquotaCofins;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNcm() {
        return ncm;
    }

    public void setNcm(String ncm) {
        this.ncm = ncm;
    }

    public String getCstPis() {
        return cstPis;
    }

    public void setCstPis(String cstPis) {
        this.cstPis = cstPis;
    }

    public Double getAliquotaPis() {
        return aliquotaPis;
    }

    public void setAliquotaPis(Double aliquotaPis) {
        this.aliquotaPis = aliquotaPis;
    }

    public String getCstCofins() {
        return cstCofins;
    }

    public void setCstCofins(String cstCofins) {
        this.cstCofins = cstCofins;
    }

    public Double getAliquotaCofins() {
        return aliquotaCofins;
    }

    public void setAliquotaCofins(Double aliquotaCofins) {
        this.aliquotaCofins = aliquotaCofins;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return "ImpostoPisCofinsModel{" +
                "id=" + id +
                ", ncm='" + ncm + '\'' +
                ", cstPis='" + cstPis + '\'' +
                ", aliquotaPis=" + aliquotaPis +
                ", cstCofins='" + cstCofins + '\'' +
                ", aliquotaCofins=" + aliquotaCofins +
                ", ativo=" + ativo +
                '}';
    }
}
