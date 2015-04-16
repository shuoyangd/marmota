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
		int currentPos = findNearestNonSpace(PennTree, PennTree.indexOf(' '));
		while(currentPos < PennTree.length()) {
			if (PennTree.charAt(currentPos) == ')') {
				break;
			}
			else if(PennTree.charAt(currentPos) != ' ') {
				Tree<String> temp = ReadPennTree(PennTree, currentPos);
				temp.setParent(root);
				L.add(temp);
			}
			else {
				currentPos++;
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
	static private Tree<String> ReadPennTree(String PennTree, int currentPos) {
		int startpt = 0, endpt = 0;
		currentPos = findNearestNonSpace(PennTree, currentPos);
		Tree<String> node;
		boolean isLexical = false;
		
		//deal with the label
		//for normal nodes
		if(PennTree.charAt(currentPos) == '(') {
			startpt = currentPos + 1;
			for(int i = currentPos; i < PennTree.length(); i++) {
				if(PennTree.charAt(i) == ' ' && i > startpt) {
					endpt = i;
					break;
				}
			}
		}
		//for lexical nodes
		else {
			isLexical = true;
			startpt = currentPos;
			for(int i = currentPos; i < PennTree.length(); i++) {
				if((PennTree.charAt(i) == '(' || PennTree.charAt(i) == ')') && i > startpt) {
					endpt = i;
					break;
				}
			}
		}
		node = new Tree<String>(PennTree.substring(startpt, endpt));
		
		//deal with the daughters & isLexical
		List<Tree<String>> L = new ArrayList<Tree<String>>();
		currentPos = findNearestNonSpace(PennTree, endpt);
		while(currentPos < PennTree.length() && !isLexical) {
			//for normal node
			if(PennTree.charAt(currentPos) == '(') {
				isLexical = false;
				Tree<String> temp = ReadPennTree(PennTree, currentPos);
				temp.setParent(node);
				L.add(temp);
			}
			//for the ends(and the lexical ones)
			else if(PennTree.charAt(currentPos) == ')') {
				if(isLexical == false) {
					currentPos++;
					currentPos = findNearestNonSpace(PennTree, currentPos);
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
