package edu.jhu.clsp.marmota.util;

import java.util.HashMap;

import fig.basic.Pair;

public class JointCounter<T> extends AbstractCounter<Pair<T, T>> {
	private Counter<T> marginalCounterX;
	private Counter<T> marginalCounterY;
	private boolean marginlock = false;
	
	public JointCounter() {
		counter = new HashMap<Pair<T, T>, Double>();
		marginalCounterX = new Counter<T>();
		marginalCounterY = new Counter<T>();
	}

	@Override
	public double increment(Pair<T, T> e) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		double count = counter.get(e) != null? counter.get(e): 0.0;
		counter.put(e, count + 1.0);
		marginalCounterX.increment(e.getFirst());
		marginalCounterY.increment(e.getSecond());
		return count + 1.0;
	}

	@Override
	public double increment(Pair<T, T> e, double inc) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		double count = counter.get(e) != null? counter.get(e): 0.0;
		counter.put(e, count + inc);
		marginalCounterX.increment(e.getFirst(), inc);
		marginalCounterY.increment(e.getSecond(), inc);
		return count + inc;
	}

	@Override
	public double count(Pair<T, T> e) {
		return counter.get(e) != null? counter.get(e): 0.0;
	}
	
	@Override
	public void set(Pair<T, T> k, Double v) {
		marginlock = true;
		counter.put(k, v);
	}
	
	public void marginalize() {
		marginalCounterX.clear();
		marginalCounterY.clear();
		for (Pair<T, T> wp: counter.keySet()) {
			marginalCounterX.increment(wp.getFirst(), counter.get(wp));
			marginalCounterY.increment(wp.getSecond(), counter.get(wp));
		}
		marginlock = false;
	}
	
	public boolean locked() {
		return marginlock;
	}
	
	public double countx(T e) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to access marginal count.");
		}
		
		return marginalCounterX.count(e);
	}

	public double county(T e) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to access marginal count.");
		}
		
		return marginalCounterY.count(e);
	}
	
	@Override
	public void clear() {
		marginalCounterX.clear();
		marginalCounterY.clear();
		counter = new HashMap<Pair<T, T>, Double>();
	}
}
