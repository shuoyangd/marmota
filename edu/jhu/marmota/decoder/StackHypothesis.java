package edu.jhu.marmota.decoder;

public class StackHypothesis extends Hypothesis {

	@Override
	public Hypothesis merge(Hypothesis h1, Hypothesis h2) {
		return null;
	}

	@Override
	public boolean equals(Object other) {
		return false;
	}
	
	@Override
	public int hashCode() {
		return 0;
	}
}
