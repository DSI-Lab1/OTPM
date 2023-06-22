package TaRP;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;


public class MainTest_THURIM {

    public static void main(String[] arg) throws IOException {

    	String input = "D:/IDEA_Workspace/Big Data Conference/TaP/src/TestDatabase/tran_kosarak_UM_New.txt";
        String output = ".//THURIM_output.txt";

        Integer[] tarHUISubsume = {6,11};
        int min_utility = 17270833;
        int min_sup = 99;
        int max_sup = 9900;
        
        THURIMAlgo THURIM = new THURIMAlgo();
        THURIM.THURIM(input, output, min_utility, min_sup, max_sup, tarHUISubsume, true);
        THURIM.printStats(input, min_utility, min_sup, max_sup, tarHUISubsume);
        // Applying the THURIM algorithm


    }
    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTest_THURIM.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }
}
