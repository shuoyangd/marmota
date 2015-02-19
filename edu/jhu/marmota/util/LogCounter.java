package edu.jhu.marmota.util;

import java.util.HashMap;

public class LogCounter<T> extends AbstractCounter<T> {

	public LogCounter() {
		counter = new HashMap<T, Double>();
	}
	
	@Override
	public double increment(T k) {
		double count = counter.get(k) != null? counter.get(k): Double.NEGATIVE_INFINITY;
		counter.put(k, LargeNumberArith.logadd(count, 1.0));
		return LargeNumberArith.logadd(count, 1.0);
	}

	@Override
	public double increment(T k, double inc) {
		double count = counter.get(k) != null? counter.get(k): Double.NEGATIVE_INFINITY;
		counter.put(k, LargeNumberArith.logadd(count, inc));
		return LargeNumberArith.logadd(count, inc);
	}

	@Override
	public double count(T k) {
		return counter.get(k) != null? counter.get(k): Double.NEGATIVE_INFINITY;
	}

	@Override
	public void set(T k, Double v) {
		counter.put(k, v);
	}

}
