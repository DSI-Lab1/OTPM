import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;

/**
 * This file shows how to run the dfs version of TALENT algorithm on an input file.
 * 
 * @author Zefeng Chen
 * @see AlgoTALENT_dfs
 */
public class MainTestTALENT_dfs {
	public static void main(String[] args) throws IOException {
		// the Input files
		String filePath = fileToPath("Dengue.txt");
		String outputPath = "output.txt";
		
		// The algorithm parameters:
		// length constraints
		int minlen = 1; // the minimum length constraint
		int maxlen = 10; // the maximumlength constraint

		// gap constraints
		int mingap = 0; // the minimum gap constraint
		int maxgap = 3; // the maximum gap constraint

		// the given minimum support threshold
		int minsup = 1600;
		
		// run the algorithm
		
		AlgoTALENT_dfs nosep_i = new AlgoTALENT_dfs();
		
		StringBuilder builder = new StringBuilder();
		
		//query sequence
		List<Integer> l1 = new ArrayList<Integer>();

		l1.add(1);
		l1.add(6);
		l1.add(8);
		l1.add(12);
		l1.add(20);
		
		AlgoTALENT_dfs.IInt Query = nosep_i.new IInt(l1,l1.size());
		Query.display(builder);
		System.out.println("Query: " + builder);
		
		nosep_i.runAlgorithm(filePath, outputPath, minlen, maxlen, mingap, maxgap, minsup, Query);
		
		nosep_i.printStats();
	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException {
		URL url = MainTestTALENT_dfs.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}
	
	
}
