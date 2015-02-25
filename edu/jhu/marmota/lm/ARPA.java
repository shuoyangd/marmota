package edu.jhu.marmota.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ARPA implements LM{

	private Map<String[], Double> model = new HashMap<String[], Double>();
	private Map<String[], Double> backoff = new HashMap<String[], Double>();
	private final String start = "<s>";
	private final String end = "</s>";
	
	/**
	 * don't use constructor, use load method instead
	 */
	private ARPA () {
		
	}
	
	@Override
	public LM load(String dir) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(new File(dir)));
		ARPA lm = new ARPA();
		String line = in.readLine();
		while (line != null) {
			if (line.split(" ").length > 1 && !line.startsWith("ngram")) {
				String[] cells = line.split("\t");
				String[] words = cells[1].split(" ");
				lm.model.put(words, Double.valueOf(cells[0]));
				if (cells.length >= 3) {
					lm.backoff.put(words, Double.valueOf(cells[2]));
				}
			}
		}
		in.close();
		return lm;
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
	public double score(String... words) {
		String[] wordarray = words;
		double score = model.get(wordarray);
		for (int i = 0; i < wordarray.length; i++) {
			score += backoff.get(Arrays.copyOfRange(wordarray, i, wordarray.length));
		}
		return score;
	}	
}
