package edu.jhu.marmota.syntax.dependency;

import java.util.Arrays;

import edu.jhu.marmota.util.Hashable;

public class DepRule implements Hashable {
	
	private DepNode left;
	private DepNode[] right;

	public DepRule(DepNode left, DepNode[] right) {
		this.left = left;
		this.right = right;
	}
	
	public DepNode getLeft() {
		return left;
	}

	public DepNode[] getRight() {
		return right;
	}
		
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof DepRule)) {
			return false;
		}
		else {
			DepRule rule = (DepRule) other;
			if (!left.equals(rule.left)) {
				return false;
			}
			if ((right == null) ^ (rule.right == null)) {
				return false;
			}
			if (right.length != rule.right.length) {
				return false;
			}
			if (!Arrays.equals(right, rule.right)) {
				return false;
			}
			return true;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append(left.toString());
		if (right != null && right.length > 0) {
			res.append(" -> ");
			for (DepNode token : right) {
				res.append(token.toString());
				res.append(" ");
			}
		}
		return res.toString().trim();
	}
	
	@Override
	public int hashCode() {
		int hash = 17 * left.hashCode();
		hash = 93 * hash + right.hashCode();
		return hash;
	}
}
