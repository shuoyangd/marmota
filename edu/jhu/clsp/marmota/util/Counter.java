package edu.jhu.clsp.marmota.util;

import java.util.HashMap;

public class Counter<T> extends AbstractCounter<T> {

	public Counter() {
		counter = new HashMap<T, Double>();
	}
	
	@Override
	public double increment(T k) {
		double count = counter.get(k) != null? counter.get(k): 0.0;
		counter.put(k, count + 1.0);
		return count + 1.0;
	}

	@Override
	public double increment(T k, double inc) {
		double count = counter.get(k) != null? counter.get(k): 0.0;
		counter.put(k, count + inc);
		return count + inc;
	}

	@Override
	public double count(T k) {
		return counter.get(k) != null? counter.get(k): 0.0;
	}

	@Override
	public void set(T k, Double v) {
		counter.put(k, v);
	}
}
