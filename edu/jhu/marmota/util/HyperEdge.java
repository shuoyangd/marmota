package edu.jhu.marmota.util;

import java.util.ArrayList;
import java.util.List;

public class HyperEdge<T> {
	
	private T head;
	private List<T> tails;
	
	public HyperEdge(T head, List<T> tail) {
		this.head = head;
		this.tails = tail;
	}
	
	public T getHead() {
		return head;
	}
	
	public List<T> getTail() {
		List<T> res = new ArrayList<T>();
		res.addAll(tails);
		return res;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		else if (other instanceof HyperEdge) {
			HyperEdge<T> he = (HyperEdge<T>) other;
			if (!head.equals(he.head)) {
				return false;
			}
			else if (tails.size() != he.tails.size()) {
				return false;
			}
			else {
				for (int i = 0; i < tails.size(); i++) {
					if (!tails.get(i).equals(he.tails.get(i))) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
}
