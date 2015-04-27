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
 * source rule ||| target string ||| f2e score ||| e2f score
 * 
 * Rules can either be lexical or postag rules. 
 * If it is a lexical rule, the pos-tag position should be filled with a *, for example:
 * 
 * möchten/* -> Ich/* kaffee/* trinken/* ||| x0 want x2 x1 ||| -0.693 -0.693 -0.693 -0.693
 * 
 * For rules with pos-tag, vice versa.
 * 
 * @author shuoyang
 *
 */
public class Dep2StrRuleTable {
	
	private Map<Dep2StrRule, Double> f2etable = new HashMap<Dep2StrRule, Double>();
	private Map<Dep2StrRule, Double> lexf2etable = new HashMap<Dep2StrRule, Double>();
	private Map<Dep2StrRule, Double> e2ftable = new HashMap<Dep2StrRule, Double>();
	private Map<Dep2StrRule, Double> lexe2ftable = new HashMap<Dep2StrRule, Double>();
	private MultiValueMap<DepRule, String[]> f2e = new MultiValueMap<DepRule, String[]>();
	
	public Dep2StrRuleTable(String dir) {
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
		DepNode[] srcRight = srcRightList.toArray(new DepNode[0]);
		DepRule src = new DepRule(srcLeft, srcRight);
		
		// parse target side rule
		String[] tar = fields[1].split(" ");
		
		Dep2StrRule newRule = new Dep2StrRule(src, tar);
		String[] scores = fields[2].split(" ");
		f2etable.put(newRule, Double.valueOf(scores[0]));
		lexf2etable.put(newRule, Double.valueOf(scores[1]));
		e2ftable.put(newRule, Double.valueOf(scores[2]));
		lexe2ftable.put(newRule, Double.valueOf(scores[3]));
		f2e.put(src, tar);
	}
	
	public double f2escore(Dep2StrRule r) {
		if (f2etable.get(r) != null) {
			return f2etable.get(r);
		}
		else {
			return Double.NEGATIVE_INFINITY;
		}
	}
	
	public double e2fscore(Dep2StrRule r) {
		if (e2ftable.get(r) != null) {
			return e2ftable.get(r);
		}
		else {
			return Double.NEGATIVE_INFINITY;
		}
	}
	
	public double lexf2escore(Dep2StrRule r) {
		if (lexf2etable.get(r) != null) {
			return lexf2etable.get(r);
		}
		else {
			return Double.NEGATIVE_INFINITY;
		}
	}
	
	public double lexe2fscore(Dep2StrRule r) {
		if (lexe2ftable.get(r) != null) {
			return lexe2ftable.get(r);
		}
		else {
			return Double.NEGATIVE_INFINITY;
		}
	}
	
	public Collection<String[]> f2e(DepRule r) {
		return f2e.getCollection(r);
	}
	
	public boolean hasRule(Dep2StrRule r) {
		return f2e.containsKey(r);
	}
}
