package edu.jhu.marmota.decoder;

import java.util.TreeSet;

public class NaiveHypothesisStack<H extends Hypothesis> implements HypothesisStack<H> {

	private TreeSet<Hypothesis> stack = new TreeSet<Hypothesis>(new HypothesisComparator());
	private int maxSize = 100;
	
	public NaiveHypothesisStack() {
		
	}
	
	public NaiveHypothesisStack(int maxSize) {
		this.maxSize = maxSize;
	}
	
	@Override
	public void push(H h) {
		if (!recombine(h)) {
			stack.add(h);
		}
	}

	@Override
	public boolean recombine(H h) {
		// TODO
		return false;
	}

	@Override
	public void prune() {
		while (stack.size() > maxSize) {
			stack.pollFirst();
		}
	}

	@Override
	public boolean isOverflow() {
		if (stack.size() > maxSize) {
			return true;
		}
		else {
			return false;
		}
	}
}
