package edu.jhu.marmota.util;

import fig.basic.Pair;

public class TripleCounter<T> extends AbstractCounter<Triple<T, T, T>> {
	// Counter for XY and X
	PairCounter<T> marginalCounterXY;
	// Counter for YZ and Y
	PairCounter<T> marginalCounterYZ;
	// Counter for ZX and Z
	PairCounter<T> marginalCounterZX;
	// mask for single variable marginal counting
	boolean mask;
	// mask for pair marginal counting
	boolean[] mask2;
	boolean marginlock = false;
	
	public TripleCounter() {
		marginalCounterXY = new PairCounter<T> (true, false);
		marginalCounterYZ = new PairCounter<T> (true, false);
		marginalCounterZX = new PairCounter<T> (true, false);
	}
	
	public TripleCounter(boolean mask, boolean mask2) {
		if (mask2) {
			marginalCounterXY = new PairCounter<T> (mask, false);
			marginalCounterYZ = new PairCounter<T> (mask, false);
			marginalCounterZX = new PairCounter<T> (mask, false);
		}
		
		this.mask = mask;
		this.mask2 = new boolean[3];
		this.mask2[0] = mask2;
		this.mask2[1] = mask2;
		this.mask2[2] = mask2;
	}
	
	public TripleCounter(boolean mask, boolean maskxy, boolean maskyz, boolean maskzx) {
		if (maskxy) {
			marginalCounterXY = new PairCounter<T> (mask, false);
		}
		if (maskyz) {
			marginalCounterYZ = new PairCounter<T> (mask, false);
		}
		if (maskzx) {
			marginalCounterZX = new PairCounter<T> (mask, false);
		}
		
		this.mask = mask;
		this.mask2 = new boolean[3];
		this.mask2[0] = maskxy;
		this.mask2[1] = maskyz;
		this.mask2[2] = maskzx;
	}
	
	@Override
	public double increment(Triple<T, T, T> k) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		double count = counter.get(k) != null? counter.get(k) : 0.0;
		counter.put(k, count + 1.0);
		if (mask2[0]) {
			marginalCounterXY.increment(new Pair<T, T> (k.getFirst(), k.getSecond()));
		}
		if (mask2[1]) {
			marginalCounterXY.increment(new Pair<T, T> (k.getSecond(), k.getThird()));
		}
		if (mask2[2]) {
			marginalCounterZX.increment(new Pair<T, T> (k.getThird(), k.getFirst()));
		}
		return count + 1.0;
	}

	@Override
	public double increment(Triple<T, T, T> k, double inc) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		double count = counter.get(k) != null? counter.get(k) : 0.0;
		counter.put(k, count + inc);
		if (mask2[0]) {
			marginalCounterXY.increment(new Pair<T, T> (k.getFirst(), k.getSecond()), inc);
		}
		if (mask2[1]) {
			marginalCounterXY.increment(new Pair<T, T> (k.getSecond(), k.getThird()), inc);
		}
		if (mask2[2]) {
			marginalCounterZX.increment(new Pair<T, T> (k.getThird(), k.getFirst()), inc);
		}
		return count + inc;
	}

	@Override
	public double count(Triple<T, T, T> k) {
		return counter.get(k) != null? counter.get(k) : 0.0;
	}

	@Override
	public void set(Triple<T, T, T> k, Double v) {
		marginlock = true && (mask || mask2[0] || mask2[1] || mask2[2]);
		counter.put(k, v);
	}
	
	public void marginalize() {
		if (mask2[0]) {
			marginalCounterXY.clear();
		}
		if (mask2[1]) {
			marginalCounterYZ.clear();
		}
		if (mask2[2]) {
			marginalCounterZX.clear();
		}
		
		for (Triple<T, T, T> triple: counter.keySet()) {
			if (mask2[0]) {
				marginalCounterXY.increment(new Pair<T, T>(triple.getFirst(), triple.getSecond()), counter.get(triple));
			}
			if (mask2[1]) {
				marginalCounterYZ.increment(new Pair<T, T>(triple.getFirst(), triple.getSecond()), counter.get(triple));
			}
			if (mask2[2]) {
				marginalCounterYZ.increment(new Pair<T, T>(triple.getFirst(), triple.getSecond()), counter.get(triple));
			}
		}
		marginlock = false;
	}
	
	public boolean locked() {
		return marginlock;
	}
	
	public double countxy(Pair<T, T> xy) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		if (mask2[0]) {
			return marginalCounterXY.count(xy);
		}
		else {
			double count = 0;
			for (Triple<T, T, T> key: counter.keySet()) {
				Pair<T, T> keyxy = new Pair<T, T>(key.getFirst(), key.getSecond());
				if (keyxy.equals(xy)) {
					count += counter.get(key);
				}
			}
			return count;
		}
	}
	
	public double countyz(Pair<T, T> yz) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		if (mask2[1]) {
			return marginalCounterYZ.count(yz);
		}
		else {
			double count = 0;
			for (Triple<T, T, T> key: counter.keySet()) {
				Pair<T, T> keyyz = new Pair<T, T>(key.getSecond(), key.getThird());
				if (keyyz.equals(yz)) {
					count += counter.get(key);
				}
			}
			return count;
		}
	}
	
	public double countzx(Pair<T, T> zx) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		if (mask2[2]) {
			return marginalCounterZX.count(zx);
		}
		else {
			double count = 0;
			for (Triple<T, T, T> key: counter.keySet()) {
				Pair<T, T> keyzx = new Pair<T, T>(key.getThird(), key.getFirst());
				if (keyzx.equals(zx)) {
					count += counter.get(key);
				}
			}
			return count;
		}
	}
	
	public double countx(T x) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		if (mask && mask2[0]) {
			return marginalCounterXY.countx(x);
		}
		else {
			double count = 0;
			for (Triple<T, T, T> key: counter.keySet()) {
				if (key.getFirst().equals(x)) {
					count += counter.get(key);
				}
			}
			return count;
		}
	}
	
	public double county(T y) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		if (mask && mask2[1]) {
			return marginalCounterYZ.countx(y);
		}
		else {
			double count = 0;
			for (Triple<T, T, T> key: counter.keySet()) {
				if (key.getSecond().equals(y)) {
					count += counter.get(key);
				}
			}
			return count;
		}
	}
	
	public double countz(T z) {
		if (marginlock) {
			throw new IllegalStateException("Counter is locked when trying to do increment.");
		}
		
		if (mask && mask2[1]) {
			return marginalCounterZX.countx(z);
		}
		else {
			double count = 0;
			for (Triple<T, T, T> key: counter.keySet()) {
				if (key.getSecond().equals(z)) {
					count += counter.get(key);
				}
			}
			return count;
		}
	}
}
