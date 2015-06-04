package edu.jhu.marmota.util;

import java.util.ArrayList;
import java.util.List;
/**
 * This is a refactored code originally improvised in LinguaView (as TSNodeLabel).
 * 
 * @author shuoyang
 *
 */
public class PennTreeReader {

	static public class Index {
		public int val;
	}

	static public Tree<String> ReadPennTree(String PennTree) {
		Tree<String> root;
		
		PennTree = PennTree.replaceAll("[\n\t\\ ]+", " ");
		PennTree = PennTree.trim();
		if(PennTree.matches("^\\( *\\)$") || PennTree.equals("")) {
			root = new Tree<String>(null);
			root.setParent(null);
			return root;
		}
		
		if (PennTree.startsWith("( (")) {
			PennTree = PennTree.substring(PennTree.indexOf('(') + 1, PennTree.lastIndexOf(')')).trim();
		}
		//from here the PennTree string cannot be revised
		//deal with label & parent & isLexical
		root = new Tree<String>(PennTree.substring(PennTree.indexOf('(') + 1, PennTree.indexOf(' ')));
		root.setParent(null);
		//deal with daughters
		List<Tree<String>> L = new ArrayList<Tree<String>>();
		Index currentPos = new Index();
		currentPos.val = findNearestNonSpace(PennTree, PennTree.indexOf(' '));
		while(currentPos.val < PennTree.length()) {
			if (PennTree.charAt(currentPos.val) == ')') {
				break;
			}
			else if(PennTree.charAt(currentPos.val) != ' ') {
				Tree<String> temp = ReadPennTree(PennTree, currentPos);
				temp.setParent(root);
				L.add(temp);
			}
			else {
				currentPos.val++;
			}
		}
		if (!L.isEmpty()) {
			for (Tree<String> child: L) {
				root.addChildren(child);
			}
		}
		return root;
	}
	
	/**
	 * Construct all the non-root TSNodeLabel recursively.
	 * @param PennTree
	 * @param currentPos
	 */
	static private Tree<String> ReadPennTree(String PennTree, Index currentPos) {
		int startpt = 0, endpt = 0;
		currentPos.val = findNearestNonSpace(PennTree, currentPos.val);
		Tree<String> node;
		boolean isLexical = false;
		
		//deal with the label
		//for normal nodes
		if(PennTree.charAt(currentPos.val) == '(') {
			startpt = currentPos.val + 1;
			for(int i = currentPos.val; i < PennTree.length(); i++) {
				if(PennTree.charAt(i) == ' ' && i > startpt) {
					endpt = i;
					break;
				}
			}
		}
		//for lexical nodes
		else {
			isLexical = true;
			startpt = currentPos.val;
			for(int i = currentPos.val; i < PennTree.length(); i++) {
				if((PennTree.charAt(i) == '(' || PennTree.charAt(i) == ')') && i > startpt) {
					endpt = i;
					break;
				}
			}
		}
		node = new Tree<String>(PennTree.substring(startpt, endpt));
		
		//deal with the daughters & isLexical
		List<Tree<String>> L = new ArrayList<Tree<String>>();
		currentPos.val = findNearestNonSpace(PennTree, endpt);
		while(currentPos.val < PennTree.length() && !isLexical) {
			//for normal node
			if(PennTree.charAt(currentPos.val) == '(') {
				isLexical = false;
				Tree<String> temp = ReadPennTree(PennTree, currentPos);
				temp.setParent(node);
				L.add(temp);
			}
			//for the ends(and the lexical ones)
			else if(PennTree.charAt(currentPos.val) == ')') {
				if(isLexical == false) {
					currentPos.val++;
					currentPos.val = findNearestNonSpace(PennTree, currentPos.val);
				}
				break;
			}
			//for nodes one level higher than the lexical nodes
			else {
				isLexical = false;
				Tree<String> temp = ReadPennTree(PennTree, currentPos);
				temp.setParent(node);
				L.add(temp);
			}
		}
		if (!L.isEmpty()) {
			for (Tree<String> child: L) {
				node.addChildren(child);
			}
		}
		return node;
	}
	
	static private int findNearestNonSpace(String PennTree, int pos) {
		while(true) {
			if(PennTree.charAt(pos) == ' ' && pos < PennTree.length()) {
				pos++;
			}
			else {
				break;
			}
		}
		return pos;
	}
}
