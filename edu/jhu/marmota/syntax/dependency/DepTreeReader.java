package edu.jhu.marmota.syntax.dependency;

import edu.jhu.marmota.util.PennTreeReader;
import edu.jhu.marmota.util.Tree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: the type hierarchy is not perfect here.
 * - PennTreeReader should be replaced with more general "ConstTreeReader"?
 * - This class should be renamed with "TupleDepTreeReader" and inherits an more general "DepTreeReader"?
 * ('cause we may someday need to write "CoNLLDepTreeReader")
 *
 * @author shuoyang
 */
public class DepTreeReader {
	
	BufferedReader treeReader;
	
	public DepTreeReader(String tree) throws FileNotFoundException {
		this.treeReader = new BufferedReader(new FileReader(new File(tree)));
	}

	/**
	 *
	 * @return
	 * @throws IOException
	 */
	public DepTree read() throws IOException {
		List<String> constr = new ArrayList<String>();
		List<String> depstr = new ArrayList<String>();
		String currentString = treeReader.readLine();
		// reaching EOF
		if (currentString == null) {
			return null;
		}

		while (!currentString.trim().equals("")) {
			constr.add(currentString);
			currentString = treeReader.readLine();
		}
		currentString = treeReader.readLine();
		while (!currentString.trim().equals("")) {
			depstr.add(currentString);
			currentString = treeReader.readLine();
		}
		return StanfordDepTreeBuilder(PennTreeReader.ReadPennTree(String.join(" ", constr.toArray(new String[constr.size()]))),
				depstr.toArray(new String[depstr.size()]));
	}

	/**
	 * Build dependency tree from stanford dependency.
	 * The children are sorted by their index in the sentence.
	 * You may want to use -keepPunct option to avoid generating "standing alone" punctuation nodes.
	 *
	 * @param constree
	 * @param depstr
	 * @return
	 */
	static public DepTree StanfordDepTreeBuilder(Tree<String> constree, String[] depstr) {
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
			String headindex = indexedTokens[0].substring(indexedTokens[0].indexOf("-") + 1);
			String depindex = indexedTokens[1].substring(indexedTokens[1].indexOf("-") + 1);
			DepTree headNode = nodeMap.get(Integer.valueOf(headindex));
			DepTree depNode = nodeMap.get(Integer.valueOf(depindex));
			headNode.addChildren(depNode, GR);
			depNode.setParent(headNode);
		}
		for (DepTree node: nodeMap.values()) {
			sortChildren(node.getChildren());
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
	 * @param children
	 */
	static private void sortChildren(List<Tree<DepNode>> children) {
		for (int i = 1; i < children.size(); i++) {
			for (int j = 0; j < i; j++) {
				if (children.get(j).getIndex() > children.get(i).getIndex()) {
					Tree<DepNode> child = children.remove(i);
					children.add(j, child);
				}
			}
		}
	}

	public void close() throws IOException {
		treeReader.close();
	}
}
