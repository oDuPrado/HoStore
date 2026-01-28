// src/util/UiKit.java
package util;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class UiKit {

    // ===== Helpers para pegar cores do tema (FlatLaf ou qualquer LAF) =====
    private static Color uiColor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return (c != null) ? c : fallback;
    }

    private static boolean isDark() {
        Object o = UIManager.get("laf.dark");
        if (o instanceof Boolean b)
            return b;

        Color bg = uiColor("Panel.background", Color.WHITE);
        int lum = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
        return lum < 140;
    }

    // Cor “elevada” para cards: tenta usar uma cor diferente do Panel.background
    private static Color cardBackground() {
        Color panelBg = uiColor("Panel.background", Color.WHITE);

        // Em FlatLaf, Table.background costuma ser um pouco diferente e funciona bem
        // como “card”.
        Color tableBg = UIManager.getColor("Table.background");
        if (tableBg != null && !tableBg.equals(panelBg))
            return tableBg;

        // Fallback: ajusta leve contraste manual (sem exagerar)
        boolean dark = isDark();
        int delta = dark ? 10 : -8;
        return shift(panelBg, delta);
    }

    private static Color shift(Color c, int delta) {
        int r = clamp(c.getRed() + delta);
        int g = clamp(c.getGreen() + delta);
        int b = clamp(c.getBlue() + delta);
        return new Color(r, g, b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // ===== Bases de layout =====
    public static void applyDialogBase(JDialog d) {
        d.getContentPane().setBackground(uiColor("Panel.background", Color.WHITE));
        if (d.getRootPane() != null) {
            d.getRootPane().setBorder(new EmptyBorder(12, 12, 12, 12));
            // ✅ remove borderWidth (não existe na sua versão e gera UnknownStyleException)
            d.getRootPane().putClientProperty(FlatClientProperties.STYLE,
                    "arc: 14; focusWidth: 0;");
        }
        d.setResizable(true);
        if (d.getRootPane() != null && d.getRootPane().getClientProperty("uikit.responsive") == null) {
            d.getRootPane().putClientProperty("uikit.responsive", Boolean.TRUE);
            d.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    fitToScreen(d);
                    SwingUtilities.invokeLater(() -> relaxSpinners(d.getContentPane()));
                }
            });
        }
    }

    private static void relaxSpinners(Container root) {
        if (root == null)
            return;
        for (Component c : root.getComponents()) {
            if (c instanceof JSpinner sp) {
                relaxSpinner(sp);
            }
            if (c instanceof Container child) {
                relaxSpinners(child);
            }
        }
    }

    private static void relaxSpinner(JSpinner s) {
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            JFormattedTextField tf = de.getTextField();
            tf.setFocusLostBehavior(JFormattedTextField.COMMIT);
            if (tf.getFormatter() instanceof javax.swing.text.NumberFormatter nf) {
                nf.setAllowsInvalid(true);
                nf.setOverwriteMode(false);
                nf.setCommitsOnValidEdit(true);
            }
        }
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setOpaque(true);

        p.setBackground(cardBackground());

        Color border = uiColor("Component.borderColor",
                isDark() ? new Color(0x2A3038) : new Color(0xE2E5EA));

        // Borda Swing + padding garantem consistência em qualquer tema
        p.setBorder(new CompoundBorder(
                new LineBorder(border, 1, true),
                new EmptyBorder(12, 12, 12, 12)));

        // FlatLaf: arredondamento (seguro)
        p.putClientProperty(FlatClientProperties.STYLE, "arc: 12;");
        return p;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.putClientProperty(FlatClientProperties.STYLE, "font: +2");
        return l;
    }

    public static JLabel hint(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(uiColor("Label.disabledForeground", new Color(0x6B7280)));
        l.setFont(l.getFont().deriveFont(12f));
        return l;
    }

    // ===== JFormattedTextField helpers =====
    public static int getIntValue(JFormattedTextField f, int def) {
        if (f == null)
            return def;
        Object v = f.getValue();
        if (v instanceof Number n)
            return n.intValue();
        String t = f.getText();
        if (t == null)
            return def;
        String s = t.trim();
        if (s.isEmpty())
            return def;
        s = s.replaceAll("[^0-9-]", "");
        if (s.isEmpty() || "-".equals(s))
            return def;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    public static double getDoubleValue(JFormattedTextField f, double def) {
        if (f == null)
            return def;
        Object v = f.getValue();
        if (v instanceof Number n)
            return n.doubleValue();
        String t = f.getText();
        if (t == null)
            return def;
        String s = t.trim();
        if (s.isEmpty())
            return def;
        s = s.replace("R$", "").replace(" ", "");
        if (s.contains(",") && s.contains(".")) {
            s = s.replace(".", "").replace(",", ".");
        } else {
            s = s.replace(",", ".");
        }
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }

    // ===== Tabelas =====
    public static void tableDefaults(JTable t) {
        t.setRowHeight(28);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        t.setIntercellSpacing(new Dimension(0, 0));

        t.setGridColor(uiColor("Table.gridColor", isDark() ? new Color(0x2A3038) : new Color(0xEEF0F3)));

        JTableHeader th = t.getTableHeader();
        th.setReorderingAllowed(false);
        th.setFont(th.getFont().deriveFont(Font.BOLD, 12f));
        th.putClientProperty(FlatClientProperties.STYLE, "height: 30;");

        Color selBg = uiColor("Table.selectionBackground", isDark() ? new Color(0x2F4A7A) : new Color(0xDDEBFF));
        Color selFg = uiColor("Table.selectionForeground", isDark() ? Color.WHITE : Color.BLACK);
        t.setSelectionBackground(selBg);
        t.setSelectionForeground(selFg);
    }

    public static DefaultTableCellRenderer zebraRenderer() {
        return new DefaultTableCellRenderer() {
            final Color bgA = uiColor("Table.background", uiColor("Panel.background", Color.WHITE));
            final Color bgB = uiColor("Table.alternateRowColor",
                    isDark() ? shift(bgA, -8) : new Color(0xFAFAFB));
            final Color fg = uiColor("Table.foreground", uiColor("Label.foreground", Color.BLACK));

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    setBackground((row % 2 == 0) ? bgA : bgB);
                    setForeground(fg);
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
    }

    public static DefaultTableCellRenderer badgeStatusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);

                String raw = (value == null) ? "" : value.toString().trim();
                String st = raw.toLowerCase();
                String label = switch (st) {
                    case "inscrito" -> "Aguardando pagto";
                    case "inscrito_comanda" -> "Em comanda";
                    case "pago" -> "Pago";
                    case "presente" -> "Presente";
                    case "desistente" -> "Desistente";
                    case "pendente" -> "Pendente";
                    default -> raw.isBlank() ? "" : raw;
                };
                l.setText(" " + label + " ");
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
                l.setBorder(new EmptyBorder(4, 10, 4, 10));

                // ✅ Selecionado: respeita o tema (não some com texto/fundo)
                if (isSelected) {
                    l.setOpaque(true);
                    l.setBackground(table.getSelectionBackground());
                    l.setForeground(table.getSelectionForeground());
                    return l;
                }

                boolean dark = isDark();

                Color fg = uiColor("Label.foreground", dark ? new Color(0xE6E8EB) : new Color(0x111827));
                Color bg;

                switch (st) {
                    case "rascunho" -> bg = dark ? new Color(0x2A3038) : new Color(0xF3F4F6);
                    case "enviado" -> bg = dark ? new Color(0x1F3A52) : new Color(0xE0F2FE);
                    case "parcialmente recebido" -> bg = dark ? new Color(0x4A3B12) : new Color(0xFEF3C7);
                    case "recebido" -> bg = dark ? new Color(0x1E3A2A) : new Color(0xDCFCE7);
                    case "completo" -> bg = dark ? new Color(0x1E3A2A) : new Color(0xDCFCE7);
                    case "parcial" -> bg = dark ? new Color(0x4A3B12) : new Color(0xFEF3C7);
                    case "pendente" -> bg = dark ? new Color(0x2A3038) : new Color(0xF3F4F6);
                    case "inscrito" -> bg = dark ? new Color(0x4A3B12) : new Color(0xFEF3C7);
                    case "inscrito_comanda" -> bg = dark ? new Color(0x1F3A52) : new Color(0xE0F2FE);
                    case "pago" -> bg = dark ? new Color(0x1E3A2A) : new Color(0xDCFCE7);
                    case "presente" -> bg = dark ? new Color(0x1E3A2A) : new Color(0xDCFCE7);
                    case "desistente" -> bg = dark ? new Color(0x4A1C1C) : new Color(0xFEE2E2);
                    default -> bg = dark ? new Color(0x2A3038) : new Color(0xF3F4F6);
                }

                l.setOpaque(true);
                l.setBackground(bg);
                l.setForeground(fg);

                // Bordazinha sutil (tema)
                Color border = uiColor("Component.borderColor",
                        dark ? new Color(0x313844) : new Color(0xE5E7EB));
                l.setBorder(new CompoundBorder(
                        new LineBorder(border, 1, true),
                        new EmptyBorder(4, 10, 4, 10)));

                return l;
            }
        };
    }

    // ===== Botões =====
    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);

        // Sem hardcode de cor: FlatLaf resolve no dark/light
        b.putClientProperty(FlatClientProperties.STYLE, "font: +1; arc: 10; focusWidth: 0;");
        b.putClientProperty("JButton.buttonType", "default"); // “botão principal”
        return b;
    }

    public static JButton ghost(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);

        // “secondary” consistente
        b.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0;");
        return b;
    }

    // ===== Bases de layout =====
    public static void applyPanelBase(JComponent c) {
        // painéis “base” não devem brigar com tema
        c.setOpaque(false);

        // padding padrão quando o painel for usado como container principal
        if (c instanceof JPanel p) {
            if (p.getBorder() == null) {
                p.setBorder(new EmptyBorder(10, 10, 10, 10));
            }
        }
    }

    // ===== ScrollPane com borda “de tema” =====
    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);

        Color border = uiColor("Component.borderColor",
                isDark() ? new Color(0x2A3038) : new Color(0xE2E5EA));

        sp.setBorder(new LineBorder(border, 1, true));
        sp.getViewport().setBackground(uiColor("Table.background", uiColor("Panel.background", Color.WHITE)));
        return sp;
    }

    private static void fitToScreen(Window w) {
        if (w == null) {
            return;
        }
        Rectangle bounds = w.getGraphicsConfiguration() != null
                ? w.getGraphicsConfiguration().getBounds()
                : new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        int maxW = (int) Math.floor(bounds.width * 0.92);
        int maxH = (int) Math.floor(bounds.height * 0.92);

        Dimension size = w.getSize();
        int newW = Math.min(size.width, maxW);
        int newH = Math.min(size.height, maxH);

        if (newW != size.width || newH != size.height) {
            w.setSize(new Dimension(newW, newH));
        }
    }
}
