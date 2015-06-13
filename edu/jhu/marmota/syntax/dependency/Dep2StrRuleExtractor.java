package edu.jhu.marmota.syntax.dependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import edu.jhu.marmota.alignment.AlignmentReader;
import edu.jhu.marmota.alignment.WordAlignedSentencePair;
import edu.jhu.marmota.lexeme.LexemeTable;
import edu.jhu.marmota.util.*;
import fig.basic.Pair;

/**
 * Extract and score the rules from input trees and alignments.
 *
 * Note that currently trees must be projective.
 *
 * TODO: not sure if we treated the rooted/un-rooted tree correctly at present, ctrl + F "rooted" to make revisions
 *
 * @author shuoyang
 */
public class Dep2StrRuleExtractor {

	private enum OpenClass {
		CD("CD"),
		DT("DT"),
		OD("OD"),
		JJ("JJ"),
		NN("NN"),
		NR("NR"),
		NT("NT"),
		AD("AD"),
		FW("FW"),
		PN("PN");

		private String postag;

		OpenClass(String postag) {
			this.postag = postag;
		}

		static public boolean contains(String postag) {
			for (OpenClass oc: values()) {
				if (oc.postag.equals(postag)) {
					return true;
				}
			}
			return false;
		}
	}

	private DepTreeReader depTreeReader;
	private AlignmentReader alignmentReader;
	private BufferedWriter ruleWriter;
	private LexemeTable lexf2e, lexe2f;
	private PairCounter<String> ruleCounter;

	public Dep2StrRuleExtractor(String align, String fr, String en, String cons, String dep,
			String rule, String f2e, String e2f) throws IOException {
		if (cons == null) {
			depTreeReader = new DepTreeReader(dep);
		}
		else {
			depTreeReader = new DepTreeReader(cons, dep);
		}
		alignmentReader = new AlignmentReader(align, fr, en);
		ruleWriter = new BufferedWriter(new FileWriter(new File(rule)));
		lexf2e = new LexemeTable(f2e, LexemeTable.Type.f2e);
		lexe2f = new LexemeTable(e2f, LexemeTable.Type.e2f);
		ruleCounter = new PairCounter<String>();
	}
	
