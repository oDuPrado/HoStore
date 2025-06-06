package model;

import java.io.Serializable;

/**
 * Modelo que representa as configurações de impressão para impressoras térmicas e PDF.
 * Aqui podem ser adicionados novos campos conforme necessário.
 */
public class PrinterConfigModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Nome da impressora padrão (conforme disponível via PrintServiceLookup) */
    private String defaultPrinterName;

    /** Quantidade de cópias a imprimir por padrão */
    private int copies;

    /** Largura de página (em caracteres) para formatação de cupom */
    private int receiptWidth;

    /** Flag que indica se deve abrir o PDF após gerar */
    private boolean openPdfAfterGenerate;

    /** Construtor padrão (valores iniciais) */
    public PrinterConfigModel() {
        this.defaultPrinterName = "";      // vazio = usar impressora padrão do sistema
        this.copies = 1;                   // 1 cópia por padrão
        this.receiptWidth = 44;            // mesma largura usada no PDFGenerator
        this.openPdfAfterGenerate = false; // por padrão não abre o PDF
    }

    /* ─────────────── Getters / Setters ─────────────── */

    public String getDefaultPrinterName() {
        return defaultPrinterName;
    }

    public void setDefaultPrinterName(String defaultPrinterName) {
        this.defaultPrinterName = defaultPrinterName;
    }

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }

    public int getReceiptWidth() {
        return receiptWidth;
    }

    public void setReceiptWidth(int receiptWidth) {
        this.receiptWidth = receiptWidth;
    }

    public boolean isOpenPdfAfterGenerate() {
        return openPdfAfterGenerate;
    }

    public void setOpenPdfAfterGenerate(boolean openPdfAfterGenerate) {
        this.openPdfAfterGenerate = openPdfAfterGenerate;
    }
}
