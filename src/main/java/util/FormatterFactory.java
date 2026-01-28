package util;

import javax.swing.*;
import javax.swing.text.*;
import java.text.*;
import java.util.Locale;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Fábrica de campos formatados padronizados:
 * - getFormattedDoubleField: valores decimais com vírgula
 * - getFormattedIntField: inteiros
 * - getFormattedDateField: data DD/MM/AAAA
 */
public class FormatterFactory {

    /** Campo decimal (2 casas, vírgula). Inicial em branco se valor for 0. */
    public static JFormattedTextField getFormattedDoubleField(double initialValue) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        NumberFormatter formatter = new NumberFormatter(nf);
        formatter.setValueClass(Double.class);
        formatter.setAllowsInvalid(true);       // permite apagar totalmente
        formatter.setOverwriteMode(false);      // insere ao invés de sobrescrever
        formatter.setCommitsOnValidEdit(true);

        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setFocusLostBehavior(JFormattedTextField.COMMIT);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(field::selectAll);
            }
        });

        // valor inicial
        if (initialValue != 0.0) field.setValue(initialValue);
        else field.setValue(null);  // deixa em branco
        return field;
    }

    /** Campo moeda (R$ 0,00). Inicial em branco se valor for 0. */
    public static JFormattedTextField getMoneyField(double initialValue) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        NumberFormatter formatter = new NumberFormatter(nf);
        formatter.setValueClass(Double.class);
        formatter.setAllowsInvalid(true);
        formatter.setOverwriteMode(false);
        formatter.setCommitsOnValidEdit(true);

        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setFocusLostBehavior(JFormattedTextField.COMMIT);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(field::selectAll);
            }
        });

        if (initialValue != 0.0) field.setValue(initialValue);
        else field.setValue(null);
        return field;
    }

    /** Campo inteiro. Inicial em 0 (aparece “0”). */
    public static JFormattedTextField getFormattedIntField(int initialValue) {
        DecimalFormat df = new DecimalFormat("#0");
        NumberFormatter formatter = new NumberFormatter(df);
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(true);
        formatter.setOverwriteMode(false);
        formatter.setCommitsOnValidEdit(true);

        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setHorizontalAlignment(JTextField.RIGHT);
        field.setFocusLostBehavior(JFormattedTextField.COMMIT);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(field::selectAll);
            }
        });

        if (initialValue != 0) {
            field.setValue(initialValue);
        } else {
            field.setValue(null);
        }
        return field;
    }

    /** Campo data no formato DD/MM/AAAA. */
    public static JFormattedTextField getFormattedDateField() {
        try {
            MaskFormatter mf = new MaskFormatter("##/##/####");
            mf.setPlaceholderCharacter('_');
            JFormattedTextField field = new JFormattedTextField(mf);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setFocusLostBehavior(JFormattedTextField.COMMIT);
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    SwingUtilities.invokeLater(field::selectAll);
                }
            });
            return field;
        } catch (ParseException e) {
            throw new RuntimeException("Erro ao criar campo de data: " + e.getMessage(), e);
        }
    }
}
