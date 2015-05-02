package edu.jhu.marmota.syntax.dependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.jhu.marmota.alignment.AlignmentReader;
import edu.jhu.marmota.alignment.WordAlignedSentencePair;
import edu.jhu.marmota.lexeme.LexemeTable;
import edu.jhu.marmota.util.Numbers;
import edu.jhu.marmota.util.PairCounter;
import edu.jhu.marmota.util.Tree;
import fig.basic.Pair;

public class Dep2StrRuleExtractor {

	DepTreeReader depTreeReader;
	AlignmentReader alignmentReader;
	BufferedWriter ruleWriter;
	LexemeTable lexf2e, lexe2f;
	PairCounter<String> ruleCounter;

	public Dep2StrRuleExtractor(String align, String fr, String en, String dep,
			String rule, String f2e, String e2f) throws IOException {
		depTreeReader = new DepTreeReader(dep);
		alignmentReader = new AlignmentReader(align, fr, en);
		ruleWriter = new BufferedWriter(new FileWriter(new File(rule)));
		lexf2e = new LexemeTable(f2e, LexemeTable.Type.f2e);
		lexe2f = new LexemeTable(e2f, LexemeTable.Type.e2f);
		ruleCounter = new PairCounter<String>();
	}

	public void extract() throws IOException {
		WordAlignedSentencePair alignment = alignmentReader.read();
		DepTree tree = depTreeReader.read();
		List<Dep2StrRule> extractedRules = new ArrayList<Dep2StrRule>();
		
		while (tree != null && alignment != null) {
			List<int[]> headSpan = headSpan(tree, alignment);
			boolean[] consistency = consistency(tree, alignment);
			List<Pair<Integer, Integer>> depSpan = depSpan(tree, alignment, headSpan, consistency);

			List<Tree<DepNode>> nodes = tree.postOrderTraverse();
			// for head-dependent segments headed at each node
			for (Tree<DepNode> node : nodes) {
				// acceptable head?
				if (consistency[node.getIndex()]) {
					// acceptable child?
					boolean allDepAcceptable = true;
					if (node.getChildren().isEmpty()) {
						for (Tree<DepNode> child : node.getChildren()) {
							if (depSpan.get(child.getIndex()) != null) {
								allDepAcceptable = false;
								break;
							}
						}
					}

					// this fragment is acceptable, extract rule
					if (allDepAcceptable) {
						// build target for the lexicalized rule
						// (it's also the target for the "complete" rule)
						String[] tar = new String[node.getChildren().size() + 1];
						List<Tree<DepNode>> rn = node.getChildren();
						rn.add(node);
						
						int[] src2tar = new int[rn.size() + 1];
						int[] tar2src = sortNodes(rn);
						// for each cell in String[] tar
						for (int i = 0; i < rn.size() + 1; i++) {
							// head or leaf
							if (tar2src[i] == rn.size() || rn.get(i).getChildren().isEmpty()) {
								// get all the words aligned to the node and dump them in tar[i]
								// where i which is the cell assigned for the head/leaf
								Collection<Integer> alignedTargetWordIndexes = alignment.f2e(rn.get(i).getIndex());
								int[] sortedTargetWordIndexes = Numbers.Integer2int(alignedTargetWordIndexes.toArray(new Integer[0]));
								Arrays.sort(sortedTargetWordIndexes);
								for (int targetWordIndex: sortedTargetWordIndexes) {
									tar[i] += (alignment.f[targetWordIndex] + " ");
								}
								tar[i].trim();
								src2tar[tar2src[i]] = i;
							}
							// internal nodes
							else {
								tar[i] = "x" + String.valueOf(tar2src[i]);
								src2tar[tar2src[i]] = i;
							}
						}
						extractedRules.addAll(generalize(rn, tar, src2tar));
					}
				}
			}
			tree = depTreeReader.read();
			alignment = alignmentReader.read();
		}
		
		// TODO
		// for all rules collected:
		// calculate score according to the count
		// write rule to the disk rule table
	}