	public void extract() throws IOException {
		WordAlignedSentencePair alignment = alignmentReader.read();
		DepTree tree = depTreeReader.read();

		while (tree != null && alignment != null) {
			List<Pair<Integer, Integer>> headSpans = headSpan(tree, alignment);
			boolean[] consistency = consistency(headSpans);
			List<Pair<Integer, Integer>> depSpans = depSpan(tree, headSpans, consistency);

			List<Tree<DepNode>> nodes = tree.postOrderTraverse();
			List<Integer> leafSites = new ArrayList<Integer>();
			List<Integer> internalSites = new ArrayList<Integer>();
			for (Tree<DepNode> node: nodes) {
				// recognize substitution sites, index them by source token #
				if (!node.getChildren().isEmpty()) {
					internalSites.add(getNodeIndex(tree.rooted, node));
				}
				// only leaf nodes belonging to Open class is substituted
				else if (OpenClass.contains(node.getSelf().postag())) {
					leafSites.add(getNodeIndex(tree.rooted, node));
				}
			}
			// for head-dependent segments headed at each node
			for (Tree<DepNode> node : nodes) {
				// acceptable head?
				if (consistency[node.getIndex()]) {
					// acceptable dependents?
					boolean allDepAcceptable = true;
					if (!node.getChildren().isEmpty()) {
						for (Tree<DepNode> child : node.getChildren()) {
							if (depSpans.get(child.getIndex()) == null) {
								allDepAcceptable = false;
								break;
							}
						}
					}

					// this fragment is acceptable, extract rule
					// TODO: the implementation is quite dirty here, maybe there are better solutions?
					if (allDepAcceptable) {
						List<Tree<DepNode>> children = node.getChildren();

						// collect raw target-side information
						// rawtar contains target string aligned by node in the source tree, one node per cell.
						// (Note that there might be more than one target token aligned. They'll be separated by space.)
						// rawtar2src contains the index which the source node will be assigned in the rule.
						// Both of them are indexed by the lower bound of the head span of corresponding source node.
						List<Indexed<String>> rawtar = new ArrayList<Indexed<String>>();
						List<Indexed<Integer>> rawtar2src = new ArrayList<Indexed<Integer>>();
						StringBuilder nodeTranslation = new StringBuilder();
						// head to target
						Pair<Integer, Integer> headSpan = headSpans.get(node.getIndex());
						for (int j = headSpan.getFirst(); j <= headSpan.getSecond(); j++) {
							nodeTranslation.append(alignment.e[j]);
							nodeTranslation.append(" ");
						}
						rawtar.add(new Indexed<String>(headSpan.getFirst(), nodeTranslation.toString().trim()));
						rawtar2src.add(new Indexed<Integer>(headSpan.getFirst(), 0));
						// dependent to target
						for (int i = 0; i < children.size(); i++) {
							Pair<Integer, Integer> depSpan = depSpans.get(children.get(i).getIndex());
							// if the node is a leaf
							// (but not necessarily in leafSites since it may not belong to OpenClass)
							if (!internalSites.contains(getNodeIndex(tree.rooted, children.get(i)))) {
								nodeTranslation = new StringBuilder();
								if (depSpan != null) {
									for (int j = depSpan.getFirst(); j <= depSpan.getSecond(); j++) {
										nodeTranslation.append(alignment.e[j]);
										nodeTranslation.append(" ");
									}
									rawtar.add(new Indexed<String>(depSpan.getFirst(), nodeTranslation.toString().trim()));
								}
							} else {
								rawtar.add(new Indexed<String>(depSpan.getFirst(), "$x"));
							}
							if (depSpan != null) {
								rawtar2src.add(new Indexed<Integer>(depSpan.getFirst(), i + 1));
							}
						}

						// build raw source side of the rules
						// ("raw" means that these nodes contain all params needed for building rules)
						Collections.sort(rawtar);
						Collections.sort(rawtar2src);

						List<Indexed<DepNode>> rawsrc = new ArrayList<Indexed<DepNode>>();
						rawsrc.add(new Indexed<DepNode>(getNodeIndex(tree.rooted, node), node.getSelf()));
						for (Tree<DepNode> child : children) {
							rawsrc.add(new Indexed<DepNode>(getNodeIndex(tree.rooted, child), child.getSelf()));
						}

						// build lexicalized rules
						Dep2StrRule lexRule = buildRule(rawsrc, rawtar, rawtar2src, alignment, null);
						String srcRuleStr = lexRule.getLeft().toString();
						String tarRuleStr = String.join(" ", lexRule.getRight());
						tarRuleStr += (" ||| " + lexRule.encodeAlignment());
						ruleCounter.increment(new Pair<String, String>(srcRuleStr, tarRuleStr));

						// build unlexicalzed rules
						// leaf substitution
						if (!leafSites.isEmpty()) {
							Dep2StrRule leafRule = buildRule(rawsrc, rawtar, rawtar2src, alignment, leafSites);
							srcRuleStr = leafRule.getLeft().toString();
							tarRuleStr = String.join(" ", leafRule.getRight());
							tarRuleStr += (" ||| " + leafRule.encodeAlignment());
							ruleCounter.increment(new Pair<String, String>(srcRuleStr, tarRuleStr));
						}

						// internal substitution
						if (!internalSites.isEmpty()) {
							Dep2StrRule internalRule = buildRule(rawsrc, rawtar, rawtar2src, alignment, internalSites);
							srcRuleStr = internalRule.getLeft().toString();
							tarRuleStr = String.join(" ", internalRule.getRight());
							tarRuleStr += (" ||| " + internalRule.encodeAlignment());
							ruleCounter.increment(new Pair<String, String>(srcRuleStr, tarRuleStr));
						}

						// leaf + internal substitution
						if (!leafSites.isEmpty() && !internalSites.isEmpty()) {
							List<Integer> allSites = new ArrayList<Integer>();
							allSites.addAll(leafSites);
							allSites.addAll(internalSites);
							Dep2StrRule leafAndInternalRule = buildRule(rawsrc, rawtar, rawtar2src, alignment, allSites);
							srcRuleStr = leafAndInternalRule.getLeft().toString();
							tarRuleStr = String.join(" ", leafAndInternalRule.getRight());
							tarRuleStr += (" ||| " + leafAndInternalRule.encodeAlignment());
							ruleCounter.increment(new Pair<String, String>(srcRuleStr, tarRuleStr));
						}
					}
				}
			}
			tree = depTreeReader.read();
			alignment = alignmentReader.read();
		}

		// dump rule table
		for (Pair<String, String> ruleStr: ruleCounter.keys()) {
			String[] srcRule = ruleStr.getFirst().split(" ");
			String[] tarRule = ruleStr.getSecond().split(" \\|\\|\\| ")[0].split(" ");
			String alignmentStr = ruleStr.getSecond().split(" \\|\\|\\| ")[1];
			int[] ruleAlignment = Dep2StrRule.buildAlignment(alignmentStr);

			// Collect number of words aligned for each token to calculate the lex score
			// Please refer to (Koehn, 2009) equation (5.9) for this part
			Counter<Integer> alignmentCounter = new Counter<Integer>();
			for (int j = 0; j < ruleAlignment.length; j++) {
				alignmentCounter.increment(ruleAlignment[j]);
			}

			double lexf2escore = 0.0, lexe2fscore = 0.0;
			for (int j = 0; j < ruleAlignment.length; j++) {
				if (!tarRule[j].startsWith("$")) {
					if (ruleAlignment[j] == 0) {
						String srctoken = new DepNode(srcRule[0]).token();
						// e2f (at most 1)
						lexe2fscore += lexe2f.score(srctoken, tarRule[j]);
						// f2e (may be more than 1)
						lexf2escore += (1 / alignmentCounter.count(0)) * lexf2e.score(srctoken, tarRule[j]);
					}
					// need to eliminate "->"
					else if (ruleAlignment[j] != -1) {
						String srctoken = new DepNode(srcRule[ruleAlignment[j] + 1]).token();
						// e2f (at most 1)
						lexe2fscore += lexe2f.score(srctoken, tarRule[j]);
						// f2e (maybe more than 1)
						lexf2escore += (1 / alignmentCounter.count(ruleAlignment[j])) * lexf2e.score(srctoken, tarRule[j]);
					}
					else {
						// e2null
						lexe2fscore += lexe2f.score("NULL", tarRule[j]);
					}
				}
			}

			// f2null
			for (int i = 0; i < srcRule.length; i++) {
				if (!alignmentCounter.keys().contains(i)) {
					lexf2escore += lexf2e.score(srcRule[i], "NULL");
				}
			}

			double f2escore = ruleCounter.count(ruleStr) / ruleCounter.countx(ruleStr.getFirst());
			double e2fscore = ruleCounter.count(ruleStr) / ruleCounter.county(ruleStr.getSecond());

			StringBuilder record = new StringBuilder();
			record.append(ruleStr.getFirst());
			record.append(" ||| ");
			record.append(String.join(" ", tarRule));
			record.append(" ||| ");
			record.append(String.valueOf(e2fscore));
			record.append(" ");
			record.append(String.valueOf(Math.exp(lexe2fscore)));
			record.append(" ");
			record.append(String.valueOf(f2escore));
			record.append(" ");
			record.append(String.valueOf(Math.exp(lexf2escore)));
			record.append(" ||| ");
			record.append(alignmentStr);
			record.append("\n");
			ruleWriter.write(record.toString());
		}
		ruleWriter.close();
		alignmentReader.close();
		depTreeReader.close();
	}

