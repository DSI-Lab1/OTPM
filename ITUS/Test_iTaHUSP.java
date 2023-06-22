package algo;

import java.io.IOException;


public class Test_iTaHUSP {

	public static void main(String[] arg) throws Exception {
		
		String dataset = "test1";
		int threshold = 581;
		
		int[][] querySequence = { { 356, -1, 10, -1, 10, -1, 10, -1, -2 }, // bible
				{ 8, -1, 17, -1, 8, -1, -2 }, // leviathan
				{ 7, -1, 6, -1,15,-1, -2 }, // msnbc
				{842,4616,-1,7752,-1,-2},
				{ 1857, 4250, -1, -2 }, // syn
				{ 8, -1, 9, -1, -2 }, // sign
				{  6, -1, 3, -1, -2 },
				//{ 11, -1, 218, -1, 6, -1, 148, -1, -2 }, // kosarak
				{ 3088, -1, -2 }// test
		};

		// run the algorithm
		// for (int i=0; i<Target.length; i++) {
		String input = "datasets/" + dataset + ".txt";
		
		iTaHUSP algo = new iTaHUSP();

		// set query sequence
		algo.setTargetsequence(querySequence[4]);

		// the path for saving the patterns found
		String output = "output/" + "TUSQ_" + dataset + "_" + threshold + ".txt";

		algo.runAlgorithm(input, output, threshold);

	}
}
