package TaRP;

/**
 * This class represents an Element of a utility list as used by the HUI-Miner algorithm.
 * 
 * @see THUIM.THUIMAlgo
 * @see THUIM.UtilityList
 * @author Philippe Fournier-Viger
 */
public class Element {
	// The four variables as described in the paper:
	/** transaction id */
	public final int tid ;   
	
	/** itemset utility */
	public final int iutils;  
	
	/** remaining utility */
	public int rutils; 
	
	/** the support value */
//	public int sups; 
	
	/**
	 * Constructor.
	 * @param tid  the transaction id
	 * @param iutils  the itemset utility
	 * @param rutils  the remaining utility
	 */
	public Element(int tid, int iutils, int rutils){
		this.tid = tid;
		this.iutils = iutils;
		this.rutils = rutils;
//		this.sups = sups;
	}
}
