package edu.jhu.marmota.decoder;

import java.util.Arrays;

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

	public double ptScore;
	public String choice;
	public String[] history;
	public int lastTranslatedIndex = -1;
	
	public NaiveHypothesis(boolean[] state, double score, double ptScore, String choice, String[] history, int lastTranslatedIndex) {
		super(state, score);
		this.ptScore = ptScore;
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
		boolean differentHistory = false;
		if (history.length == ((NaiveHypothesis) other).history.length) {
			for (int i = 0; i < history.length; i++) {
				if (!history[i].equals(((NaiveHypothesis) other).history[i])) {
					differentHistory = true;
				}
			}
		}
		else{
			differentHistory = true;
		}
		
		if (!((NaiveHypothesis)other).choice.equals(choice) && differentHistory) {
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
		
		NaiveHypothesis template = score > other.score? this: (NaiveHypothesis)other;
		NaiveHypothesis merged = new NaiveHypothesis(Arrays.copyOf(template.state, template.state.length),
				template.score, template.ptScore, template.choice, 
				Arrays.copyOf(template.history, template.history.length), template.lastTranslatedIndex);
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
			if (this.ptScore != sh.ptScore) {
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

	@Override
	public String toString() {
		return "{score " + String.valueOf(score) + "} {state " + Arrays.toString(state) + 
				"} {history " + Arrays.toString(history) + "} {choice " + choice + 
				"} {lastTranslatedIndex " + String.valueOf(lastTranslatedIndex);
	}
}
