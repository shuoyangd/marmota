package edu.jhu.marmota.syntax.dependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import edu.jhu.marmota.alignment.AlignmentReader;
import edu.jhu.marmota.alignment.WordAlignedSentencePair;
import edu.jhu.marmota.lexeme.LexemeTable;
import edu.jhu.marmota.util.Indexed;
import edu.jhu.marmota.util.Numbers;
import edu.jhu.marmota.util.PairCounter;
import edu.jhu.marmota.util.Tree;
import fig.basic.Option;
import fig.basic.OptionsParser;
import fig.basic.Pair;

/**
 * Extract and score the rules from input trees and alignments.
 *
 * Note that currently trees must be projective.
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

		while (tree != null && alignment != null) {
			List<Pair<Integer, Integer>> headSpans = headSpan(tree, alignment);
			boolean[] consistency = consistency(headSpans);
			List<Pair<Integer, Integer>> depSpan = depSpan(tree, headSpans, consistency);

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
					// TODO: the implementation is quite dirty here, maybe there are better solutions?
					if (allDepAcceptable) {
						List<Tree<DepNode>> children = node.getChildren();
						// recognize substitution sites
						List<Integer> leafSites = new ArrayList<Integer>();
						List<Integer> internalSites = new ArrayList<Integer>();
						for (int i = 0; i < children.size(); i++) {
							Tree<DepNode> child = children.get(i);
							if (child.getChildren().isEmpty()) {
								leafSites.add(i + 1);
							} else if (OpenClass.contains(child.getSelf().postag())) {
								internalSites.add(i + 1);
							}
						}

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
							if (!internalSites.contains(i + 1)) {
								nodeTranslation = new StringBuilder();
								headSpan = headSpans.get(children.get(i).getIndex());
								for (int j = headSpan.getFirst(); j <= headSpan.getSecond(); j++) {
									nodeTranslation.append(alignment.e[j]);
									nodeTranslation.append(" ");
								}
								rawtar.add(new Indexed<String>(headSpan.getFirst(), nodeTranslation.toString().trim()));
							} else {
								rawtar.add(new Indexed<String>(headSpan.getFirst(), "$x"));
							}
							rawtar2src.add(new Indexed<Integer>(headSpan.getFirst(), i + 1));
						}

						// build raw source side of the rules
						// ("raw" means that these nodes contain all params needed for building rules)
						Collections.sort(rawtar);
						Collections.sort(rawtar2src);
						DepNode rawHead = node.getSelf();
						List<DepNode> dependentsList = new ArrayList<DepNode>();
						for (Tree<DepNode> child : children) {
							dependentsList.add(child.getSelf());
						}
						DepNode[] rawDependents = dependentsList.toArray(new DepNode[dependentsList.size()]);

						// build lexicalized rules
						Dep2StrRule lexRule = buildRule(rawtar, rawtar2src, rawHead, rawDependents, null);
						String srcRuleStr = lexRule.getLeft().toString();
						String tarRuleStr = String.join(" ", lexRule.getRight());
						tarRuleStr += (" ||| " + lexRule.encodeAlignment());
						ruleCounter.increment(new Pair<String, String>(srcRuleStr, tarRuleStr));

						// build unlexicalzed rules
						// leaf substitution
						Dep2StrRule leafRule = buildRule(rawtar, rawtar2src, rawHead, rawDependents, leafSites);
						srcRuleStr = leafRule.getLeft().toString();
						tarRuleStr = String.join(" ", lexRule.getRight());
						tarRuleStr += (" ||| " + lexRule.encodeAlignment());
						ruleCounter.increment(new Pair<String, String>(srcRuleStr, tarRuleStr));

						// internal substitution
						Dep2StrRule internalRule = buildRule(rawtar, rawtar2src, rawHead, rawDependents, internalSites);
						srcRuleStr = internalRule.getLeft().toString();
						tarRuleStr = String.join(" ", lexRule.getRight());
						tarRuleStr += (" ||| " + lexRule.encodeAlignment());
						ruleCounter.increment(new Pair<String, String>(srcRuleStr, tarRuleStr));

						// leaf + internal substitution
						List<Integer> allSites = new ArrayList<Integer>();
						allSites.addAll(leafSites);
						allSites.addAll(internalSites);
						Dep2StrRule leafAndInternalRule = buildRule(rawtar, rawtar2src, rawHead, rawDependents, allSites);
						srcRuleStr = leafAndInternalRule.getLeft().toString();
						tarRuleStr = String.join(" ", lexRule.getRight());
						tarRuleStr += (" ||| " + lexRule.encodeAlignment());
						ruleCounter.increment(new Pair<String, String>(srcRuleStr, tarRuleStr));
					}
				}
			}
			tree = depTreeReader.read();
			alignment = alignmentReader.read();
		}

		// dump rule table
		for (Pair<String, String> ruleStr: ruleCounter.keys()) {
			String[] srcRule = ruleStr.getFirst().split(" ");
			String[] tarRule = ruleStr.getSecond().split(" ||| ")[0].split(" ");
			String alignmentStr = ruleStr.getSecond().split(" ||| ")[1];
			int[] ruleAlignment = Dep2StrRule.buildAlignment(alignmentStr);

			double lexf2escore = 0.0, lexe2fscore = 0.0;
			for (int j = 0; j < ruleAlignment.length; j++) {
				if (!tarRule[j].startsWith("$")) {
					if (ruleAlignment[j] == 0) {
						lexe2fscore += lexe2f.score(srcRule[0], tarRule[j]);
						lexf2escore += lexf2e.score(srcRule[0], tarRule[j]);
					}
					// need to eliminate "->"
					else {
						lexe2fscore += lexe2f.score(srcRule[ruleAlignment[j] + 1], tarRule[j]);
						lexf2escore += lexf2e.score(srcRule[ruleAlignment[j] + 1], tarRule[j]);
					}
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
			record.append(String.valueOf(lexe2fscore));
			record.append(" ");
			record.append(String.valueOf(f2escore));
			record.append(" ");
			record.append(String.valueOf(lexf2escore));
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
		List<Pair<Integer, Integer>> headSpan = new ArrayList<Pair<Integer, Integer>>(nodes.size());
		for (Tree<DepNode> node : nodes) {
			List<Integer> span = new ArrayList<Integer>();
			span.addAll(alignment.f2e(node.getIndex()));
			headSpan.set(node.getIndex(), new Pair<Integer, Integer>(Collections.min(span), Collections.max(span)));
		}
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
		for (int i = 1; i < headSpans.size(); i++) {
			for (int j = 0; j < i; j++) {
				Pair<Integer, Integer> hs1 = headSpans.get(i);
				Pair<Integer, Integer> hs2 = headSpans.get(j);
				// judge intersection
				if (hs1.getSecond() >= hs2.getFirst()) {
					res[i] = false;
					res[j] = false;
				}
			}
		}
		return res;
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
		List<Pair<Integer, Integer>> res = new ArrayList<Pair<Integer, Integer>>(nodes.size());
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
				res.set(node.getIndex(), new Pair<Integer, Integer>(lb, ub));
			}
		}
		return res;
	}

	/**
	 * build lex- and unlexicalized rules give raw target and raw source information.
	 *
	 * @param rawtar each cell in raw tar corresponds to the head span of a source node
	 * @param rawtar2src mapping from head span to source node
	 * @param rawHead head node containing both token and postag
	 * @param rawDependents dependents node containing both token and postag
	 * @param substitutionSites indexes of the substitution sites, if none just pass null
	 * @return
	 */
	private Dep2StrRule buildRule(List<Indexed<String>> rawtar, List<Indexed<Integer>> rawtar2src,
						  DepNode rawHead, DepNode[] rawDependents, List<Integer> substitutionSites) {
		if (substitutionSites == null) {
			substitutionSites = new ArrayList<Integer>(0);
		}

		// build source
		// if site needs to be substituted, build source node with postag, otherwise lexical item
		DepNode srcleft = new DepNode(rawHead.token(), null);
		DepNode[] srcright = new DepNode[rawDependents.length];
		for (int i = 0; i < rawDependents.length; i++) {
			if (substitutionSites.contains(i + 1)) {
				srcright[i] = new DepNode(null, rawDependents[i].postag());
			}
			else {
				srcright[i] = new DepNode(rawDependents[i].token(), null);
			}
		}

		// build target
		// either the site is substituted or internal, it should be represented with a variable
		List<String> tarList = new ArrayList<String>();
		List<Integer> alignmentList = new ArrayList<Integer>();
		for (int j = 0; j < rawtar.size(); j++) {
			int alignedSource = rawtar2src.get(j).getE();
			if (substitutionSites.contains(alignedSource)) {
				Indexed<String> indexedRawTars = rawtar.get(j);
				List<String> decomposedTars = Arrays.asList(indexedRawTars.getE().split(" "));
				tarList.addAll(decomposedTars);
				for (int k = 0; k < decomposedTars.size(); k++) {
					alignmentList.add(alignedSource);
				}
			}
			else {
				tarList.add("$x");
				alignmentList.add(alignedSource);
			}
		}

		String[] tar = tarList.toArray(new String[tarList.size()]);
		int[] alignment = Numbers.Integer2int(alignmentList.toArray(new Integer[alignmentList.size()]));

		return new Dep2StrRule(srcleft, srcright, tar, alignment);
	}
}

