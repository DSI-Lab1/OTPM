package TaRP;

import java.io.*;
import java.util.*;




public class THURIMAlgo {
    /** the time at which the algorithm started */
    public long startTimestamp = 0;
    /** the time at which the algorithm ended */
    public long endTimestamp = 0;

    public int thuriCount = 0;

    /** Map to remember the TWU of each item */
    Map<Integer, Long> mapItemToTWU;
	
	/** Map to store the sup of each item */
    Map<Integer, Long> mapItemToSup;
    
	/** The EUSCS structure:  key: item   key: another item   value: [twu, sup] */
	Map<Integer, Map<Integer, long[]>> EUSCS;

	private int strategy_TWU;
	private int strategy_EUSCS;
	private int strategy_LA;


    /**the max length of tarPattern */
    int maxTarLength = 0;

    /**the length of tarPattern */
    int tarPatternCount = 0;

    /** writer to write the output file  */
    BufferedWriter writer = null;

    /** the number of utility-list that was constructed */
    private long joinCount;

    /** buffer for storing the current itemset that is mined when performing mining
     * the idea is to always reuse the same buffer to reduce memory usage. */
    final int BUFFERS_SIZE = 200;
    private int[] itemsetBuffer = null;


    public boolean DEBUG = true;
    /** this class represent an item and its utility in a transaction */
    class Pair{
        int item = 0;
        int utility = 0;
    }

    /**
     * Default constructor
     */
    public THURIMAlgo() {
    }

    /**
     * Run the algorithm
     *
     * @param input the input file path
     * @param output the output file path
     * @param minUtility the minimum utility threshold
     * @throws IOException exception if error while writing the file
     */
    public void THURIM(String input, String output, int minUtility, int minSup, int maxSup, Integer[] tarPattern, Boolean EUSCSFlag) throws IOException {

        MemoryLogger.getInstance().reset();

        // initialize the buffer for storing the current itemset
        itemsetBuffer = new int[BUFFERS_SIZE];

        startTimestamp = System.currentTimeMillis();

        writer = new BufferedWriter(new FileWriter(output));
        
        if (EUSCSFlag) EUSCS =  new HashMap<Integer, Map<Integer, long[]>>();

        //  We create a  map to store the TWU of each item
        mapItemToTWU = new HashMap<Integer, Long>();
        mapItemToSup = new HashMap<Integer, Long>();

        tarPatternCount = tarPattern.length;

        maxTarLength = 0;

        // We scan the database a first time to calculate the TWU of each item.
        BufferedReader myInput = null;
        String thisLine;
        
        // structure to store the horizontal database
        // structure to store useless TID
        ArrayList<Integer> uselessTID= new ArrayList<Integer>();  
        try {
            // prepare the object for reading the file
            myInput = new BufferedReader(new InputStreamReader( new FileInputStream(new File(input))));
            int tid = 0;
            // for each line (transaction) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a
                // kind of metadata
                if (thisLine.isEmpty() == true ||
                        thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }
                boolean hasTargetPattern = false;

                // split the transaction according to the : separator
                String split[] = thisLine.split(":");
                // the first part is the list of items
                String items[] = split[0].split(" ");
                // the second part is the transaction utility
                int transactionUtility = Integer.parseInt(split[1]);
                
                HashSet<Integer> transactionSet = new HashSet<Integer>();
                // for each item, we add the transaction utility to its TWU
                for(int i = 0; i < items.length; i++){
                    // convert item to integer
                    Integer item = Integer.parseInt(items[i]);
                    transactionSet.add(item);
                    // get the current TWU of that item
                    Long twu = mapItemToTWU.get(item);
                    Long sup = mapItemToSup.get(item);
                    // add the utility of the item in the current transaction to its twu
                    twu = (twu == null)? transactionUtility : twu + transactionUtility;
                    mapItemToTWU.put(item, twu);   
					sup = (sup == null)? 1 : sup + 1;
                    mapItemToSup.put(item, sup); 
                }
                
                if (transactionSet.size() >= tarPatternCount) {
                	hasTargetPattern = isSubset(transactionSet, tarPattern);
                }
 
                if (!hasTargetPattern) {
                	uselessTID.add(tid);
				}
                tid++;
            }
        } catch (Exception e) {
            // catches exception if error while reading the input file
            e.printStackTrace();
        }finally {
            if(myInput != null){
                myInput.close();
            }
        }

