package algo;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import tree.*;

public class iTaHUSP {

	// the time the algorithm started
	double startTimestamp = 0;
	// the time the algorithm terminated
	double endTimestamp = 0;
	// the number of patterns generated
	int patternCount = 0;

	int incPatternCount = 0;

	int nodeNumber = 0;
	// writer to write the output file
	BufferedWriter writer = null;

	final int BUFFERS_SIZE = 2000;

	// if true, debugging information will be shown in the console
	final int DEBUG = 0;
	int DEBUG_flag = 0;

	// if true, save result to file in a format that is easier to read by humans
	final boolean SAVE_RESULT_EASIER_TO_READ_FORMAT = false;

	// the minUtility threshold
	int minUtility = 0;

	// max pattern length
	int maxPatternLength = 1000;
	// the input file path
	String input;
	// store the query sequence
	int[] querySequence;
	// store the number of items in the querySequence
	int querySequencelength = 0;
	// the number of Candidate
	int NumOfCandidate = 0;
	// construct tree by TESU
	private TreeNode Tree;
	// store the number of sequences
	int sequenceNumber = -1;

	// all sequence in sequence database
	ArrayList<ArrayList<Item>> MemoryDB = new ArrayList<>();
	// store the IDs of updated sequences
	Map<Integer, Integer> UDB = null;
	// store the positions of the last intance of query sequence. key:Sequece ID
	HashMap<Integer, ArrayList<Integer>> LastInstanceOfQS = new HashMap<Integer, ArrayList<Integer>>();
	// store all 1-sequences
	HashMap<Integer, TreeNode> treeNodes = new HashMap<Integer, TreeNode>();
	// Used to store all position information of items in the database
	HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> itemPositionMap = new HashMap<>();
	// construct the tree for storing high TESU sequences
	TreeNode tree = new TreeNode();

	public void runAlgorithm(String input, String output, int threshold) throws Exception {
		// reset maximum
		MemoryLogger.getInstance().reset();
		this.Tree = new TreeNode();
		// input path
		this.input = input;
		// record the start time of the algorithm
		startTimestamp = System.currentTimeMillis();
		// create a writer object to write results to file
		writer = new BufferedWriter(new FileWriter(output));
		this.minUtility = threshold;
		// step 1: scan DB and construct treeNodes for all 1-sequences
		readDB(input);
		// step2: traverse 1-sequences and construct the targeted candidate pattern tree
		traverse1sequences();
		// check the memory usage again and close the file.
		MemoryLogger.getInstance().checkMemory();

		endTimestamp = System.currentTimeMillis();
		printStatistics();
		// the number of record increments
		int turn = 2;
		while (turn <= 8) {
			startTimestamp = System.currentTimeMillis();
			//
			incPatternCount = 0;
			MemoryLogger.getInstance().checkMemory();
			String incInput = "datasets/test";
			incInput = incInput + turn + ".txt";
			incrementalPhase(incInput);
			endTimestamp = System.currentTimeMillis();
			int res[] = new int[0];
			// print tree and calculate the pattern count
			printTree(tree, res);
			printStatisticsInc();
			writer.close();
			turn++;
		}

	}

	// traverse 1-sequences for recursive growth
	private void traverse1sequences() throws InterruptedException {
		// TODO Auto-generated method stub
		int lm = 0;
		for (Entry<Integer, TreeNode> entry : treeNodes.entrySet()) {
			NumOfCandidate++;
			int[] patternBuffer = new int[1];
			TreeNode treeNode = entry.getValue();
			int item = treeNode.getItem();
			treeNode.extType = true;
			patternBuffer[0] = item;
			// initial the node information
			if (item == querySequence[0]) {
				treeNode.temIMatch = 1;
				treeNode.IMatch = 0;
			} else {
				treeNode.IMatch = 0;
				treeNode.temIMatch = 0;
			}
			// determine if it is a THUSP
			if (treeNode.temIMatch >= querySequence.length - 2) {
				treeNode.isTaHUSP = true;
				patternCount++;
			} else {
				treeNode.isTaHUSP = false;
			}
			// determine whether it is a high TESU sequence
			if (treeNode.getTESU() >= minUtility) {
				tree.addChild(treeNode);
				TaHUSP(treeNode, patternBuffer);
			} else {
				if (lm < treeNode.getTESU())
					lm = treeNode.getTESU();
			}
		}
		tree.LMtesu = lm;
	}

