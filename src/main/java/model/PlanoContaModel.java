package model;
import java.util.Objects;

/**
 * Modelo simplificado para Plano de Contas.
 * Não exibe 'codigo' na UI, mas mantém internamente para hierarquia.
 */
public class PlanoContaModel {
    private String id;           // UUID interno
    private String codigo;       // gerado automaticamente no service
    private String descricao;    // exibido na UI
    private String tipo;         // Ativo, Passivo, Receita, Custo
    private String parentId;     // id da conta pai (pode ser null)
    private String observacoes;  // livre

    public PlanoContaModel(String id, String codigo, String descricao,
                           String tipo, String parentId, String observacoes) {
        this.id = id;
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.parentId = parentId;
        this.observacoes = observacoes;
    }

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    @Override
public String toString() {
    return descricao;
}

@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    PlanoContaModel that = (PlanoContaModel) obj;
    return Objects.equals(id, that.id);
}

@Override
public int hashCode() {
    return Objects.hash(id);
}

    
}
