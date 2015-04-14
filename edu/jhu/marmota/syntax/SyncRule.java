package edu.jhu.marmota.syntax;

import fig.basic.Pair;

public abstract class SyncRule<SL, SR, TL, TR>  {
	 
	/**
	 * decide whether the rule can be applied
	 * @return
	 */
	abstract public boolean match(SL srcleft, TL tarleft);

	/**
	 * return the content after the rule is applied
	 * @return
	 */
	abstract public Pair<SR, TR> transform(SL srcleft, TL tarleft);
	
	/**
	 * decide whether the two rules are actually the same
	 * @return 
	 */
	@Override
	abstract public boolean equals(Object other);
}
