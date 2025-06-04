// PedidoCompraModel – Financeiro
package model;

import java.util.List;
import java.util.Objects;

public class PedidoCompraModel {

    private String id;
    private String nome;
    private String data; // ISO (yyyy-MM-dd)
    private String status; // rascunho, enviado, recebido, etc.
    private String fornecedorId;
    private String observacoes;

    // Transiente (não vai para o banco) – preenchido manualmente:
    private String fornecedorNome;
    private List<PedidoEstoqueProdutoModel> produtos;

    // Transiente: IDs dos títulos a pagar vinculados (opcional)
    private List<String> contasVinculadas;

    public List<String> getContasVinculadas() {
        return contasVinculadas;
    }

    public void setContasVinculadas(List<String> contasVinculadas) {
        this.contasVinculadas = contasVinculadas;
    }

    public PedidoCompraModel(String id, String nome, String data,
            String status, String fornecedorId,
            String observacoes) {
        this.id = id;
        this.nome = nome;
        this.data = data;
        this.status = status;
        this.fornecedorId = fornecedorId;
        this.observacoes = observacoes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFornecedorId() {
        return fornecedorId;
    }

    public void setFornecedorId(String fornecedorId) {
        this.fornecedorId = fornecedorId;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getFornecedorNome() {
        return fornecedorNome;
    }

    public void setFornecedorNome(String fornecedorNome) {
        this.fornecedorNome = fornecedorNome;
    }

    public List<PedidoEstoqueProdutoModel> getProdutos() {
        return produtos;
    }

    public void setProdutos(List<PedidoEstoqueProdutoModel> produtos) {
        this.produtos = produtos;
    }

    @Override
    public String toString() {
        return nome + (data != null ? " [" + data + "]" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PedidoCompraModel))
            return false;
        PedidoCompraModel that = (PedidoCompraModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
