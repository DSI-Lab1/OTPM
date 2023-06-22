/**
 * Copyright (C), 2015-2020, HITSZ
 * FileName: MainTestTHUSSpan
 * Author:   qj Dai
 * Date:     2020/10/31 13:23
 * Description: This file is for testing the AlgoTHUSSpan algorithm.
 */

import TUSQ.AlgoTUSQ;
import TUSQ.AlgoTUSQ_SRU;
import TUSQ.AlgoTUSQ_TDU;

import java.io.IOException;

public class MainTestTUSQ {

    public static void main(String[] arg) throws IOException {
        /**设置数据集和阈值**/
        String dataset = "MSNBC";
        double thresholdratio = 0.055;
        //int index = 50;
        int[][] targetSequence = {
                {356,-1,10,-1,10,-1,10,-1,-2}, //bible
                {8,-1,17,-1,8,-1,-2},  //leviathan
                {7,-1,6,-1,-2},               //msnbc
                {1857,4250,-1,-2},            //syn
                {8,-1,9,-1,-2},       //sign
                {11,-1,218,-1,6,-1,148,-1,-2}    //kosarak
        };

        // run the algorithm
        //for (int i=0; i<Target.length; i++) {
            // the input database
            String input = "input/" + dataset + ".txt";
            //for (int j=0; j<utilityRatio.length; j++) {

            AlgoTUSQ algo = new AlgoTUSQ();

            // set the maximum pattern length (optional)
            algo.setMaxPatternLength(1000);

            //set target sequence
            algo.setTargetsequence(targetSequence[2]);

            // the path for saving the patterns found
            String output = "output/" + "TUSQ_" + dataset + "_" + thresholdratio + ".txt";

            algo.runAlgorithm(input, output, thresholdratio);
            // print statistics
            algo.printStatistics();
            //index++;
            //}
        //}
    }
}