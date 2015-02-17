package edu.jhu.clsp.marmota.util;

import java.util.HashMap;
import java.util.Set;

public abstract class AbstractCounter<T> {
	protected HashMap<T, Double> counter;
	
	abstract public double increment(T k);
	abstract public double increment(T k, double inc);
	abstract public double count(T k);
	abstract public void set(T k, Double v);

	public Set<T> keys() {
		return counter.keySet();
	}
	
	public boolean empty() {
		return counter.size() == 0;
	}
	
	public void clear() {
		counter = new HashMap<T, Double>();
	}
}
