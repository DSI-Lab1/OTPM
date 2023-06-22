package tree;

import java.util.ArrayList;
import java.util.List;

public class SequenceList {

	// sid
	public int sid;

	// UtilityList:
	public List<PositionChain> positionChain;

	// length
	public int LengthOfUtilityList;

	// SRU：Suffix Remain Utility
	public int SRU = 0;
	
	// TESU:Tight Extension Sequence Utility
	public int TESU = 0;

	// the utility of sequnece
	public int U = 0;

	public SequenceList() {
		this.LengthOfUtilityList = 0;
		this.positionChain = new ArrayList<PositionChain>();
	}

	public SequenceList(int id) {
		this.sid = id;
		this.LengthOfUtilityList = 0;
		this.positionChain = new ArrayList<PositionChain>();
	}

	public void add(int p, int acu) {
		this.positionChain.add(new PositionChain(p, acu));
		this.LengthOfUtilityList++;
	}

	public void set_SRU(int sru) {
		this.SRU = sru;
	}

	public int getSid() {
		return sid;
	}

	public void setSid(int sid) {
		this.sid = sid;
	}

	public List<PositionChain> getPositionChain() {
		return positionChain;
	}

	public void setPositionChain(List<PositionChain> positionChain) {
		this.positionChain = positionChain;
	}

	public int getLengthOfUtilityList() {
		return LengthOfUtilityList;
	}

	public void setLengthOfUtilityList(int lengthOfUtilityList) {
		LengthOfUtilityList = lengthOfUtilityList;
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

	public void set_sid(int sid) {
		this.sid = sid;
	}

	public int get_sid() {
		return this.sid;
	}

}