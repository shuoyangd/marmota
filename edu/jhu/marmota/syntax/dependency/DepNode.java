package edu.jhu.marmota.syntax.dependency;

import edu.jhu.marmota.util.Hashable;

public class DepNode implements Hashable {
	
	private String token, postag;
	
	public DepNode(String token, String postag) {
		this.token = token;
		this.postag = postag;
	}

	public String token() {
		return token;
	}
	
	public String postag() {
		return postag;
	}
	
	public boolean lexicalizedEquals(Object other) {
		if (!(other instanceof DepNode)) {
			return false;
		}
		else {
			DepNode node = (DepNode) other;
			if (token.equals(node.token)) {
				return true;
			}
			return false;
		}
	}
	
	public boolean postagEquals(Object other) {
		if (!(other instanceof DepNode)) {
			return false;
		}
		else {
			DepNode node = (DepNode) other;
			if (postag.equals(node.postag)) {
				return true;
			}
			return false;
		}
	}
		
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof DepNode)) {
			return false;
		}
		else {
			DepNode node = (DepNode) other;
			if (token.equals(node.token) 
					&& postag.equals(node.postag)) {
				return true;
			}
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		hash += 23 * postag.hashCode();
		hash += 59 * (hash + token.hashCode());
		return hash;
	}
	
	@Override
	public String toString() {
		String res = "(";
		res += token;
		res += "/";
		res += postag;
		res += ")";
		return res;
	}
}
