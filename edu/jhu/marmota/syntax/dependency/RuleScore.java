package edu.jhu.marmota.syntax.dependency;

public class RuleScore {

	final public double f2escore;
	final public double lexf2escore;
	final public double e2fscore;
	final public double lexe2fscore;
	
	public RuleScore(double f2escore, double lexf2escore,
			double e2fscore, double lexe2fscore) {
		this.f2escore = f2escore;
		this.lexf2escore = lexf2escore;
		this.e2fscore = e2fscore;
		this.lexe2fscore = lexe2fscore;
	}
}
