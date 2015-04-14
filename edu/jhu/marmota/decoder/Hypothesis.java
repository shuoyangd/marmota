package edu.jhu.marmota.decoder;

import java.util.Comparator;

import edu.jhu.marmota.util.Hashable;

public abstract class Hypothesis implements Comparable<Hypothesis>, Hashable {
	
	/**
	 * element i indicates whether the i-th word in the input sentence is translated
	 */
	public boolean[] state;
	
	public double score;
	
	abstract public Hypothesis merge(Hypothesis other);
	
	public Hypothesis(boolean[] state, double score) {
		this.state = state;
		this.score = score;
	}
	
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
	
	@Override
	abstract public String toString();
}

class HypothesisComparator implements Comparator<Hypothesis> {

	@Override
	public int compare(Hypothesis o1, Hypothesis o2) {
		return Double.compare(o1.score, o2.score);
	}

}
