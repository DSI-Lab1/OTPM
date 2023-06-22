/**
 * Copyright (C), 2015-2021, HITSZ
 * FileName: AlgoTUSQ
 * Author: dqj
 * Date: 2020/11/01 18:05
 * Description: The implementation of TUSQ algorithm.
 * Pruning strategy: SRU + TDU
 * Data structure: Targeted list + q-matrix + LOT
 */
package TUSQ;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;


public class AlgoTUSQ {

    //the time the algorithm started
    double startTimestamp = 0;
    //the time the algorithm terminated
    double endTimestamp = 0;
    //the number of patterns generated
    int patternCount = 0;

    //writer to write the output file
    BufferedWriter writer = null;

    final int BUFFERS_SIZE = 2000;

    //buffer for storing the current pattern that is mined when performing mining
    private int[] patternBuffer = null;

    //if true, debugging information will be shown in the console
    final int DEBUG = 0; //1:qmatrix,2:UtilityChain,3:Utility and SRU of each 1-sequence,4:Pre-insertion,5:projected UtilityChain,6:search order,7:q-seq包含target seq
    int DEBUG_flag = 0;

    //if true, save result to file in a format that is easier to read by humans
    final boolean SAVE_RESULT_EASIER_TO_READ_FORMAT = false;

    //the minUtility threshold
    double minUtility = 0;

    //max pattern length
    int maxPatternLength = 1000;

    //the input file path
    String input;

    //Target sequence
    int[] targetSequence;

    // the number of Candidate
    int NumOfCandidate = 0;

    /**
     * Default constructor
     */
    public AlgoTUSQ() {
    }

    /**
     * @param input the input file path
     * @param output the output file path
     * @param utilityratio the minimum utility threshold ratio
     * @throws IOException exception if error while writing the file
     */
    public void runAlgorithm(String input, String output, double utilityratio) throws IOException {
        // reset maximum
        MemoryLogger.getInstance().reset();

        // input path
        this.input = input;

        // initialize the buffer for storing the current itemset
        patternBuffer = new int[BUFFERS_SIZE];

        // record the start time of the algorithm
        startTimestamp = System.currentTimeMillis();

        // create a writer object to write results to file
        writer = new BufferedWriter(new FileWriter(output));

        // for storing the current sequence number
        int NumberOfSequence = 0;

        // for storing the utility of all sequence
        int totalUtility = 0;

        BufferedReader myInput = null;
        String thisLine;

        //================  First DATABASE SCAN TO CONSTRUCT QMATRIX (DEFULT STORAGE MODE IN THE PAPER)	===================
        //================  CONSTRUCT UTILITYCHAINs OF ALL 1-SEQUENCEs, CALCULATE UTILITY AND SRU OF THEM =================
        // Read the database to create the QMatrix for each sequence；数据集中所有q-seq的效用矩阵
        List<QMatrix> database  = new ArrayList<>();
        // for storing an UtilityChain of each 1-sequence；
        // 1-seq的utilitychain
        Map<Integer,ArrayList<TargetedList>> mapItemUC = new HashMap<>();

        //for storing an Utility and SRU of each 1-sequence；1-seq的效用和SRU
        Map<Integer,Integer> mapItemUtility = new HashMap<>();
        Map<Integer,Integer> mapItemSRU = new HashMap<>();

        //LOT；记录target sequence的每个项集在每个q-seq的最后出现位置
        ArrayList<ArrayList<Integer>> LOT = new ArrayList<>();

        //目标序列的List形式
        ArrayList<ArrayList> targetseq = convertT(targetSequence);

        try {
            // prepare the object for reading the file
            myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));

            // We will read each sequence in buffers.
            // The first buffer will store the items of a sequence and the -1 between them)；itemBuffer存q-seq中所有的item和-1
            int[] itemBuffer = new int[BUFFERS_SIZE];
            // The second buffer will store the utility of items in a sequence and the -1 between them)；utilityBuffer存q-seq中item的效用和-1
            int[] utilityBuffer = new int[BUFFERS_SIZE];
            // The following variable will contain the length of the data stored in the two previous buffer；以上两个数组的长度
            int itemBufferLength;
            // Finally, we create another buffer for storing the items from a sequence without
            // the -1. This is just used so that we can collect the list of items in that sequence
            // efficiently. We will use this information later to create the number of rows in the
            // QMatrix for that sequence.
            // itemsSequenceBuffer存seq的所有item（不含-1，-2）
            int[] itemsSequenceBuffer = new int[BUFFERS_SIZE];
            // The following variable will contain the length of the data stored in the previous buffer；
            // 一个q-seq包含的item数量（不含-1，-2）
            int itemsLength;

