package tree;

public class Item {
	//store the information of q-sequence database
	int item;
	int utility;
	int sum ;
	
	public Item(int item, int utility) {
		super();
		this.item = item;
		this.utility = utility;
	}
	
	public int getItem() {
		return item;
	}
	public void setItem(int item) {
		this.item = item;
	}
	public int getUtility() {
		return utility;
	}
	public void setUtility(int utility) {
		this.utility = utility;
	}

	public int getSum() {
		return sum;
	}

	public void setSum(int sum) {
		this.sum = sum;
	}

	@Override
	public String toString() {
		return "Item [item=" + item + ", utility=" + utility + ", sum=" + sum + "]";
	}


	

}
