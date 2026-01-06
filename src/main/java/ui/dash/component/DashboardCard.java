package ui.dash.component;

import util.MoedaUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DashboardCard extends JPanel {

    private final JLabel lblTitle = new JLabel();
    private final JLabel lblValue = new JLabel();
    private final JLabel lblDelta = new JLabel();
    private final JButton btInfo = new JButton("ⓘ");

    private Runnable onClick;
    private Runnable onInfo;

    public DashboardCard(String title) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        lblTitle.setText(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 18));

        lblDelta.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        btInfo.setFocusable(false);
        btInfo.setMargin(new Insets(2, 6, 2, 6));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblTitle, BorderLayout.WEST);
        top.add(btInfo, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(lblValue, BorderLayout.CENTER);
        center.add(lblDelta, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (onClick != null) onClick.run(); }
        });

        btInfo.addActionListener(e -> { if (onInfo != null) onInfo.run(); });
    }

    public void setValueText(String text) { lblValue.setText(text); }

    public void setMoney(double v) { lblValue.setText(MoedaUtil.brl(v)); }
    public void setNumber(int v) { lblValue.setText(String.valueOf(v)); }
    public void setPercent(double v) { lblValue.setText(MoedaUtil.pct(v)); }

    public void setDelta(String text, Color color) {
        lblDelta.setText(text);
        lblDelta.setForeground(color);
    }

    public void setOnClick(Runnable r) { this.onClick = r; }
    public void setOnInfo(Runnable r) { this.onInfo = r; }
}
