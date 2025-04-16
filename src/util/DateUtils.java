
package util;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static String now(){
        return LocalDateTime.now().format(ISO);
    }
}
