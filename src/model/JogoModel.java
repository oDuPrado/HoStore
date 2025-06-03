// Procure em: src/model/JogoModel.java
package model;

/**
 * Model que representa um Jogo (TCG) no sistema.
 * Contém apenas o identificador único e o nome legível.
 */
public class JogoModel {
    // —————————— Campos principais ——————————
    
    /**
     * Identificador único do jogo. 
     * Exemplos: "POKEMON", "YUGIOH", "MAGIC".
     * Usado como FOREIGN KEY em outras tabelas.
     */
    private String id;
    
    /**
     * Nome completo do jogo. 
     * Exemplo: "Pokémon TCG", "Yu-Gi-Oh!".
     * Exibido na interface (JComboBox, tabelas, etc.).
     */
    private String nome;

    // —————————— Construtores ——————————

    /**
     * Construtor padrão (sem argumentos). 
     * Útil em frameworks ou se precisar instanciar antes e setar valores.
     */
    public JogoModel() {
        // vazio; pode ser utilizado pelo DAO antes de popular via setters
    }

    /**
     * Construtor para criar instâncias definindo id e nome.
     * @param id   Identificador único do jogo.
     * @param nome Nome legível do jogo.
     */
    public JogoModel(String id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    // —————————— Getters e Setters ——————————

    /**
     * Retorna o identificador do jogo.
     * @return String com o id (ex: "POKEMON").
     */
    public String getId() {
        return id;
    }

    /**
     * Define o identificador do jogo.
     * @param id String única (ex: "YUGIOH").
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Retorna o nome legível do jogo.
     * @return String com o nome (ex: "Yu-Gi-Oh!").
     */
    public String getNome() {
        return nome;
    }

    /**
     * Define o nome legível do jogo.
     * @param nome String legível (ex: "Magic: The Gathering").
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    // —————————— toString() para exibição em JComboBox ——————————

    /**
     * Retorna a representação em texto do modelo. 
     * Observação: o JComboBox usa esse método para exibir o item.
     * @return o nome do jogo.
     */
    @Override
    public String toString() {
        return nome;
    }
}
