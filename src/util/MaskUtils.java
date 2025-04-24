package util;

import javax.swing.JFormattedTextField;
import javax.swing.text.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class MaskUtils {

    public static JFormattedTextField moneyField(double valorInicial) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        DecimalFormat df = (DecimalFormat) nf;
        df.setNegativePrefix("-R$ ");
        df.setPositivePrefix("R$ ");
        NumberFormatter formatter = new NumberFormatter(df);
        formatter.setAllowsInvalid(false);
        formatter.setMinimum(0.0);
        JFormattedTextField f = new JFormattedTextField(formatter);
        f.setValue(valorInicial);
        return f;
    }

    public static JFormattedTextField getFormattedIntField(int valorInicial) {
        NumberFormatter formatter = new NumberFormatter(NumberFormat.getIntegerInstance());
        formatter.setAllowsInvalid(false);
        formatter.setMinimum(0);
        JFormattedTextField f = new JFormattedTextField(formatter);
        f.setValue(valorInicial);
        return f;
    }
    
}