        // CREATE A LIST TO STORE THE UTILITY LIST OF ITEMS WITH TWU  >= MIN_UTILITY.
        List<UtilityList> listOfUtilityLists = new ArrayList<UtilityList>();
        // CREATE A MAP TO STORE THE UTILITY LIST FOR EACH ITEM.
        // Key : item    Value :  utility list associated to that item
        Map<Integer, UtilityList> mapItemToUtilityList = new HashMap<Integer, UtilityList>();


        // For each item
        for(Integer item: mapItemToTWU.keySet()){
            // if the item is promising  (TWU >= minutility)
            // System.out.println(item + " TWU: " + mapItemToTWU.get(item));
            if(mapItemToTWU.get(item) >= minUtility && mapItemToSup.get(item) >= minSup){
                //System.out.print("[" + item + " " + mapItemToTWU.get(item) + "]" + " ");
                //System.out.println(item + " : " + mapItemToTWU.get(item));
                // create an empty Utility List that we will fill later.
                UtilityList uList = new UtilityList(item);
                mapItemToUtilityList.put(item, uList);
                // add the item to the list of high TWU items
                listOfUtilityLists.add(uList);

            }else {
				strategy_TWU++;
			}
        }

        // SORT THE LIST OF HIGH TWU ITEMS IN ASCENDING ORDER
        for(Integer item : tarPattern){
            if(mapItemToTWU.get(item) < minUtility) return;
        }

        Collections.sort(listOfUtilityLists, new Comparator<UtilityList>(){
            public int compare(UtilityList o1, UtilityList o2) {
                // compare the TWU of the items
                return compareItems(o1.item, o2.item);
            }
        } );

        Arrays.sort(tarPattern, new Comparator<Integer>(){
            public int compare(Integer o1, Integer o2){
                return compareItems(o1, o2);
            }
        });
        if(DEBUG){
            System.out.println(Arrays.toString(tarPattern));
        }
        
