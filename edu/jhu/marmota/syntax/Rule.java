package edu.jhu.marmota.syntax;

/**
 * General syntax rules
 * 
 * @author shuoyang
 *
 */
public abstract class Rule<L, R> {
	
	/**
	 * decide whether the rule can be applied
	 * @return
	 */
	abstract public boolean match(L left);

	/**
	 * return the content after the rule is applied
	 * @return
	 */
	abstract public R transform(L left);
	
	/**
	 * decide whether the two rules are actually the same
	 * @return 
	 */
	@Override
	abstract public boolean equals(Object other);
}
