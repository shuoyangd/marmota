package edu.jhu.marmota.syntax.dependency;

import edu.jhu.marmota.util.Hashable;

/**
 * Node in the dependency tree.
 *
 * @author shuoyang
 */
public class DepNode implements Hashable {
	
	private String token, postag;

	/**
	 * if the token or the postag is not specified, just provide null for that.
	 *
	 * @param token
	 * @param postag
	 */
	public DepNode(String token, String postag) {
		this.token = token;
		this.postag = postag;
	}

	public DepNode(String dump) {
		dump = dump.trim();
		int sepIndex = dump.indexOf("/");
		this.token = dump.substring(1, sepIndex);
		this.postag = dump.substring(sepIndex + 1, dump.length() - 1);
	}

	public String token() {
		if (token == null) return "";
		else return token;
	}
	
	public String postag() {
		if (postag == null) return "";
		else return postag;
	}
	
	public boolean lexicalizedEquals(Object other) {
		if (!(other instanceof DepNode)) {
			return false;
		}
		else {
			DepNode node = (DepNode) other;
			if (token == null || node.token == null) {
				return true;
			}
			else if (token == null || node.token != null) {
				return false;
			}
			else if (token.equals(node.token)) {
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
			if (postag == null || node.postag == null) {
				return true;
			}
			else if (postag == null || node.postag != null) {
				return false;
			}
			else if (postag.equals(node.postag)) {
				return true;
			}
			return false;
		}
	}
		
	@Override
	public boolean equals(Object other) {
		if (lexicalizedEquals(other) && postagEquals(other)) {
			return true;
		}
		else {
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
		StringBuilder res = new StringBuilder();
		res.append("(");
		if (token != null) {
			res.append(token);
		}
		else {
			res.append("*");
		}
		res.append("/");
		if (postag != null) {
			res.append(postag);
		}
		else {
			res.append("*");
		}
		res.append(")");
		return res.toString();
	}
}

