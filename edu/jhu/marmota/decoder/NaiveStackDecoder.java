package edu.jhu.marmota.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.jhu.marmota.lm.ARPA;
import edu.jhu.marmota.phrase.PhraseTable;
import edu.jhu.marmota.util.Strings;
import fig.basic.Pair;

/**
 * This is a naive beam search stack decoder for machine translation. It is "naive" because: 
 * 1. we do not keep a back pointer during decoding
 * 2. we keep all the translation history in the hypothesis
 * 3. we just select the best able hypothesis from the final stack
 * 
 * @author shuoyang
 * 
 */
public class NaiveStackDecoder implements AbstractDecoder {
	private String ptdir, lmdir;
	private PhraseTable pt;
	private ARPA lm;
	private int maxSize = 100;
	private int distortionLimit = 5;
	private int maxPhraseLength = 5;

	public NaiveStackDecoder(String ptdir, String lmdir) {
		this.ptdir = ptdir;
		this.lmdir = lmdir;
	}

	public NaiveStackDecoder(String ptdir, String lmdir, int maxSize, int distortionLimit, int maxPhraseLength) {
		this.ptdir = ptdir;
		this.lmdir = lmdir;
		this.maxSize = maxSize;
		this.distortionLimit = distortionLimit;
		this.maxPhraseLength = maxPhraseLength;
	}

	@Override
	public void init() {
		lm = new ARPA(lmdir);
		pt = new PhraseTable(ptdir);
		lm.score("what happended last Tuesday </s>");
	}

	@Override
	public String decode(String input) {
		// init stacks
		String[] tokens = input.split(" ");
		List<NaiveHypothesisStack<NaiveHypothesis>> stacks = new ArrayList<NaiveHypothesisStack<NaiveHypothesis>>();
		for (int i = 0; i < tokens.length + 1; i++) {
			stacks.add(new NaiveHypothesisStack<NaiveHypothesis>(maxSize));
		}
		stacks.get(0).push(new NaiveHypothesis(new boolean[tokens.length], 0.0, 0.0, "", new String[0], -1));
		// pre-collect translation options
		// we want to know for each position i, what translations can we find for a length n foreign phrase tokens[i:i + n]
		Map<Pair<Integer, Integer>, Collection<String>> options = new HashMap<Pair<Integer, Integer>, Collection<String>>();
		for (int i = 0; i < tokens.length; i++) {
			// j is the length of phrase we are trying to find
			String phrase = tokens[i];
			for (int j = 1; j <= Math.min(tokens.length - i, maxPhraseLength); j++) {
				if (j > 1) {
					phrase += (" " + tokens[i + j - 1]);
				}
				if (pt.hasPhrase(phrase)) {
					options.put(new Pair<Integer, Integer>(i, j), pt.f2e(phrase));
				}
			}
		}

		// this map contains all the calculated future cost
		Map<Pair<Integer, Integer>, Double> futureCosts = new HashMap<Pair<Integer, Integer>, Double>();

		// when i words are translated
		for (int i = 0; i < tokens.length; i++) {
			NaiveHypothesisStack<NaiveHypothesis> stack = stacks.get(i);
			// for each hypothesis in the stack
			for (Hypothesis hypo : stack) {
				/**
				 * collecting translation options when constructing new hypothesis, we just translate ONE MORE PHRASE than the current hypothesis
				 */
				// I just cannot get the iterator to return NaiveHypothesis, damn you JVM
				NaiveHypothesis naivehypo = (NaiveHypothesis) hypo;
				int center = naivehypo.lastTranslatedIndex + 1;
				int start, end;
				if (center == -1) {
					start = 0;
					end = Math.min(tokens.length, distortionLimit);
				}
				else {
					start = Math.max(0, center - distortionLimit);
					end = Math.min(tokens.length, center + distortionLimit);
				}
				
				// find left-most uncovered position to avoid dead end (Bisazza et al. 2015)
				int leftmost = 0;
				for (int l = 0; l < naivehypo.state.length; l++) {
					if (!naivehypo.state[l]) {
						leftmost = l;
						break;
					}
				}
				
				// for each start position of the phrase that falls into the beam
				for (int j = start; j < Math.min(end, leftmost + distortionLimit); j++) {
					// you cannot start with some token that has already been translated
					if (naivehypo.state[j]) {
						continue;
					}
					
					// for each phrase length that is allowed
					for (int k = 1; k <= Math.min(Math.min(maxPhraseLength, tokens.length - j), leftmost + distortionLimit - j + 1); k++) {
						// if there are translated tokens in between, don't expand anymore
						if (naivehypo.state[j + k - 1]) {
							break;
						}

						String expandingForeignPhrase = Strings.consolidate(Arrays.copyOfRange(tokens, j, j + k));
						Collection<String> expandingEnglishPhrases = options.get(new Pair<Integer, Integer>(j, k));

						if (expandingEnglishPhrases == null && k == 1) {
							expandingEnglishPhrases = new HashSet<String>();
							expandingEnglishPhrases.add(expandingForeignPhrase);
						}

						// deal with state
						boolean[] newState = naivehypo.state.clone();
						for (int l = j; l < j + k; l++) {
							newState[l] = true;
						}
						
						// deal with score
						double futureCost = 0.0;
						int futureCostStart = 0;
						boolean inUntranslatedSpan = false;
						for (int l = 0; l < tokens.length; l++) {
							if (newState[l] && inUntranslatedSpan) {
								inUntranslatedSpan = false;
								futureCost += futureCost(futureCosts, tokens, futureCostStart, l);
							}
							else if (!newState[l] && !inUntranslatedSpan) {
								futureCostStart = l;
								inUntranslatedSpan = true;
							}
						}
						if (!newState[tokens.length - 1] && inUntranslatedSpan) {
							futureCost += futureCost(futureCosts, tokens, futureCostStart, tokens.length);
						}

						// for each translation option collected, expand the hypothesis
						if (expandingEnglishPhrases != null) {
							for (String expandingEnglishPhrase : expandingEnglishPhrases) {
								// deal with pt score
								double ptScore = naivehypo.ptScore + pt.score(expandingForeignPhrase, expandingEnglishPhrase);
								if (ptScore == Double.NEGATIVE_INFINITY)
									continue;

								// deal with lm score
								String[] oldHistory = naivehypo.history;
								String[] newHistory;
								if (oldHistory.length > 0) {
									newHistory = (Strings.consolidate(oldHistory) + " " + expandingEnglishPhrase).split(" ");
								}
								else {
									newHistory = expandingEnglishPhrase.split(" ");
								}
								double lmScore;
								if (i + k == tokens.length - 1) {
									lmScore = lm.score(Strings.consolidate(newHistory) + " "+ lm.end());
								}
								else {
									lmScore = lm.score(Strings.consolidate(newHistory));
								}

								double score = ptScore + lmScore - Math.abs(j - naivehypo.lastTranslatedIndex - 1) + futureCost;
								// System.out.println(ptScore);
								// System.out.println(lmScore);
								// System.out.println(Math.abs(j - naivehypo.lastTranslatedIndex - 1));
								// System.out.println(futureCost);

								// push a new hypothesis
								NaiveHypothesis newHypo = new NaiveHypothesis(newState, score, ptScore, expandingEnglishPhrase, newHistory, j + k - 1);
								stacks.get(i + k).push(newHypo);
//								System.out.println(newHypo.toString());
							}
							// prune the stack
							stacks.get(i + 1).prune();
						}
					}
				}
			}
			
		}

		// get best translation possible
		NaiveHypothesis winner = stacks.get(stacks.size() - 1).pop();
		if (winner == null) {
			return "SEARCH ERROR";
		}
		return Strings.consolidate(winner.history);
	}
	
