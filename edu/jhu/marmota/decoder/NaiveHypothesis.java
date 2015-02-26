package edu.jhu.marmota.decoder;

public class NaiveHypothesis extends Hypothesis {

	String choice;
	
	public NaiveHypothesis(boolean[] state, double score, String choice) {
		super(state, score);
		this.choice = choice;
	}
	
	@Override
	public Hypothesis merge(Hypothesis h1, Hypothesis h2) {
		// TODO
		return null;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		else if (other instanceof NaiveHypothesis) {
			NaiveHypothesis sh = (NaiveHypothesis) other;
			if (this.state.length != sh.state.length) {
				return false;
			}
			for (int i = 0; i < this.state.length; i++) {
				if (this.state[i] != sh.state[i]) {
					return false;
				}
			}
			if (this.score != sh.score) {
				return false;
			}
			if (!this.choice.equals(sh.choice)) {
				return false;
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
