package edu.jhu.marmota.alignment;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.map.MultiValueMap;

import fig.basic.Pair;

public class WordAlignedSentencePair {
	
	public String[] f;
	public String[] e;
	public MultiValueMap<Integer, Integer> f2e;
	public MultiValueMap<Integer, Integer> e2f;
	
	public WordAlignedSentencePair(String[] f, String[] e, List<Pair<Integer, Integer>> alignments) {
		this.f = f;
		this.e = e;
		f2e = new MultiValueMap<Integer, Integer>();
		e2f = new MultiValueMap<Integer, Integer>();
		for (Pair<Integer, Integer> alignment: alignments) {
			f2e.put(alignment.getFirst(), alignment.getSecond());
			e2f.put(alignment.getSecond(), alignment.getFirst());
		}
	}
	
	public WordAlignedSentencePair(String f, String e, List<Pair<Integer, Integer>> alignments) {
		this.f = f.split(" ");
		this.e = e.split(" ");
		f2e = new MultiValueMap<Integer, Integer>();
		e2f = new MultiValueMap<Integer, Integer>();
		for (Pair<Integer, Integer> alignment: alignments) {
			f2e.put(alignment.getFirst(), alignment.getSecond());
			e2f.put(alignment.getSecond(), alignment.getFirst());
		}
	}
	
	public Collection<Integer> f2e(int findex) {
		return f2e.getCollection(findex);
	}
	
	public Collection<Integer> e2f(int eindex) {
		return e2f.getCollection(eindex);
	}
}