	/**
	 * Get the head span of every node in the tree rooted at root.
	 * By root I assume the index of nodes in this tree needs to start from 0 and be continuous.
	 *
	 * @param root
	 * @param alignment
	 * @return
	 */
	private List<Pair<Integer, Integer>> headSpan(DepTree root, WordAlignedSentencePair alignment) {
		List<Tree<DepNode>> nodes = root.postOrderTraverse();
		Map<Integer, Pair<Integer, Integer>> spanMap = new TreeMap<Integer, Pair<Integer, Integer>>();
		for (Tree<DepNode> node : nodes) {
			List<Integer> span = new ArrayList<Integer>();
			Collection<Integer> alignments;
			if (root.rooted) {
				alignments = alignment.f2e(node.getIndex() - 1);
			}
			else {
				alignments = alignment.f2e(node.getIndex());
			}
			if (alignments != null) {
				span.addAll(alignments);
				spanMap.put(node.getIndex(), new Pair<Integer, Integer>(Collections.min(span), Collections.max(span)));
			}
			else {
				spanMap.put(node.getIndex(), null);
			}
		}
		List<Pair<Integer, Integer>> headSpan = new ArrayList<Pair<Integer, Integer>>();
		// TODO: used to occupy ROOT position for un-rooted tree, but dunno whether this is the correct way
		if (!root.rooted) {
			headSpan.add(null);
		}
		headSpan.addAll(spanMap.values());
		return headSpan;
	}

	/**
	 * Get the consistency of every span in the list.
	 *
	 * @param headSpans
	 * @return
	 */
	private boolean[] consistency(List<Pair<Integer, Integer>> headSpans) {
		boolean[] res = new boolean[headSpans.size()];
		Arrays.fill(res, true);
		for (int i = 1; i < headSpans.size(); i++) {
			for (int j = 0; j < i; j++) {
				Pair<Integer, Integer> hs1 = headSpans.get(i);
				Pair<Integer, Integer> hs2 = headSpans.get(j);
				// judge intersection
				if (hs1 == null) {
					res[i] = false;
				}
				else if (hs2 == null) {
					res[j] = false;
				}
				else if (spanIntersect(hs1, hs2)) {
					res[i] = false;
					res[j] = false;
				}
			}
		}
		return res;
	}

