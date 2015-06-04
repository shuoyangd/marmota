package edu.jhu.marmota.syntax.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.jhu.marmota.util.PennTreeReader;
import edu.jhu.marmota.util.Tree;

/**
 * Dependency tree (either rooted or not rooted, labeled or unlabeled)
 *
 * @author shuoyang
 */
public class DepTree extends Tree<DepNode> {
	
	private List<String> edgeLabels;

	/**
	 * whether the tree has a specific "ROOT" node (as default in stanford dependency)
	 */
	public final boolean rooted;

	/**
	 * To build a dependency tree, use the DepTreeBuilder instead.
	 * 
	 * @param self
	 */
	private DepTree(DepNode self, boolean rooted) {
		super(self);
		edgeLabels = new ArrayList<String>();
		this.rooted = rooted;
	}
	
	/**
	 * To build a dependency tree, use the DepTreeBuilder instead.
	 *
	 * @param self
	 * @param index
	 */
	private DepTree(DepNode self, int index, boolean rooted) {
		super(self, index);
		edgeLabels = new ArrayList<String>();
		this.rooted = rooted;
	}

	/**
	 * Build dependency tree from stanford dependency.
	 * The children are sorted by their index in the sentence.
	 * You may want to use -keepPunct option to avoid generating "standing alone" punctuation nodes.
	 * 
	 * @param constr
	 * @param depstr
	 * @return
	 */
	static public DepTree StanfordDepTreeBuilder(String[] constr, String[] depstr) {
		Tree<String> constree = PennTreeReader.ReadPennTree(String.join(" ", constr));
		List<String> tokens = constree.Terminals();
		List<String> postags = constree.preTerminals();

		Map<Integer, DepTree> nodeMap = new HashMap<Integer, DepTree>();
		nodeMap.put(0, new DepTree(new DepNode("ROOT", "ROOT"), 0, true));
		for (int i = 0; i < tokens.size(); i++) {
			nodeMap.put(i + 1, new DepTree(new DepNode(tokens.get(i), postags.get(i)), i + 1, true));
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
			sortChildren(node);
		}
		for (DepTree node: nodeMap.values()) {
			if (node.getParent() == null && !node.getChildren().isEmpty()) {
				return node;
			}
		}
		return null;
	}
	
	/**
	 * Sort children according to index.
	 * (insertion sort)
	 *
	 * @param head
	 */
	static private void sortChildren(DepTree head) {
		List<Tree<DepNode>> children = head.getChildren();
		for (int i = 1; i < children.size(); i++) {
			for (int j = 0; j < i; j++) {
				if (children.get(j).getIndex() > children.get(i).getIndex()) {
					Tree<DepNode> child = children.remove(i);
					children.add(j, child);
				}
			}
		}
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

