package edu.jhu.marmota.util;

public class LargeNumberArith {
	private LargeNumberArith() {
		
	}
	
	public static double logadd(double x, double y) {
		if (y <= x) {
			return x + Math.log(1 + Math.exp(y - x));
		}
		else {
			return y + Math.log(1 + Math.exp(x - y));
		}
	}
}
