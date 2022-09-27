package TargetUM;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;


/**
 * Example of how to use the TargetUM algorithm
 * from the source code.
 *
 *  Targeted high-utility itemset querying
 *  IEEE Transactions on Artificial Intelligence
 *
 * @author Jinbao Miao, 2021
 */
public class MainTest_TargetUM {
    public static void main(String[] args) throws IOException {
        String input = "data.txt";
        String output = ".//output.txt";

        int minUtil = 5;
        int tarminUtil = minUtil;
        Integer[] tarHUISubsume = {3, 5};
        int runTimes = 1; //
        int select = 0; // 0: TWU asc; 1 : TWU dec; 2 : dic order

        AlgoTargetUM algo = new AlgoTargetUM();
        for (int i = 0; i < runTimes; i++) {
            algo.runTargetUM(input, output, minUtil, 0.0, tarHUISubsume, tarminUtil, select);
            algo.printStats();
        }
    }
    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTest_TargetUM.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }
}
