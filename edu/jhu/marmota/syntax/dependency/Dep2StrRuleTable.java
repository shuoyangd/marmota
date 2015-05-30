package edu.jhu.marmota.syntax.dependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.MultiValueMap;

/**
 * Each line of the disk rule table should look like:
 * 
 * source rule ||| target string ||| f2e score e2f score ||| alignments
 * 
 * Rules can either be lexical or postag rules. 
 * If it is a lexical rule, the pos-tag position should be filled with a *, for example:
 * 
 * möchten/* -> Ich/* kaffee/* trinken/* ||| $x want $x $x ||| -0.693 -0.693 -0.693 -0.693 ||| 0-1 1-0 2-3 3-2
 *
 * On the source side, the index always starts with the head from 0.
 * On the target side, the index starts with the first token from 0.
 * 
 * For rules with pos-tag, vice versa.
 * 
 * @author shuoyang
 *
 */
public class Dep2StrRuleTable {

	private String dir;
	private Map<Dep2StrRule, RuleScore> scoretable = new HashMap<Dep2StrRule, RuleScore>();
	private MultiValueMap<DepRule, String[]> f2e = new MultiValueMap<DepRule, String[]>();

	public Dep2StrRuleTable(String dir) {
		this.dir = dir;
	}

	public void load() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(new File(dir)));
			String line = in.readLine();
			while (line != null) {
				addRule(line);
			}
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addRule(String record) {
		String[] fields = record.split(" \\|\\|\\| ");
		
		// parse source side rule
		String srcLeftStr = fields[0].split(" -> ")[0];
		String[] srcRightStrs = fields[0].split(" -> ")[1].split(" ");
		String token = null, postag = null;
		if (!srcLeftStr.split("\\/")[0].equals("*")) {
			token = srcLeftStr.split("\\/")[0];
		}
		if (!srcLeftStr.split("\\/")[1].equals("*")) {
			postag = srcLeftStr.split("\\/")[1];
		}
		DepNode srcLeft = new DepNode(token, postag);
		List<DepNode> srcRightList = new ArrayList<DepNode>();
		for (String srcRightStr: srcRightStrs) {
			if (!srcLeftStr.split("\\/")[0].equals("*")) {
				token = srcRightStr.split("\\/")[0];
			}
			if (!srcLeftStr.split("\\/")[1].equals("*")) {
				postag = srcRightStr.split("\\/")[1];
			}
			srcRightList.add(new DepNode(token, postag));
		}
		DepNode[] srcRight = srcRightList.toArray(new DepNode[srcRightList.size()]);
		DepRule src = new DepRule(srcLeft, srcRight);
		
		// parse target side rule
		String[] tar = fields[1].split(" ");

		Dep2StrRule newRule = new Dep2StrRule(src, tar, Dep2StrRule.buildAlignment(fields[3]));
		String[] scoreTokens = fields[2].split(" ");
		RuleScore scores = new RuleScore(Double.valueOf(scoreTokens[0]), Double.valueOf(scoreTokens[1]),
				Double.valueOf(scoreTokens[2]), Double.valueOf(scoreTokens[3]));
		scoretable.put(newRule, scores);
		f2e.put(src, tar);
	}

	public double f2escore(Dep2StrRule r) {
		if (scoretable.get(r) != null) {
			return scoretable.get(r).f2escore;
		}
		else {
			return Double.NEGATIVE_INFINITY;
		}
	}
	
	public double e2fscore(Dep2StrRule r) {
		if (scoretable.get(r) != null) {
			return scoretable.get(r).e2fscore;
		}
		else {
			return Double.NEGATIVE_INFINITY;
		}
	}
	
	public double lexf2escore(Dep2StrRule r) {
		if (scoretable.get(r) != null) {
			return scoretable.get(r).lexf2escore;
		}
		else {
			return Double.NEGATIVE_INFINITY;
		}
	}
	
	public double lexe2fscore(Dep2StrRule r) {
		if (scoretable.get(r) != null) {
			return scoretable.get(r).lexe2fscore;
		}
		else {
			return Double.NEGATIVE_INFINITY;
		}
	}
	
	public Collection<String[]> f2e(DepRule r) {
		return f2e.getCollection(r);
	}
	
	public boolean hasRule(Dep2StrRule r) {
		return scoretable.containsKey(r);
	}
}