	private void readDB(String input) throws IOException {
		// TODO read DB and construct the sequenceList for all 1-sequences
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
		String thisLine;
		while ((thisLine = reader.readLine()) != null) {
			if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
					|| thisLine.charAt(0) == '@') {
				continue;
			}
			sequenceNumber++;
			// for storing an UtilityList of each 1-sequence in the current
			Map<Integer, SequenceList> map1SequenceUtilityList = new HashMap<Integer, SequenceList>();

			// We reset the two following buffer length to zero because we are
			// reading a new sequence.

			// split the sequence according to the " " separator
			String tokens[] = thisLine.split(" ");
			// store the current sequence
			ArrayList<Item> qSequence = new ArrayList<Item>();

			// get the sequence utility (the last token on the line)
			String sequenceUtilityString = tokens[tokens.length - 1];
			int positionColons = sequenceUtilityString.indexOf(':');
			int sequenceUtility = Integer.parseInt(sequenceUtilityString.substring(positionColons + 1));

			// For each token on the line except the last three tokens (the -1
			// -2 and sequence utility).
			for (int i = 0; i < tokens.length - 3; i++) {
				String currentToken = tokens[i];
				// if empty, continue to next token
				if (currentToken.length() == 0) {
					continue;
				}
				int size = qSequence.size() - 1;
				// if the current token is -1 ,the ending sign of an itemset
				if (currentToken.equals("-1")) {
					// It means that it is the end of an itemset.
					Item item = new Item(-1, 0);
					item.setSum(qSequence.get(size).getSum());
					qSequence.add(item);
				} else {
					// We will extract the item from the string:
					int positionLeftBracketString = currentToken.indexOf('[');
					int positionRightBracketString = currentToken.indexOf(']');
					String itemString = currentToken.substring(0, positionLeftBracketString);
					Integer item = Integer.parseInt(itemString);
					// We also extract the utility from the string:
					String utilityString = currentToken.substring(positionLeftBracketString + 1,
							positionRightBracketString);
					Integer itemUtility = Integer.parseInt(utilityString);
					Item ele = new Item(item, itemUtility);
					if (itemPositionMap.get(sequenceNumber) == null) {
						HashMap<Integer, ArrayList<Integer>> temp = new HashMap<>();
						ArrayList<Integer> p = new ArrayList<>();
						p.add(size + 1);
						temp.put(item, p);
						itemPositionMap.put(sequenceNumber, temp);
					} else {
						HashMap<Integer, ArrayList<Integer>> temp = itemPositionMap.get(sequenceNumber);
						if (temp.get(item) == null) {
							ArrayList<Integer> p = new ArrayList<>();
							p.add(size + 1);
							temp.put(item, p);
						} else {
							temp.get(item).add(size + 1);
						}
					}
					if (size < 0) {
						ele.setSum(itemUtility);
					} else {
						ele.setSum(qSequence.get(size).getSum() + itemUtility);
					}
					qSequence.add(ele);

				}
			}
			MemoryDB.add(qSequence);
			// determine whether the sequence is a promising sequence
			if (sequenceUtility == 0 || isContainQuerySequence(qSequence, sequenceNumber) == false) {
				itemPositionMap.remove(sequenceNumber);
				if (DEBUG == 7) {
					for (int i = 0; i < qSequence.size(); i++) {
						System.out.print(qSequence.get(i).getItem() + " ");
					}
					System.out.println();
				}
				continue;
			}

			int remainUtility = sequenceUtility;

			// construct the sequenceList for each 1-sequence
			// use the unpromising positions pruning strategy
			for (int i = 0; i <= LastInstanceOfQS.get(sequenceNumber).get(0); i++) {
				int item = qSequence.get(i).getItem();
				int itemUtility = qSequence.get(i).getUtility();
				if (item == -1)
					continue;
				remainUtility = remainUtility - itemUtility;
				// SRU
				int SRU = remainUtility + itemUtility;
				// update global information
				if (map1SequenceUtilityList.get(item) == null) {
					SequenceList temSequenceList = new SequenceList();
					temSequenceList.SRU = SRU;
					temSequenceList.sid = sequenceNumber;
					temSequenceList.TESU = SRU;
					temSequenceList.U = itemUtility;
					map1SequenceUtilityList.put(item, temSequenceList);
				}
				SequenceList cur = map1SequenceUtilityList.get(item);
				if (cur.U < itemUtility) {
					cur.U = itemUtility;
				}
				if (cur.SRU < SRU) {
					cur.SRU = SRU;
				}
				PositionChain pChain = new PositionChain(i, itemUtility);
				cur.positionChain.add(pChain);
			}
			// traverse map1SequenceUtilityList to update node information
			for (Map.Entry<Integer, SequenceList> entry : map1SequenceUtilityList.entrySet()) {
				int item = entry.getKey();
				SequenceList sequenceList = entry.getValue();
				if (treeNodes.get(item) == null) {
					treeNodes.put(item, new TreeNode(item));
				}
				TreeNode treeNode = treeNodes.get(item);
				treeNode.acu += sequenceList.U;
				treeNode.SRU += sequenceList.SRU;
				treeNode.TESU = treeNode.SRU;
				treeNode.sequenceList.add(sequenceList);
			}
		}
		reader.close();
	}

	// the first phase
	private void TaHUSP(TreeNode treeNode, int patternBuffer[]) throws InterruptedException {

		// SRU pruning
		if (treeNode.getSRU() < minUtility)
			return;
		// for storing TESU after extension
		Map<Integer, Integer> mapiItemTESU = new HashMap<>();
		Map<Integer, Integer> mapsItemTESU = new HashMap<>();
		int lm = 0;
		/**********************************************
		 * construct ilist and slist
		 **********************************************/
		// scan prefix-projected DB once to find items to be extended
		// traverse the sequenceLists of node information to find the extension items
		for (SequenceList sequenceList : treeNode.getSequenceList()) {
			int sequenceID = sequenceList.sid;
			ArrayList<Item> qSequence = MemoryDB.get(sequenceID);

			// find the matching position of the sequence by treeNode.IMatch and
			// treeNode.temIMatch
			int matchIndex = getMatchIndexBySequenceID(treeNode.IMatch, treeNode.temIMatch, sequenceID);

			int acUtility = sequenceList.U;
			int utilityOfPositon1 = sequenceList.positionChain.get(0).acu;
			int sequenceUtility = qSequence.get(qSequence.size() - 1).getSum();
			boolean isEqual = false;
			// determines if the pattern's utility is equal to the utility of the first
			// instance
			if (acUtility == utilityOfPositon1) {
				isEqual = true;
			}
			Map<Integer, Integer> iItemTESUTemp = new HashMap<>();
			/****************** I-Extension items ************/
			for (PositionChain pChain : sequenceList.positionChain) {
				if (pChain.p > matchIndex)
					break;
				// I-Extension items only need to traverse the last itemset where the instance
				// is located
				for (int i = pChain.p + 1; i < qSequence.size(); i++) {
					if (i > matchIndex)
						break;
					Item item = qSequence.get(i);
					// separator
					if (item.getItem() < 0)
						break;
					else {
						// calculate the TESU of item
						int TESU = pChain.acu + item.getUtility() + sequenceUtility - item.getSum();
						// refer to the definition of TESU
						if (isEqual == true) {
							if (iItemTESUTemp.get(item.getItem()) == null) {
								iItemTESUTemp.put(item.getItem(), TESU);
							} else
								;
						} else {
							iItemTESUTemp.put(item.getItem(), sequenceList.SRU);
						}
					}
				}
			}
			// update the mapiItemTESU
			for (Entry<Integer, Integer> entry : iItemTESUTemp.entrySet()) {
				int item = entry.getKey();
				if (mapiItemTESU.get(item) == null) {
					mapiItemTESU.put(item, entry.getValue());
				} else {
					int temTESU = mapiItemTESU.get(item);
					mapiItemTESU.put(item, entry.getValue() + temTESU);
				}
			}
			// for S-Extension items
			Map<Integer, Integer> sItemTESUTemp = new HashMap<>();
			int i = sequenceList.positionChain.get(0).p + 1;
			// Start with the next itemset where the instance is located
			while (i <= matchIndex && qSequence.get(i).getItem() != -1)
				i++;
			for (; i <= matchIndex; i++) {
				Item item = qSequence.get(i);
				if (item.getItem() < 0)
					continue;
				// refer to the definition of TESU
				if (isEqual == true) {
					// calculate TESU of the item
					int TESU = sequenceList.positionChain.get(0).acu + item.getUtility() + sequenceUtility
							- item.getSum();
					if (sItemTESUTemp.get(item.getItem()) == null) {

						sItemTESUTemp.put(item.getItem(), TESU);
					} else {
						;
					}
				} else {
					sItemTESUTemp.put(item.getItem(), sequenceList.SRU);
				}

			}

			// update mapsItemTESU
			for (Entry<Integer, Integer> entry : sItemTESUTemp.entrySet()) {
				int item = entry.getKey();
				if (mapsItemTESU.get(item) == null) {
					mapsItemTESU.put(item, entry.getValue());
				} else {
					int temTESU = mapsItemTESU.get(item);
					mapsItemTESU.put(item, entry.getValue() + temTESU);
				}
			}
		}
		//
		/************************ I-Extension ****************************/
		for (Entry<Integer, Integer> entry : mapiItemTESU.entrySet()) {
			// delete the I-Extension item of low TESU
			if (entry.getValue() < minUtility) {
				if (entry.getValue() > lm)
					lm = entry.getValue();
				continue;
			}

			// construct the child node
			TreeNode temNode = buildIExtensionTreeNode(entry.getKey(), treeNode, entry.getValue());
			int[] temPatternBuffer = arrayCopy(patternBuffer, entry.getKey(), false);
			NumOfCandidate++;

			// determine if it is a TaHUSP
			if (temNode.temIMatch >= querySequence.length - 2 && temNode.acu >= minUtility) {
				patternCount++;
				temNode.isTaHUSP = true;
			}

			treeNode.addChild(temNode);
			// Recursively expand the pattern
			TaHUSP(temNode, temPatternBuffer);
		}

		/************************ S-Extension ****************************/
		for (Entry<Integer, Integer> entry : mapsItemTESU.entrySet()) {
			// delete the I-Extension item of low TESU
			if (entry.getValue() < minUtility) {
				if (entry.getValue() > lm)
					lm = entry.getValue();
				continue;
			}
			// construct the child node
			TreeNode temNode = buildSExtensionTreeNode(entry.getKey(), treeNode, entry.getValue());

			int[] temPatternBuffer = arrayCopy(patternBuffer, entry.getKey(), true);
			NumOfCandidate++;
			// determine if it is a TaHUSP
			if (temNode.temIMatch >= querySequence.length - 2 && temNode.acu >= minUtility) {
				patternCount++;
				temNode.isTaHUSP = true;
			}

			treeNode.children.add(temNode);
			// Recursively expand the pattern
			TaHUSP(temNode, temPatternBuffer);
		}
		// update the LMtesu of treeNode
		treeNode.LMtesu = lm;
	}

	private int[] arrayCopy(int[] patternBuffer, Integer key, boolean type) {
		// TODO Auto-generated method stub
		int newLen = 0;
		int[] temPatternBuffer;
		if (type == false) {
			newLen = patternBuffer.length + 1;
			temPatternBuffer = new int[newLen];
			temPatternBuffer[newLen - 1] = key;
		} else {
			newLen = patternBuffer.length + 2;
			temPatternBuffer = new int[newLen];
			temPatternBuffer[newLen - 2] = -1;
			temPatternBuffer[newLen - 1] = key;
		}

		System.arraycopy(patternBuffer, 0, temPatternBuffer, 0, patternBuffer.length);

		return temPatternBuffer;
	}

	private TreeNode buildIExtensionTreeNode(Integer item, TreeNode parent, Integer TESU) {
		// TODO Auto-generated method stub
		TreeNode ichild = new TreeNode();

		ichild.TESU = TESU;
		int totalUtility = 0;
		int totalSRU = 0;

		int temMatch = parent.temIMatch;
		int IMatch = parent.IMatch;
		// update the matching flags
		if (temMatch < querySequence.length && querySequence[temMatch] != -1) {
			if (item == querySequence[temMatch])
				temMatch++;
		}
		ichild.IMatch = IMatch;
		ichild.temIMatch = temMatch;
		// TreeNode ichild
		for (SequenceList sequenceList : parent.sequenceList) {
			int sequenceID = sequenceList.get_sid();
			if (sequenceList.positionChain == null || sequenceList.positionChain.size() == 0)
				continue;

			SequenceList tempList = new SequenceList(sequenceID);
			ArrayList<Item> sequence = MemoryDB.get(sequenceID);
			int sequenceUtility = sequence.get(sequence.size() - 1).getSum();
			for (PositionChain pChain : sequenceList.positionChain) {

				int pUtility = pChain.acu;

				for (int i = pChain.p + 1;; i++) {
					Item entry = sequence.get(i);
					if (entry.getItem() < 0)
						break;
					// determine whether it is equal to the I-Extension item
					if (entry.getItem() == item) {
						// update the information
						int temu = pUtility + entry.getUtility();
						int remainUtility = sequenceUtility - sequence.get(i).getSum();
						int temSRU = pUtility + entry.getUtility() + remainUtility;

						if (temu > tempList.U) {
							tempList.U = temu;
						}
						if (temSRU > tempList.SRU) {
							tempList.SRU = temSRU;
						}
						tempList.add(i, temu);
					}
				}
			}
			if (tempList.positionChain.size() > 0) {
				totalUtility += tempList.U;
				totalSRU += tempList.SRU;
				ichild.sequenceList.add(tempList);
			}
		}
		ichild.acu = totalUtility;
		ichild.SRU = totalSRU;
		ichild.item = item;
		ichild.extType = false;
		return ichild;
	}

	private TreeNode buildSExtensionTreeNode(Integer item, TreeNode parent, Integer TESU) {

		// TODO Auto-generated method stub
		TreeNode schild = new TreeNode();
		schild.TESU = TESU;
		int totalUtility = 0;
		int totalSRU = 0;

		int temMatch = parent.temIMatch;
		int IMatch = parent.IMatch;
		// update the matching flags
		if (temMatch < querySequence.length && querySequence[temMatch] == -1) {// 璇存槑璇ラ」闆嗗凡鍖归厤瀹岋紝鏇存柊蹇參鎸囬拡
			temMatch++;
			IMatch = temMatch;
		} else {
			temMatch = IMatch;
		}
		if (temMatch < querySequence.length) {
			if (item == querySequence[temMatch]) {
				temMatch++;
			}
		}
		schild.IMatch = IMatch;
		schild.temIMatch = temMatch;

		for (SequenceList sequenceList : parent.sequenceList) {
			int sequenceID = sequenceList.get_sid();

			ArrayList<Integer> itemPositions = itemPositionMap.get(sequenceID).get(item);
			if (itemPositions == null)
				continue;
			if (sequenceList.positionChain == null || sequenceList.positionChain.size() == 0)
				continue;
			SequenceList tempList = new SequenceList(sequenceID);
			ArrayList<Item> sequence = MemoryDB.get(sequenceID);
			int sequenceUtility = sequence.get(sequence.size() - 1).getSum();
			HashMap<Integer, Integer> maxUtility = new HashMap<>();

			for (PositionChain pChain : sequenceList.positionChain) {

				int pUtility = pChain.acu;
				int i = pChain.p + 1;
				while (i < sequence.size() && sequence.get(i).getItem() != -1) {
					i++;
				}
				i++;
				for (int j = 0; j < itemPositions.size(); j++) {
					int p = itemPositions.get(j);
					if (p >= i) {
						Item entry = sequence.get(p);
						int temu = pUtility + entry.getUtility();
						int remainUtility = sequenceUtility - sequence.get(p).getSum();
						int temSRU = pUtility + entry.getUtility() + remainUtility;
						if (temu > tempList.U)
							tempList.U = temu;
						if (temSRU > tempList.SRU)
							tempList.SRU = temSRU;
						if (maxUtility.get(p) == null || maxUtility.get(p) < temu) {
							maxUtility.put(p, temu);
						}
					} else {
						continue;
					}
				}
			}
			for (Entry<Integer, Integer> entry : maxUtility.entrySet()) {
				tempList.add(entry.getKey(), entry.getValue());
			}
			if (tempList.positionChain.size() > 0) {
				// Sort by position from smallest to largest
				Collections.sort(tempList.positionChain, new PositionChain());
				totalUtility += tempList.U;
				totalSRU += tempList.SRU;
				schild.sequenceList.add(tempList);
			}
		}
		schild.acu = totalUtility;
		schild.SRU = totalSRU;
		schild.item = item;
		schild.extType = true; //
		return schild;
	}

	private int getMatchIndexBySequenceID(int iMatch, int temIMatch, int sequenceID) {

		ArrayList<Integer> lastInstance = LastInstanceOfQS.get(sequenceID);
		int index = temIMatch;
		if (index < lastInstance.size() && lastInstance.get(index) == -1)
			index++;
		if (index >= lastInstance.size())
			return MemoryDB.get(sequenceID).size() - 1;
		else {
			return lastInstance.get(index);
		}

	}

	// determine whether the sequence contains qs, and record the position of the
	// last instance of qs in the sequence
	public boolean isContainQuerySequence(ArrayList<Item> qSequence, int sequenceID) {

		if (querySequence.length <= 1)
			return true;

		int index = querySequence.length - 3;

		ArrayList<Integer> positionsOfLastIntance = new ArrayList<Integer>();
		for (int i = 0; i <= index; i++) {
			positionsOfLastIntance.add(-1);
		}
		// Match qs in reverse to find the location of the last instance
		for (int i = qSequence.size() - 1; i >= 0;) {
			int item = qSequence.get(i).getItem();
			if (item < 0) {
				i--;
				continue;
			}
			if (item == querySequence[index]) {
				int temIndex = index;
				boolean isMatchItemset = false;
				while (i >= 0 && qSequence.get(i).getItem() != -1) {

					if (qSequence.get(i).getItem() == querySequence[index]) {
						positionsOfLastIntance.set(index, i);
						index--;
					}
					i--;
					// determine whether the match is complete
					if (index < 0) {
						LastInstanceOfQS.put(sequenceID, positionsOfLastIntance);
						return true;
					} else if (querySequence[index] == -1) {
						isMatchItemset = true;
						index--;
						break;
					}

				}

				while (i >= 0 && qSequence.get(i).getItem() != -1) {
					i--;
				}
				// if the itemset fails to match, it needs to re-match from index
				if (isMatchItemset == false) {
					index = temIndex;
				}
			} else {
				i--;
			}
		}

		return false;
	}

	/**
	 * Set the query sequence Set the number of items in the query sequence
	 * 
	 * @param targetSequence target sequence
	 */
	public void setTargetsequence(int[] querySequence) {
		for (int i = 0; i < querySequence.length; i++) {
			if (querySequence[i] >= 0)
				querySequencelength++;
		}
		this.querySequence = querySequence;
	}

	// print the tree node information of 1-sequences

	/**
	 * Print statistics about the latest execution to System.out.
	 * 
	 */
	public void printStatistics() {
		System.out.println("============= ALGOTaHUSP - STATS =============");
		System.out.println(" Target sequence: " + Arrays.toString(querySequence));
		System.out.println(" Threshold:" + this.minUtility);
		System.out.println(" Total time ~ " + (endTimestamp - startTimestamp) / 1000 + " s");
		System.out.println(" Max Memory ~ " + MemoryLogger.getInstance().getMaxMemory() + " MB");
		System.out.println(" High-utility sequential pattern count : " + patternCount);
		System.out.println(" Number Of Candidate : " + NumOfCandidate);
		// System.out.println(" Conruntime : " + conruntime/1000 +" s");
		System.out.println("========================================================" + " \n");
	}

	public void printStatisticsInc() {
		System.out.println("============= ALGOiTaHUSP - STATS =============");
		System.out.println(" Target sequence: " + Arrays.toString(querySequence));
		System.out.println(" Threshold:" + this.minUtility);
		System.out.println(" Total time ~ " + (endTimestamp - startTimestamp) / 1000 + " s");
		System.out.println(" Max Memory ~ " + MemoryLogger.getInstance().getMaxMemory() + " MB");
		System.out.println(" High-utility sequential pattern count : " + incPatternCount);
		System.out.println(" Number Of Candidate : " + NumOfCandidate);
		// System.out.println(" Conruntime : " + conruntime/1000 +" s");
		System.out.println("========================================================" + " \n");
	}

	private void printTree(TreeNode node, int[] patternBuffer) {
		// TODO Auto-generated method stub

		if (node.children == null || node.children.size() == 0) {
			return;
		} else {
			for (int i = 0; i < node.children.size(); i++) {
				TreeNode root = node.children.get(i);
				int res[] = arrayCopy(patternBuffer, root.item, root.extType);

				if (root.isTaHUSP == true) {
					incPatternCount++;
				}
				printTree(root, res);
			}
		}
	}

	// read incremental data and update related information(memoryDB，LastInstance)
	private void readIncremetalData(String input) throws Exception, IOException {
		// TODO read DB and construct the sequenceList for all 1-sequences

		UDB = new HashMap<Integer, Integer>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
		String thisLine;
		while ((thisLine = reader.readLine()) != null) {
			if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
					|| thisLine.charAt(0) == '@') {
				continue;
			}
			// for storing an UtilityList of each 1-sequence in the current
			// split the sequence according to the " " separator
			String tokens[] = thisLine.split(" ");

			// get the sequence utility (the last token on the line)
			String sequenceUtilityString = tokens[tokens.length - 1];
			int positionColons = sequenceUtilityString.indexOf(':');
			int sequenceUtility = Integer.parseInt(sequenceUtilityString.substring(positionColons + 1));

			ArrayList<Item> qSequence = null;
			int oriLastIndex = -1;

			sequenceNumber++;
			int sequenceID = sequenceNumber;
			// store the current transcation
			qSequence = new ArrayList<Item>();
			for (int i = 0; i < tokens.length - 3; i++) {
				String currentToken = tokens[i];
				// if empty, continue to next token
				if (currentToken.length() == 0) {
					continue;
				}
				int size = qSequence.size() - 1;
				// if the current token is -1 ,the ending sign of an itemset
				if (currentToken.equals("-1")) {
					// It means that it is the end of an itemset.
					Item item = new Item(-1, 0);
					item.setSum(qSequence.get(size).getSum());
					qSequence.add(item);
				} else {
					// We will extract the item from the string:
					int positionLeftBracketString = currentToken.indexOf('[');
					int positionRightBracketString = currentToken.indexOf(']');
					String itemString = currentToken.substring(0, positionLeftBracketString);
					Integer item = Integer.parseInt(itemString);
					// We also extract the utility from the string:
					String utilityString = currentToken.substring(positionLeftBracketString + 1,
							positionRightBracketString);
					Integer itemUtility = Integer.parseInt(utilityString);
					Item ele = new Item(item, itemUtility);
					if (size < 0) {
						ele.setSum(itemUtility);
					} else {
						ele.setSum(qSequence.get(size).getSum() + itemUtility);
					}
					qSequence.add(ele);
				}
			}
			MemoryDB.add(qSequence);
			// determine whether the update sequence contains qs, and update lastInstanceMap
			// at the same time
			if (isContainQuerySequence(qSequence, sequenceID)) {
				UDB.put(sequenceID, sequenceID);

				List<Item> sequence = MemoryDB.get(sequenceID);
				for (int i = 0; i < sequence.size(); i++) {
					int item = sequence.get(i).getItem();
					if (item < 0)
						continue;
					if (itemPositionMap.get(sequenceID) == null) {
						HashMap<Integer, ArrayList<Integer>> temp = new HashMap<>();
						ArrayList<Integer> p = new ArrayList<>();
						p.add(i);
						temp.put(item, p);
						itemPositionMap.put(sequenceNumber, temp);
					} else {
						HashMap<Integer, ArrayList<Integer>> temp = itemPositionMap.get(sequenceNumber);
						if (temp.get(item) == null) {
							ArrayList<Integer> p = new ArrayList<>();
							p.add(i);
							temp.put(item, p);
						} else {
							temp.get(item).add(i);
						}
					}

				}
			}
		}
		reader.close();
	}

	private void incrementalPhase(String input) throws IOException, Exception {
		// read the incremental data
		readIncremetalData(input);
		updateTreeNodeFor1SequenceTreeNode();
		// printTreeNodes();
		List<TreeNode> children = tree.getChildren();
		int lm = tree.LMtesu;
		// traverse 1-sequence
		for (Entry<Integer, TreeNode> entry : treeNodes.entrySet()) {
			// determine whether it is a newly generated 1-sequence node
			int item = entry.getKey();
			boolean isChild = false;
			TreeNode treeNode = entry.getValue();
			int[] patternBuffer = new int[1];
			patternBuffer[0] = item;
			if (entry.getValue().getTESU() < minUtility)
				continue;
			for (TreeNode child : children) {
				if (item == child.getItem()) {
					isChild = true;
					break;
				}
			}
			// is a newly generated node
			if (isChild == false) {
				treeNode.extType = true;
				if (item == querySequence[0]) {
					treeNode.temIMatch = 1;
					treeNode.IMatch = 0;
				} else {
					treeNode.IMatch = 0;
					treeNode.temIMatch = 0;
				}
				// determine whether it is a TaHUSP
				if (treeNode.temIMatch >= querySequence.length - 2) {
					treeNode.isTaHUSP = true;
					// patternCount++;
				}
				// dertermine if it is a high TESU sequence
				if (treeNode.getTESU() >= minUtility) {
					tree.addChild(treeNode);
					// If it is a newly generated node, call THUSP to construct the subtree
					TaHUSP(treeNode, patternBuffer);
				} else {
					if (lm < treeNode.getTESU())
						lm = treeNode.getTESU();
				}
			} else {
				// If the node already exists in the tree, call IncTaHUSP for incremental update
				IncTaHUSP(treeNode, patternBuffer);
			}
		}

		tree.LMtesu = lm;
	}

	/**
	 * Determine whether there is an updated sequence in the sequence containing the
	 * pattern, if not, it can directly skip the update of the node
	 **/
	private boolean isUpdate(TreeNode node) {
		for (SequenceList slist : node.sequenceList) {
			int sid = slist.get_sid();
			if (UDB.get(sid) != null) {
				return true;
			}
		}
		return false;
	}

	// Update the relevant information of the corresponding nodes of 1-sequences
	private void updateTreeNodeFor1SequenceTreeNode() {
		// traverse UDB
		for (Entry<Integer, Integer> entryLDB : UDB.entrySet()) {
			int sid = entryLDB.getKey();
			List<Item> qSequence = MemoryDB.get(sid);

			int matchIndex = LastInstanceOfQS.get(sid).get(0);
			int sequenceUtility = qSequence.get(qSequence.size() - 1).getSum();
			int sequenceU = 0;
			int sequenceSRU = 0;

			Map<Integer, SequenceList> mapSequenceUtilityList = new HashMap<Integer, SequenceList>();
			for (int i = 0; i <= matchIndex; i++) {
				Item item = qSequence.get(i);
				if (item.getItem() < 0)
					continue;

				int SRU = item.getUtility() + sequenceUtility - item.getSum();
				if (mapSequenceUtilityList.get(item.getItem()) == null) {
					SequenceList temSequenceList = new SequenceList(sid);
					temSequenceList.SRU = SRU;
					temSequenceList.TESU = SRU;
					temSequenceList.U = item.getUtility();
					mapSequenceUtilityList.put(item.getItem(), temSequenceList);
				}
				SequenceList slist = mapSequenceUtilityList.get(item.getItem());
				if (slist.U < item.getUtility()) {
					slist.U = item.getUtility();
				}
				if (slist.SRU < SRU) {
					slist.SRU = SRU;
				}
				slist.positionChain.add(new PositionChain(i, item.getUtility()));
			}

			for (Map.Entry<Integer, SequenceList> entry : mapSequenceUtilityList.entrySet()) {
				int item = entry.getKey();
				SequenceList sequenceList = entry.getValue();
				if (treeNodes.get(item) == null) {
					treeNodes.put(item, new TreeNode(item));
				}
				TreeNode treeNode = treeNodes.get(item);
				treeNode.acu += sequenceList.U;
				treeNode.SRU += sequenceList.SRU;
				treeNode.TESU = treeNode.SRU;
				treeNode.sequenceList.add(sequenceList);
			}

		}
	}

	// main algorithms for incremental mining
	private void IncTaHUSP(TreeNode treeNode, int[] patternBuffer) throws InterruptedException {
		// TODO Auto-generated method stub
		// SRU pruning
		if (treeNode.SRU < minUtility) {
			return;
		}
		// patternBuffer is not contained in any sequence in UDB
		if (isUpdate(treeNode) == false) {
			return;
		}
		// the leaf node
		if (treeNode.children.size() == 0) {
			TaHUSP(treeNode, patternBuffer);
			return;
		}

		int emltesu = treeNode.LMtesu;
		int SRUldb = getSRUOfLDB(treeNode);

		/**
		 * It means that no new extensions will be generated, and it is sufficient to
		 * traverse UDB to update information
		 */
		if (treeNode.LMtesu == 0 || treeNode.LMtesu + SRUldb < minUtility) {
			// scan UDB to get the new extension items
			Map<Integer, Integer> iItemTESULDB = new HashMap<>();
			Map<Integer, Integer> sItemTESULDB = new HashMap<>();
			int temTesu = 0;
			/**********************************************
			 * construct ilist and slist (UDB)
			 **********************************************/
			// scan prefix-projected DB once to find items to be extended
			for (SequenceList sequenceList : treeNode.getSequenceList()) {
				// get the sequence ID
				int sequenceID = sequenceList.sid;
				if (UDB.get(sequenceID) == null)
					continue;
				ArrayList<Item> qSequence = MemoryDB.get(sequenceID);

				int matchIndex = getMatchIndexBySequenceID(treeNode.IMatch, treeNode.temIMatch, sequenceID);
				int acUtility = sequenceList.U;
				int utilityOfPositon1 = sequenceList.positionChain.get(0).acu;
				int sequenceUtility = qSequence.get(qSequence.size() - 1).getSum();
				boolean isEqual = false;
				// refer to the definition of TESU
				if (acUtility == utilityOfPositon1) {
					isEqual = true;
				}
				Map<Integer, Integer> iItemTESUTemp = new HashMap<>();
				/****************** I-Extension items ************/
				for (PositionChain pChain : sequenceList.positionChain) {
					if (pChain.p > matchIndex)
						break;
					for (int i = pChain.p + 1; i < qSequence.size(); i++) {
						if (i > matchIndex)
							break;
						Item item = qSequence.get(i);

						if (item.getItem() < 0)
							break;
						else {
							// calculate the TESU of the item
							int TESU = pChain.acu + item.getUtility() + sequenceUtility - item.getSum();
							if (isEqual == true) {
								if (iItemTESUTemp.get(item.getItem()) == null) {
									iItemTESUTemp.put(item.getItem(), TESU);
								} else {
									;
								}
							} else {
								iItemTESUTemp.put(item.getItem(), sequenceList.SRU);
							}

						}
					}

				}
				// positionChain,mapiItemTESU
				for (Entry<Integer, Integer> entry : iItemTESUTemp.entrySet()) {
					int item = entry.getKey();
					if (iItemTESULDB.get(item) == null) {
						iItemTESULDB.put(item, entry.getValue());
					} else {
						int temTESU = iItemTESULDB.get(item);
						iItemTESULDB.put(item, entry.getValue() + temTESU);
					}
				}
				Map<Integer, Integer> sItemTESUTemp = new HashMap<>();
				int i = sequenceList.positionChain.get(0).p + 1;
				while (i <= matchIndex && qSequence.get(i).getItem() != -1)
					i++;
				for (; i <= matchIndex; i++) {
					Item item = qSequence.get(i);
					if (item.getItem() < 0)
						continue;

					if (isEqual == true) {
						int TESU = sequenceList.positionChain.get(0).acu + item.getUtility() + sequenceUtility
								- item.getSum();
						if (sItemTESUTemp.get(item.getItem()) == null) {
							sItemTESUTemp.put(item.getItem(), TESU);
						} else {
							;
						}
					} else {
						sItemTESUTemp.put(item.getItem(), sequenceList.SRU);
					}

				}

				// Update TESU values for all extended sequences
				for (Entry<Integer, Integer> entry : sItemTESUTemp.entrySet()) {
					int item = entry.getKey();
					if (sItemTESULDB.get(item) == null) {
						sItemTESULDB.put(item, entry.getValue());
					} else {
						int temTESU = sItemTESULDB.get(item);
						sItemTESULDB.put(item, entry.getValue() + temTESU);
					}
				}
			}
			// I-Extension items
			for (Entry<Integer, Integer> entry : iItemTESULDB.entrySet()) {
				int item = entry.getKey();
				int ldbTESU = entry.getValue();
				TreeNode iChild = isChildNode(treeNode, item, false);
				int[] temPatternBuffer = arrayCopy(patternBuffer, entry.getKey(), false);
				// Indicates that it is not a newly generated child node
				if (iChild != null) {
					// update the node information
					UpdateNodeForIExtension(treeNode, iChild, ldbTESU, false);
					// judge whether it is a THUSP
					if (iChild.isTaHUSP == false && iChild.temIMatch >= querySequence.length - 2
							&& iChild.acu >= minUtility) {
						iChild.isTaHUSP = true;

					}
					// call IncTaHUSP to update the information of children recursively
					IncTaHUSP(iChild, temPatternBuffer);

				} else if (ldbTESU >= minUtility) {
					// A new child node is generated
					TreeNode temNode = buildIExtensionTreeNode(entry.getKey(), treeNode, entry.getValue());
					NumOfCandidate++;
					if (temNode.temIMatch >= querySequence.length - 2 && temNode.acu >= minUtility) {
						temNode.isTaHUSP = true;
					}
					treeNode.addChild(temNode);
					// Call TaHUSP to construct the subtree
					TaHUSP(temNode, temPatternBuffer);
				} else {
					if (ldbTESU > temTesu)
						temTesu = ldbTESU;
				}
			}

			// S-Extension items
			for (Entry<Integer, Integer> entry : sItemTESULDB.entrySet()) {
				int item = entry.getKey();
				int ldbTESU = entry.getValue();

				TreeNode iChild = isChildNode(treeNode, item, true);
				int[] temPatternBuffer = arrayCopy(patternBuffer, entry.getKey(), true);

				// Indicates that it is not a newly generated child node
				if (iChild != null) {
					UpdateNodeForSExtension(treeNode, iChild, ldbTESU, false);
					if (iChild.isTaHUSP == false && iChild.temIMatch >= querySequence.length - 2
							&& iChild.acu >= minUtility) {
						iChild.isTaHUSP = true;
					}
					// call IncTaHUSP to update the information of children recursively
					IncTaHUSP(iChild, temPatternBuffer);

				} else if (ldbTESU >= minUtility) {
					// A new child node is generated
					TreeNode temNode = buildSExtensionTreeNode(entry.getKey(), treeNode, entry.getValue());
					NumOfCandidate++;
					if (temNode.temIMatch >= querySequence.length - 2 && temNode.acu >= minUtility) {
						temNode.isTaHUSP = true;
					}
					treeNode.addChild(temNode);
					// Call TaHUSP to construct the subtree
					TaHUSP(temNode, temPatternBuffer);
				} else {
					if (ldbTESU > temTesu)
						temTesu = ldbTESU;
				}
			}
			// update the value of LMtesu
			treeNode.LMtesu += temTesu;
		} else {
			// scan the DB' to get the TESU for 1-sequences
			// scan UDB to get the new extension items
			Map<Integer, Integer> iItemTESUDB = new HashMap<>();
			Map<Integer, Integer> sItemTESUDB = new HashMap<>();
			Map<Integer, Integer> iUDBItem = new HashMap<>();
			Map<Integer, Integer> sUDBItem = new HashMap<>();
			/**********************************************
			 * construct ilist and slist (DB)
			 **********************************************/
			// scan prefix-projected DB once to find items to be extended
			for (SequenceList sequenceList : treeNode.getSequenceList()) {
				// get the sequence ID
				int sequenceID = sequenceList.sid;
				ArrayList<Item> qSequence = MemoryDB.get(sequenceID);
				boolean isUpdateItem = false;
				if (UDB.get(sequenceID) != null)
					isUpdateItem = true;
				int matchIndex = getMatchIndexBySequenceID(treeNode.IMatch, treeNode.temIMatch, sequenceID);
				int acUtility = sequenceList.U;
				int utilityOfPositon1 = sequenceList.positionChain.get(0).acu;
				int sequenceUtility = qSequence.get(qSequence.size() - 1).getSum();
				boolean isEqual = false;

				if (acUtility == utilityOfPositon1) {
					isEqual = true;
				}
				Map<Integer, Integer> iItemTESUTemp = new HashMap<>();
				/****************** I-Extension items ************/
				for (PositionChain pChain : sequenceList.positionChain) {
					if (pChain.p > matchIndex)
						break;
					for (int i = pChain.p + 1; i < qSequence.size(); i++) {
						if (i > matchIndex)
							break;
						Item item = qSequence.get(i);

						if (item.getItem() < 0)
							break;
						else {

							int TESU = pChain.acu + item.getUtility() + sequenceUtility - item.getSum();
							if (isEqual == true) {
								if (iItemTESUTemp.get(item.getItem()) == null) {
									iItemTESUTemp.put(item.getItem(), TESU);
								} else {
									;
								}
							} else {
								iItemTESUTemp.put(item.getItem(), sequenceList.SRU);
							}

						}
					}

				}
				for (Entry<Integer, Integer> entry : iItemTESUTemp.entrySet()) {
					int item = entry.getKey();
					if (isUpdateItem == true) {
						iUDBItem.put(item, item);
					}
					if (iItemTESUDB.get(item) == null) {
						iItemTESUDB.put(item, entry.getValue());
					} else {
						int temTESU = iItemTESUDB.get(item);
						iItemTESUDB.put(item, entry.getValue() + temTESU);
					}
				}
				Map<Integer, Integer> sItemTESUTemp = new HashMap<>();
				int i = sequenceList.positionChain.get(0).p + 1;
				while (i <= matchIndex && qSequence.get(i).getItem() != -1)
					i++;
				for (; i <= matchIndex; i++) {
					Item item = qSequence.get(i);
					if (item.getItem() < 0)
						continue;
					// refer to the definition of TESU
					if (isEqual == true) {

						int TESU = sequenceList.positionChain.get(0).acu + item.getUtility() + sequenceUtility
								- item.getSum();
						if (sItemTESUTemp.get(item.getItem()) == null) {
							sItemTESUTemp.put(item.getItem(), TESU);
						} else {
							;
						}
					} else {
						sItemTESUTemp.put(item.getItem(), sequenceList.SRU);
					}

				}

				for (Entry<Integer, Integer> entry : sItemTESUTemp.entrySet()) {
					int item = entry.getKey();
					if (isUpdateItem == true) {
						sUDBItem.put(item, item);
					}
					if (sItemTESUDB.get(item) == null) {
						sItemTESUDB.put(item, entry.getValue());
					} else {
						int temTESU = sItemTESUDB.get(item);
						sItemTESUDB.put(item, entry.getValue() + temTESU);
					}
				}
			}

			// I-Extension items
			for (Entry<Integer, Integer> entry : iItemTESUDB.entrySet()) {
				int item = entry.getKey();
				if (iUDBItem.get(item) == null)
					continue;
				int dbTESU = entry.getValue();
				if (dbTESU < minUtility) {
					if (dbTESU > emltesu)
						emltesu = dbTESU;
				} else {
					TreeNode iChild = isChildNode(treeNode, item, false);
					int[] temPatternBuffer = arrayCopy(patternBuffer, entry.getKey(), false);
					// Indicates that it is not a newly generated child node
					if (iChild != null) {
						// update the information of node
						UpdateNodeForIExtension(treeNode, iChild, dbTESU, true);
						if (iChild.isTaHUSP == false && iChild.temIMatch >= querySequence.length - 2
								&& iChild.acu >= minUtility) {
							iChild.isTaHUSP = true;
						}
						// call IncTaHUSP to update the information of children recursively
						IncTaHUSP(iChild, temPatternBuffer);
					} else {
						// Indicates that it is a newly generated child node, and it need to construct
						// the child node
						TreeNode temNode = buildIExtensionTreeNode(entry.getKey(), treeNode, entry.getValue());
						NumOfCandidate++;
						if (temNode.temIMatch >= querySequence.length - 2 && temNode.acu >= minUtility) {

							temNode.isTaHUSP = true;
						}
						treeNode.addChild(temNode);
						// call TaHUSP to construct subtree recursively
						TaHUSP(temNode, temPatternBuffer);
					}
				}

			}
			// S-Extension items
			for (Entry<Integer, Integer> entry : sItemTESUDB.entrySet()) {
				int item = entry.getKey();
				int dbTESU = entry.getValue();
				if (sUDBItem.get(item) == null)
					continue;
				if (dbTESU < minUtility) {
					if (dbTESU > emltesu)
						emltesu = dbTESU;
				} else {
					TreeNode iChild = isChildNode(treeNode, item, true);
					int[] temPatternBuffer = arrayCopy(patternBuffer, entry.getKey(), true);

					// The process is similar to the I-Extension
					if (iChild != null) {
						UpdateNodeForSExtension(treeNode, iChild, dbTESU, true);
						if (iChild.isTaHUSP == false && iChild.temIMatch >= querySequence.length - 2
								&& iChild.acu >= minUtility) {
							patternCount++;
							iChild.isTaHUSP = true;
						}
						IncTaHUSP(iChild, temPatternBuffer);
					} else {
						TreeNode temNode = buildSExtensionTreeNode(entry.getKey(), treeNode, entry.getValue());
						NumOfCandidate++;
						if (temNode.temIMatch >= querySequence.length - 2 && temNode.acu >= minUtility) {
							patternCount++;
							temNode.isTaHUSP = true;
						}
						treeNode.addChild(temNode);
						TaHUSP(temNode, temPatternBuffer);
					}
				}
			}
			treeNode.LMtesu = emltesu;
		}

	}

	private void UpdateNodeForIExtension(TreeNode parent, TreeNode child, int dbTESU, boolean type) {
		if (type == false)
			child.TESU += dbTESU;
		else
			child.TESU = dbTESU;
		int totalUtility = child.acu;
		int totalSRU = child.SRU;
		int item = child.item;

		// TreeNode ichild
		for (SequenceList sequenceList : parent.sequenceList) {
			int sequenceID = sequenceList.get_sid();
			if (sequenceList.positionChain == null || sequenceList.positionChain.size() == 0)
				continue;
			if (UDB.get(sequenceID) == null)
				continue;
			SequenceList tempList = new SequenceList(sequenceID);
			ArrayList<Item> sequence = MemoryDB.get(sequenceID);
			int sequenceUtility = sequence.get(sequence.size() - 1).getSum();
			for (PositionChain pChain : sequenceList.positionChain) {
				int pUtility = pChain.acu;
				for (int i = pChain.p + 1;; i++) {
					Item entry = sequence.get(i);
					if (entry.getItem() < 0)
						break;
					if (entry.getItem() == item) {
						int temu = pUtility + entry.getUtility();
						int remainUtility = sequenceUtility - sequence.get(i).getSum();
						int temSRU = pUtility + entry.getUtility() + remainUtility;
						if (temu > tempList.U) {
							tempList.U = temu;
						}
						if (temSRU > tempList.SRU) {
							tempList.SRU = temSRU;
						}
						tempList.add(i, temu);
					}
				}
			}
			if (tempList.positionChain.size() > 0) {
				totalUtility += tempList.U;
				totalSRU += tempList.SRU;
				child.sequenceList.add(tempList);
			}
		}
		child.acu = totalUtility;
		child.SRU = totalSRU;
		child.item = item;
	}

	// update node information of S-Extension
	private void UpdateNodeForSExtension(TreeNode parent, TreeNode child, int dbTESU, boolean type) {
		if (type == false)
			child.TESU += dbTESU;
		else
			child.TESU = dbTESU;
		int totalUtility = child.acu;
		int totalSRU = child.SRU;
		int item = child.item;
		// TreeNode parent
		for (SequenceList sequenceList : parent.sequenceList) {
			int sequenceID = sequenceList.get_sid();
			ArrayList<Integer> itemPositions = itemPositionMap.get(sequenceID).get(item);
			if (itemPositions == null)
				continue;
			if (UDB.get(sequenceID) == null)
				continue;
			if (sequenceList.positionChain == null || sequenceList.positionChain.size() == 0)
				continue;
			SequenceList tempList = new SequenceList(sequenceID);
			ArrayList<Item> sequence = MemoryDB.get(sequenceID);
			int sequenceUtility = sequence.get(sequence.size() - 1).getSum();
			HashMap<Integer, Integer> maxUtility = new HashMap<>();
			for (PositionChain pChain : sequenceList.positionChain) {
				int pUtility = pChain.acu;
				int i = pChain.p + 1;

				while (i < sequence.size() && sequence.get(i).getItem() != -1) {
					i++;
				}
				i++;
				for (int j = 0; j < itemPositions.size(); j++) {
					int p = itemPositions.get(j);
					if (p >= i) {
						Item entry = sequence.get(p);
						int temu = pUtility + entry.getUtility();
						int remainUtility = sequenceUtility - sequence.get(p).getSum();
						int temSRU = pUtility + entry.getUtility() + remainUtility;
						if (temu > tempList.U)
							tempList.U = temu;
						if (temSRU > tempList.SRU)
							tempList.SRU = temSRU;
						if (maxUtility.get(p) == null || maxUtility.get(p) < temu) {
							maxUtility.put(p, temu);
						}
					} else {
						continue;
					}
				}
			}
			for (Entry<Integer, Integer> entry : maxUtility.entrySet()) {
				tempList.add(entry.getKey(), entry.getValue());
			}
			if (tempList.positionChain.size() > 0) {
				Collections.sort(tempList.positionChain, new PositionChain());
				totalUtility += tempList.U;
				totalSRU += tempList.SRU;
				child.sequenceList.add(tempList);
			}
		}
		child.acu = totalUtility;
		child.SRU = totalSRU;
		child.item = item;
		child.extType = true;
	}

	private TreeNode isChildNode(TreeNode treeNode, int item, boolean type) {
		if (treeNode.children == null || treeNode.children.size() == 0)
			return null;
		for (TreeNode node : treeNode.children) {
			if (node.getItem() == item && node.extType == type)
				return node;
		}
		return null;
	}

	private int getSRUOfLDB(TreeNode treeNode) {
		int SRUldb = 0;
		for (SequenceList slist : treeNode.sequenceList) {
			int sid = slist.get_sid();
			if (UDB.get(sid) != null) {
				SRUldb += slist.SRU;
			}
		}
		return SRUldb;
	}

}
