package edu.jhu.marmota.syntax.dependency;

import java.util.Arrays;

import edu.jhu.marmota.syntax.Rule;

public class DepRule extends Rule<DepNode, DepNode[]> {
	
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
	
	/**
	 * matching conducted according to the lexicalized matching of dependency nodes
	 */
	@Override
	public boolean match(DepNode left) {
		return this.left.lexicalizedEquals(left);
	}
	
	/**
	 * matching conducted according to the postag matching of dependency nodes
	 * @param left
	 * @return
	 */
	public boolean weakmatch(DepNode left) {
		return this.left.postagEquals(left);
	}

	@Override
	public DepNode[] transform(DepNode left) {
		return this.right;
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
