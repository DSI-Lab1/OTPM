package TargetUM;

/**
 * This class represents an Element of a utility list as used by the HUI-Miner algorithm.
 *
 * @see TargetUM.AlgoTargetUM
 * @see TargetUM.UtilityList
 */
public class Element {
    /**
     * transaction id
     */
    public final int tid;
    /**
     * itemset utility
     */
    public final int iutils;
    /**
     * remaining utility
     */
    public int rutils;

    /**
     * Constructor.
     *
     * @param tid    the transaction id
     * @param iutils the itemset utility
     * @param rutils the remaining utility
     */
    public Element(int tid, int iutils, int rutils) {
        this.tid = tid;
        this.iutils = iutils;
        this.rutils = rutils;
    }
}
