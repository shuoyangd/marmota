package edu.jhu.marmota.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.jhu.marmota.util.Strings;
import fig.basic.LogInfo;

/**
 * Read language model from arpa-format dump file and score strings.
 * @author shuoyang
 *
 */
public class ARPA implements LM{

	private Map<String, Double> model = new HashMap<String, Double>();
	private Map<String, Double> backoff = new HashMap<String, Double>();
	private final String start = "<s>";
	private final String end = "</s>";
	private int ngram;
	
	public ARPA() {
		
	}
	
	public ARPA(int ngram) {
		this.ngram = ngram;
	}
	
	public ARPA (String dir) {
		try {
			this.ngram = load(dir);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ARPA (String dir, int ngram) {
		try {
			load(dir);
			this.ngram = ngram;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public int load(String dir) throws IOException {
		LogInfo.begin_track("loading language model from ARPA dump");
		BufferedReader in = new BufferedReader(new FileReader(new File(dir)));
		String line = in.readLine();
		int linen = 0;
		int ngram = 0;
		while (line != null) {
			if (linen % 1000 == 0) {
				System.err.print(".");
			}
			if (line.startsWith("ngram")) {
				line = in.readLine();
				ngram++;
				continue;
			}
			if (line.split("\t").length > 1 && !line.startsWith("ngram")) {
				String[] cells = line.split("\t");
				model.put(cells[1], Double.valueOf(cells[0]));
				if (cells.length >= 3) {
					backoff.put(cells[1], Double.valueOf(cells[2]));
				}
			}
			line = in.readLine();
			linen++;
		}
		in.close();
		LogInfo.end_track();
		
		return ngram;
	}
	
	@Override
	public String begin() {
		return start;
	}

	@Override
	public String end(String... words) {
		return end;
	}
	
	@Override
	public double score(String babble) {
		String[] words = babble.split(" ");
		double score = 0.0;
		for (int i = 1; i <= words.length; i++) {
			if (i - ngram < 0) {
				String[] scoringGrams = new String[i + 1];
				scoringGrams[0] = "<s>";
				for (int j = 0; j < i; j++) {
					scoringGrams[j + 1] = words[j];
				}
				score += ngramScore(scoringGrams);
			}
			else {
				score += ngramScore(Arrays.copyOfRange(words, i - ngram, i));
			}
		}
		return score;
	}
	
	public double localscore(String babble) {
		String[] words = babble.split(" ");
		double score = 0.0;
		for (int i = 0; i < words.length - ngram + 1; i++) {
			score += ngramScore(Arrays.copyOfRange(words, i, i + ngram));
		}
		if (words.length < ngram) {
			score += ngramScore(Arrays.copyOfRange(words, words.length - (words.length % ngram), words.length));
		}
		return score;
	}

	private double ngramScore(String[] words) {
		if (words.length > ngram) {
			throw new IllegalArgumentException("not correct length words to be scored!");
		}
		
		double score = 0.0;
		while (words.length > 0) {
			if (model.containsKey(Strings.consolidate(words))) {
				return score += model.get(Strings.consolidate(words));
			}
			else if (backoff.containsKey(Strings.consolidate(Arrays.copyOfRange(words, 0, words.length - 1))) && words.length > 1) {
				score += backoff.get(Strings.consolidate(Arrays.copyOfRange(words, 0, words.length - 1)));
			}
			words = Arrays.copyOfRange(words, 1, words.length);
		}
		score += model.get("<unk>");
		return score;
	}
	
	@Override
	public int ngram() {
		return ngram;
	}
}
