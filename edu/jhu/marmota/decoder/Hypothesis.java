package edu.jhu.marmota.decoder;

import edu.jhu.marmota.util.Hashable;

public abstract class Hypothesis implements Comparable<Hypothesis>, Hashable<Hypothesis> {
	
	/**
	 * element i indicates whether the i-th word in the input sentence is translated
	 */
	public boolean[] state;
	
	public double score;
	
	abstract public Hypothesis merge(Hypothesis h1, Hypothesis h2);
	
	@Override
	public int compareTo(Hypothesis h) {
		if (score > h.score) {
			return 1;
		}
		else if (score < h.score) {
			return -1;
		}
		else {
			return 0;
		}
	}
	
	public boolean isNull() {
		for (int i = 0; i< state.length; i++) {
			if (state[i]) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isComplete() {
		for (int i = 0; i< state.length; i++) {
			if (!state[i]) {
				return false;
			}
		}
		return true;
	}
}