	private double futureCost(Map<Pair<Integer, Integer>, Double> costs, String[] input, int i, int j) {
		double bestScore = Double.NEGATIVE_INFINITY;
		if (costs.get(new Pair<Integer, Integer>(i, j)) != null) {
			return costs.get(new Pair<Integer, Integer>(i, j));
		}
		if (j - i == 1) {
			String phrase = input[i];
			Collection<String> translations = pt.f2e(phrase);
			if (translations != null) {
				for (String translation : translations) {
					if (bestScore < pt.score(phrase, translation) + lm.localscore(translation)) {
						bestScore = pt.score(phrase, translation) + lm.localscore(translation);
					}
				}
				costs.put(new Pair<Integer, Integer>(i, j), bestScore);
				return bestScore;
			}
			else {
				return 0.0;
			}
		}

		String phrase = Strings.consolidate(Arrays.copyOfRange(input, i, j));
		Collection<String> translations = pt.f2e(phrase);
		if (translations != null) {
			for (String translation : translations) {
				if (bestScore < pt.score(phrase, translation) + lm.localscore(translation)) {
					bestScore = pt.score(phrase, translation) + lm.localscore(translation);
				}
			}
		}
		for (int k = i + 1; k < j; k++) {
			double fik;
			double fkj;
			if (costs.get(new Pair<Integer, Integer>(i, k)) == null) {
				fik = futureCost(costs, input, i, k);
			}
			else {
				fik = costs.get(new Pair<Integer, Integer>(i, k));
			}
			if (fik == Double.NEGATIVE_INFINITY) {
				continue;
			}

			if (costs.get(new Pair<Integer, Integer>(k, j)) == null) {
				fkj = futureCost(costs, input, k, j);
			}
			else {
				fkj = costs.get(new Pair<Integer, Integer>(k, j));
			}
			if (fkj == Double.NEGATIVE_INFINITY) {
				continue;
			}

			if (bestScore < fik + fkj) {
				bestScore = fik + fkj;
			}
		}
		// System.out.println(new Pair<Integer, Integer>(i, j).toString() + String.valueOf(bestScore));
		costs.put(new Pair<Integer, Integer>(i, j), bestScore);
		return bestScore;
	}
}