	/**
	 * Check whether two spans intersect with each other.
	 *
	 * @param hs1
	 * @param hs2
	 * @return
	 */
	private boolean spanIntersect(Pair<Integer, Integer> hs1, Pair<Integer, Integer> hs2) {
		if (hs1.getFirst() > hs2.getSecond()) {
			return false;
		}
		else if (hs1.getSecond() < hs2.getFirst()) {
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Get the dependency span of every node in the tree rooted at root.
	 * By root I mean the index of nodes in this tree needs to start from 0 and be continuous.
	 * The headSpans and consistency should also be co-indexed with the nodes in the dependency tree.
	 * 
	 * @param root
	 * @param headSpans
	 * @param consistency
	 * @return
	 */
	private List<Pair<Integer, Integer>> depSpan(DepTree root, List<Pair<Integer, Integer>> headSpans, boolean[] consistency) {
		List<Tree<DepNode>> nodes = root.postOrderTraverse();
		Map<Integer, Pair<Integer, Integer>> spanMap = new TreeMap<Integer, Pair<Integer, Integer>>();
		for (Tree<DepNode> node: nodes) {
			Pair<Integer, Integer> headSpan = headSpans.get(node.getIndex());
			int lb = -1, ub = -1;
			if (consistency[node.getIndex()]) {
				lb = headSpan.getFirst();
				ub = headSpan.getSecond();
			}
			for (Tree<DepNode> child: node.getChildren()) {
				if (consistency[child.getIndex()]) {
					Pair<Integer, Integer> childHeadSpan = headSpans.get(child.getIndex());
					if (lb > childHeadSpan.getFirst() || lb < 0) {
						lb = childHeadSpan.getFirst();
					}
					if (ub < childHeadSpan.getSecond() || ub < 0) {
						ub = childHeadSpan.getSecond();
					}
				}
			}
			if (lb != -1 && ub != -1) {
				spanMap.put(node.getIndex(), new Pair<Integer, Integer>(lb, ub));
			}
			else {
				spanMap.put(node.getIndex(), null);
			}
		}
		List<Pair<Integer, Integer>> depSpan = new ArrayList<Pair<Integer, Integer>>();
		depSpan.addAll(spanMap.values());
		return depSpan;
	}

	/**
	 * build lex- and unlexicalized rules give raw target and raw source information.
	 *
	 * @param rawsrc the first cell contains the head, and then the dependents
	 * @param rawtar each cell in raw tar corresponds to the head span of a source node
	 * @param rawtar2src mapping from head span to source node
	 * @param alignment word alignment of the sentence pair
	 * @param substitutionSites indexes of the substitution sites, if none just pass null
	 * @return
	 */
	private Dep2StrRule buildRule(List<Indexed<DepNode>> rawsrc, List<Indexed<String>> rawtar,
								  List<Indexed<Integer>> rawtar2src, WordAlignedSentencePair alignment,
								  List<Integer> substitutionSites) {
		if (substitutionSites == null) {
			substitutionSites = new ArrayList<Integer>(0);
		}

		// build source
		// if site needs to be substituted, build source node with postag, otherwise lexical item
		DepNode srcleft = new DepNode(rawsrc.get(0).getE().token(), null);
		DepNode[] srcright = new DepNode[rawsrc.size() - 1];
		for (int i = 1; i < rawsrc.size(); i++) {
			if (substitutionSites.contains(rawsrc.get(i).getIndex())) {
				srcright[i - 1] = new DepNode(null, rawsrc.get(i).getE().postag());
			}
			else {
				srcright[i - 1] = new DepNode(rawsrc.get(i).getE().token(), null);
			}
		}

		// build target
		// either the site is substituted or internal, it should be represented with a variable
		List<String> tarList = new ArrayList<String>();
		List<Integer> alignmentList = new ArrayList<Integer>();
		for (int j = 0; j < rawtar.size(); j++) {
			int alignedSource = rawtar2src.get(j).getE();
			if (alignedSource == 0 || !substitutionSites.contains(rawsrc.get(alignedSource).getIndex())) {
				Indexed<String> indexedRawTars = rawtar.get(j);
				List<String> decomposedTars = Arrays.asList(indexedRawTars.getE().split(" "));
				tarList.addAll(decomposedTars);
				for (int k = 0; k < decomposedTars.size(); k++) {
					if (alignment.isAligned(rawsrc.get(alignedSource).getIndex(), indexedRawTars.getIndex() + k)) {
						alignmentList.add(alignedSource);
					}
					else {
						alignmentList.add(-1);
					}
				}
			}
			else {
				tarList.add("$x");
				alignmentList.add(alignedSource);
			}
		}

		String[] tar = tarList.toArray(new String[tarList.size()]);
		return new Dep2StrRule(srcleft, srcright, tar, Numbers.Integer2int(alignmentList.toArray(new Integer[alignmentList.size()])));
	}

	private int getNodeIndex(boolean rooted, Tree<DepNode> node) {
		if (rooted) {
			return node.getIndex() - 1;
		}
		else {
			return node.getIndex();
		}
	}
}