        // SECOND DATABASE PASS TO CONSTRUCT THE UTILITY LISTS
        // OF 1-ITEMSETS  HAVING TWU >= minutil (promising items)
        try {
            // prepare object for reading the file
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            // variable to count the number of transaction
            int tid =0;
            // for each line (transaction) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is  a comment, is  empty or is a
                // kind of metadata
                if (thisLine.isEmpty() == true ||
                        thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }
                if (uselessTID.contains(tid)) {
                	tid++;
                	continue;
                }

                // split the line according to the separator
                String split[] = thisLine.split(":");
                // get the list of items
                String items[] = split[0].split(" ");
                // get the list of utility values corresponding to each item
                // for that transaction
				String utilityValues[] = split[2].split(" ");

                // Copy the transaction into lists but
                // without items with TWU < minutility

                int remainingUtility =0;
                long newTWU = 0;

                // Create a list to store items
                List<Pair> revisedTransaction = new ArrayList<Pair>();
                // for each item
                for(int i = 0; i < items.length; i++){
                    /// convert values to integers
                    Pair pair = new Pair();
                    pair.item = Integer.parseInt(items[i]);
                    pair.utility = Integer.parseInt(utilityValues[i]);

                    // if the item has enough utility
                    if(mapItemToTWU.get(pair.item) >= minUtility && mapItemToSup.get(pair.item) >= minSup){
                        // add it
                        revisedTransaction.add(pair);
                        remainingUtility += pair.utility;
                        newTWU += pair.utility;
                    }
                }

                Collections.sort(revisedTransaction, new Comparator<Pair>(){
                    public int compare(Pair o1, Pair o2) {
                        return compareItems(o1.item, o2.item);
                    }});


                // for each item left in the transaction
                for(int i = 0; i < revisedTransaction.size(); i++){
                	Pair pair = revisedTransaction.get(i);
                	
                    // subtract the utility of this item from the remaining utility
                    remainingUtility = remainingUtility - pair.utility;

                    // get the utility list of this item
                    UtilityList utilityListOfItem = mapItemToUtilityList.get(pair.item);

                    // Add a new Element to the utility list of this item corresponding to this transaction
                    Element element = new Element(tid, pair.utility, remainingUtility);

                    utilityListOfItem.addElement(element);
                    
                    // BEGIN OPTIMIZATION: construct the EUSCS
                    if (EUSCSFlag) {
                        if (remainingUtility != 0) {
        					Map<Integer, long[]> EUSCSItem = EUSCS.get(pair.item);
        					if(EUSCSItem == null) {
        						EUSCSItem = new HashMap<Integer, long[]>();
        						EUSCS.put(pair.item, EUSCSItem);
        					}
        					
        					for(int j = i+1; j < revisedTransaction.size(); j++){
        						Pair pairAfter = revisedTransaction.get(j);
        						long[] TWUSupArr = EUSCSItem.get(pairAfter.item);
//        						Long twuSum = EUSCSItem.get(pairAfter.item).get(0);
        						if(TWUSupArr == null) {
        							long[] newArr = new long[2];
        							newArr[0] = newTWU;
        							newArr[1] = 1;
        							EUSCSItem.put(pairAfter.item, newArr);
        						}else {
        							TWUSupArr[0] += newTWU;
        							TWUSupArr[1] += 1;
        							EUSCSItem.put(pairAfter.item, TWUSupArr);
        						}
        					}
                        }
                    }

					// END OPTIMIZATION 
                }
                tid++; // increase tid number for next transaction
            }
        } catch (Exception e) {
            // to catch error while reading the input file
            e.printStackTrace();
        }finally {
            if(myInput != null){
                myInput.close();
            }
        }

        long EUSCSTimeStamp = System.currentTimeMillis();

        // check the memory usage
        MemoryLogger.getInstance().checkMemory();

        // Mine the database recursively
        THURIM_miner(itemsetBuffer, 0, null, listOfUtilityLists, minUtility, minSup, maxSup, 0, tarPattern, EUSCSFlag);

        // check the memory usage again and close the file.
        MemoryLogger.getInstance().checkMemory();
        // close output file
        writer.close();
        // record end time
        endTimestamp = System.currentTimeMillis();
    }

    private int compareItems(int item1, int item2) {
        long a = mapItemToTWU.get(item1);
        long b = mapItemToTWU.get(item2);
        //int compare = mapItemToTWU.get(item1) - mapItemToTWU.get(item2);
        int compare = a == b ? 0 : a > b ? 1 : -1;
        // if the same, use the lexical order otherwise use the TWU
        return (compare == 0)? item1 - item2 :  compare;
    }

    /**
     * This is the recursive method to find all high utility itemsets. It writes
     * the itemsets to the output file.
     *
     * @param prefix  This is the current prefix. Initially, it is empty.
     * @param pUL This is the Utility List of the prefix. Initially, it is empty.
     * @param ULs The utility lists corresponding to each extension of the prefix.
     * @param minUtility The minUtility threshold.
     * @param prefixLength The current prefix length	
     * @throws IOException
     */
    private void THURIM_miner(int [] prefix, int prefixLength, UtilityList pUL, List<UtilityList> ULs, int minUtility, int minSup, int maxSup, int index, Integer[] tarPattern, Boolean EUSCSFlag)
            throws IOException {
        // For each extension X of prefix P
        if( ULs == null ) return;
        boolean match = false;
        for(int i=0; i < ULs.size(); i++){
            if (match)
			{
				break;
			}
            // make sure update subCount every time
            UtilityList X = ULs.get(i);           

            int currentindex = index;

            if (currentindex < tarPatternCount && X.item.equals(tarPattern[currentindex])) { 
        		currentindex++;
        		match = true;
            }      
  
            // If pX is a high ut ility itemset. 
            // we save the itemset:  pX
            if(X.sumIutils >= minUtility && currentindex >= tarPatternCount){
            	if (X.sumSups >= minSup && X.sumSups < maxSup) {
                    // save to file
                    writeOut(prefix, prefixLength, X.item, X.sumIutils, X.sumSups, currentindex);
            	}
            }

            // If the sum of the remaining utilities for pX
            // is higher than minUtility, we explore extensions of pX.
            // (this is the pruning condition)
            // ============================== //

            if(X.sumIutils + X.sumRutils >= minUtility && X.sumSups >= minSup){
            	
                // This list will contain the utility lists of pX extensions.
                List<UtilityList> exULs = new ArrayList<UtilityList>();
                // For each extension of p appearing
                // after X according to the ascending order
                for(int j = i + 1; j < ULs.size(); j++){
                    // the all can combine with X,can't only judge one item
                    UtilityList Y = ULs.get(j);
                    
                    if (EUSCSFlag) {
                        Map<Integer, long[]>mapTWUSupArr = EUSCS.get(X.item);
                        if (mapTWUSupArr != null) {
                        	long[] TWUSupCouple = mapTWUSupArr.get(Y.item);
                        	if (TWUSupCouple == null || TWUSupCouple[0] < minUtility || TWUSupCouple[1] < minSup) { 
//                    		if (TWUSupCouple == null || TWUSupCouple[0] < minUtility) { 
                        		strategy_EUSCS++;
                        		continue;
                        	}
                        }
                    }

                    UtilityList temp = construct(pUL, X, Y, minUtility, minSup);
                    if (temp != null) {
                        exULs.add(temp);
                        joinCount++;
                    }
                }
                
                // We create new prefix pX
                //System.out.println("exULs.size : "+ exULs.size());
                itemsetBuffer[prefixLength] = X.item; // itemsetBuffer can make sure the subsume maxlength

                // We make a recursive call to discover all itemsets with the prefix pXY
                //System.out.println("end");
                
                THURIM_miner(itemsetBuffer, prefixLength+1, X, exULs, minUtility, minSup, maxSup, currentindex, tarPattern, EUSCSFlag);
            }
        }
    }

    /**
     * This method constructs the utility list of pXY
     *
     * @param P :  the utility list of prefix P.
     * @param px : the utility list of pX
     * @param py : the utility list of pY
     * @return the utility list of pXY
     */
    private UtilityList construct(UtilityList P, UtilityList px, UtilityList py, int minUtility, int minSup) {

        UtilityList pxyUL = new UtilityList(py.item);
        
		// BEGIN LA-prune
		// Initialize the sum of total utility
		long totalUtility = px.sumIutils + px.sumRutils;
		long totalSupport = px.sumSups;
		// END LA-prune
		
        // for each element in the utility list of pX
        for(Element ex : px.elements){
            // do a binary search to find element ey in py with tid = ex.tid
            Element ey = findElementWithTID(py, ex.tid);
            if(ey == null){
				// BEGIN LA-prune
				totalUtility -= (ex.iutils + ex.rutils);
				totalSupport--;
				if(totalUtility < minUtility || totalSupport < minSup) {
					strategy_LA++;
					return null;
				}
				// END LA-prune
				continue;
            }
            
            // if the prefix p is null
            if(P == null){
                // Create the new element
                Element eXY = new Element(ex.tid, ex.iutils + ey.iutils, ey.rutils);
                // add the new element to the utility list of pXY
                pxyUL.addElement(eXY);

            }else{
                // find the element in the utility list of p wih the same tid
                Element e = findElementWithTID(P, ex.tid);
                if(e != null){
                    // Create new element
                    Element eXY = new Element(ex.tid, ex.iutils + ey.iutils - e.iutils,
                            ey.rutils);
                    // add the new element to the utility list of pXY
                    pxyUL.addElement(eXY);
                }
            }
        }
        // return the utility list of pXY.
        return pxyUL;
    }

    /**
     * Do a binary search to find the element with a given tid in a utility list
     *
     * @param ulist the utility list
     * @param tid  the tid
     * @return  the element or null if none has the tid.
     */
    private Element findElementWithTID(UtilityList ulist, int tid){
        List<Element> list = ulist.elements;

        // perform a binary search to check if  the subset appears in  level k-1.
        int first = 0;
        int last = list.size() - 1;

        // the binary search
        while( first <= last )
        {
            int middle = ( first + last ) >>> 1; // divide by 2

            if(list.get(middle).tid < tid){
                first = middle + 1;
                //  the itemset compared is larger than the subset according to the lexical order
            }
            else if(list.get(middle).tid > tid){
                last = middle - 1;
                //  the itemset compared is smaller than the subset  is smaller according to the lexical order
            }
            else{
                return list.get(middle);
            }
        }
        return null;
    }

    /**
     * Method to write a high utility itemset to the output file.
     *
     * @param prefix: the prefix to be writent o the output file
     * @param item: an item to be appended to the prefix
     * @param utility: the utility of the prefix concatenated with the item
     * @param prefixLength: the prefix length
     */
    private void writeOut(int[] prefix, int prefixLength, int item, long utility, long sup, int index) throws IOException {
        thuriCount++; // increase the number of high utility itemsets found

        //Create a string buffer
        StringBuilder buffer = new StringBuilder();
        // append the prefix
        for (int i = 0; i < prefixLength; i++) {
            buffer.append(prefix[i]);
            buffer.append(' ');
        }
        // append the last item
        buffer.append(item);
        // append the utility value
        buffer.append(" #UTIL: ");
        buffer.append(utility);
		buffer.append(" #SUP: ");
		buffer.append(sup);
        // write to file

        buffer.append(" #Target: ");
        buffer.append(index);

        writer.write(buffer.toString());
        writer.newLine();
    }

    
    
    boolean isSubset(HashSet<Integer> transactionSet, Integer tarParrtern[]) 
    {         
        // loop to check if all elements of tarParrtern also 
        // lies in transactionSet 
        for(int i = 0; i < tarParrtern.length; i++) 
        { 
            if(!transactionSet.contains(tarParrtern[i])) 
                return false; 
        } 
        return true; 
    } 
    
    /**
     * Print statistics about the latest execution to System.out.
     * pruning for minging Target Pattern
     *
     */
    public void printStats(String input, int minutil, int min_sup, int max_sup, Integer[] tararray) {
        System.out.println("========== THURIM ALGORITHM v1.0 - STATS  ==========");
        System.out.println(" Input file: " + input.toString());
        System.out.println(" minimum utility: " + minutil);
        System.out.println(" minimum support: " + min_sup);
        System.out.println(" maximum support: " + max_sup);
        System.out.println(" Target pattern: " + Arrays.toString(tararray));
        System.out.println(" Total time: " + (endTimestamp - startTimestamp)/1000.0 + " s");
        System.out.println(" Maximal memory: " + MemoryLogger.getInstance().getMaxMemory() + " MB");
        System.out.println(" Target HUIs count: " + thuriCount);
        System.out.println(" Join count: " + joinCount);
        System.out.println(" Pruning Strategy TWU: " + strategy_TWU);
        System.out.println(" Pruning Strategy EUSCS: " + strategy_EUSCS);
        System.out.println(" Pruning Strategy LA: " + strategy_LA);
        System.out.println("===================================================");
    }
}
