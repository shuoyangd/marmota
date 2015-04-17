package edu.jhu.marmota.syntax.dependency;

import java.util.Arrays;

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
 * @author shuoyang
 *
 */
public class Dep2StrRule implements Hashable {

	private DepRule srcrule;
	private String[] tarright;
	
	public Dep2StrRule(DepNode srcleft, DepNode[] srcright, String[] tarright) {
		srcrule = new DepRule(srcleft, srcright);
		this.tarright = tarright;
	}

	public Dep2StrRule(DepRule srcrule, String[] tarright) {
		this.srcrule = srcrule;
		this.tarright = tarright;
	}
	
	public DepRule getLeft() {
		return srcrule;
	}
	
	public String[] getRight() {
		return tarright;
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
	public int hashCode() {
		// TODO
		return 0;
	}
}
