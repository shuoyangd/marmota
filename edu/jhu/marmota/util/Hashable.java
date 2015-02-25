package edu.jhu.marmota.util;

public interface Hashable<T> {
	
	public boolean equals(Object other);
	public int hashCode();
}
