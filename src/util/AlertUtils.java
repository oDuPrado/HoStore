
package util;
import javax.swing.*;

public class AlertUtils {
    public static void info(String msg){
        JOptionPane.showMessageDialog(null,msg,"Info",JOptionPane.INFORMATION_MESSAGE);
    }
    public static void error(String msg){
        JOptionPane.showMessageDialog(null,msg,"Erro",JOptionPane.ERROR_MESSAGE);
    }
}
