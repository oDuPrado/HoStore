// src/ui/dash/component/DashboardCard.java
package ui.dash.component;

import com.formdev.flatlaf.FlatClientProperties;
import util.MoedaUtil;
import util.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DashboardCard extends JPanel {

    private final JLabel lblTitle = new JLabel();
    private final JLabel lblValue = new JLabel();
    private final JLabel lblDelta = new JLabel();
    private final JButton btInfo = UiKit.ghost("ⓘ");

    private Runnable onClick;
    private Runnable onInfo;

    public DashboardCard(String title) {
        UiKit.applyPanelBase(this);
        setLayout(new BorderLayout(10, 8));
        setOpaque(true);

        // Usa o card do UiKit como “base visual” do componente
        // Aqui não dá pra “trocar o this por UiKit.card()”, então replicamos o estilo:
        putClientProperty(FlatClientProperties.STYLE, "arc: 12;");
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                new EmptyBorder(12, 12, 12, 12)));
        setBackground(UIManager.getColor("Table.background") != null
                ? UIManager.getColor("Table.background")
                : UIManager.getColor("Panel.background"));

        // Título
        lblTitle.setText(title);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: -1;");

        // Valor principal (destaque)
        lblValue.putClientProperty(FlatClientProperties.STYLE, "font: +6;");

        // Delta (texto menor)
        lblDelta.putClientProperty(FlatClientProperties.STYLE, "font: -1;");
        lblDelta.setForeground(UIManager.getColor("Label.disabledForeground"));

        // Info button compacto
        btInfo.setFocusable(false);
        btInfo.setMargin(new Insets(2, 8, 2, 8));
        btInfo.putClientProperty(FlatClientProperties.STYLE, "arc: 10; focusWidth: 0;");

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(lblTitle, BorderLayout.WEST);
        top.add(btInfo, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.setOpaque(false);
        center.add(lblValue, BorderLayout.CENTER);
        center.add(lblDelta, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // Click no card
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null)
                    onClick.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setHover(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHover(false);
            }
        });

        btInfo.addActionListener(e -> {
            if (onInfo != null)
                onInfo.run();
        });
    }

    private void setHover(boolean hover) {
        // hover sutil sem inventar paleta fixa
        Color bg = UIManager.getColor("Table.background");
        if (bg == null)
            bg = UIManager.getColor("Panel.background");
        if (bg == null)
            bg = Color.WHITE;

        setBackground(hover ? new Color(
                Math.min(255, bg.getRed() + 6),
                Math.min(255, bg.getGreen() + 6),
                Math.min(255, bg.getBlue() + 6)) : bg);

        repaint();
    }

    public void setValueText(String text) {
        lblValue.setText(text);
    }

    public void setMoney(double v) {
        lblValue.setText(MoedaUtil.brl(v));
    }

    public void setNumber(int v) {
        lblValue.setText(String.valueOf(v));
    }

    public void setPercent(double v) {
        lblValue.setText(MoedaUtil.pct(v));
    }

    public void setDelta(String text, Color color) {
        lblDelta.setText(text);
        lblDelta.setForeground(color);
    }

    public void setOnClick(Runnable r) {
        this.onClick = r;
    }

    public void setOnInfo(Runnable r) {
        this.onInfo = r;
    }
}
