package TargetUM;

import java.util.ArrayList;
import java.util.List;


/**
 * This class represents a UtilityList as used by the HUI-Miner algorithm.
 *
 * @see TargetUM.AlgoTargetUM
 * @see TargetUM.Element
 */
public class UtilityList {
    /**
     * the item name
     */
    public Integer item;
    /**
     * the sum of item utilities
     */
    public int sumIutilsD = 0;
    /**
     * the sum of remaining utilities
     */
    public int sumRutilsD = 0;
    /**
     * the elements
     */
    public List<Element> elementsD = new ArrayList<Element>();

    /**
     * Constructor.
     *
     * @param item the item that is used for this utility list
     */
    public UtilityList(Integer item) {
        super();
        this.item = item;
    }

    /**
     * Method to add an element to this utility list and update the sums at the same time.
     */
    public void addElement(Element element) {
        sumIutilsD += element.iutils;
        sumRutilsD += element.rutils;
        elementsD.add(element);
    }
}