	private List<Dep2StrRule> generalize(List<Tree<DepNode>> rn, String[] tarright, int[] src2tar) {
		List<Dep2StrRule> rules = new ArrayList<Dep2StrRule>();
		
		// lexicalized
		DepNode lexicalizedHead = new DepNode(rn.get(rn.size() - 1).getSelf().token(), null);
		DepNode[] lexicalizedDependent = new DepNode[rn.size() - 1];
		for (int i = 0; i < rn.size() - 1; i++) {
			lexicalizedDependent[i] = new DepNode(rn.get(i).getSelf().token(), null);
		}
		rules.add(new Dep2StrRule(new DepRule(lexicalizedHead, lexicalizedDependent), Arrays.copyOf(tarright, tarright.length)));
		
		// unlexicalized rules start from here
		// here is a tiny optimization from the paper: we don't unlexicalize head anymore. 
		// if the head does not come with any specific rule, we'll attempt to match head with a lexical translation during decoding 
		// otherwise, if there is not lexical translation, we just don't translate it
		// (it would hardly be any better if we match it with a VV or NR)
		
		// lists of certain "type" of nodes
		Set<DepNode> leaves = new HashSet<DepNode>();
		Set<DepNode> internals = new HashSet<DepNode>();
		for (int i = 0; i < rn.size() - 1; i++) {
			if (rn.get(i).getChildren().isEmpty()) {
				leaves.add(rn.get(i).getSelf());
			}
			else {
				internals.add(rn.get(i).getSelf());
			}
		}
		
		// generate rules with unlexicalized leaf
		lexicalizedDependent = new DepNode[rn.size() - 1];
		String[] unlexicalizedTarRight = Arrays.copyOf(tarright, tarright.length);
		for (int i = 0; i < rn.size() - 1; i++) {
			if (leaves.contains(rn.get(i))) {
				lexicalizedDependent[i] = new DepNode(null, rn.get(i).getSelf().postag());
				unlexicalizedTarRight[src2tar[i]] = "x" + String.valueOf(i);
			}
			else {
				lexicalizedDependent[i] = new DepNode(rn.get(i).getSelf().token(), null);
			}
		}
		rules.add(new Dep2StrRule(new DepRule(lexicalizedHead, lexicalizedDependent), Arrays.copyOf(unlexicalizedTarRight, unlexicalizedTarRight.length)));
		
		// generate rules with unlexicalized leaf and internal
		for (int i = 0; i < rn.size() - 1; i++) {
			if (internals.contains(rn.get(i))) {
				lexicalizedDependent[i] = new DepNode(null, rn.get(i).getSelf().postag());
			}
		}
		rules.add(new Dep2StrRule(new DepRule(lexicalizedHead, lexicalizedDependent), Arrays.copyOf(unlexicalizedTarRight, unlexicalizedTarRight.length)));
		
		// generate rules with unlexicalized internal
		lexicalizedDependent = new DepNode[rn.size() - 1];
		// first step: internal
		for (int i = 0; i < rn.size() - 1; i++) {
			if (internals.contains(rn.get(i))) {
				lexicalizedDependent[i] = new DepNode(null, rn.get(i).getSelf().postag());
			}
			else {
				lexicalizedDependent[i] = new DepNode(rn.get(i).getSelf().token(), null);
			}
		}
		rules.add(new Dep2StrRule(new DepRule(lexicalizedHead, lexicalizedDependent), Arrays.copyOf(tarright, tarright.length)));
		
		return rules;
	}
	
	
	
	/**
	 * Get the head span of every node in the tree rooted at root. By root I assume the index of nodes in this tree needs to start from 0 and be continuous.
	 * 
	 * @param root
	 * @param alignment
	 * @return
	 */
	private List<int[]> headSpan(DepTree root, WordAlignedSentencePair alignment) {
		List<int[]> headSpan = new ArrayList<int[]>();
		List<Tree<DepNode>> nodes = root.postOrderTraverse();
		for (Tree<DepNode> node : nodes) {
			List<Integer> span = new ArrayList<Integer>();
			span.addAll(alignment.f2e(node.getIndex()));
			headSpan.add(node.getIndex(), Numbers.Integer2int(span.toArray(new Integer[0])));
		}
		return headSpan;
	}

	/**
	 * Get the consistency of every node in the tree rooted at root. By root I mean the index of nodes in this tree needs to start from 0 and be continuous.
	 * 
	 * @param root
	 * @param alignment
	 * @return
	 */
	private boolean[] consistency(DepTree root, WordAlignedSentencePair alignment) {
		boolean[] res = new boolean[root.Terminals().size()];
		List<Tree<DepNode>> nodes = root.postOrderTraverse();
		for (Tree<DepNode> node : nodes) {
			Collection<Integer> targetAlignments = alignment.f2e(node.getIndex());
			for (Integer targetAlignment : targetAlignments) {
				if (alignment.e2f(targetAlignment).size() > 1) {
					res[node.getIndex()] = false;
				}
				else {
					res[node.getIndex()] = true;
				}
			}
		}
		return res;
	}

	/**
	 * Get the dependency span of every node in the tree rooted at root. By root I mean the index of nodes in this tree needs to start from 0 and be continuous.
	 * 
	 * @param root
	 * @param alignment
	 * @param headSpans
	 * @return
	 */
	private List<Pair<Integer, Integer>> depSpan(DepTree root, WordAlignedSentencePair alignment, List<int[]> headSpans, boolean[] consistency) {
		List<Pair<Integer, Integer>> res = new ArrayList<Pair<Integer, Integer>>();
		List<Tree<DepNode>> nodes = root.postOrderTraverse();
		for (Tree<DepNode> node : nodes) {
			if (consistency[node.getIndex()]) {
				int lowerbound = node.getIndex(), upperbound = node.getIndex();
				List<Tree<DepNode>> subnodes = node.postOrderTraverse();
				for (Tree<DepNode> subnode : subnodes) {
					int[] headSpan = headSpans.get(subnode.getIndex());
					for (int head : headSpan) {
						if (head < lowerbound) {
							lowerbound = head;
						}
						if (head > upperbound) {
							upperbound = head;
						}
					}
				}
				res.add(node.getIndex(), new Pair<Integer, Integer>(lowerbound, upperbound));
			}
			else {
				res.add(node.getIndex(), null);
			}
		}
		return res;
	}
	
	/**
	 * Sort children according to index.
	 * 
	 * @param nodes
	 */
	private int[] sortNodes(List<Tree<DepNode>> nodes) {
		List<Integer> index = new ArrayList<Integer>();
		index.add(0);
		for (int i = 1; i < nodes.size(); i++) {
			for (int j = 0; j < i; j++) {
				if (nodes.get(j).getIndex() > nodes.get(i).getIndex()) {
					Tree<DepNode> child = nodes.remove(i);
					nodes.add(j, child);
					index.add(j, i);
				}
			}
		}
		return Numbers.Integer2int(index.toArray(new Integer[0]));
	}
}
