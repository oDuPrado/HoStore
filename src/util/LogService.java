
package util;
import java.util.logging.*;
import java.io.IOException;
import java.nio.file.*;

public class LogService {
    private static final Logger logger = Logger.getLogger("HoStore");
    static{
        try{
            Files.createDirectories(Paths.get("data"));
            Handler file = new FileHandler("data/hos_logs.txt", true);
            logger.addHandler(file);
            SimpleFormatter fmt = new SimpleFormatter();
            file.setFormatter(fmt);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void info(String msg){ logger.info(msg); }
    public static void error(String msg, Throwable t){ logger.log(Level.SEVERE,msg,t); }
}
