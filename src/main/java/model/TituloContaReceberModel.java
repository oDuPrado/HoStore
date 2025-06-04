package model;

import java.util.UUID;

/**
 * @CR: Representa o “título” (cabecalho) de uma conta a receber.
 * Há sempre 1 título por venda, mas ele também pode ser criado manualmente.
 */
public class TituloContaReceberModel {

    // ─── Campos persistidos ────────────────────────────────────────────────
    private String id;              // PK – UUID
    private String clienteId;       // FK -> clientes.id
    private String codigoSelecao;   // Agrupador opcional (ex: lote)
    private String dataGeracao;     // ISO yyyy-MM-dd
    private double valorTotal;
    private String status;          // aberto, quitado, vencido, cancelado
    private String observacoes;

    // ─── Construtores ─────────────────────────────────────────────────────
    public TituloContaReceberModel() {
        this.id = UUID.randomUUID().toString();
        this.status = "aberto";
    }

    // Getters/Setters (gerados por IDE para brevidade)
    public String getId()                     { return id; }
    public void setId(String id)              { this.id = id; }
    public String getClienteId()              { return clienteId; }
    public void setClienteId(String clienteId){ this.clienteId = clienteId; }
    public String getCodigoSelecao()          { return codigoSelecao; }
    public void setCodigoSelecao(String c)    { this.codigoSelecao = c; }
    public String getDataGeracao()            { return dataGeracao; }
    public void setDataGeracao(String d)      { this.dataGeracao = d; }
    public double getValorTotal()             { return valorTotal; }
    public void setValorTotal(double v)       { this.valorTotal = v; }
    public String getStatus()                 { return status; }
    public void setStatus(String status)      { this.status = status; }
    public String getObservacoes()            { return observacoes; }
    public void setObservacoes(String obs)    { this.observacoes = obs; }
}
