package edu.jhu.marmota.decoder;

/**
 * This is the hypothesis used by the naive stack decoder.
 * It is "naive" because:
 * 1. we do not keep a back pointer during decoding
 * 2. we keep all the translation history in the hypothesis
 * 
 * @author shuoyang
 *
 */
public class NaiveHypothesis extends Hypothesis {

	public String choice;
	public String[] history;
	public int lastTranslatedIndex = -1;
	
	public NaiveHypothesis(boolean[] state, double score, String choice, String[] history, int lastTranslatedIndex) {
		super(state, score);
		this.choice = choice;
		this.history = history;
		this.lastTranslatedIndex = lastTranslatedIndex;
	}
	
	/**
	 * among two merging conditions described in SMT P161, only deal with the second
	 * return new hypothesis if merge succeed, else return null
	 */
	@Override
	public Hypothesis merge(Hypothesis other) {
		if (!(other instanceof NaiveHypothesis)) {
			return null;
		}
		if (!((NaiveHypothesis)other).choice.equals(choice)) {
			return null;
		}
		boolean[] ostate = other.state;
		if (state.length != ostate.length) {
			return null;
		}
		for (int i = 0; i < state.length; i++) {
			if (state[i] != ostate[i]) {
				return null;
			}
		}
		
		boolean mergeDecision = score > other.score? true: false;
		double mergeScore = mergeDecision? score: other.score;
		String[] mergeHistory = mergeDecision? history: ((NaiveHypothesis)other).history;
		int mergeIndex = mergeDecision? lastTranslatedIndex: ((NaiveHypothesis)other).lastTranslatedIndex;
		NaiveHypothesis merged = new NaiveHypothesis(state, mergeScore, choice, mergeHistory, mergeIndex);
		return merged;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		else if (other instanceof NaiveHypothesis) {
			NaiveHypothesis sh = (NaiveHypothesis) other;
			if (this.score != sh.score) {
				return false;
			}
			if (this.lastTranslatedIndex != sh.lastTranslatedIndex) {
				return false;
			}
			if (!this.choice.equals(sh.choice)) {
				return false;
			}
			if (this.state.length != sh.state.length) {
				return false;
			}
			if (this.history.length != sh.history.length) {
				return false;
			}
			for (int i = 0; i < this.state.length; i++) {
				if (this.state[i] != sh.state[i]) {
					return false;
				}
			}
			for (int i = 0; i < this.history.length; i++) {
				if (!this.history[i].equals(sh.history[i])) {
					return false;
				}
			}
		}
		else {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int hashCode = 0;
		hashCode += (state == null? 0: state.hashCode());
		hashCode += (29 * (int) score);
		hashCode += (choice == null? 0: 37 * choice.hashCode());
		return hashCode;
	}
}
