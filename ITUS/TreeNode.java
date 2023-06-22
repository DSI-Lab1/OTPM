package tree;

import java.util.ArrayList;

import tree.SequenceList;

public class TreeNode {

	public int item;
	
	public int SRU;
	
	public int TESU;
	
	public int acu;
	
	// maximum low TESU value.
	public int LMtesu;
	
	public boolean isTaHUSP = false;
	
	//Extension type, a value of 0 means that it is extended from the parent node by I-Extension; otherwise by S-Extension.
	public boolean extType;
	
	//position of the target sequence where the current pattern matches
	public int IMatch;
	public int temIMatch;
	
	//store the information of sequences that contain the pattern.
	public ArrayList<SequenceList> sequenceList;
	//the nodes of children
	public ArrayList<TreeNode> children = new ArrayList<>() ;
	
	public TreeNode(int item, int sRU, int tESU, int acu, boolean isTaHUSP, boolean extType, ArrayList<SequenceList> sequenceList,
			ArrayList<TreeNode> children) {
		super();
		this.item = item;
		this.SRU = sRU;
		this.TESU = tESU;
		this.acu = acu;
		this.isTaHUSP = isTaHUSP;
		this.extType = extType;
		this.sequenceList = sequenceList;
		this.children = children;
		this.isTaHUSP = false;
	}
	

	public TreeNode() {
		this.children = new ArrayList<TreeNode>();
		this.IMatch = 0;
		this.temIMatch = 0;
		this.sequenceList  = new ArrayList<SequenceList>();
		this.LMtesu = 0;
	}

	public TreeNode(int item) {
		this.item = item;
		this.IMatch = 0;
		this.temIMatch = 0;
		this.LMtesu = 0;
		this.children = new ArrayList<TreeNode>();
		this.sequenceList  = new ArrayList<SequenceList>();
		this.isTaHUSP = false;
	}

	public int getItem() {
		return item;
	}

	public void setItem(int item) {
		this.item = item;
	}

	public int getSRU() {
		return SRU;
	}

	public void setSRU(int sRU) {
		SRU = sRU;
	}

	public int getTESU() {
		return TESU;
	}

	public void setTESU(int tESU) {
		TESU = tESU;
	}

	public int getAcu() {
		return acu;
	}

	public void setAcu(int acu) {
		this.acu = acu;
	}

	public boolean isTaHUSP() {
		return isTaHUSP;
	}

	public void setTaHUSP(boolean isTaHUSP) {
		this.isTaHUSP = isTaHUSP;
	}

	public boolean isExtType() {
		return extType;
	}

	public void setExtType(boolean extType) {
		this.extType = extType;
	}

	public ArrayList<SequenceList> getSequenceList() {
		return sequenceList;
	}

	public void setSequenceList(ArrayList<SequenceList> sequenceList) {
		this.sequenceList = sequenceList;
	}

	public ArrayList<TreeNode> getChildren() {
		return children;
	}

	public void setChildren(ArrayList<TreeNode> children) {
		this.children = children;
	}
	
	public void addChild(TreeNode treeNode){
		this.children.add(treeNode);
	}
	
	@Override
	public String toString() {
		return "TreeNode [item=" + item + ", SRU=" + SRU + ", TESU=" + TESU + ", acu=" + acu + ", isTaHUSP=" + isTaHUSP
				+ ", extType=" + extType + ", IMatch=" + IMatch + ", temIMatch=" + temIMatch + ", sequenceList="
				+ sequenceList + ", children=" + children + "]";
	}

}
