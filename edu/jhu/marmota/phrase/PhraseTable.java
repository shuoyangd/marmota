package edu.jhu.marmota.phrase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.MultiValueMap;

import fig.basic.LogInfo;
import fig.basic.Pair;

/**
 * "php" means "phrase pair".
 * What did you say? Server-side scripting language?
 * @author shuoyang
 *
 */
public class PhraseTable {
	private Map<Pair<String, String>, Double> table = new HashMap<Pair<String, String>, Double>();
	private MultiValueMap<String, String> f2e = new MultiValueMap<String, String>();
	private int maxLengthFR, maxLengthEN;

	public PhraseTable(String dir) {
		LogInfo.begin_track("loading phrase table from file");
		try {
			BufferedReader in = new BufferedReader(new FileReader(new File(dir)));
			String line = in.readLine();
			int linen = 0;
			while (line != null) {
				if (linen % 1000 == 0) {
					System.err.print(".");
				}
				String[] fields = line.split(" \\|\\|\\| ");
				Pair<String, String> php = new Pair<String, String>(fields[0], fields[1]);
				table.put(php, Double.valueOf(fields[2]));
				f2e.put(fields[0], fields[1]);
				
				if (fields[0].split(" ").length > maxLengthFR) {
					maxLengthFR = fields[0].split(" ").length;
				}
				if (fields[1].split(" ").length > maxLengthEN) {
					maxLengthEN = fields[1].split(" ").length;
				}
				line = in.readLine();
				linen++;
			}
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		LogInfo.end_track();
	}
	
	public double score(String fr, String en) {
		Pair<String, String> php = new Pair<String, String>(fr, en);
		if (table.get(php) != null) {
			return table.get(php);
		}
		else {
			// translate unknown foreign word "as is"
			if (fr.split(" ").length == 1 && !hasPhrase(fr)) {
				return 0.0;
			}
			else {
				return Double.NEGATIVE_INFINITY;
			}
		}
	}
	
	public Collection<String> f2e(String fr) {
		return f2e.getCollection(fr);
	}
	
	public boolean hasPhrase(String fr) {
		return f2e.containsKey(fr);
	}
	
	public int maxLengthFR() {
		return maxLengthFR;
	}
	
	public int maxLengthEN() {
		return maxLengthEN;
	}
}
