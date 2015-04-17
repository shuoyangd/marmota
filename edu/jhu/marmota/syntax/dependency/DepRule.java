package edu.jhu.marmota.syntax.dependency;

import java.util.Arrays;

public class DepRule {
	
	private DepNode left;
	private DepNode[] right;

	public DepRule(DepNode left, DepNode[] right) {
		this.left = left;
		this.right = right;
	}
	
	public DepRule() {
		
	}
	
	public void setLeft(DepNode left) {
		this.left = left;
	}
	
	public void setRight(DepNode[] right) {
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
		// TODO
		return null;
	}
}
