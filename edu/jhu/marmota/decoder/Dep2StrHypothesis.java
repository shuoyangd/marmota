package edu.jhu.marmota.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.jhu.marmota.syntax.dependency.Dep2StrRule;

/**
 * Hypothesis designed for dependency-to-string parsing
 * 
 * @author shuoyang
 *
 */
public class Dep2StrHypothesis extends Hypothesis {

	public Dep2StrRule choice;
	public String[] lmhistory;
	public double f2escore;
	public double e2fscore;
	public double lmscore;
	public double lexf2escore;
	public double lexe2fscore;
	public double wordpenalty;
	public Dep2StrHypothesis bp;
	public List<Dep2StrHypothesis> mergedbp = null;

	public Dep2StrHypothesis(boolean[] state, double score, Dep2StrRule choice, String[] lmhistory, 
			double f2escore, double e2fscore, double lexf2escore, double lexe2fscore,
			double lmscore, double wordpenalty, Dep2StrHypothesis bp) {
		super(state, score);
		
		this.choice = choice;
		this.lmhistory = lmhistory;
		this.f2escore = f2escore;
		this.e2fscore = e2fscore;
		this.lexf2escore = lexf2escore;
		this.lexe2fscore = lexe2fscore;
		this.wordpenalty = wordpenalty;
		this.lmscore = lmscore;
		this.bp = bp;
	}
	
	public Dep2StrHypothesis(boolean[] state, double score, Dep2StrRule choice, String[] lmhistory, 
			double f2escore, double e2fscore, double lexf2escore, double lexe2fscore,
			double lmscore, double wordpenalty, Dep2StrHypothesis bp, List<Dep2StrHypothesis> mergedbp) {
		this(state, score, choice, lmhistory, f2escore, e2fscore, lexf2escore, lexe2fscore,
				lmscore, wordpenalty, bp);
		this.mergedbp = mergedbp;
	}

	@Override
	public Hypothesis merge(Hypothesis other) {
		if (other instanceof Dep2StrHypothesis) {
			Dep2StrHypothesis sh = (Dep2StrHypothesis)other;
			if (!Arrays.equals(this.state, sh.state)) {
				return null;
			}
			if (!this.choice.equals(sh.choice) && !Arrays.equals(this.lmhistory, sh.lmhistory)) {
				return null;
			}
			
			Dep2StrHypothesis chosen = this.score > sh.score? this: sh;
			Dep2StrHypothesis abandoned = this.score > sh.score? sh: this;
			// merging hypothesis with different backpointers
			if (chosen.bp != abandoned.bp || chosen.mergedbp != null || abandoned.mergedbp != null) {
				List<Dep2StrHypothesis> mergedbp = new ArrayList<Dep2StrHypothesis>();
				if (chosen.mergedbp != null) {
					mergedbp.addAll(chosen.mergedbp);
				}
				if (abandoned.mergedbp != null) {
					mergedbp.addAll(abandoned.mergedbp);
				}
				mergedbp.add(abandoned.bp);
				Dep2StrHypothesis merged = new Dep2StrHypothesis(Arrays.copyOf(chosen.state, chosen.state.length),
					chosen.score, chosen.choice, Arrays.copyOf(chosen.lmhistory, chosen.lmhistory.length),
					chosen.f2escore, chosen.e2fscore, chosen.lexf2escore, chosen.lexe2fscore,
					chosen.lmscore, chosen.wordpenalty, chosen.bp, mergedbp);
				return merged;
			}
			// merging hypothesis that shares the same backpointer
			else {
				Dep2StrHypothesis merged = new Dep2StrHypothesis(Arrays.copyOf(chosen.state, chosen.state.length),
						chosen.score, chosen.choice, Arrays.copyOf(chosen.lmhistory, chosen.lmhistory.length),
						chosen.f2escore, chosen.e2fscore, chosen.lexf2escore, chosen.lexe2fscore,
						chosen.lmscore, chosen.wordpenalty, chosen.bp);
				return merged;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "{score " + String.valueOf(score) + "} {state " + Arrays.toString(state) + 
				"} {history " + Arrays.toString(lmhistory) + "} {choice " + choice + "}";
	}
	
	/**
	 * We only do shallow equals. 
	 * That is, we only check for the equality of hashcode of backpointers, instead of their contents. 
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		else if (other instanceof Dep2StrHypothesis) {
			Dep2StrHypothesis sh = (Dep2StrHypothesis)other;
			if (this.score != sh.score ||
					this.f2escore != sh.f2escore ||
					this.e2fscore != sh.e2fscore ||
					this.lexf2escore != sh.lexf2escore ||
					this.lexe2fscore != sh.lexe2fscore ||
					this.wordpenalty != sh.wordpenalty ||
					this.lmscore != sh.lmscore) {
				return false;
			}
			if (!this.choice.equals(sh.choice)) {
				return false;
			}
			if (!Arrays.equals(this.state, sh.state)) {
				return false;
			}
			if (!Arrays.equals(this.lmhistory, sh.lmhistory)) {
				return false;
			}
			if (this.bp != sh.bp) {
				return false;
			}
			if (this.mergedbp != sh.mergedbp) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		hash += 3 * choice.hashCode();
		hash += 5 * lmhistory.hashCode();
		hash += 7 * Double.hashCode(f2escore);
		hash += 11 * Double.hashCode(e2fscore);
		hash += 13 * Double.hashCode(lmscore);
		hash += 17 * Double.hashCode(lexf2escore);
		hash += 19 * Double.hashCode(lexe2fscore);
		hash += 23 * Double.hashCode(wordpenalty);
		hash += 29 * bp.hashCode();
		if (mergedbp != null) {
			hash += 31 * mergedbp.hashCode();
		}
		return hash;
	}
}
