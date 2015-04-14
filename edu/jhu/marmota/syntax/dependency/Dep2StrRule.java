package edu.jhu.marmota.syntax.dependency;

import java.util.Arrays;

import edu.jhu.marmota.syntax.SyncRule;
import fig.basic.Pair;

/**
 * 
 * this synchrounous rule should look like this:
 * 
 * deprule: 发表/VV -> 他/PRN (是/AUX) (谈话/NN)
 * strrule: x -> He x1 x2
 * 
 * where x1 corresponds to DepNode[1] and x2 corresponds to DepNode[2]
 * 
 * By the way, since we are doing Dep2Str, the target rule does not have "left-hand side".
 * So we just ignore them in all the functions such as match() and transform(), etc.
 * 
 * @author shuoyang
 *
 */
public class Dep2StrRule extends SyncRule<DepNode, DepNode[], Object, String[]> {

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
	
	public boolean match(DepNode srcleft) {
		return match(srcleft, null);
	}
	
	@Override
	public boolean match(DepNode srcleft, Object tarleft) {
		if (!srcrule.match(srcleft)) {
			return false;
		}
		
		return true;
	}

	public Pair<DepNode[], String[]> transform(DepNode srcleft) {
		return transform(srcleft, null);
	}
	
	@Override
	public Pair<DepNode[], String[]> transform(DepNode srcleft, Object tarleft) {
		return new Pair<DepNode[], String[]>(srcrule.transform(srcleft), tarright);
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
}
