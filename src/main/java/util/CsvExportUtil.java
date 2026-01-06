package util;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class CsvExportUtil {

    public static void exportarTabelaParaCsv(JTable table, File file) throws IOException {
        TableModel m = table.getModel();

        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            // header
            for (int c = 0; c < m.getColumnCount(); c++) {
                if (c > 0) w.write(";");
                w.write(escape(String.valueOf(m.getColumnName(c))));
            }
            w.write("\n");

            // rows
            for (int r = 0; r < m.getRowCount(); r++) {
                for (int c = 0; c < m.getColumnCount(); c++) {
                    if (c > 0) w.write(";");
                    Object v = m.getValueAt(r, c);
                    w.write(escape(v == null ? "" : String.valueOf(v)));
                }
                w.write("\n");
            }
        }
    }

    private static String escape(String s) {
        // CSV simples; separador ";"
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }
}
