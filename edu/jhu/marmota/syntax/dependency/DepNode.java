package edu.jhu.marmota.syntax.dependency;

import edu.jhu.marmota.util.Hashable;

public class DepNode implements Hashable {
	
	private boolean isTerminal;
	
	private String token, postag;
	
	public DepNode(String token, String postag, boolean isTerminal) {
		this.isTerminal = isTerminal;
		this.token = token;
		this.postag = postag;
	}
	
	public boolean isTerminal() {
		return isTerminal;
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
			if (token.equals(node.token) 
					&& isTerminal == node.isTerminal) {
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
			if ( postag.equals(node.postag)
					&& isTerminal == node.isTerminal) {
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
					&& postag.equals(node.postag)
					&& isTerminal == node.isTerminal) {
				return true;
			}
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		// TODO
		return 0;
	}
	
	@Override
	public String toString() {
		String res = "";
		res += token;
		res += "/";
		res += postag;
		
		if (!isTerminal) {
			res = "(" + res;
			res += ")";
		}
		return res;
	}
}
