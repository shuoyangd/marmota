package edu.jhu.marmota.decoder;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * A naive hypothesis stack only allowing to poll the "largest" element.
 * 
 * If you want to iterate all the element in this stack, the right way to do so is to call iterator() method.
 * 
 * @author shuoyang
 *
 * @param <H>
 */
public class NaiveHypothesisStack<H extends Hypothesis> extends HypothesisStack<H> implements Iterable<Hypothesis> {

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

	@SuppressWarnings("unchecked")
	@Override
	public H pop() {
		return (H)stack.pollLast();
	}
	
	@Override
	public boolean recombine(H h) {
		for (Hypothesis hypo: stack) {
			@SuppressWarnings("unchecked")
			H merged = (H) hypo.merge(h);
			if (merged != null) {
				stack.remove(hypo);
				stack.add(merged);
				return true;
			}
		}
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

	@Override
	public Iterator<Hypothesis> iterator() {
		return stack.descendingIterator();
	}

	@Override
	public int size() {
		return stack.size();
	}
}
