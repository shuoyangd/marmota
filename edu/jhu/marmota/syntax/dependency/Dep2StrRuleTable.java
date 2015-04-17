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
 * for example:
 * 
 * möchten/VV -> Ich/PRN kaffee/NN trinken/VV ||| x0 want x2 x1 ||| -2.718 ||| -2.718
 * 
 * For efficiency reasons, we don't extract all the lexicalized & unlexicalized rules as in (Xie, 2011). 
 * We just provide all the information we need and select what we need when doing rule-matching.
 * 
 * @author shuoyang
 *
 */
public class Dep2StrRuleTable {
	
	private Map<Dep2StrRule, Double> f2etable = new HashMap<Dep2StrRule, Double>();
	private Map<Dep2StrRule, Double> e2ftable = new HashMap<Dep2StrRule, Double>();
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
		DepNode srcLeft = new DepNode(srcLeftStr.split("\\/")[0], srcLeftStr.split("\\/")[1]);
		List<DepNode> srcRightList = new ArrayList<DepNode>();
		for (String srcRightStr: srcRightStrs) {
			srcRightList.add(new DepNode(srcRightStr.split("\\/")[0], srcRightStr.split("\\/")[1]));
		}
		DepNode[] srcRight = srcRightList.toArray(new DepNode[0]);
		DepRule src = new DepRule(srcLeft, srcRight);
		
		// parse target side rule
		String[] tar = fields[1].split(" ");
		
		Dep2StrRule newRule = new Dep2StrRule(src, tar);
		f2etable.put(newRule, Double.valueOf(fields[2]));
		e2ftable.put(newRule, Double.valueOf(fields[3]));
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
	
	public Collection<String[]> f2e(Dep2StrRule r) {
		return f2e.getCollection(r);
	}
	
	public boolean hasRule(Dep2StrRule r) {
		return f2e.containsKey(r);
	}
}
