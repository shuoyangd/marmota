package edu.jhu.marmota.syntax.dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.jhu.marmota.util.Hashable;

/**
 * 
 * this synchrounous rule should look like this:
 * 
 * deprule: möchten/VV -> Ich/PRN kaffee/NN trinken/VV
 * strrule: x -> x0 want x2 x1
 * 
 * where x1 corresponds to DepNode[1] and x2 corresponds to DepNode[2]
 * 
 * By the way, since we are doing Dep2Str, the target rule does not have "left-hand side".
 * So we just ignore them in all the functions such as match() and transform(), etc. 
 * 
 * TODO: May need a code refactor to include a Dep2StrRuleBuilder (from string)? 
 * Currently we are just parsing the string and build the components outside this class.
 * 
 * @author shuoyang
 *
 */
public class Dep2StrRule implements Hashable {

	private DepRule srcrule;
	private String[] tarright;
	/**
	 * alignments[j] = i
	 * means the i-th node on the source side is aligned to the j-th node on the target side
	 */
	private int[] alignments;
	
	public Dep2StrRule(DepNode srcleft, DepNode[] srcright, String[] tarright, int[] alignments) {
		srcrule = new DepRule(srcleft, srcright);
		this.tarright = tarright;
		this.alignments = alignments;
	}

	public Dep2StrRule(DepRule srcrule, String[] tarright, int[] alignments) {
		this.srcrule = srcrule;
		this.tarright = tarright;
		this.alignments = alignments;
	}

	/**
	 * The record takes the form "0-1 1-0 2-3 3-2". It should be a one-to-many mapping from the source to the target.
	 *
	 * @param record
	 */
	static public int[] buildAlignment(String record) {
		String[] tokens= record.split(" ");
		int[] alignments = new int[tokens.length];
		for (String token: tokens) {
			String[] indices = token.split("-");
			alignments[Integer.valueOf(indices[1])] = Integer.valueOf(indices[0]);
		}
		return alignments;
	}

	/**
	 *
	 *
	 * @return
	 */
	public String encodeAlignment() {
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < alignments.length; i++) {
			res.append(String.valueOf(i) + "-" + alignments[i]);
			res.append(" ");
		}
		return res.toString().trim();
	}

	public DepRule getLeft() {
		return srcrule;
	}
	
	public String[] getRight() {
		return tarright;
	}
	
	public List<Integer> src2tar(int i) {
		List<Integer> res = new ArrayList<Integer>();
		for (int j = 0; j < alignments.length; j++) {
			if (alignments[j] == i) {
				res.add(i);
			}
		}
		return res;
	}
	
	public int tar2src(int j) {
		return alignments[j];
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Dep2StrRule)) {
			return false;
		}
		else {
			Dep2StrRule syncrule = (Dep2StrRule) other;
			if (!srcrule.equals(syncrule.srcrule)) {
				return false;
			}
			if (!Arrays.equals(tarright, syncrule.tarright)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append(srcrule.toString());
		res.append(" ||| ");
		res.append(String.join(" ", tarright));
		res.append(" ||| ");
		res.append(encodeAlignment());
		return res.toString();
	}
	
	@Override
	public int hashCode() {
		int hash = srcrule.hashCode();
		hash = (23 * hash) + tarright.hashCode();
		hash = (59 * hash) + alignments.hashCode();
		return hash;
	}
}

