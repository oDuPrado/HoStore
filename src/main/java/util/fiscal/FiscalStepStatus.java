package util.fiscal;

/**
 * Estados do pipeline de geração/validação/assinatura de NFCe
 * Define os passos determinísticos da emissão fiscal offline e online
 */
public enum FiscalStepStatus {
    PENDENTE("Pendente de processamento"),
    XML_GERADO("XML gerado mas não validado"),
    XSD_OK("XML validado contra XSD com sucesso"),
    ASSINADO("XML assinado digitalmente"),
    ASS_LAB_OK("Assinatura de laboratório validada"),
    PRONTO_PARA_ENVIO("Pronto para ser enviado ao SEFAZ"),
    ENVIADO("Enviado ao SEFAZ, aguardando resposta"),
    AUTORIZADO("Autorizado pelo SEFAZ"),
    REJEITADO("Rejeitado pelo SEFAZ"),
    CANCELADO("Cancelado pelo usuário"),
    ERRO("Erro durante processamento");

    private final String descricao;

    FiscalStepStatus(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
