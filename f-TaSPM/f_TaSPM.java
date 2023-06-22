import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class f_TaSPM {

	// for statistics
	/** start time of last algorithm execution */
	private long startTime;  
	/** end time of last algorithm execution */
	private long endTime;   
	
	/** number of patterns */
    public int patternCount;
    
    /** minsup */
    private int minsup = 0;
    
    /** object to write to a file */
    BufferedWriter writer = null;
    
    /** Vertical database */
    Map<Integer, Bitmap> verticalDB = new HashMap<Integer, Bitmap>();
    
    /** List indicating the number of bits per sequence */
    List<Integer> sequencesSize = null;
    
    /** the last bit position that is used in bitmaps */
    int lastBitIndex = 0;  
    
	/** maximum pattern length in terms of item count */
	private int minimumPatternLength = 0;
    /** maximum pattern length in terms of item count */
    private int maximumPatternLength = 1000;
	   
	/** items that need to appear in patterns found by the algorithm 
	* (or any items if the array is empty) */

    // Obviously, it only can match a sequence 
    // containing mustAppearItems according to alphabetic order 
    // 5, 3 will be wrong, it is because 3 < 5
	int[] mustAppearItems; 
    
    /**Map: key: item   value:  another item that followed the first item + support
    * (could be replaced with a triangular matrix...) */
    Map<Integer, Map<Integer, Integer>> coocMapAfter = null;
    Map<Integer, Map<Integer, Integer>> coocMapEquals = null;
    
    /** Map indicating for each item, the smallest tid containing this item
    * in a sequence. */
    Map<Integer, Short> lastItemPositionMap;
    boolean useCMAPPruning = true;
    boolean useLastPositionPruning = false;
    
	/** the max gap between two itemsets of a pattern. It is an optional parameter that the user can set. */
	private int maxGap = Integer.MAX_VALUE;
	
	/**  if true, sequence ids of each pattern will be shown when they will be output */
	boolean outputSequenceIdentifiers;

    Prefix Query = new Prefix(); 
    
    ArrayList<Integer> ueslessSID = new ArrayList<Integer>(); 
    
	boolean UPIP = true;
	boolean USIP = true;
	boolean UIIP =  true;
	
	Integer lastItem;
	Integer firstItem;
	int finishMatch = -1;
    
	
	List<Bitmap> pruningMap = new ArrayList<>();
	
	private void createPruningMap() {

		int index = 0;
		for (int i = Query.itemsets.size() - 1; i >= 0; i--) {
			index += Query.get(i).size();
		}
		index--;

		// System.out.println(index);
		List<Bitmap> bitmapList = new ArrayList<Bitmap>();
		for (int i = 0; i < Query.itemsets.size(); i++) {
			Itemset itemset = Query.get(i);
			Bitmap bitmap = verticalDB.get(itemset.get(0));

			pruningMap.add(bitmap);
			for (int j = 1; j < itemset.size(); j++) {
				int tem = itemset.get(j);
				Bitmap bp = verticalDB.get(tem);
				pruningMap.add(bp);

				bitmap = bitmap.createNewBitmapIStep(bp, sequencesSize, index);
			}
			bitmapList.add(bitmap);
		}

		// 鏈�鍚庝竴涓」闆嗙殑bitmap
		Bitmap bitmap = new Bitmap(lastBitIndex);
		for (int i = Query.itemsets.size() - 1; i >= 0; i--) {
			// 鑾峰緱褰撳墠椤归泦鐨刡itmap
			Bitmap bp = bitmapList.get(i);
			// System.out.println("椤归泦鐨刡itmap:i "+i+" "+bp.toString());
			if (i == Query.itemsets.size() - 1) {
				// 寰楀埌鏈�鍚庝竴涓綅缃殑bitmap
				// Bitmap tem = getLastItemBitmap(bp);
				for (int j = 0; j < Query.itemsets.get(i).size(); j++) {
					pruningMap.set(index, bp);
					index--;
				}
				bitmap = bp;
			} else {
				Bitmap tem = sstepBitmap(bp, bitmap);
				for (int j = 0; j < Query.itemsets.get(i).size(); j++) {
					pruningMap.set(index, tem);
					index--;
				}
				bitmap = tem;

			}
		}
	}

	private Bitmap sstepBitmap(Bitmap first, Bitmap second) {
		Bitmap bp = new Bitmap(lastBitIndex);
		
		for (int bitK = second.bitmap.previousSetBit(lastBitIndex); bitK >= 0;) {

			int sid = bitToSID(bitK);
			// System.out.println(bitK+" bit sid: "+sid);
			int index = first.bitmap.previousSetBit(bitK - 1);

			for (int newbit = index; newbit >= 0; newbit = first.bitmap.previousSetBit(newbit - 1)) {
				int newsid = bitToSID(newbit);
				if (newsid == sid) {
					bp.bitmap.set(newbit);
				} else {
					break;
				}
			}
			while (true) {
				bitK = second.bitmap.previousSetBit(bitK - 1);
				if (bitK < 0)
					break;
				int newsid = bitToSID(bitK);
				if (newsid != sid)
					break;
			}

			//
		}
		return bp;
	}

	private Bitmap getLastItemBitmap(Bitmap newbitmap) {
		
		Bitmap bp = new Bitmap(lastBitIndex);
		for (int bitK = newbitmap.bitmap.nextSetBit(0); bitK >= 0; bitK = newbitmap.bitmap.nextSetBit(bitK + 1)) {
			// find the sequence (sid) which includes this bit
			int sid = bitToSID(bitK);
			int tem = newbitmap.bitmap.nextSetBit(bitK + 1);
			int sid1 = bitToSID(tem);
			if (sid == sid1)
				continue;
			else {
				bp.bitmap.set(bitK);
			}

		}
		return bp;
	}
    /**
     * Default constructor
     */
    public f_TaSPM() {
    }

    /**
     * Method to run the algorithm
     *
     * @param input path to an input file
     * @param outputFilePath path for writing the output file
     * @param minsupRel the minimum support as a relative value
     * @param outputSequenceIdentifiers  if true, sequence ids will be shown with each output pattern
     * @param query 
     * @throws IOException exception if error while writing the file or reading
     */
    public void runAlgorithm(String input, String outputFilePath, int minsupAbs, boolean outputSequenceIdentifiers, Prefix query) throws IOException {
        this.Query = query;
        this.lastItem = query.getLastItem();
        this.firstItem = query.getFirstItem();
        finishMatch = query.size();
        
    	this.outputSequenceIdentifiers = outputSequenceIdentifiers;
    	
    	Bitmap.INTERSECTION_COUNT = 0;
        // create an object to write the file
        writer = new BufferedWriter(new FileWriter(outputFilePath));
        // initialize the number of patterns found
        patternCount = 0;
        // to log the memory used
        MemoryLogger.getInstance().reset();

        // record start time
        startTime = System.currentTimeMillis();
        // RUN THE ALGORITHM
        spam(input, minsupAbs);
        // record end time
        endTime = System.currentTimeMillis();
        // close the file
        writer.close();
    }

    /**
     * This is the main method for the SPAM algorithm
     *
     * @param an input file
     * @param minsupRel the minimum support as a relative value
     * @throws IOException
     */
    private void spam(String input, int minsupAbs) throws IOException {
        // the structure to store the vertical database
        // key: an item    value : bitmap
        verticalDB = new HashMap<Integer, Bitmap>();

        // structure to store the horizontal database
        List<int[]> inMemoryDB = new ArrayList<int[]>();

        // STEP 0: SCAN THE DATABASE TO STORE THE FIRST BIT POSITION OF EACH SEQUENCE 
        // AND CALCULATE THE TOTAL NUMBER OF BIT FOR EACH BITMAP
        // 0, s1.len, s1.len+s2.len ... 
        sequencesSize = new ArrayList<Integer>();
        lastBitIndex = 0; // variable to record the last bit position that we will use in bitmaps
        try {
            // read the file
            FileInputStream fin = new FileInputStream(new File(input));
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            String thisLine;
            int bitIndex = 0;
            int sid = 0;
            // for each line (sequence) in the file until the end
            while ((thisLine = reader.readLine()) != null) {
                // if the line is  a comment, is  empty or is a
                // kind of metadata
                if (thisLine.isEmpty() == true
                        || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }

                // record the length of the current sequence (for optimizations)
                sequencesSize.add(bitIndex);
                // split the sequence according to spaces into tokens

                String tokens[] = thisLine.split(" ");
                int[] transactionArray = new int[tokens.length];
                
				boolean containsAMustAppearItem = false;
				
				// filter database
				int imatch = 0, iimatch = 0;
				boolean hasQuery = false;
				boolean hasQueryI = false;
				
				
                for (int i = 0; i < tokens.length; i++) {
                    int item = Integer.parseInt(tokens[i]);
                    transactionArray[i] = item;
                    // if it is not an itemset separator
                    if (item == -1) { // indicate the end of an itemset
                        // increase the number of bits that we will need for each bitmap
                        bitIndex++;
                        hasQueryI = false;
                        iimatch = 0;
                    }
                    if(hasQuery == false && hasQueryI == false && item == Query.get(imatch).get(iimatch)) {
                    	iimatch++;
                    	if(iimatch == Query.get(imatch).size()) {
                    		hasQueryI = true;
                    		imatch++;
                    		iimatch = 0;
                    		if(imatch == Query.size()) {
                    			hasQuery = true;
                    		}
                    	}
                    }
                    
					// check if this item must appear in patterns (optional)
					if(itemMustAppearInPatterns(item)) {
						containsAMustAppearItem = true;
					}
                }
				// if this transaction contains at least an item that must appear
				// in the pattern (this feature is optional), we keep the sequence
				// otherwise we don't add it
				if(containsAMustAppearItem && hasQuery) {
					inMemoryDB.add(transactionArray);
				}else {
					ueslessSID.add(sid);
				}
				sid++;
            }
            // record the last bit position for the bitmaps
            lastBitIndex = bitIndex - 1;
            reader.close(); // close the input file
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Calculate the absolute minimum support 
        // by multipling the percentage with the number of
        // sequences in this database
        minsup = minsupAbs;

        // STEP1: SCAN THE DATABASE TO CREATE THE BITMAP VERTICAL DATABASE REPRESENTATION
        try {
            FileInputStream fin = new FileInputStream(new File(input));
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            String thisLine;
            int sid = 0; // to know which sequence we are scanning
            int tid = 0;  // to know which itemset we are scanning

            // for each line (sequence) from the input file
            while ((thisLine = reader.readLine()) != null) {
				// if the line is  a comment, is  empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true ||
						thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
								|| thisLine.charAt(0) == '@') {
					continue;
				}
				if(ueslessSID.contains(sid)) {
					sid++;
					continue;
				}
            	
                // split the sequence according to spaces into tokens
                for (String token : thisLine.split(" ")) {
                    if (token.equals("-1")) { // indicate the end of an itemset
                        tid++;
                    } else if (token.equals("-2")) { // indicate the end of a sequence
                        sid++;
                        tid = 0;
                    } else {  // indicate an item
                        // Get the bitmap for this item. If none, create one.
                        Integer item = Integer.parseInt(token);
                        Bitmap bitmapItem = verticalDB.get(item);
                        if (bitmapItem == null) {
                            bitmapItem = new Bitmap(lastBitIndex);
                            verticalDB.put(item, bitmapItem);
                        }
                        // Register the bit in the bitmap for this item
                        bitmapItem.registerBit(sid, tid, sequencesSize);
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // STEP2: REMOVE INFREQUENT ITEMS FROM THE DATABASE BECAUSE THEY WILL NOT APPEAR IN ANY FREQUENT SEQUENTIAL PATTERNS
        List<Integer> frequentItems = new ArrayList<Integer>();
        Iterator<Entry<Integer, Bitmap>> iter = verticalDB.entrySet().iterator();
        // we iterate over items from the vertical database that we have in memory
        while (iter.hasNext()) {
            //  we get the bitmap for this item
            Map.Entry<Integer, Bitmap> entry = (Map.Entry<Integer, Bitmap>) iter.next();
            // if the cardinality of this bitmap is lower than minsup
            if (entry.getValue().getSupport() < minsup) {
                // we remove this item from the database.
                iter.remove();
            } else {
                // otherwise, we save this item as a frequent
                // sequential pattern of size 1
            	if(minimumPatternLength <= 1 && maximumPatternLength >=1) {
            		if(Query.size() == 0 || (Query.size() == 1 && Query.get(0).size() == 1 && Query.get(0).get(0).equals(entry.getKey()))) {
            			savePattern(entry.getKey(), entry.getValue());
            		}
            		
            	}
                // and we add this item to a list of frequent items
                // that we will use later.
                frequentItems.add(entry.getKey());
            }
        }
        
        if (maximumPatternLength <= 1) {
            return;
        }

        // STEP 3.1  CREATE CMAP
        coocMapEquals = new HashMap<Integer, Map<Integer, Integer>>(frequentItems.size());
        coocMapAfter = new HashMap<Integer, Map<Integer, Integer>>(frequentItems.size());

        if (useLastPositionPruning) {
            lastItemPositionMap = new HashMap<Integer, Short>(frequentItems.size());
        }
        for (int[] transaction : inMemoryDB) {
            short itemsetCount = 0;

            Set<Integer> alreadyProcessed = new HashSet<Integer>();
            Map<Integer, Set<Integer>> equalProcessed = new HashMap<>();
            loopI:
            for (int i = 0; i < transaction.length; i++) {
                Integer itemI = transaction[i];

                Set equalSet = equalProcessed.get(itemI);
                if (equalSet == null) {
                    equalSet = new HashSet();
                    equalProcessed.put(itemI, equalSet);
                }

                if (itemI < 0) {
                    itemsetCount++;
                    continue;
                }

                // update lastItemMap
                if (useLastPositionPruning) {
                    Short last = lastItemPositionMap.get(itemI);
                    if (last == null || last < itemsetCount) {
                        lastItemPositionMap.put(itemI, itemsetCount);
                    }
                }

                Bitmap bitmapOfItem = verticalDB.get(itemI);
                if (bitmapOfItem == null || bitmapOfItem.getSupport() < minsup) {
                    continue;
                }

                Set<Integer> alreadyProcessedB = new HashSet<Integer>(); // NEW

                boolean sameItemset = true;
                for (int j = i + 1; j < transaction.length; j++) {
                    Integer itemJ = transaction[j];

                    if (itemJ < 0) {
                        sameItemset = false;
                        continue;
                    }

                    Bitmap bitmapOfitemJ = verticalDB.get(itemJ);
                    if (bitmapOfitemJ == null || bitmapOfitemJ.getSupport() < minsup) {
                        continue;
                    }
                    Map<Integer, Integer> map = null;
                    if (sameItemset) {
                        if (!equalSet.contains(itemJ)) {
                            map = coocMapEquals.get(itemI);
                            if (map == null) {
                                map = new HashMap<Integer, Integer>();
                                coocMapEquals.put(itemI, map);
                            }
                            Integer support = map.get(itemJ);
                            if (support == null) {
                                map.put(itemJ, 1);
                            } else {
                                map.put(itemJ, ++support);
                            }
                            equalSet.add(itemJ);
                        }
                    } else if (!alreadyProcessedB.contains(itemJ)) {
                        if (alreadyProcessed.contains(itemI)) {
                            continue loopI;
                        }
                        map = coocMapAfter.get(itemI);
                        if (map == null) {
                            map = new HashMap<Integer, Integer>();
                            coocMapAfter.put(itemI, map);
                        }
                        Integer support = map.get(itemJ);
                        if (support == null) {
                            map.put(itemJ, 1);
                        } else {
                            map.put(itemJ, ++support);
                        }
                        alreadyProcessedB.add(itemJ); // NEW
                    }
                }
                alreadyProcessed.add(itemI);
            }
        }
      
        //create the pruning map
        createPruningMap();
        // STEP3: WE PERFORM THE RECURSIVE DEPTH FIRST SEARCH
        // to find longer sequential patterns recursively

        // for each frequent item
        for (Entry<Integer, Bitmap> entry : verticalDB.entrySet()) {
        	//System.out.println("item: "+entry.getKey()+" sup: "+entry.getValue().getSupport());
        	if(UPIP) {
        		int lastSID = -1;
        		int UPIPCount = 0;
        		if(verticalDB.get(firstItem) == null)return;
        		BitSet bitmap = getBitmapByFlag(0,0).bitmap;
        		//BitSet bitmap = verticalDB.get(firstItem).bitmap;
        		BitSet bitmapItem = entry.getValue().bitmap;
        		
        		if(entry.getKey().equals(firstItem)) {
                    Prefix prefix = new Prefix();
                    prefix.addItemset(new Itemset(entry.getKey()));
                    int imatch = 0;
                    int iimatch = 1;
                    if(iimatch == Query.get(0).size()) {
                    	imatch++;
                    	iimatch = 0;
                    }
                    dfsPruning(imatch, iimatch, prefix, entry.getValue(), frequentItems, frequentItems, entry.getKey(), 2, entry.getKey(), true);
        		}else {
        			
            		for(int bitK = bitmapItem.nextSetBit(0); bitK >= 0; bitK = bitmapItem.nextSetBit(bitK+1)) {
        				// find the sequence (sid) which includes this bit
        				int sid = bitToSID(bitK);

        				// get the index of the last bit representing this sequence (sid)
        	 			int lastBitOfSID = lastBitOfSID(sid, lastBitIndex);
        	 			
        	 			boolean UPIPmatch = false;
        	 			
        	 			int bit = bitmap.nextSetBit(bitK+1); 
        	 			
        	 			if(entry.getKey() < firstItem) {
        	 				bit = bitmap.nextSetBit(bitK); 
        	 			}
        	 			
        				for (;bit >= 0 && bit <= lastBitOfSID; bit = bitmap.nextSetBit(bit+1)) {
        					UPIPmatch = true;
        				}
        				
        				if(UPIPmatch){
        					// update the support
        					if(sid != lastSID){
        						UPIPCount++;
        						lastSID = sid;
        					}
        				}
        				
        				bitK = lastBitOfSID;
            		}
            	
        			if(UPIPCount >= minsup) {
                        // We create a prefix with that item
                        Prefix prefix = new Prefix();
                        prefix.addItemset(new Itemset(entry.getKey()));
                        dfsPruning(0, 0, prefix, entry.getValue(), frequentItems, frequentItems, entry.getKey(), 2, entry.getKey(), false);
        			}
        		}
        		

        	}else {
                // We create a prefix with that item
                Prefix prefix = new Prefix();
                prefix.addItemset(new Itemset(entry.getKey()));
                // We call the depth first search method with that prefix
                // and the list of frequent items to try to find
                // larger sequential patterns by appending some of these
                // items.
                int imatch = 0;
                int iimatch = 0;
                if(entry.getKey().equals(Query.get(0).get(0))) {
                	iimatch++;
                }
                if(iimatch == Query.get(0).size()) {
                	imatch++;
                	iimatch = 0;
                }
                dfsPruning(imatch, iimatch, prefix, entry.getValue(), frequentItems, frequentItems, entry.getKey(), 2, entry.getKey(), false);
        	}
        }
    }

    private Bitmap getBitmapByFlag(int i, int j) {
    	int cnt = 0;
		for(int k = 0; k<i;k++){
			cnt+= Query.get(k).size();
		}
		cnt = cnt+j;
		return pruningMap.get(cnt);
	}
	private int bitToSID(int bit) {
		// Do a binary search
		int result = Collections.binarySearch(sequencesSize, bit);
		if(result >= 0){
			return result;
		}
		return 0 - result -2;
	}

	private int lastBitOfSID(int sid, int lastBitIndex) {
		if(sid+1 >= sequencesSize.size()){
			return lastBitIndex;
		}else{
			return sequencesSize.get(sid+1) -1;
		}
	}
    
	/**
     * This is the dfsPruning method as described in the SPAM paper.
     *
     * @param prefix the current prefix
     * @param prefixBitmap the bitmap corresponding to the current prefix
     * @param sn a list of items to be considered for i-steps
     * @param in a list of items to be considered for s-steps
     * @param hasToBeGreaterThanForIStep
     * @param m size of the current prefix in terms of items
     * @param lastAppendedItem the last appended item to the prefix
     * @throws IOException if there is an error writing a pattern to the output
     * file
     */
    private void dfsPruning(int imatch, int iimatch, Prefix prefix, Bitmap prefixBitmap, 
    		List<Integer> sn, List<Integer> in, int hasToBeGreaterThanForIStep, int m, Integer lastAppendedItem, boolean finishMatchI) throws IOException {

        //  ======  S-STEPS ======
        // Temporary variables (as described in the paper)
        List<Integer> sTemp = new ArrayList<Integer>();
        List<Bitmap> sTempBitmaps = new ArrayList<Bitmap>();

        // for CMAP pruning, we will only check against the last appended item
        Map<Integer, Integer> mapSupportItemsAfter = coocMapAfter.get(lastAppendedItem);

        // for each item in sn
        loopi:
        for (Integer i : sn) {

            // CMAP PRUNING
            // we only check with the last appended item
            if (useCMAPPruning) {
                if (mapSupportItemsAfter == null) {
                    continue loopi;
                }
                Integer support = mapSupportItemsAfter.get(i);
                if (support == null || support < minsup) {
//							System.out.println("PRUNE");
                    continue loopi;
                }
            }

            // perform the S-STEP with that item to get a new bitmap
            Bitmap.INTERSECTION_COUNT++;

            Bitmap newBitmap = prefixBitmap.createNewBitmapSStep(verticalDB.get(i), sequencesSize, lastBitIndex, maxGap);
            // if the support is higher than minsup
            if (newBitmap.getSupportWithoutGapTotal() >= minsup) {	
                sTemp.add(i);
                sTempBitmaps.add(newBitmap);

            }
        }
        // for each pattern recorded for the s-step
        for (int k = 0; k < sTemp.size(); k++) {
            int item = sTemp.get(k);
           
            // obtain new match position
            int newimatch = imatch;
            int newiimatch = iimatch;
            boolean newfinishMatchI = false;
            
            if(newimatch != Query.size()) {
            	if(newiimatch != 0) {
            	    if(item == Query.get(newimatch).get(0)) {
            	    	newiimatch = 1;
                    	if(newiimatch == Query.get(newimatch).size()) {
                    		newimatch = newimatch+1;
                    		newiimatch = 0;
                    		newfinishMatchI = true;
                    	}
            	    }else {
            	    	newiimatch = 0;
            	    }
            	}else {
                	if(item == Query.get(newimatch).get(newiimatch)) {
                		newiimatch = newiimatch+1;
                	}
                	if(newiimatch == Query.get(newimatch).size()) {
                		newimatch = newimatch+1;
                		newiimatch = 0;
                		newfinishMatchI = true;
                	}
            	}

            }   
            
            // create the new bitmap
            Bitmap newBitmap = sTempBitmaps.get(k);
            
            if(USIP && newimatch != Query.size()) {
            	Integer queryItem = Query.get(newimatch).get(newiimatch);
            	
        		int lastSID = -1;
        		int USIPCount = 0;
        		BitSet bitmap = getBitmapByFlag(newimatch,newiimatch).bitmap;
        		
        		for(int bitK = newBitmap.bitmap.nextSetBit(0); bitK >= 0; bitK = newBitmap.bitmap.nextSetBit(bitK+1)) {
    				// find the sequence (sid) which includes this bit
    				int sid = bitToSID(bitK);

    				// get the index of the last bit representing this sequence (sid)
    	 			int lastBitOfSID = lastBitOfSID(sid, lastBitIndex);
    	 			
    	 			boolean USIPmatch = false;
    	 			
    	 			int bit = bitmap.nextSetBit(bitK+1);
    	 			if(item <= queryItem) {
    	 				bit = bitmap.nextSetBit(bitK);
    	 			}
    	 			
    				for (; bit >= 0 && bit <= lastBitOfSID; bit = bitmap.nextSetBit(bit+1)) {
    					USIPmatch = true;
    					break;
    				}
    				
    				if(USIPmatch){
    					// update the support
    					if(sid != lastSID){
    						USIPCount++;
    						lastSID = sid;
    					}
    				}
    				
    				bitK = lastBitOfSID;
        		}
        		          	
	        	if(USIPCount >= minsup) {
	                Prefix prefixSStep = prefix.cloneSequence();
	                prefixSStep.addItemset(new Itemset(item));
	                 // save the pattern to the file
	                if(newBitmap.getSupport() >= minsup) {
	    	            if(m >= minimumPatternLength && newimatch == Query.size()) {
	    	            	savePattern(prefixSStep, newBitmap);
	    	            }
	    	            // recursively try to extend that pattern
	    	            if (maximumPatternLength > m) {
	    	                dfsPruning(newimatch, newiimatch, prefixSStep, newBitmap, sTemp, sTemp, item, m + 1, item, newfinishMatchI);
	    	            }
	                }
	        	}  else{ 		
	        	}
            }else {
              Prefix prefixSStep = prefix.cloneSequence();
              prefixSStep.addItemset(new Itemset(item));

               // save the pattern to the file
              if(newBitmap.getSupport() >= minsup) {
  	            if(m >= minimumPatternLength && newimatch == Query.size()) {
  	            	savePattern(prefixSStep, newBitmap);
  	            }
  	            // recursively try to extend that pattern
  	            if (maximumPatternLength > m) {
  	                dfsPruning(newimatch, newiimatch, prefixSStep, newBitmap, sTemp, sTemp, item, m + 1, item, newfinishMatchI);
  	            }
              }
            }
            
           
        }

        Map<Integer, Integer> mapSupportItemsEquals = coocMapEquals.get(lastAppendedItem);
        // ========  I STEPS =======
        // Temporary variables
        List<Integer> iTemp = new ArrayList<Integer>();
        List<Bitmap> iTempBitmaps = new ArrayList<Bitmap>();

        // for each item in in
        loop2:
        for (Integer i : in) {


            // the item has to be greater than the largest item
            // already in the last itemset of prefix.
            if (i > hasToBeGreaterThanForIStep) {

                // LAST POSITION PRUNING
                /*if (useLastPositionPruning && lastItemPositionMap.get(i) < prefixBitmap.firstItemsetID) {
                 continue loop2;
                 }*/

                // CMAP PRUNING
                if (useCMAPPruning) {
                    if (mapSupportItemsEquals == null) {
                        continue loop2;
                    }
                    Integer support = mapSupportItemsEquals.get(i);
                    // System.out.println(lastAppendedItem + " " + i + "  " + mapSupportItemsEquals.get(i));
                    if (support == null || support < minsup) {
                        continue loop2;
                    }
                }

                // Perform an i-step with this item and the current prefix.
                // This creates a new bitmap
                Bitmap.INTERSECTION_COUNT++;
                Bitmap newBitmap = prefixBitmap.createNewBitmapIStep(verticalDB.get(i), sequencesSize, lastBitIndex);
                // If the support is no less than minsup
                if (newBitmap.getSupport() >= minsup) {
                    // record that item and pattern in temporary variables
                    iTemp.add(i);
                    iTempBitmaps.add(newBitmap);
                }
            }
        }
        for (int k = 0; k < iTemp.size(); k++) {
            int item = iTemp.get(k);

            // obtain new match position
            int newimatch = imatch;
            int newiimatch = iimatch;
            boolean newfinishMatchI = finishMatchI;
            
            if(!finishMatchI) {
                if(newimatch != Query.size()) {
                	if(item == Query.get(newimatch).get(newiimatch)) {
                		newiimatch = newiimatch+1;
                    	if(newiimatch == Query.get(newimatch).size()) {
                    		newimatch = newimatch+1;
                    		newiimatch = 0;
                    		newfinishMatchI = true;
                    	}
                        
                	}else if(item > Query.get(newimatch).get(newiimatch)) {
                		newiimatch = 0;
                	}
                }
            }

            // create the new bitmap
            Bitmap newBitmap = iTempBitmaps.get(k);
            
            if(UIIP && newimatch != Query.size()) {
            	Integer queryItem = Query.get(newimatch).get(newiimatch);
            	
        		int lastSID = -1;
        		int UIIPCount = 0;
        		BitSet bitmap = getBitmapByFlag(newimatch, newiimatch).bitmap;
        		//BitSet bitmap = verticalDB.get(queryItem).bitmap;

        		
        		for(int bitK = newBitmap.bitmap.nextSetBit(0); bitK >= 0; bitK = newBitmap.bitmap.nextSetBit(bitK+1)) {
    				// find the sequence (sid) which includes this bit
    				int sid = bitToSID(bitK);

    				// get the index of the last bit representing this sequence (sid)
    	 			int lastBitOfSID = lastBitOfSID(sid, lastBitIndex);
    	 			
    	 			boolean UIIPmatch = false;
    	 			
    	 			int bit = bitmap.nextSetBit(bitK+1);
    	 			if(item <= queryItem) {
    	 				bit = bitmap.nextSetBit(bitK);
    	 			}
    	 			
    				for (; bit >= 0 && bit <= lastBitOfSID; bit = bitmap.nextSetBit(bit+1)) {
    					UIIPmatch = true;
    					break;
    				}
    				
    				if(UIIPmatch){
    					// update the support
    					if(sid != lastSID){
    						UIIPCount++;
    						lastSID = sid;
    					}
    				}	
    				
    				bitK = lastBitOfSID;
        		}    		
        		          	
	        	if(UIIPCount >= minsup) {
	                // create the new prefix
	                Prefix prefixIStep = prefix.cloneSequence();
	                prefixIStep.getItemsets().get(prefixIStep.size() - 1).addItem(item);

	                // save the pattern
	                if(m >= minimumPatternLength && newimatch == Query.size()) {
	                	savePattern(prefixIStep, newBitmap);
	                }
	                // recursively try to extend that pattern
	                if (maximumPatternLength > m) {
	                    dfsPruning(newimatch, newiimatch, prefixIStep, newBitmap, sTemp, iTemp, item, m + 1, item, newfinishMatchI);
	                }
	        	}else{
	        	}
            }else {
                // create the new prefix
                Prefix prefixIStep = prefix.cloneSequence();
                prefixIStep.getItemsets().get(prefixIStep.size() - 1).addItem(item);


                // save the pattern
                if(m >= minimumPatternLength && newimatch == Query.size()) {
                	savePattern(prefixIStep, newBitmap);
                }
                // recursively try to extend that pattern
                if (maximumPatternLength > m) {
                    dfsPruning(newimatch, newiimatch, prefixIStep, newBitmap, sTemp, iTemp, item, m + 1, item, newfinishMatchI);
                }
            }
        }
        // check the memory usage
        MemoryLogger.getInstance().checkMemory();
    }

    /**
     * Save a pattern of size 1 to the output file
     *
     * @param item the item
     * @param bitmap its bitmap
     * @throws IOException exception if error while writing to the file
     */
    private void savePattern(Integer item, Bitmap bitmap) throws IOException { 	
    	// First, we check if the pattern contains the desired items (optional)
		// We only do that if the user has specified some items that must appear in
		// patterns.
		if(mustAppearItems != null) {
			
			// if the pattern does not contains all required items, then return
			if(mustAppearItems.length > 1) {
				return;
			}
			if(item.equals(mustAppearItems[0]) == false){
				return;
			}
		}
		
        patternCount++; // increase the pattern count
        StringBuilder r = new StringBuilder("");
        r.append(item);
        r.append(" -1 ");
        r.append("#SUP: ");
        r.append(bitmap.getSupport());
        // if the user wants the sequence IDs, we will show them
        if(outputSequenceIdentifiers) {
        	r.append(" #SID: ");
        	r.append(bitmap.getSIDs(sequencesSize));
        }
        writer.write(r.toString());
        writer.newLine();
    }

    /**
     * Save a pattern of size > 1 to the output file.
     *
     * @param prefix the prefix
     * @param bitmap its bitmap
     * @throws IOException exception if error while writing to the file
     */
    private void savePattern(Prefix prefix, Bitmap bitmap) throws IOException {
    	
		// First, we check if the pattern contains the desired items (optional)
		// We only do that if the user has specified some items that must appear in
		// patterns.
		if(mustAppearItems != null) {
			Set<Integer> itemsFound = new HashSet<Integer>();
			// for each item in the pattern
loop:		for(Itemset itemset : prefix.getItemsets()){
				for(Integer item : itemset.getItems()) {
					// if the user required that this item must appear in all patterns 
					if(itemMustAppearInPatterns(item)) {
						// we note it
						itemsFound.add(item);
						if(itemsFound.size() == mustAppearItems.length) {
							break loop;
						}
					}
				}
			}
			// if the pattern does not contains all required items, then return
			if(itemsFound.size() != mustAppearItems.length) {
				return;
			}
		}
        patternCount++;

        StringBuilder r = new StringBuilder("");
        for (Itemset itemset : prefix.getItemsets()) {
            for (Integer item : itemset.getItems()) {
                String string = item.toString();
                r.append(string);
                r.append(' ');
            }
            r.append("-1 ");
        }

        r.append("#SUP: ");
        r.append(bitmap.getSupport());
        // if the user wants the sequence IDs, we will show them
        if(outputSequenceIdentifiers) {
        	r.append(" #SID: ");
        	r.append(bitmap.getSIDs(sequencesSize));
        }
        writer.write(r.toString());
        writer.newLine();
    }

    /**
     * Print the statistics of the algorithm execution to System.out.
     */
    public void printStatistics() {
        StringBuilder r = new StringBuilder(200);
        r.append("=============== f-TaSPM ===============\n Total time: ");
        r.append((endTime - startTime)/1000.0);
        r.append("s\n");
        r.append(" Frequent sequences count: " + patternCount);
        r.append('\n');
        r.append(" Max memory (mb): ");
        r.append(MemoryLogger.getInstance().getMaxMemory());
        r.append(patternCount);
        r.append('\n');
        r.append(" minsup: " + minsup);
        r.append('\n');
        r.append(" Intersection count: " + Bitmap.INTERSECTION_COUNT + " \n");
        r.append("========================================\n");
        System.out.println(r.toString());
    }

    /**
     * Get the maximum length of patterns to be found (in terms of itemset
     * count)
     *
     * @return the maximumPatternLength
     */
    public int getMaximumPatternLength() {
        return maximumPatternLength;
    }

    /**
     * Set the maximum length of patterns to be found (in terms of itemset
     * count)
     *
     * @param maximumPatternLength the maximumPatternLength to set
     */
    public void setMaximumPatternLength(int maximumPatternLength) {
        this.maximumPatternLength = maximumPatternLength;
    }
    
	/**
	 * Set the minimum length of patterns to be found (in terms of itemset count)
	 * @param minimumPatternLength the minimum pattern length to set
	 */
	public void setMinimumPatternLength(int minimumPatternLength) {
		this.minimumPatternLength = minimumPatternLength;
	}
	
	/**
	 * Optional method to specify the items that must appears in patterns found by TKS
	 * @param mustAppearItems an array of items
	 */
	public void setMustAppearItems(int[] mustAppearItems) {
		if(mustAppearItems.length > 0) {
			this.mustAppearItems = mustAppearItems;
		}else {
			this.mustAppearItems = null;
		}
	}
	
	/**
	 * Check if an item must appear in the pattern
	 * @param item the item
	 * @return true if the user has specified that this item must appear in the pattern
	 */
	public boolean itemMustAppearInPatterns(int item) {
		return (mustAppearItems == null)
				|| Arrays.binarySearch(mustAppearItems, item) >=0;
	}
	
	/**
	 * This method allows to specify the maximum gap 
	 * between itemsets of patterns found by the algorithm. 
	 * If set to 1, only patterns of contiguous itemsets
	*  will be found (no gap).
	 * @param maxGap the maximum gap (an integer)
	 */
	public void setMaxGap(int maxGap) {
		this.maxGap = maxGap;
	}
}
