package edu.jhu.marmota.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.jhu.marmota.syntax.dependency.Dep2StrRule;
import edu.jhu.marmota.util.Hashable;
import edu.jhu.marmota.util.HyperEdge;

/**
 * Hypothesis designed for dependency-to-string translation
 * 
 * @author shuoyang
 *
 */
public class Dep2StrHypothesis extends Hypothesis implements Hashable {

	public Dep2StrRule choice;
	public String[] history;
	public int headindex;
	public double f2escore;
	public double e2fscore;
	public double lmscore;
	public double lexf2escore;
	public double lexe2fscore;
	public double wordpenalty;
	public HyperEdge<Dep2StrHypothesis> bp;
	public List<HyperEdge<Dep2StrHypothesis>> mergedbp = null;

	public Dep2StrHypothesis(boolean[] state, double score, Dep2StrRule choice, String[] lmhistory, 
			int headindex, double f2escore, double lexf2escore, double e2fscore, double lexe2fscore,
			double lmscore, double wordpenalty) {
		super(state, score);
		
		this.choice = choice;
		this.history = lmhistory;
		this.headindex = headindex;
		this.f2escore = f2escore;
		this.e2fscore = e2fscore;
		this.lexf2escore = lexf2escore;
		this.lexe2fscore = lexe2fscore;
		this.wordpenalty = wordpenalty;
		this.lmscore = lmscore;
	}

	public void setbp(HyperEdge<Dep2StrHypothesis> bp) {
		this.bp = bp;
	}
	
	public void setMergedBp(List<HyperEdge<Dep2StrHypothesis>> mergedbp) {
		this.mergedbp = mergedbp;
	}

	@Override
	public Hypothesis merge(Hypothesis other) {
		if (other instanceof Dep2StrHypothesis) {
			Dep2StrHypothesis sh = (Dep2StrHypothesis)other;
			if (headindex != sh.headindex) {
				return null;
			}
			if (!Arrays.equals(this.state, sh.state)) {
				return null;
			}
			if (!this.choice.equals(sh.choice) && !Arrays.equals(this.history, sh.history)) {
				return null;
			}
			
			Dep2StrHypothesis chosen = this.score > sh.score? this: sh;
			Dep2StrHypothesis abandoned = this.score > sh.score? sh: this;
			// merging hypothesis with different backpointers
			if (chosen.bp != abandoned.bp || chosen.mergedbp != null || abandoned.mergedbp != null) {
				List<HyperEdge<Dep2StrHypothesis>> mergedbp = new ArrayList<HyperEdge<Dep2StrHypothesis>>();
				if (chosen.mergedbp != null) {
					mergedbp.addAll(chosen.mergedbp);
				}
				if (abandoned.mergedbp != null) {
					mergedbp.addAll(abandoned.mergedbp);
				}
				mergedbp.add(abandoned.bp);
				Dep2StrHypothesis merged = new Dep2StrHypothesis(Arrays.copyOf(chosen.state, chosen.state.length),
					chosen.score, chosen.choice, Arrays.copyOf(chosen.history, chosen.history.length), 
					chosen.headindex, chosen.f2escore, chosen.e2fscore, chosen.lexf2escore, chosen.lexe2fscore,
					chosen.lmscore, chosen.wordpenalty);
				merged.setbp(chosen.bp);
				merged.setMergedBp(mergedbp);
				return merged;
			}
			// merging hypothesis that shares the same backpointer
			else {
				Dep2StrHypothesis merged = new Dep2StrHypothesis(Arrays.copyOf(chosen.state, chosen.state.length),
						chosen.score, chosen.choice, Arrays.copyOf(chosen.history, chosen.history.length), 
						chosen.headindex, chosen.f2escore, chosen.e2fscore, chosen.lexf2escore, chosen.lexe2fscore,
						chosen.lmscore, chosen.wordpenalty);
				merged.setbp(chosen.bp);
				return merged;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "{score " + String.valueOf(score) + "} {state " + Arrays.toString(state) + 
				"} {history " + Arrays.toString(history) + "} {choice " + choice + "}";
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
					this.headindex != sh.headindex || 
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
			if (!Arrays.equals(this.history, sh.history)) {
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
		hash += 3 * state.hashCode();
		hash += 5 * (hash + Double.hashCode(score));
		hash += 7 * (hash + choice.hashCode());
		hash += 11 * (hash + history.hashCode());
		hash += 13 * (hash + headindex);
		hash += 17 * (hash + Double.hashCode(f2escore));
		hash += 19 * (hash + Double.hashCode(e2fscore));
		hash += 23 * (hash + Double.hashCode(lmscore));
		hash += 29 * (hash + Double.hashCode(lexf2escore));
		hash += 31 * (hash + Double.hashCode(lexe2fscore));
		hash += 37 * (hash + Double.hashCode(wordpenalty));
		hash += 41 * (hash + bp.hashCode());
		if (mergedbp != null) {
			hash += 43 * mergedbp.hashCode();
		}
		return hash;
	}
}
