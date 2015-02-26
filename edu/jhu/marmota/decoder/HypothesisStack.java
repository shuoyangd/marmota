package edu.jhu.marmota.decoder;

/**
 * Designed for stack decoding of machine translation
 * @author shuoyang
 *
 * @param <H> indicates what hypothesis are you using in the stack
 */
public interface HypothesisStack<H extends Hypothesis> {
	
	/**
	 * push a hypothesis into the stack (you may or may not choose to recombine or check overflow first)
	 * @param h
	 */
	public void push(H h);
	
	/**
	 * given a new hypothesis h, see if it can be combined with any hypothesis in the stack
	 * @param h
	 * @return
	 */
	public boolean recombine(H h);
	
	/**
	 * prune the stack when its too large
	 */
	public void prune();
	
	/**
	 * check if the stack has reached its size limit
	 * @return
	 */
	public boolean isOverflow();
}
