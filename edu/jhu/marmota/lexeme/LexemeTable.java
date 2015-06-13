package edu.jhu.marmota.lexeme;

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
 * The score used in lexeme table of Moses is real space. 
 * To ensure overflow safety, I transformed this into log space.
 * 
 * @author shuoyang
 *
 */
public class LexemeTable {
	
	public enum Type {
		f2e, e2f
	};
	Type type;
	
	private Map<Pair<String, String>, Double> table = new HashMap<Pair<String, String>, Double>();
	private MultiValueMap<String, String> f2e = null;
	private MultiValueMap<String, String> e2f = null;
	
	public LexemeTable(String dir, Type type) {
		LogInfo.begin_track("loading lexical translation table from file");
		this.type = type;
		if (type == Type.f2e) {
			f2e = new MultiValueMap<String, String>();
		}
		else if (type == Type.e2f) {
			e2f = new MultiValueMap<String, String>();
		}
		try {
			BufferedReader in = new BufferedReader(new FileReader(new File(dir)));
			String line = in.readLine();
			int linen = 0;
			while (line != null) {
				if (linen % 1000 == 0) {
					System.err.print(".");
				}
				String[] fields = line.split(" ");
				table.put(new Pair<String, String>(fields[1], fields[0]), Math.log(Double.valueOf(fields[2])));
				if (type == Type.f2e) {
					f2e.put(fields[1], fields[0]);
				}
				else if (type == Type.e2f) {
					e2f.put(fields[1], fields[0]);
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
	
	/**
	 * Caution: no matter what the direction is, 
	 * always supply the source lexeme as the first argument and target lexeme as the second
	 * 
	 * @param fr
	 * @param en
	 * @return
	 */
	public double score(String fr, String en) {
		Pair<String, String> lexpair;
		if (type == Type.f2e) {
			lexpair = new Pair<String, String>(fr, en);
		}
		else {
			lexpair = new Pair<String, String>(en, fr);
		}
		if (table.get(lexpair) != null) {
			return table.get(lexpair);
		}
		else {
			if (!transformable(fr)) {
				// translate unknown foreign word "as is"
				return 0.0;
			} else {
				return Double.NEGATIVE_INFINITY;
			}
		}
	}
	
	/**
	 * For f2e tables, find all lexemes corresponding to a foreign lexeme. 
	 * For e2f tables, find all lexemes corresponding to a English lexeme.
	 * @param from
	 * @return
	 */
	public Collection<String> transform(String from) {
		if (type == Type.f2e) {
			return f2e.getCollection(from);
		}
		else if (type == Type.e2f) {
			return e2f.getCollection(from);
		}
		return null;
	}
	
	/**
	 * For f2e tables, find whether there are lexeme corresponding to a foreign lexeme. 
	 * For e2f tables, find whether there are lexeme corresponding to a English lexeme.
	 * @param from
	 * @return
	 */
	public boolean transformable(String from) {
		if (type == Type.f2e) {
			return f2e.containsKey(from);
		}
		else if (type == Type.e2f) {
			return e2f.containsKey(from);
		}
		return false;
	}
}
