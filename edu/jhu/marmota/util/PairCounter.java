package edu.jhu.marmota.util;

import java.util.HashMap;

import fig.basic.Pair;

public class PairCounter<T> extends AbstractCounter<Pair<T, T>> {
	protected Counter<T> marginalCounterX;
	protected Counter<T> marginalCounterY;
	protected boolean marginlock = false;
	protected boolean marginSwitchX = true;
	protected boolean marginSwitchY = true;
	
	public PairCounter() {
		counter = new HashMap<Pair<T, T>, Double>();
		marginalCounterX = new Counter<T>();
		marginalCounterY = new Counter<T>();
	}
	
	public PairCounter(boolean marginSwitchX, boolean marginSwitchY) {
		counter = new HashMap<Pair<T, T>, Double>();
		if (marginSwitchX) {
			marginalCounterX = new Counter<T>();
		}
		if (marginSwitchY) {
			marginalCounterY = new Counter<T>();
		}
		this.marginSwitchX = marginSwitchX;
		this.marginSwitchY = marginSwitchY;
	}

	@Override
	public double increment(Pair<T, T> e) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		double count = counter.get(e) != null? counter.get(e): 0.0;
		counter.put(e, count + 1.0);
		if (marginSwitchX) {
			marginalCounterX.increment(e.getFirst());
		}
		if (marginSwitchY) {
			marginalCounterY.increment(e.getSecond());
		}
		return count + 1.0;
	}

	@Override
	public double increment(Pair<T, T> e, double inc) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		double count = counter.get(e) != null? counter.get(e): 0.0;
		counter.put(e, count + inc);
		if (marginSwitchX) {
			marginalCounterX.increment(e.getFirst(), inc);
		}
		if (marginSwitchY) {
			marginalCounterY.increment(e.getSecond(), inc);
		}
		return count + inc;
	}

	@Override
	public double count(Pair<T, T> e) {
		return counter.get(e) != null? counter.get(e): 0.0;
	}
	
	@Override
	public void set(Pair<T, T> k, Double v) {
		marginlock = true && (marginSwitchX || marginSwitchY);
		counter.put(k, v);
	}
	
	public void marginalize() {
		if (marginSwitchX) {
			marginalCounterX.clear();
		}
		if (marginSwitchY) {
			marginalCounterY.clear();
		}
		for (Pair<T, T> wp: counter.keySet()) {
			if (marginSwitchX) {
				marginalCounterX.increment(wp.getFirst(), counter.get(wp));
			}
			if (marginSwitchY) {
				marginalCounterY.increment(wp.getSecond(), counter.get(wp));
			}
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
		
		if (marginSwitchX) {
			return marginalCounterX.count(e);
		}
		else {
			double count = 0;
			for (Pair<T, T> key: counter.keySet()) {
				if (key.getFirst().equals(e)) {
					count += counter.get(key);
				}
			}
			return count;
		}
	}

	public double county(T e) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to access marginal count.");
		}
		
		if (marginSwitchY) {
			return marginalCounterY.count(e);
		}
		else {
			double count = 0;
			for (Pair<T, T> key: counter.keySet()) {
				if (key.getSecond().equals(e)) {
					count += counter.get(key);
				}
			}
			return count;
		}
	}
	
	@Override
	public void clear() {
		if (marginSwitchX) {
			marginalCounterX.clear();
		}
		if (marginSwitchY) {
			marginalCounterY.clear();
		}
		counter = new HashMap<Pair<T, T>, Double>();
	}
}

