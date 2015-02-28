package edu.jhu.marmota.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.jhu.marmota.lm.ARPA;
import edu.jhu.marmota.lm.LM;
import edu.jhu.marmota.phrase.PhraseTable;
import fig.basic.Pair;

/**
 * This is a naive beam search stack decoder for machine translation.
 * It is "naive" because:
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
	private LM lm;
	private int distortionLimit = 5;
	private int maxPhraseLength = 20;

	public NaiveStackDecoder(String ptdir, String lmdir) {
		this.ptdir = ptdir;
		this.lmdir = lmdir;
	}

	public NaiveStackDecoder(String ptdir, String lmdir, int distortionLimit) {
		this.ptdir = ptdir;
		this.lmdir = lmdir;
		this.distortionLimit = distortionLimit;
	}

	@Override
	public void init() {
		lm = new ARPA(ptdir);
		pt = new PhraseTable(lmdir);
	}

	@Override
	public String decode(String input) {
		// init stacks
		String[] tokens = input.split(" ");
		List<NaiveHypothesisStack<NaiveHypothesis>> stacks = new ArrayList<NaiveHypothesisStack<NaiveHypothesis>>();
		for (int i = 0; i < tokens.length + 1; i++) {
			stacks.add(new NaiveHypothesisStack<NaiveHypothesis>());
		}
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
		
		// when i words are translated
		for (int i = 0; i < tokens.length; i++) {
			NaiveHypothesisStack<NaiveHypothesis> stack = stacks.get(i);
			// for each hypothesis in the stack
			for (Hypothesis hypo : stack) {
				/**
				 * collecting translation options
				 * when constructing new hypothesis, we just translate ONE MORE PHRASE than the current hypothesis
				 */
				// I just cannot get the iterator to return NaiveHypothesis, damn you JVM
				NaiveHypothesis naivehypo = (NaiveHypothesis) hypo;
				int center = naivehypo.lastTranslatedIndex;
				int start, end;
				if (center == -1) {
					start = 0;
					end = distortionLimit;
				}
				else {
					start = Math.max(0, center - distortionLimit);
					end = Math.min(tokens.length, center + distortionLimit);
				}
				// for each start position of the phrase that falls into the beam
				for (int j = start; j < end; j++) {
					// you cannot start with some token that has already been translated
					if (naivehypo.state[j]) {
						continue;
					}
					// for each phrase length that is allowed
					for (int k = 1; k < Math.min(maxPhraseLength, tokens.length - start); k++) {
						// if there are translated tokens in between, don't expand anymore
						if (naivehypo.state[j + k]) {
							break;
						}
						
						String expandingForeignPhrase = consolidate(Arrays.copyOfRange(tokens, j, j + k));
						Collection<String> expandingEnglishPhrases = options.get(new Pair<Integer, Integer>(j, j + k));
						
						// for each translation option collected, expand the hypothesis
						for (String expandingEnglishPhrase: expandingEnglishPhrases) {					
							// deal with pt score
							double ptScore = pt.score(expandingForeignPhrase, expandingEnglishPhrase);
							
							// deal with lm score
							double lmScore;
							String[] oldHistory = naivehypo.history;
							String[] englishPhraseTokens = expandingEnglishPhrase.split(" ");
							String[] newHistory = (consolidate(oldHistory) + " " + consolidate(englishPhraseTokens)).split(" ");
							int ehlen = newHistory.length;
							if (ehlen == 1) {
								lmScore = lm.score(lm.begin(), newHistory[0]);
							}
							else if (ehlen == 2) {
								lmScore = lm.score(lm.begin(), newHistory[0], newHistory[1]);
							}
							else {
								lmScore = lm.score(newHistory[ehlen - 3], newHistory[ehlen - 2], newHistory[ehlen - 1]);
							}
							
							// deal with state
							boolean[] newState = naivehypo.state;
							for (int l = j; l < j + k; l++) {
								newState[l] = true;
							}
							
							// push a new hypothesis
							NaiveHypothesis newHypo = new NaiveHypothesis(newState, ptScore + lmScore, expandingEnglishPhrase, newHistory, j + k - 1);
							stacks.get(i + k).push(newHypo);
						}
						// prune the stack
						stacks.get(i + k).prune();
					}
				}
			}
		}
		
		// get best translation possible
		return consolidate(stacks.get(stacks.size() - 1).pop().history);
	}
	
	private String consolidate(String[] tokens) {
		if (tokens.length == 0) {
			return "";
		}
		else {
			String res = tokens[0];
			for (int i = 1; i < tokens.length; i++) {
				res += (" " + tokens[i]);
			}
			return res;
		}
	}
}
