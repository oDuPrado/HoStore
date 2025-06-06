package dao;

import model.PrinterConfigModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * DAO para salvar e carregar PrinterConfigModel em um arquivo de propriedades.
 * O arquivo padrão é data/printConfig.properties.
 */
public class PrinterConfigDAO {

    // 🔍 CTRL+F por "CONFIG_FILE" para encontrar o local do arquivo
    private static final String CONFIG_FILE = "data/printConfig.properties";

    /**
     * Carrega as configurações de impressão do arquivo.
     * Se não existir, retorna um modelo com valores padrão.
     */
    public PrinterConfigModel loadConfig() {
        PrinterConfigModel model = new PrinterConfigModel();
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) {
                // Se não existe ainda, retorna modelo padrão sem lançar erro
                return model;
            }
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            }

            String printerName = props.getProperty("defaultPrinterName", "");
            int copies = Integer.parseInt(props.getProperty("copies", "1"));
            int receiptWidth = Integer.parseInt(props.getProperty("receiptWidth", "44"));
            boolean openPdf = Boolean.parseBoolean(props.getProperty("openPdfAfterGenerate", "false"));

            model.setDefaultPrinterName(printerName);
            model.setCopies(copies);
            model.setReceiptWidth(receiptWidth);
            model.setOpenPdfAfterGenerate(openPdf);

        } catch (Exception e) {
            e.printStackTrace();
            // Se der erro, retornamos um modelo padrão
        }
        return model;
    }

    /**
     * Salva as configurações de impressão no arquivo data/printConfig.properties.
     * Cria a pasta data/ se necessário.
     */
    public void saveConfig(PrinterConfigModel model) {
        try {
            File file = new File(CONFIG_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs(); // cria pasta data/ se não existir
            }

            Properties props = new Properties();
            props.setProperty("defaultPrinterName", model.getDefaultPrinterName());
            props.setProperty("copies", String.valueOf(model.getCopies()));
            props.setProperty("receiptWidth", String.valueOf(model.getReceiptWidth()));
            props.setProperty("openPdfAfterGenerate", String.valueOf(model.isOpenPdfAfterGenerate()));

            try (FileOutputStream fos = new FileOutputStream(file)) {
                props.store(fos, "Configurações de Impressão - HoStore");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
