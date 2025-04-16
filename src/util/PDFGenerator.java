package util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import model.VendaModel;
import model.VendaItemModel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PDFGenerator {

    private static final String EXPORT_DIR = "data/export";
    private static final String FONT_DIR   = "data/fonts";

    public static File gerarComprovanteVenda(VendaModel venda, List<VendaItemModel> itens) throws IOException {
        // garante diretórios
        Files.createDirectories(Paths.get(EXPORT_DIR));
        Files.createDirectories(Paths.get(FONT_DIR));

        // arquivo de saída
        File pdfFile = new File(EXPORT_DIR, "comprovante_" + venda.getId() + ".pdf");

        try (PDDocument doc = new PDDocument()) {
            // carrega página
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // carrega fontes externas (coloque Roboto-Regular.ttf e Roboto-Bold.ttf em data/fonts)
            PDType0Font fonteTitulo  = PDType0Font.load(doc, new File(FONT_DIR, "Roboto-Bold.ttf"));
            PDType0Font fonteNormal  = PDType0Font.load(doc, new File(FONT_DIR, "Roboto-Regular.ttf"));

            // escreve texto
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setLeading(16f);
                cs.beginText();
                cs.setFont(fonteTitulo, 16);
                cs.newLineAtOffset(50, 750);
                cs.showText("Comprovante de Venda - HoStore");
                
                cs.newLine();
                cs.setFont(fonteNormal, 12);
                cs.showText("Venda #" + venda.getId() + "   Data: " + venda.getDataVenda());

                cs.newLine();
                cs.showText("Cliente: " + venda.getClienteId());
                cs.newLine();
                cs.showText("========================================");

                for (VendaItemModel it : itens) {
                    cs.newLine();
                    cs.showText(it.getCartaId()
                            + "  x" + it.getQtd()
                            + "  R$" + String.format("%.2f", it.getPreco()));
                }

                cs.newLine();
                cs.showText("----------------------------------------");
                cs.newLine();
                cs.showText("Total: R$" 
                        + String.format("%.2f", venda.getTotal())
                        + "   Desconto: R$" 
                        + String.format("%.2f", venda.getDesconto()));

                cs.endText();
            }

            // salva
            doc.save(pdfFile);
        }

        return pdfFile;
    }
}
