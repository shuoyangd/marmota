package edu.jhu.marmota.syntax.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.jhu.marmota.util.PennTreeReader;
import edu.jhu.marmota.util.Strings;
import edu.jhu.marmota.util.Tree;

public class DepTree extends Tree<DepNode> {
	
	private List<String> edgeLabels;
	
	/**
	 * To build a dependency tree, use the DepTreeBuilder instead.
	 * 
	 * @param self
	 */
	private DepTree(DepNode self) {
		super(self);
	}
	
	/**
	 * To build a dependency tree, use the DepTreeBuilder instead.
	 * @param self
	 * @param index
	 */
	private DepTree(DepNode self, int index) {
		super(self, index);
	}

	/**
	 * Build dependency tree from stanford dependency. 
	 * You may want to use -keepPunct option to avoid generating "standing alone" punctuation nodes.
	 * 
	 * @param constr
	 * @param depstr
	 * @return
	 */
	static public DepTree DepTreeBuilder(String[] constr, String[] depstr) {
		Tree<String> constree = PennTreeReader.ReadPennTree(Strings.consolidate(constr));
		List<String> tokens = constree.Terminals();
		List<String> postags = constree.preTerminals();
		
		Map<Integer, DepTree> nodeMap = new HashMap<Integer, DepTree>();
		for (int i = 0; i < tokens.size(); i++) {
			nodeMap.put(i, new DepTree(new DepNode(tokens.get(i), postags.get(i)), i));
		}
		for (String line: depstr) {
			String GR = line.substring(0, line.indexOf("("));
			String tuple = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
			String[] indexedTokens = tuple.split(", ");
			// String head = indexedTokens[0].substring(0, indexedTokens[0].indexOf("-"));
			String headindex = indexedTokens[0].substring(indexedTokens[0].indexOf("-") + 1);
			// String dep = indexedTokens[1].substring(0, indexedTokens[1].indexOf("-"));
			String depindex = indexedTokens[1].substring(indexedTokens[1].indexOf("-") + 1);
			DepTree headNode = nodeMap.get(Integer.valueOf(headindex));
			DepTree depNode = nodeMap.get(Integer.valueOf(depindex));
			headNode.addChildren(depNode, GR);
			depNode.setParent(headNode);
		}
		for (DepTree node: nodeMap.values()) {
			if (node.getParent() != null && !node.getChildren().isEmpty()) {
				return node;
			}
		}
		return null;
	}
	
	public void addChildren(DepNode child, String label) {
		super.addChildren(child);
		edgeLabels.add(label);
	}
	
	public void addChildren(DepTree child, String label) {
		super.addChildren(child);
		edgeLabels.add(label);
	}
	
	public String getEdgeLabel(int index) {
		return edgeLabels.get(index);
	}
	
	public List<String> getEdgeLabels() {
		List<String> res = new ArrayList<String>();
		res.addAll(edgeLabels);
		return res;
	}
}