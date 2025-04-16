
package util;
import java.io.*;

public class PythonCaller {
    public static String call(String script, String args) throws IOException, InterruptedException {
        Process p = new ProcessBuilder("python", script, args).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line, out="";
        while((line=br.readLine())!=null){ out+=line+"\n"; }
        p.waitFor();
        return out;
    }
}