            int seqnum = 0;
            // for each line (transaction) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                seqnum++;
                // if the line is  a comment, is  empty or is a kind of metadata
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }

                // for storing an UtilityList of each 1-sequence in the current transaction；每个1-seq在当前序列上的utilitylist
                Map<Integer, TargetedList> mapItemUL = new HashMap<Integer, TargetedList>();

                //for storing utility and remaining utility of each 1-sequence in the current transaction；每个1-seq在当前序列上的效用和剩余效用
                Map<Integer, Integer> mapItemU = new HashMap<Integer, Integer>();
                Map<Integer, Integer> mapItemP = new HashMap<Integer, Integer>();

                // We reset the two following buffer length to zero because we are reading a new sequence.
                itemBufferLength = 0;
                itemsLength = 0;

                // split the sequence according to the " " separator
                String tokens[] = thisLine.split(" ");

                // get the sequence utility (the last token on the line)
                String sequenceUtilityString = tokens[tokens.length - 1];
                int positionColons = sequenceUtilityString.indexOf(':');
                int sequenceUtility = Integer.parseInt(sequenceUtilityString.substring(positionColons + 1));

                // This variable will count the number of itemsets；项集的个数，从1开始是因为最后一个项集的-1没被读入
                int nbItemsets = 1;

                //recording the remaining utility when constructing UtilityList(For Task 2)；记录剩余效用，用于构建utilitylist
                int restUtility = sequenceUtility;

                // For each token on the line except the last three tokens (the -1 -2 and sequence utility).
                // 处理序列中的每一个token（tokens.length - 4:排除最后的-1，-2和SUtility）
                for (int i = 0; i < tokens.length - 4; i++) {  //-2与SUtility间隔为2个空格，tokens.length-4
                    String currentToken = tokens[i];
                    // if empty, continue to next token
                    if (currentToken.length() == 0) {
                        continue;
                    }
                    // if the current token is -1 ,the ending sign of an itemset
                    if (currentToken.equals("-1")) {
                        // It means that it is the end of an itemset.
                        // We store the -1 in the respective buffers
                        itemBuffer[itemBufferLength] = -1;
                        utilityBuffer[itemBufferLength] = -1;
                        // We increase the length of the data stored in the buffers
                        itemBufferLength++;

                        // we update the number of itemsets in that sequence that are not empty
                        nbItemsets++;
                    } else {
                        //We need to finish the following three tasks if the current token is an item
                        //三个任务:构建qmatrix；构建1-seq的utilitylist；计算1-seq的SRU和utility

                        /* Task 1: Construct QMtriax *//*记录item及其效用，用于构建qmatrix*/

                        // We will extract the item from the string:
                        int positionLeftBracketString = currentToken.indexOf('[');
                        int positionRightBracketString = currentToken.indexOf(']');
                        String itemString = currentToken.substring(0, positionLeftBracketString);
                        Integer item = Integer.parseInt(itemString);

                        // We also extract the utility from the string:
                        String utilityString = currentToken.substring(positionLeftBracketString + 1, positionRightBracketString);
                        Integer itemUtility = Integer.parseInt(utilityString);

                        // We store the item and its utility in the buffers
                        // for temporarily storing the sequence
                        itemBuffer[itemBufferLength] = item;
                        utilityBuffer[itemBufferLength] = itemUtility;
                        itemBufferLength++;

                        // We also put this item in the buffer for all items of this sequence
                        itemsSequenceBuffer[itemsLength++] = item;
                    }
                }// task1:构建当前序列的itemBuffer、utilityBuffer和itemsequenceBuffer 完成

                //DPP策略，判断当前q-seq是否包含target seq
                //如果当前q-seq不包含target seq，则跳过该q-seq
                if(sequenceUtility == 0||seqContain(itemBuffer, itemBufferLength,targetSequence)==false) {
                    if(DEBUG==7){
                        System.out.print(seqnum+" 不包含target的 q-seq:");
                        for(int i=0;i<itemBufferLength;i++){
                            System.out.print(itemBuffer[i]+" ");
                        }
                        System.out.println();
                    }
                    continue;
                }

                //totalUtility只统计包含target seq的效用
                totalUtility += sequenceUtility;

                /************************填LOT************************/
                fillLOT(LOT,targetseq,itemBuffer,itemBufferLength);
                if(DEBUG==8){
                    System.out.print("sid:"+NumberOfSequence+" ");
                    System.out.println(LOT.get(NumberOfSequence).toString());
                    System.out.println();
                }

                /**********************************以下完成task2和task3******************************************/

                /* Task 2: Construct UtilityList */
                //构建1-seq在当前序列的UtilityList

                //记录当前itemset是当前q-seq的第几个itemset
                int currentItemset = 0;

                //以下构建各个1-seq的utilitylist
                for (int i = 0; i < itemBufferLength; i++) {
                    int item = itemBuffer[i];
                    int itemUtility = utilityBuffer[i];
                    if(item == -1)  currentItemset++;
                    else {
                        restUtility -= itemUtility;

                        //求前缀长度oneseqprel
                        int oneseqprel = 0;
                        if (item == targetSequence[0]) {
                            oneseqprel = 1;
                        }

                        /*****************判断剩余序列是否包含后缀****************/
                        boolean rsContainsuf;
                        //后缀首个项集在T中的位置（即T的第几个项集）
                        int firstSetPos = getFirstSetPos(oneseqprel,targetSequence);
                        if(firstSetPos==99999) rsContainsuf=true;
                        else{
                            //后缀的首个项集在当前q-seq中最后出现的位置（即q-seq的第几个项集）
                            int lastpos = LOT.get(NumberOfSequence).get(firstSetPos).intValue();
                            if(lastpos<currentItemset){
                                rsContainsuf = false;
                            }else
                                rsContainsuf = true;
                        }

                        // if the item has not appeared in the current transaction
                        if (mapItemUL.get(item) == null) {
                            TargetedList tempUL = new TargetedList();
                            tempUL.set_sid(NumberOfSequence);
                            //当剩余序列包含后缀时，才把这个instance放到utilitylist中
                            if (rsContainsuf) {
                                tempUL.set_prel(oneseqprel);
                                tempUL.add( currentItemset, itemUtility, restUtility);
                            }
                            mapItemUL.put(item, tempUL);
                        } else { // has appeared
                            //当剩余序列包含后缀时，才把这个instance放到utilitylist中
                            if (rsContainsuf) {
                                TargetedList tempUL = mapItemUL.get(item);
                                tempUL.add( currentItemset, itemUtility, restUtility);
                                mapItemUL.put(item, tempUL);
                            }
                        } //mapItemUL: for storing an UtilityList of each 1-sequence in the current transaction
                        //task2完成

                        /* Task 3: Calculate  Utility and SRU */
                        //计算1-seq的效用和SRU

                        int tempSRU = 0;
                        // if ru==0 then SRU=0。 序列的最后一个item的ru==0
                        if (i==itemBufferLength-1) {
                            tempSRU = 0;
                        }else {
                            //因为1-seq的SRU不是根据utilitylist计算的，所以计算它们的SRU时，要增加剩余序列是否包含后缀的判断条件
                            if (rsContainsuf) {
                                tempSRU = itemUtility + restUtility;
                            }
                        }
                        //if the item has not appeared in the current transaction
                        if (mapItemU.get(item) == null) {
                            mapItemU.put(item, itemUtility);
                            mapItemP.put(item, tempSRU);
                            mapItemUL.get(item).set_SRU(tempSRU);
                        } else { //has appeared
                            if (itemUtility > mapItemU.get(item)) {
                                mapItemU.put(item, itemUtility);
                            }
                            //当前instance的SRU更大，则更新SRU
                            if (tempSRU > mapItemP.get(item)) {
                                mapItemP.put(item, tempSRU);
                                mapItemUL.get(item).set_SRU(tempSRU);
                            }
                        }
                    }
                } // 处理完所有token，task2,3结束


                //Update global variables mapItemUtility, mapItemSRU and mapItemUC according to the current transaction
                //根据以上得到的单个序列的mapItemU、mapItemP、mapItemUL更新全局变量mapItemUtility、mapItemSRU、mapItemUC
                //更新各个1-seq的全局utility
                for(Entry<Integer,Integer> entry: mapItemU.entrySet()){
                    int item = entry.getKey();
                    if(mapItemUtility.get(item)==null){
                        mapItemUtility.put(item,entry.getValue());
                    }else{
                        mapItemUtility.put(item,entry.getValue()+ mapItemUtility.get(item));
                    }
                }
                //更新各个1-seq的全局SRU
                for(Entry<Integer,Integer> entry: mapItemP.entrySet()){
                    int item = entry.getKey();
                    if(mapItemSRU.get(item)==null){
                        mapItemSRU.put(item,entry.getValue());
                    }else{
                        mapItemSRU.put(item,entry.getValue()+ mapItemSRU.get(item));
                    }
                }
                //更新各个1-seq的utilitychain，即mapItemUC
                for(Entry<Integer, TargetedList> entry: mapItemUL.entrySet()){
                    int item = entry.getKey();
                    ArrayList<TargetedList> tempChain = new ArrayList<TargetedList>();
                    if(mapItemUC.get(item) != null)
                        tempChain = mapItemUC.get(item);
                    //把utilitylist加到utilitychain
                    tempChain.add(entry.getValue());
                    mapItemUC.put(item,tempChain);
                }

                /******************************以下构建当前q-seq的qmatrix***********************************/
                // Now, we sort the buffer for storing all items from the current sequence in alphabetical order
                //对itemsSequenceBuffer中所有item进行排序
                Arrays.sort(itemsSequenceBuffer,0, itemsLength);
                // but an item may appear multiple times in that buffer so we will loop over the buffer to remove duplicates
                //要去掉itemsSequenceBuffer中重复的item
                // This variable remember the last insertion position in the buffer:
                int newItemsPos = 0;
                // This variable remember the last item read in that buffer
                int lastItemSeen = -999;
                // for each position in that buffer
                for(int i=0; i< itemsLength; i++) {
                    // get the item
                    int item = itemsSequenceBuffer[i];
                    // if the item was not seen previously
                    if(item != lastItemSeen) {
                        // we copy it at the current insertion position
                        itemsSequenceBuffer[newItemsPos++] = item;
                        // we remember this item as the last seen item
                        lastItemSeen = item;
                    }
                } // remove repeating items of itemsSequenceBuffer (length: newItemsPos) and sort it in ascending order

                // New we count the number of items in that sequence；序列中不同item的数量
                int nbItems = newItemsPos;

                // And we will create the Qmatrix for that sequence
                // 构建当前序列的qmatrix
                QMatrix matrix = new QMatrix(nbItems, nbItemsets, itemsSequenceBuffer, nbItems, sequenceUtility);
                // We add the QMatrix to the initial sequence database.
                database.add(matrix);
                //System.out.println("database中qmatrix的数量为:"+database.size());

                // Next we will fill the matrix column by column
                // 逐列填充qmatrix
                // This variable will represent the position in the sequence；itemBuffer的当前访问位置
                int posBuffer = 0;
                // 对于每一列（每一个项集）
                for(int itemset=0; itemset < nbItemsets; itemset++) {
                    // This variable represent the position in the list of items in the QMatrix
                    //posNames指定当前序列的itemNames中的一个item，也就是指定qmatrix的某一行
                    int posNames = 0;

                    // While we did not reach the end of the sequence；处理项集中的每个item
                    while(posBuffer < itemBufferLength ) {
                        // Get the item at the current position in the sequence
                        int item = itemBuffer[posBuffer];

                        // if it is an itemset separator, we move to next position in the sequence
                        //如果当前item是-1，则处理下一个itemset
                        if(item == -1) {
                            posBuffer++;
                            break;
                        }
                        // else if it is the item that correspond to the next row in the matrix
                        //如果当前item是posNames指定的那个item，则填充qmatrix中的(posNames, itemset)项
                        else if(item == matrix.itemNames[posNames]) {
                            // calculate the utility for this item/itemset cell in the matrix
                            int utility = utilityBuffer[posBuffer];
                            // We update the reamining utility by subtracting the utility of the
                            // current item/itemset
                            sequenceUtility -= utility;
                            // update the cell in the matrix
                            matrix.registerItem(posNames, itemset, utility, sequenceUtility);
                            // move to the next item in the matrix and in the sequence
                            //移动到qmatrix的下一行和itemBuffer的下一个item
                            posNames++;
                            posBuffer++;
                        }else if(item > matrix.itemNames[posNames]) {
                            // if the next item in the sequence is larger than the current row in the matrix
                            // it means that the item do not appear in that itemset, so we put a utility of 0
                            // for that item and move to the next row in the matrix.
                            matrix.registerItem(posNames, itemset, 0, sequenceUtility);
                            posNames++;
                        }else {
                            // Otherwise, we put a utility of 0 for the current row in the matrix and move
                            // to the next item in the sequence
                            matrix.registerItem(posNames, itemset, 0, sequenceUtility);
                            posBuffer++;
                        } // By default, the items in reverse order are deleted.
                    }
                } // QMatrix of the current transaction has been built.

                // if in debug mode, we print the q-matrix that we have just built
                if(DEBUG==1) {
                    System.out.println(matrix.toString());
                    System.out.println();
                }
                //System.out.print(NumberOfSequence+"------");System.out.println(thisLine);

                // we update the number of transactions；序列数++，准备处理下一个序列
                NumberOfSequence++;
            } // finish scaning a transaction each time through the loop

            System.out.println("num of q-seq that contain target sequence:" + NumberOfSequence);

            // if in debug mode, we print the UtilityChain that we have just built
            if(DEBUG==2) {
                for (Entry<Integer,ArrayList<TargetedList>> entry: mapItemUC.entrySet()){
                    System.out.println("item:"+entry.getKey());
                    for(int i=0;i<entry.getValue().size();i++){
                        System.out.println(i+"-th UtilityList:");
                        for(int j=0;j<entry.getValue().get(i).LengthOfUtilityList;j++){
                            System.out.print(j+"-th element: ");
                            System.out.print("sid:"+entry.getValue().get(i).get_sid());
                            System.out.print("  tid:"+entry.getValue().get(i).List.get(j).tid);
                            System.out.print("  acu:"+entry.getValue().get(i).List.get(j).acu);
                            System.out.println("  ru:"+entry.getValue().get(i).List.get(j).ru);
                        }
                        System.out.println("End of a UtilityList");
                    }
                    System.out.println("******");
                }
            }

            // if in debug mode, we print the Utility and SRU of each 1-sequence that we have just built
            if(DEBUG==3) {
                System.out.println("SRU:");
                for (Entry<Integer,Integer> entry: mapItemSRU.entrySet()){
                    System.out.println(entry.getKey()+" : "+entry.getValue());
                }
                System.out.println("******");
                System.out.println("Utility:");
                for (Entry<Integer,Integer> entry: mapItemUtility.entrySet()){
                    System.out.println(entry.getKey()+" : "+entry.getValue());
                }
            }
        } catch (Exception e) {
            // catches exception if error while reading the input file
            e.printStackTrace();
        }finally {
            if(myInput != null){
                // close the input file
                myInput.close();
            }
        }//完成读取数据

        //System.out.println("time cost of loading data:" + (System.currentTimeMillis() - startTimestamp)/1000 + " s");
        // check the memory usage
        MemoryLogger.getInstance().checkMemory();

        //设置阈值
        this.minUtility = utilityratio * totalUtility;
        System.out.println("total utility: "+totalUtility+" utility ratio:"+utilityratio+" Threshold:"+this.minUtility);

        // Mine the database recursively
        for(Entry<Integer, Integer> entry : mapItemSRU.entrySet()){
            int item = entry.getKey();
            patternBuffer[0]= item;
            patternBuffer[1] = -1;//项集结束标识
            patternBuffer[2] = -2;//序列结束标识

            //候选数++
            NumOfCandidate++;

            //检查1-seq是否高效用且包含target seq
            if(mapItemUtility.get(item)>=minUtility && seqContain(patternBuffer,1,targetSequence)){
                //writeOut(patternBuffer,1,mapItemUtility.get(item));
                patternCount++;
            }

            // SRU pruning strategy
            //用SRU对1-seq深度剪枝
            if (entry.getValue() >= minUtility) {
                TUSQ(patternBuffer, 1, database, mapItemUC.get(item), 1, targetSequence, LOT);
            }
        }

        double runtime = System.currentTimeMillis() - startTimestamp;
        StringBuilder buffer = new StringBuilder();
        buffer.append("============= ALGOTUSQ  v1.0 - STATS =============\n");
        buffer.append(" Target sequence:");
        for(int item:targetSequence){
            buffer.append(item+",");
        }
        buffer.append("\n");
        buffer.append(" num of q-seq that contain target sequence :" + NumberOfSequence + " \n");
        buffer.append(" Threshold :"+this.minUtility+" \n");
        buffer.append(" Total time : " + runtime/1000 + " s\n");
        buffer.append(" Max Memory : " + MemoryLogger.getInstance().getMaxMemory() + " MB\n");
        buffer.append(" Number of candidates : " + NumOfCandidate + " \n");
        buffer.append(" High-utility sequential pattern count : " + patternCount);
        writer.write(buffer.toString());
        writer.newLine();

        // check the memory usage again and close the file.
        MemoryLogger.getInstance().checkMemory();
        // close output file
        writer.close();
        // record end time
        endTimestamp = System.currentTimeMillis();
    }

    //This inner class is used to store the information of candidates after concatenating the item.
    public class ItemConcatnation implements Comparable<ItemConcatnation>{
        // utility
        int utility;
        // SRU
        int SRU;
        // projected database UtilityChain；候选序列的utilitychain
        ArrayList<TargetedList> UChain;
        // Candidate after concatenating the item
        public int[] candidate;
        // length of Candidate after concatenating the item
        int candidateLength;

        // Constructor
        public ItemConcatnation(int utility, int SRU, ArrayList<TargetedList> UChain, int[] candidate, int candidateLength){
            this.utility = utility;
            this.SRU = SRU;
            this.UChain = UChain;
            this.candidateLength = candidateLength;
            this.candidate = new int[BUFFERS_SIZE];
            System.arraycopy(candidate, 0, this.candidate, 0, candidateLength);
        }

        // overwrite the compareTo function；
        public int compareTo(ItemConcatnation t){
            if (this.SRU > t.SRU)
                return -1;
            else if(this.SRU < t.SRU)
                return 1;
            else
                return 0;
        }
    }

    /**
     * construct target chain of candidates
     * @param candidate a sequence t'
     * @param candidateLength length of sequence t'
     * @param database q-matrix of all sequences 
     * @param utilitychain target chain of t which is the prefix of t'
     * @param kind 0:i-Concatenation，1:s-Concatenation
     * @param T target sequence
     * @param LOT last instance table
     */
    private ItemConcatnation constructUtilityChain(int[] candidate, int candidateLength, List<QMatrix> database, ArrayList<TargetedList> utilitychain, int kind, int[] T, ArrayList<ArrayList<Integer>> LOT){
        //store UtilityChain of candidate
        ArrayList<TargetedList> uc = new ArrayList<TargetedList>();
        // 注意！“item”是将要扩展的item，但是已经放入candidate中，即t'的最后一个item
        int item = candidate[candidateLength-1];
        // store utility and SRU of candidate
        //新序列t'的全局效用和SRU
        int Utility = 0;
        int SRU = 0;

        //for each UtilityList of candidate's Prefix
        //对于t'前缀t的每一个utilitylist
        for (TargetedList utilitylist:utilitychain){
            // LocalUtility、LocalSRU记录t'在此sequence中的utility和SRU
            int LocalUtility = 0;
            int LocalSRU = 0;
            //判断当前utilitylist是否有element，没有则处理下一个utilitylist
            if(utilitylist.List.size()==0){
                continue;
            }
            //initialize the variables
            //store serial number of transaction
            int sid = utilitylist.get_sid();

            //t'的utilitylist
            TargetedList ul = new TargetedList();
            ul.set_sid(sid);
            //get qmatrix of the current transaction
            QMatrix qmatrix = database.get(sid);
            //ItemNum:当前序列包含的不同item的数量
            int ItemNum = qmatrix.itemNames.length;
            int row = 0;
            //找t'的最后一个item在当前序列的qmatrix中所在的行
            while (row!=ItemNum){
                if (item==qmatrix.itemNames[row]){
                    break;
                }
                row++;
            }
            //如果t'的最后一个item不出现在当前序列，那么可以处理下一个序列
            if (row==qmatrix.itemNames.length)
                continue;

            /***************************************i-concatenation**********************************************/
            if(kind==0){
                //construct UtilityList of candidate in the current transaction
                //对于前缀的utilitylist中每一个element，即t最后一个item所在的每一个项集
                for (int j = 0; j<utilitylist.LengthOfUtilityList; j++){
                    int column = utilitylist.List.get(j).tid;
                    int ItemUtility = qmatrix.matrixItemUtility[row][column];

                    //如果t'的最后一个item出现在该项集，则可以在该项集把t做i-extension形成t'，即可以创建一个element，插入到t'的utilitylist
                    if (ItemUtility!=0){
                        //当前element的prel
                        int prel = utilitylist.get_prel();
                        //新的前缀长度
                        int newprel = updateprel(item, prel, T,0);

                        /*****************判断剩余序列是否包含后缀****************/
                        boolean rsContainsuf;
                        //后缀首个项集在T中的位置（即T的第几个项集）
                        int firstSetPos = getFirstSetPos(newprel,targetSequence);
                        if(firstSetPos==99999) rsContainsuf=true;
                        else{
                            //后缀的首个项集在当前q-seq中最后出现的位置（即q-seq的第几个项集）
                            int lastpos = LOT.get(sid).get(firstSetPos);
                            if(lastpos<column){
                                rsContainsuf = false;
                            }else
                                rsContainsuf = true;
                        }

                        //如果剩余序列包含后缀，则创建新的element加入到t'的utilitylist，并设置utilitylist的prel
                        if(rsContainsuf) {
                            ul.set_prel(newprel);
                            ul.add(column, utilitylist.List.get(j).acu + ItemUtility, qmatrix.matrixItemRemainingUtility[row][column]);
                        }
                    }
                }
                // if the current transaction does not hold candidate
                //无法将t扩展成t'，则处理下一个序列
                if (ul.LengthOfUtilityList==0)
                    continue;
            }//i-extension形成的新候选序列t'的utilitylist构建完成
            else{
                /******************************************s-concatenation****************************************/
                //当前序列itemset的个数
                int ItemsetNum = qmatrix.matrixItemUtility[0].length;
                //for temporarily storing UtilityElement
                //mapUE暂存s-extension构建的element，即存放以某个项集结尾的t'的instance的element
                Map<Integer, TargetedList.UtilityElement> mapUE = new HashMap<Integer, TargetedList.UtilityElement>();
                //an instance of UtilityList
                TargetedList l = new TargetedList();

                //construct UtilityList of candidate in the current transaction
                //对于t的utilitylist中每一个element，即t最后一个item所在的每一个项集
                for (int j = 0; j<utilitylist.LengthOfUtilityList; j++){
                    int column = utilitylist.List.get(j).tid;
                    //当前element的prel
                    int prel = utilitylist.get_prel();
                    //搜索qmatrix中 行号为row，列号为i（i大于t最后一个item所在列，即column）的所有项
                    for (int i=column+1;i<ItemsetNum;i++){
                        int ItemUtility = qmatrix.matrixItemUtility[row][i];
                        //如果第i个项集包含t'的最后一个item，则可以在该项集上对t做s-extension形成t'
                        if (ItemUtility!=0) {

                            //新的前缀长度
                            int newprel = updateprel(item, prel, T, 1);

                            /*****************判断剩余序列是否包含后缀****************/
                            boolean rsContainsuf;
                            //后缀首个项集在T中的位置（即T的第几个项集）
                            int firstSetPos = getFirstSetPos(newprel,targetSequence);
                            if(firstSetPos==99999) rsContainsuf=true;
                            else{
                                //后缀的首个项集在当前q-seq中最后出现的位置（即q-seq的第几个项集）
                                int lastpos = LOT.get(sid).get(firstSetPos);
                                if(lastpos< i ){
                                    rsContainsuf = false;
                                }else
                                    rsContainsuf = true;
                            }

                            //仅当剩余序列包含后缀，才暂存新建的element，并更新utilitylist的prel
                            if (rsContainsuf) {
                                int PrefixUtility = utilitylist.List.get(j).acu;
                                ul.set_prel(newprel);
                                // We need to merge them into an UtilityElement if prefix positions are different but pivot is the same.
                                //如果正在构建的utilitylist中没有tid为i的element，则暂存
                                if (mapUE.get(i) == null)
                                    mapUE.put(i, l.new UtilityElement(i, PrefixUtility + ItemUtility, qmatrix.matrixItemRemainingUtility[row][i]));
                                    //如果正在构建的utilitylist中有tid为i的element，则比较utility大小，留下大的
                                else
                                    //store the UtilityElement whose utility is larger prefix positions are different but pivot is the same.
                                    if (mapUE.get(i).acu < PrefixUtility + ItemUtility)
                                        mapUE.put(i, l.new UtilityElement(i, PrefixUtility + ItemUtility, qmatrix.matrixItemRemainingUtility[row][i]));
                            }
                        }
                    }
                }
                // if the current transaction does not hold candidate
                //无法将t扩展成t'，则处理下一个序列
                if (mapUE.size()==0)
                    continue;

                //sort mapUE in tid ascending order
                //按tid升序排序mapUE，准备形成utilitylist
                /*List<Entry<Integer, UtilityList_New.UtilityElement>> list = new ArrayList<Entry<Integer, UtilityList_New.UtilityElement>>(mapUE.entrySet());
                if (mapUE.size()!=1){
                    Collections.sort(list, new Comparator<Entry<Integer, UtilityList_New.UtilityElement>>() {
                        @Override
                        public int compare(Entry<Integer, UtilityList_New.UtilityElement> o1, Entry<Integer, UtilityList_New.UtilityElement> o2) {
                            return o1.getKey().compareTo(o2.getKey());
                        }
                    });
                }
                // finish updating UtilityList of the current transaction
                for (int i = 0; i < list.size(); i++) {
                    UtilityList_New.UtilityElement tmpUE = list.get(i).getValue();
                    ul.add(tmpUE.tid,tmpUE.acu,tmpUE.ru);
                }*/
                for(Entry<Integer, TargetedList.UtilityElement> entry:mapUE.entrySet()){
                    TargetedList.UtilityElement tmpUE = entry.getValue();
                    ul.add(tmpUE.tid,tmpUE.acu,tmpUE.ru);
                }
            }//s-extension形成的t'的utilitylist构建完成

            // calculate utility and SRU of candidate in the current transaction
            //计算t'在当前序列中的效用和SRU
            for (int i=0; i<ul.LengthOfUtilityList; i++){
                TargetedList.UtilityElement ue = ul.List.get(i);
                if (ue.acu>LocalUtility)
                    LocalUtility = ue.acu;
                if (ue.ru==0)
                    //TODO:break改continue
                    // DONE
                    //break;
                    continue;
                if(ue.acu+ue.ru>LocalSRU)
                    LocalSRU = ue.acu+ue.ru;
            }

            //calculate utility and SRU of candidate and update the two global variable SRU and Utility
            Utility += LocalUtility;
            SRU += LocalSRU;
            ul.set_SRU(LocalSRU);

            //add UtilityList to UtilityChain
            uc.add(ul);
        }

        // if in debug mode, we print the UtilityChain that we have just built
        if(DEBUG==5){
            //System.out.println("**********************");
            System.out.print("Pattern: ");
            for (int i = 0; i < candidateLength; i++) {
                System.out.print(candidate[i] + " ");
            }
            //System.out.println();
            System.out.println("  总SRU:" + SRU + " 总Utility:" + Utility);
            for (int i = 0; i < uc.size(); i++) {
                System.out.println(i + "-th UtilityList:");
                for (int j = 0; j < uc.get(i).LengthOfUtilityList; j++) {
                    System.out.print("Element" + j + ": ");
                    System.out.print("sid:" + uc.get(i).get_sid());
                    System.out.print("  tid:" + uc.get(i).List.get(j).tid);
                    System.out.print("  acu:" + uc.get(i).List.get(j).acu);
                    System.out.println("  ru:" + uc.get(i).List.get(j).ru);
                }
                System.out.println("End of a UtilityList");
            }
            System.out.println("#####################");
        }

        candidate[candidateLength]=-1;
        candidate[candidateLength+1]=-2;

        //return an instance
        return new ItemConcatnation(Utility, SRU, uc, candidate, candidateLength);
    }

    /**
     * a recursive function to mine high utility pattern
     * @param prefix mine high utility pattern based on prefix
     * @param prefixLength length of prefix
     * @param database q-matrix of all sequences
     * @param utilitychain target chain of prefix
     * @param itemCount number of items in prefix
     * @param T target sequence
     * @param LOT last instance table
     */
    private void TUSQ(int[] prefix, int prefixLength, List<QMatrix> database, ArrayList<TargetedList> utilitychain, int itemCount, int[] T, ArrayList<ArrayList<Integer>>LOT){
        //Update the count of the candidate
        if(DEBUG==6){
            if (DEBUG_flag>0&&DEBUG_flag<10000){
                // Print the current prefix
                for(int i=0; i< prefixLength; i++){
                    System.out.print(prefix[i] + " ");
                }
                System.out.println(DEBUG_flag+"TmpMinUtility:"+minUtility);
                System.out.println();
            }
        }

        //for storing TDU  after concatenation
        //以下map保存i-extension和s-extension后形成的序列的TDU，同时也是ilist和slist
        Map<Integer,Integer>mapiItemTDU = new HashMap<>();
        Map<Integer,Integer>mapsItemTDU = new HashMap<>();

        /**********************************************构建ilist和slist**********************************************/
        //scan prefix-projected DB once to find items to be concatenated
        //对于前缀t所在的每一个q-seq
        for (TargetedList utilitylist:utilitychain) {
            //store the qmatrix of the current transaction
            //如果当前utilitylist没有element，则处理下一个utilitylist
            if(utilitylist.List.size()==0) {
                continue;
            }
            int sid = utilitylist.get_sid();
            QMatrix qmatrix = database.get(sid);

            //record the last item of Prefix
            int item = prefix[prefixLength-1];
            //row:前缀t最后一个item在当前序列的qmatrix中的行号
            int row = 0;
            while (item!=qmatrix.itemNames[row]){
                row++;
            }

            /***********************************以下构建ilist********************************/
            // put i-extension items into ilist
            // update the global variable mapiItemTDU
            int ItemNum = qmatrix.itemNames.length;

            //对于当前utilitylist的每一个element，也就是当前q-seq的utilitymatrix中item出现的每一列
            for (int j = 0; j<utilitylist.LengthOfUtilityList;j++){
                int column = utilitylist.List.get(j).tid;
                //获取当前element的prel
                int prel = utilitylist.get_prel();
                //找出该element下可用于做i-extension的item，也就是当前q-seq的qmatrix的(row, column)项下方的那些item
                for (int i=row+1;i<ItemNum;i++){
                    int ConItem = qmatrix.itemNames[i]; //当前extension item
                    /*******************************构建ilist并计算其中各个item的TDU*********************************/
                    if (qmatrix.matrixItemUtility[i][column]!=0){

                        //获取后缀
                        int newprel = updateprel(ConItem, prel, T, 0);

                        /*****************判断剩余序列是否包含后缀****************/
                        boolean rsContainsuf;
                        //后缀首个项集在T中的位置（即T的第几个项集）
                        int firstSetPos = getFirstSetPos(newprel,targetSequence);
                        if(firstSetPos==99999) rsContainsuf=true;
                        else{
                            //后缀的首个项集在当前q-seq中最后出现的位置（即q-seq的第几个项集）
                            int lastpos = LOT.get(sid).get(firstSetPos);
                            if(lastpos<column){
                                rsContainsuf = false;
                            }else
                                rsContainsuf = true;
                        }

                        //判断剩余序列是否包含后缀，如果不包含，则当前extension item不能加到ilist
                        if(rsContainsuf) {
                            if (mapiItemTDU.get(ConItem) == null) {
                                mapiItemTDU.put(ConItem, utilitylist.SRU);
                            } else {
                                int tmpSRU = mapiItemTDU.get(ConItem);
                                mapiItemTDU.put(ConItem, utilitylist.SRU + tmpSRU);
                            }
                        }
                    }
                }
            }

            /***********************************以下构建slist********************************/
            //put s-extension items into slist
            //get the items to be s-concatenated
            //构造slist，与构造ilist不同，这时只看t的utilitylist 的首个element就行
            int column = utilitylist.List.get(0).tid;
            //获取首个element（对应t在当前序列的首个instance）的prel
            int prel = utilitylist.get_prel();
            Map<Integer,Integer>temp_mapitemSRU = new HashMap<Integer, Integer>();
            int ItemsetNum = qmatrix.matrixItemUtility[0].length;
            //qmatrix中列号更大，且效用值不为0的项，都可以加入slist
            for (int j = column+1; j < ItemsetNum; j++) {
                for (int i=0;i<ItemNum;i++) {
                    int ConItem = qmatrix.itemNames[i];
                    if (qmatrix.matrixItemUtility[i][j] != 0) {
                        //获取新的前缀长度
                        int newprel = updateprel(ConItem, prel, T, 1);
                        /*****************判断剩余序列是否包含后缀****************/
                        boolean rsContainsuf;
                        //后缀首个项集在T中的位置（即T的第几个项集）
                        int firstSetPos = getFirstSetPos(newprel,targetSequence);
                        if(firstSetPos == 99999) rsContainsuf = true;
                        else{
                            //后缀的首个项集在当前q-seq中最后出现的位置（即q-seq的第几个项集）
                            int lastpos = LOT.get(sid).get(firstSetPos);
                            if(lastpos<j){
                                rsContainsuf = false;
                            }else
                                rsContainsuf = true;
                        }

                        //判断剩余序列是否包含后缀，如果不包含，则当前extension item不能加到slist
                        if(rsContainsuf) {
                            if (temp_mapitemSRU.get(ConItem) == null) {
                                temp_mapitemSRU.put(ConItem, utilitylist.SRU);
                            }
                        }
                    }
                }
            }

            /******************************计算slist中各个item的TDU*****************************/
            //update the global variable mapsItemTDU
            for (Entry<Integer,Integer> entry:temp_mapitemSRU.entrySet()){
                item = entry.getKey();
                if(mapsItemTDU.get(item)==null){
                    mapsItemTDU.put(item,entry.getValue());
                }else{
                    int tmpSRU = mapsItemTDU.get(entry.getKey());
                    mapsItemTDU.put(entry.getKey(), entry.getValue() + tmpSRU);
                }
            }
        }//完成构建prefix的ilist和slist，并得到了它们的TDU

        // for temporarily storing item and its SRU
        //存放i-或s-extension后形成的t'的信息（效用、SRU、utilitychain）
        ItemConcatnation ItemCom;

        /************************************  I-CONCATENATIONS  ***********************************/
        // We first try to perform I-Concatenations to grow the pattern larger.
        // be concatenated to the prefix.
        for (Entry<Integer,Integer> entry:mapiItemTDU.entrySet()){
            int tdu = entry.getValue();
            int item = entry.getKey();

            //construct the candidate after concatenating
            prefix[prefixLength]=item;

            /*******************根据TDU进行宽度剪枝**********************/
            if (tdu<minUtility){
                //mapiItemTDU.remove(item);
                continue;
            }

            //候选数++
            NumOfCandidate++;

            //call the function to construct UtilityChain of the candidate
            /*************************构建新候选模式的utilitychain****************************/
            //构造t'的utilitychain
            if (itemCount+1<=maxPatternLength){
                ItemCom = constructUtilityChain(prefix,prefixLength+1,database,utilitychain,0,T,LOT);

                /********************如果新候选模式的效用高于阈值且包含目标序列，就输出该模式***********************/
                if(ItemCom.utility >= minUtility && ItemCom.UChain.get(0).prel >= T.length - 2){
                    //写入文件
                    //writeOut(ItemCom.candidate,ItemCom.candidateLength,ItemCom.utility);
                    patternCount++;
                    //输出挖掘结果
                    //System.out.println("[ "+ToString(ItemCom.candidate, ItemCom.candidateLength)+"]"+"    Utility: "+ItemCom.utility);
                }
                // SRU pruning strategy
                /******************************根据SRU做深度剪枝**********************************/
                if (ItemCom.SRU>=minUtility){
                    //Mine the database recursively
                    TUSQ(ItemCom.candidate, ItemCom.candidateLength, database, ItemCom.UChain, itemCount+1,T,LOT);
                    }
            }
        }

        /************************************  S-CONCATENATIONS  ***********************************/
        // We first try to perform S-Concatenations to grow the pattern larger.
        // be concatenated to the prefix.
        for (Entry<Integer,Integer> entry:mapsItemTDU.entrySet()){
            int tdu = entry.getValue();
            int item = entry.getKey();

            //s-extension后的序列
            prefix[prefixLength]=-1;
            prefix[prefixLength+1]=item;
            //根据TDU做宽度剪枝
            if (tdu<minUtility){
                continue;
            }

            //候选数++
            NumOfCandidate++;

            /*************************构建新候选模式的utilitychain****************************/
            //call the function to construct UtilityChain of the candidate
            if (itemCount+1<=maxPatternLength){
                ItemCom = constructUtilityChain(prefix,prefixLength+2,database,utilitychain,1,T,LOT);
                //put all sequences in ilist and slist into clist;
                /*ItemConList.add(ItemCom);*/
                /********************如果新候选模式的效用高于阈值且包含目标序列，就输出该模式**********************/
                //如果t'的效用高于阈值并且包含目标序列，就输出该t'
                if(ItemCom.utility>=minUtility && ItemCom.UChain.get(0).prel >= T.length - 2){
                    //写入文件
                    //writeOut(ItemCom.candidate,ItemCom.candidateLength,ItemCom.utility);
                    patternCount++;
                    //输出挖掘结果
                    //System.out.println("[ "+ToString(ItemCom.candidate, ItemCom.candidateLength)+"]"+"    Utility: "+ItemCom.utility);
                }
                // SRU pruning strategy
                /******************************根据SRU做深度剪枝**********************************/
                if (ItemCom.SRU>=minUtility){
                    //Mine the database recursively
                    TUSQ(ItemCom.candidate, ItemCom.candidateLength, database, ItemCom.UChain, itemCount+1,T,LOT);
                    }
            }
        }

        // We check the memory usage
        MemoryLogger.getInstance().checkMemory();
    }


    /**
     * update prel value
     * @param exitem extension item
     * @param prel current prel value
     * @param T target sequence
     * @param kind 0:i-Concatenation，1:s-Concatenation
     * @return new prel value
     */
    public int updateprel(int exitem, int prel, int[] T, int kind){
        //如果prel已经等于T长度，直接返回prel
        if(prel == T.length) return prel;
        //如果T后缀首个元素是-2，返回prel+1
        else if(T[prel]==-2) return  prel+1;
        //以下分i-extension 和s-extension 两种情况
        else{
            //i-extension
            if(kind==0){
                //T后缀首个元素是-1，则暂不处理，直到做s-extension时再处理
                if(T[prel]==-1) return prel;
                else {
                    //extension item比T后缀首个元素大，则prel要回退到T当前项集的首个元素位置，
                    // 这是因为t的当前项集及其i扩展不可能包含T当前项集了
                    if (T[prel] < exitem) {
                        while (prel > 0 && T[prel - 1] != -1) {
                            prel--;
                        }
                        return prel;
                    }
                    //扩展item比T后缀首个元素小，返回prel
                    else if (T[prel] > exitem) return prel;
                    //扩展item等于T后缀首个元素，返回prel+1
                    else return prel + 1;
                }
            }
            //s-extension
            else{
                //T当前项集结束，prel++，前进到下一个项集，然后按i-extension方法判断
                if(T[prel]==-1){
                    prel++;
                    return updateprel(exitem, prel, T, 0);
                }
                //如果T当前项集未结束，但t做s-extension，则prel要回退到T当前项集的首个元素位置
                else{
                    while(prel>0&&T[prel-1]!=-1) { prel--; }
                    return updateprel(exitem, prel, T ,0);
                }
            }
        }
    }

    /**
     * 求后缀的首个项集是或者属于T的第几个项集
     * @param prel current prel value
     * @param T target sequence
     * @return 后缀的首个项集在T中的位置，后缀为空时返回一个特定的数99999
     */
    public int getFirstSetPos(int prel, int []T){
        //前缀长度达到T.length-2，说明前缀已经包含整个T（因为T最后两个元素是-1，-2）
        if(prel>=T.length-2) return 99999;
        else{
            int curitemset = 0;
            for(int i=0;i<=prel;i++){
                if(T[i]==-1)curitemset++;
            }
            return curitemset;
        }
    }

    /**
     * judge if sequence A contains sequence B
     * @param seqA sequence A
     * @param lenofA length of A
     * @param seqB sequence B
     * @return ture: A contains B; false: A does't contain B.
     */
    public boolean seqContain(int[] seqA, int lenofA, int[] seqB) {
        //将seqA和seqB转化为二维list
        List<List> A = new ArrayList<List>();
        List<List> B = new ArrayList<List>();
        List templist = new ArrayList();
        //将seqA放到A
        for (int i = 0; i < lenofA; i++) {
            if (seqA[i] == -1) {
                A.add(templist);
                templist = new ArrayList();
            } else {
                templist.add(seqA[i]);
            }
        }
        A.add(templist);
        templist = new ArrayList();
        //将seqB放到B
        int i = 0;
        while (seqB[i] != -2) {
            if (seqB[i] == -1) {
                B.add(templist);
                templist = new ArrayList();
                i++;
            } else {
                templist.add(seqB[i]);
                i++;
            }
        }
        //判断A是否包含B
        int j = 0, k = 0;
        while (j < B.size() && k < A.size()) {
            //System.out.println("A["+k+"]="+A.get(k).toString());
            //System.out.println("B["+j+"]="+B.get(j).toString());
            if (A.get(k).containsAll(B.get(j))) {
                j++;
                k++;
            } else {
                k++;
            }
        }
        if (j >= B.size()) {
            return true;
        } else return false;
    }

    /**
     * transfer array A to a list
     * @param T target sequence
     * @return list T.
     */
    public ArrayList<ArrayList> convertT(int[] T){
        ArrayList<ArrayList> B = new ArrayList<ArrayList>();
        int i = 0;
        ArrayList templist = new ArrayList();
        while (T[i] != -2) {
            if (T[i] == -1) {
                B.add(templist);
                templist = new ArrayList();
                i++;
            } else {
                templist.add(T[i]);
                i++;
            }
        }
        return B;
    }


    /**
     * fill the LOT
     * @param lot an empty table
     * @param T target sequence
     * @param itembuffer items in a sequence
     * @param bufferlength length of itembuffer
     */
    public void fillLOT(List<ArrayList<Integer>> lot, List<ArrayList>T,  int[]itembuffer, int bufferlength){
        //记录T各项集当前最后出现位置
        int[] lastpos = new int[T.size()];
        //初始化为-1
        for(int i=0;i<lastpos.length;i++){
            lastpos[i]=-1;
        }

        //把itembuffer转为list形式，方便操作
        ArrayList<ArrayList> qseq = new ArrayList<ArrayList>();
        ArrayList templist = new ArrayList();
        //将seqA放到A
        for (int i = 0; i < bufferlength; i++) {
            if (itembuffer[i] == -1) {
                qseq.add(templist);
                templist = new ArrayList();
            } else {
                templist.add(itembuffer[i]);
            }
        }
        qseq.add(templist);

        int j = T.size()-1; //从T最后一个项集开始
        //从qseq最后一个项集开始
        for(int i=qseq.size()-1; i>=0; i--){
            if(j<0) break;
            else {
                //如果qseq[i]包含T[j]，则记录位置
                if(qseq.get(i).containsAll(T.get(j))){
                    lastpos[j]=i;
                    j--;
                }
            }
        }

        templist = new ArrayList();
        //填LOT
        for(int i=0;i<lastpos.length;i++) {
            templist.add(lastpos[i]);
        }
        lot.add(templist);
    }


    /**
     * Set the maximum pattern length
     * @param maxPatternLength the maximum pattern length
     */
    public void setMaxPatternLength(int maxPatternLength) {
        this.maxPatternLength = maxPatternLength;
    }

    /**
     * Set the target sequence
     * @param targetSequence target sequence
     */
    public void setTargetsequence(int []targetSequence){
        this.targetSequence = targetSequence;}

    /**
     * Method to write a high utility itemset to the output file.
     * @param prefix prefix to be written o the output file
     * @param utility the utility of the prefix concatenated with the item
     * @param prefixLength the prefix length
     */
    private void writeOut(int[] prefix, int prefixLength, int utility) throws IOException {
        // increase the number of high utility itemsets found
        //patternCount++;

        StringBuilder buffer = new StringBuilder();

        // If the user wants to save in SPMF format
        if(SAVE_RESULT_EASIER_TO_READ_FORMAT == false) {
            // append each item of the pattern
            for (int i = 0; i < prefixLength; i++) {
                buffer.append(prefix[i]);
                buffer.append(' ');
            }

            // append the end of itemset symbol (-1) and end of sequence symbol (-2)
            buffer.append("-1 #UTIL: ");
            // append the utility of the pattern
            buffer.append(utility);
        }
        else {
            // Otherwise, if the user wants to save in a format that is easier to read for debugging.
            // Append each item of the pattern
            buffer.append('<');
            buffer.append('(');
            for (int i = 0; i < prefixLength; i++) {
                if(prefix[i] == -1) {
                    buffer.append(")(");
                }else {
                    buffer.append(prefix[i]);
                }
            }
            buffer.append(")>:");
            buffer.append(utility);
        }

        // write the pattern to the output file
        writer.write(buffer.toString());
        writer.newLine();

        // if in debugging mode, then also print the pattern to the console
        if(DEBUG==6) {
            System.out.println(" SAVING : " + buffer.toString());
            System.out.println();

            // check if the calculated utility is correct by reading the file
            // for debugging purpose
            checkIfUtilityOfPatternIsCorrect(prefix, prefixLength, utility);
        }
    }

    /**
     * This method check if the utility of a pattern has been correctly calculated for
     * debugging purposes. It is not designed to be efficient since it is just used for
     * debugging.
     * @param prefix a pattern stored in a buffer
     * @param prefixLength the pattern length
     * @param utility the utility of the pattern
     * @throws IOException if error while writting to file
     */
    private void checkIfUtilityOfPatternIsCorrect(int[] prefix, int prefixLength, int utility) throws IOException {
        int calculatedUtility = 0;

        BufferedReader myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));
        // we will read the database
        try {
            // prepare the object for reading the file

            String thisLine;
            // for each line (transaction) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a kind of metadata
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }

                // split the sequence according to the " " separator
                String tokens[] = thisLine.split(" ");

                int tokensLength = tokens.length -3;

                int[] sequence = new int[tokensLength];
                int[] sequenceUtility = new int[tokensLength];

                // Copy the current sequence in the sequence buffer.
                // For each token on the line except the last three tokens
                // (the -1 -2 and sequence utility).
                for(int i=0; i< tokensLength; i++) {
                    String currentToken = tokens[i];

                    // if empty, continue to next token
                    if(currentToken.length() == 0) {
                        continue;
                    }

                    // read the current item
                    int item;
                    int itemUtility;

                    // if the current token is -1
                    if(currentToken.equals("-1")) {
                        item = -1;
                        itemUtility = 0;
                    }else {
                        // if  the current token is an item
                        //  We will extract the item from the string:
                        int positionLeftBracketString = currentToken.indexOf('[');
                        int positionRightBracketString = currentToken.indexOf(']');
                        String itemString = currentToken.substring(0, positionLeftBracketString);
                        item = Integer.parseInt(itemString);

                        // We also extract the utility from the string:
                        String utilityString = currentToken.substring(positionLeftBracketString+1, positionRightBracketString);
                        itemUtility = Integer.parseInt(utilityString);
                    }
                    sequence[i] = item;
                    sequenceUtility[i] = itemUtility;
                }

                // For each position of the sequence
                int util = tryToMatch(sequence,sequenceUtility, prefix, prefixLength, 0, 0, 0);
                calculatedUtility += util;
            }
        } catch (Exception e) {
            // catches exception if error while reading the input file
            e.printStackTrace();
        }finally {
            if(myInput != null){
                // close the input file
                myInput.close();
            }
        }

        if(calculatedUtility != utility) {
            System.out.print(" ERROR, WRONG UTILITY FOR PATTERN : ");
            for(int i=0; i<prefixLength; i++) {
                System.out.print(prefix[i]);
            }
            System.out.println(" utility is: " + utility + " but should be: " + calculatedUtility);
            System.in.read();
        }
    }

    static String arrTostr(int[] seq){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0;i<seq.length; i++){
            stringBuilder.append(seq[i]+" ");
        }
        return stringBuilder.toString();
    }

    /**
     * This is some code for verifying that the utility of a pattern is correctly calculated
     * for debugging only. It is not efficient. But it is a mean to verify that
     * the result is correct.
     * @param sequence a sequence (the items and -1)
     * @param sequenceUtility a sequence (the utility values and -1)
     * @param prefix the current pattern stored in a buffer
     * @param prefixLength the current pattern length
     * @param prefixPos the position in the current pattern that we will try to match with the sequence
     * @param seqPos the position in the sequence that we will try to match with the pattenr
     * @param utility the calculated utility until now
     * @return the utility of the pattern
     */
    private int tryToMatch(int[] sequence, int[] sequenceUtility, int[] prefix,	int prefixLength,
                           int prefixPos, int seqPos, int utility) {

        // Note: I do not put much comment in this method because it is just
        // used for debugging.

        List<Integer> otherUtilityValues = new ArrayList<Integer>();

        // try to match the current itemset of prefix
        int posP = prefixPos;
        int posS = seqPos;

        int previousPrefixPos = prefixPos;
        int itemsetUtility = 0;
        while(posP < prefixLength & posS < sequence.length) {
            if(prefix[posP] == -1 && sequence[posS] == -1) {
                posS++;

                // try to skip the itemset in prefix
                int otherUtility = tryToMatch(sequence, sequenceUtility, prefix, prefixLength, previousPrefixPos, posS, utility);
                otherUtilityValues.add(otherUtility);

                posP++;
                utility += itemsetUtility;
                itemsetUtility = 0;
                previousPrefixPos = posP;
            }else if(prefix[posP] == -1) {
                // move to next itemset of sequence
                while(posS < sequence.length && sequence[posS] != -1){
                    posS++;
                }

                // try to skip the itemset in prefix
                int otherUtility = tryToMatch(sequence, sequenceUtility, prefix, prefixLength, previousPrefixPos, posS, utility);
                otherUtilityValues.add(otherUtility);

                utility += itemsetUtility;
                itemsetUtility = 0;
                previousPrefixPos = posP;

            }else if(sequence[posS] == -1) {
                posP = previousPrefixPos;
                itemsetUtility = 0;
                posS++;
            }else if(prefix[posP] == sequence[posS]) {
                posP++;
                itemsetUtility += sequenceUtility[posS];
                posS++;
                if(posP == prefixLength) {

                    // try to skip the itemset in prefix
                    // move to next itemset of sequence
                    while(posS < sequence.length && sequence[posS] != -1){
                        posS++;
                    }
                    int otherUtility = tryToMatch(sequence, sequenceUtility, prefix, prefixLength, previousPrefixPos, posS, utility);
                    otherUtilityValues.add(otherUtility);


                    utility += itemsetUtility;
                }
            }else if(prefix[posP] != sequence[posS]) {
                posS++;
            }
        }

        int max = 0;
        if(posP == prefixLength) {
            max = utility;
        }
        for(int utilValue : otherUtilityValues) {
            if(utilValue > utility) {
                max = utilValue;
            }
        }
        return max;
    }

    /**
     * Print statistics about the latest execution to System.out.
     */
    public void printStatistics() {
        System.out.println("============= ALGOTUSQ v1.0 - STATS =============");
        System.out.println(" Target sequence: " + arrTostr(targetSequence));
        System.out.println(" Threshold:" + this.minUtility);
        System.out.println(" Total time ~ " + (endTimestamp - startTimestamp)/1000 + " s");
        System.out.println(" Max Memory ~ " + MemoryLogger.getInstance().getMaxMemory() + " MB");
        System.out.println(" High-utility sequential pattern count : " + patternCount);
        System.out.println(" Number Of Candidate : " + NumOfCandidate);
        //System.out.println(" Conruntime : " + conruntime/1000 +" s");
        System.out.println("========================================================"+" \n");
    }
}

