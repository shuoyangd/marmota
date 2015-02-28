package edu.jhu.marmota.decoder;

/**
 * Designed for stack decoding of machine translation
 * @author shuoyang
 *
 * @param <H> indicates what hypothesis are you using in the stack
 */
public abstract class HypothesisStack<H extends Hypothesis> {
	
	/**
	 * push a hypothesis into the stack (you may or may not choose to recombine or check overflow first)
	 * @param h
	 */
	abstract public void push(H h);
	
	/**
	 * pop the best hypothesis possible
	 */
	abstract public H pop();
	
	/**
	 * given a new hypothesis h, see if it can be combined with any hypothesis in the stack
	 * @param h
	 * @return
	 */
	abstract public boolean recombine(H h);
	
	/**
	 * prune the stack when its too large
	 */
	abstract public void prune();
	
	/**
	 * check if the stack has reached its size limit
	 * @return
	 */
	abstract public boolean isOverflow();
}
