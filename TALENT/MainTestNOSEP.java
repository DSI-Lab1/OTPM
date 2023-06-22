import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * This file shows how to run the NOSEP algorithm on an input file.
 * 
 * @author Yao Tong
 * @see AlgoNOSEP
 */
public class MainTestNOSEP {
	public static void main(String[] args) throws IOException {
		// the Input files
		String filePath = fileToPath("chimpanzee30.txt");
		String outputPath = "output.txt";
		
		// The algorithm parameters:
		// length constraints
		int minlen = 1; // the minimum length constraint
		int maxlen = 8; // the maximumlength constraint

		// gap constraints
		int mingap = 0; // the minimum gap constraint
		int maxgap = 3; // the maximum gap constraint

		// the given minimum support threshold
		// int minsup = 1;
		int minsup = 100000;

		// run the algorithm
		
		AlgoNOSEP nosep_i = new AlgoNOSEP();
		nosep_i.runAlgorithm(filePath, outputPath, minlen, maxlen, mingap, maxgap, minsup);
		
//		AlgoTNOSP nosep_i = new AlgoTNOSP();
//		nosep_i.runAlgorithm(filePath, outputPath, minlen, maxlen, mingap, maxgap, k, L);
//		NOSTOPK nosep_i = new NOSTOPK();
//		nosep_i.runAlgorithm(filePath, outputPath, minlen, maxlen, mingap, maxgap, k, L);
		nosep_i.printStats();
	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException {
		URL url = MainTestNOSEP.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}
	
	
}
