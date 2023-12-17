import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;


public class MainTestSPAM_saveToFile {

	public static void main(String [] arg) throws IOException{    
		// Load a sequence database
		String input = fileToPath("Sy10k.txt");
		String output = ".//output.txt";

		Prefix Query = new Prefix();
		Itemset Q1 = new Itemset();
		Q1.addItem(1069);
		Itemset Q2 = new Itemset();
		Q2.addItem(8808);
		Itemset Q3 = new Itemset();
		Q3.addItem(9661);
		
		Query.addItemset(Q1);
		Query.addItemset(Q2);
		Query.addItemset(Q3);
		
		System.out.println("Query: " + Query);
		
		// Create an instance of the algorithm 
		TaSPM algo = new TaSPM();

		algo.runAlgorithm(input, output, 3, false, Query);    
		algo.printStatistics();
	}
	
	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = MainTestSPAM_saveToFile.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}