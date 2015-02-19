package edu.jhu.marmota.util;

import java.util.HashMap;

import fig.basic.Pair;

public class LogPairCounter<T> extends AbstractCounter<Pair<T, T>> {
	protected LogCounter<T> marginalCounterX;
	protected LogCounter<T> marginalCounterY;
	protected boolean marginlock = false;
	protected boolean marginSwitchX = true;
	protected boolean marginSwitchY = true;
	
	public LogPairCounter() {
		counter = new HashMap<Pair<T, T>, Double>();
		marginalCounterX = new LogCounter<T>();
		marginalCounterY = new LogCounter<T>();
	}
	
	public LogPairCounter(boolean marginSwitchX, boolean marginSwitchY) {
		counter = new HashMap<Pair<T, T>, Double>();
		if (marginSwitchX) {
			marginalCounterX = new LogCounter<T>();
		}
		if (marginSwitchY) {
			marginalCounterY = new LogCounter<T>();
		}
		this.marginSwitchX = marginSwitchX;
		this.marginSwitchY = marginSwitchY;
	}

	@Override
	public double increment(Pair<T, T> e) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		double count = counter.get(e) != null? counter.get(e): Double.NEGATIVE_INFINITY;
		counter.put(e, LargeNumberArith.logadd(count, 1.0));
		if (marginSwitchX) {
			marginalCounterX.increment(e.getFirst());
		}
		if (marginSwitchY) {
			marginalCounterY.increment(e.getSecond());
		}
		return LargeNumberArith.logadd(count, 1.0);
	}

	@Override
	public double increment(Pair<T, T> e, double inc) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		double count = counter.get(e) != null? counter.get(e): Double.NEGATIVE_INFINITY;
		counter.put(e, LargeNumberArith.logadd(count, inc));
		if (marginSwitchX) {
			marginalCounterX.increment(e.getFirst(), inc);
		}
		if (marginSwitchY) {
			marginalCounterY.increment(e.getSecond(), inc);
		}
		return LargeNumberArith.logadd(count, inc);
	}

	@Override
	public double count(Pair<T, T> e) {
		return counter.get(e) != null? counter.get(e): Double.NEGATIVE_INFINITY;
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
					count = LargeNumberArith.logadd(count, counter.get(key));
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
					count = LargeNumberArith.logadd(count, counter.get(key));
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
