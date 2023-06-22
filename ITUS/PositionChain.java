package tree;

import java.util.Comparator;

public class PositionChain implements Comparator<PositionChain> {

	/** the position of sequence **/
	public int p;

	/** 效用 **/
	public int acu;

	public PositionChain(int p, int acu) {
		super();
		this.p = p;
		this.acu = acu;
	}

	public PositionChain() {
		super();
	}

	@Override
	public String toString() {
		return "PositionChain [p=" + p + ", acu=" + acu + "]";
	}

	
	@Override
	public int compare(PositionChain o1, PositionChain o2) {
		// TODO Auto-generated method stub
		return o1.p - o2.p;
	}

	
}
