package edu.jhu.marmota.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.jhu.marmota.lm.ARPA;
import edu.jhu.marmota.syntax.dependency.*;
import edu.jhu.marmota.util.HyperEdge;
import edu.jhu.marmota.util.PennTreeReader;
import edu.jhu.marmota.util.Tree;

public class Dep2StrDecoder implements AbstractDecoder {

	private String d2stdir, lmdir;
	private Dep2StrRuleTable d2st;
	private ARPA lm;
	private int maxsize = 50;
	private double[] weights;

	public Dep2StrDecoder(String d2stdir, String lmdir, String f2edir, String e2fdir) {
		this.d2stdir = d2stdir;
		this.lmdir = lmdir;
	}

	public Dep2StrDecoder(String d2stdir, String lmdir, String f2edir, String e2fdir, int maxsize, double[] weights) {
		this.d2stdir = d2stdir;
		this.lmdir = lmdir;
		this.maxsize = maxsize;
		this.weights = weights;
	}

	@Override
	public void init() {
		d2st = new Dep2StrRuleTable(d2stdir);
		lm = new ARPA(lmdir);
	}

	/**
	 * The input should be a consituent tree and a dependency tree separated by a blank line (\n\n). 
	 * The current version does not support k-best output
	 */
	@Override
	public String decode(String input) {
		String[] stanford = input.split("\\n\\n");
		String[] cstrs = stanford[0].split("\\n");
		String[] dstrs = stanford[1].split("\\n");
		DepTree tree = DepTreeReader.StanfordDepTreeBuilder(PennTreeReader.ReadPennTree(String.join(" ", cstrs)), dstrs);
		Map<Tree<DepNode>, NaiveHypothesisStack<Dep2StrHypothesis>> stacks = new HashMap<Tree<DepNode>, NaiveHypothesisStack<Dep2StrHypothesis>>();
		List<Tree<DepNode>> nodes = tree.postOrderTraverse();
		for (Tree<DepNode> node : nodes) {
			List<NaiveHypothesisStack<Dep2StrHypothesis>> childStacks = new ArrayList<NaiveHypothesisStack<Dep2StrHypothesis>>();
			for (Tree<DepNode> child : node.getChildren()) {
				childStacks.add(stacks.get(child));
			}
			NaiveHypothesisStack<Dep2StrHypothesis> stack = new NaiveHypothesisStack<Dep2StrHypothesis>();

			DepRule srcRule = new DepRule(node.getSelf(), node.getChildren().toArray(new DepNode[0]));
			Collection<String[]> tars = d2st.f2e(srcRule);

			// construct pseudo rule (only lexical token is specified, the postag is left NULL)
			List<Dep2StrRule> choices = new ArrayList<Dep2StrRule>();
			if (tars.isEmpty()) {
				// construct target side
				String[] tar = new String[node.getChildren().size()];
				int[] index = new int[node.getChildren().size() + 1];
				List<Tree<DepNode>> children = node.getChildren();
				for (int i = 0; i < index.length - 1; i++) {
					index[i] = children.get(i).getIndex();
				}
				index[index.length - 1] = node.getIndex();
				Arrays.sort(index);
				int headpos = -1;
				for (int i = 0; i < index.length; i++) {
					boolean ishead = true;
					for (int j = 0; j < children.size(); j++) {
						if (index[i] == children.get(j).getIndex()) {
							tar[i] = "x" + String.valueOf(j);
							ishead = false;
							break;
						}
					}
					if (ishead) {
						headpos = i;
					}
				}
				
				DepRule headOnly = new DepRule(node.getSelf(), null);
				// check if there is any "dictionary rule" in the rule table
				Collection<String[]> headtars = d2st.f2e(headOnly);
				// oov, build pseudo rule by not translating the source word
				if (headtars.isEmpty()) {
					tar[headpos] = node.getSelf().token();
//					choices.add(new Dep2StrRule(srcRule, tar));
				}
				// dictionary, build pseudo rules by applying "dictionary rule" on head
				else {
					// tars should be of length 1
					for (String[] headtar: headtars) {
						tar[headpos] = headtar[0];
//						choices.add(new Dep2StrRule(srcRule, tar));
					}
				}
			}
			// no need to construct pseudo rule
			else {
				for (String[] tar : tars) {
//					choices.add(new Dep2StrRule(srcRule, tar));
				}
			}
			// push stack
			for (Dep2StrHypothesis hypo : cubePruning(childStacks, choices, node.getIndex(), maxsize)) {
				stack.push(hypo);
			}
			stack.prune();
			stacks.put(node, stack);
		}
		// use bp to generate best translation
		NaiveHypothesisStack<Dep2StrHypothesis> finalStack = stacks.get(nodes.get(nodes.size() - 1));
		return String.join(" ", finalStack.pop().history);
	}

	/**
	 * Look at the subsuming hypothesis and use cube pruning to build new ones. Note that our implementation is not restricted to the case of two non-terminals.
	 * 
	 * @param childStacks
	 * @return
	 */
	private List<Dep2StrHypothesis> cubePruning(List<NaiveHypothesisStack<Dep2StrHypothesis>> childStacks, List<Dep2StrRule> choices, int headindex, int maxsize) {
		List<Dep2StrHypothesis> res = new ArrayList<Dep2StrHypothesis>();
		List<List<Dep2StrHypothesis>> childStackLists = new ArrayList<List<Dep2StrHypothesis>>();
		for (NaiveHypothesisStack<Dep2StrHypothesis> childStack : childStacks) {
			List<Dep2StrHypothesis> childStackAsList = new ArrayList<Dep2StrHypothesis>();
			// again cannot get Dep2StrHypothesis instead of Hypothesis, damn you JVM
			for (Hypothesis childHypothesis : childStack) {
				childStackAsList.add((Dep2StrHypothesis) childHypothesis);
			}
			childStackLists.add(childStackAsList);
		}

		// initialize (start from position (0, 0))
		int[] expanding = new int[childStacks.size()];
		for (int i = 0; i < expanding.length; i++) {
			expanding[i] = 0;
		}
		int[] ending = new int[childStacks.size()];
		for (int i = 0; i < ending.length; i++) {
			ending[i] = childStacks.get(i).size();
		}
		List<Integer> visited = new ArrayList<Integer>();
		visited.add(pointHashing(expanding));

		List<Dep2StrHypothesis> childHypothesis = new ArrayList<Dep2StrHypothesis>();
		for (List<Dep2StrHypothesis> childStackList : childStackLists) {
			childHypothesis.add(childStackList.get(0));
		}
		Dep2StrHypothesis start = findBestHypothesis(childHypothesis, choices, headindex);
		res.add(start);

		// while the cube is not traversed and the maximum size of the stack is not reached
		while ((!Arrays.equals(expanding, ending)) && (res.size() < maxsize)) {
			// find the upcoming searching points
			List<int[]> frontier = new ArrayList<int[]>();
			int[] searching = Arrays.copyOf(expanding, expanding.length);
			for (int i = 0; i < expanding.length; i++) {
				if (searching[i] > 0) {
					searching[i] -= 1;
					if (!visited.contains(pointHashing(searching))) {
						frontier.add(searching);
						visited.add(pointHashing(searching));
						searching = Arrays.copyOf(expanding, expanding.length);

					}
				}
				else if (searching[i] < ending[i]) {
					searching[i] += 1;
					if (!visited.contains(pointHashing(searching))) {
						frontier.add(searching);
						visited.add(pointHashing(searching));
					}
				}
			}

			// derive new hypothesis from the searching points
			double bestScore = Double.NEGATIVE_INFINITY;
			Dep2StrHypothesis newhypo = null;
			int[] nextExpandingPoint = null;
			for (int[] point : frontier) {
				childHypothesis = new ArrayList<Dep2StrHypothesis>();
				for (int i = 0; i < childStackLists.size(); i++) {
					childHypothesis.add(childStackLists.get(i).get(point[i]));
				}
				Dep2StrHypothesis candidate = findBestHypothesis(childHypothesis, choices, headindex);
				if (candidate.score > bestScore) {
					candidate = newhypo;
					nextExpandingPoint = point;
					bestScore = candidate.score;
				}
			}
			// only save the best among the frontier
			res.add(newhypo);
			expanding = nextExpandingPoint;
		}

		return res;
	}

	/**
	 * The current version does not consider fuzzy matching.
	 * 
	 * @param childHypothesis
	 *            the subsuming hypothesis to derive from. for terminals in the dependency tree, provide an empty list
	 * 
	 * @param choice
	 *            the chosen rule for the derivation
	 * 
	 * @param headindex
	 *            the index of the head of parameter choice
	 *
	 * @return
	 */
	private Dep2StrHypothesis derivHypothesis(List<Dep2StrHypothesis> childHypothesis, Dep2StrRule choice, int headindex) {
		String[] history = substitute(childHypothesis, choice);

		double f2escore = 0.0;
		double e2fscore = 0.0;
		for (Dep2StrHypothesis child : childHypothesis) {
			f2escore += child.f2escore;
			e2fscore += child.e2fscore;
		}
		f2escore += d2st.f2escore(choice);
		e2fscore += d2st.e2fscore(choice);

		double lmscore = lm.localscore(String.join(" ", history));

		double lexf2escore = 0.0;
		lexf2escore += d2st.lexf2escore(choice);
		double lexe2fscore = 0.0;
		lexe2fscore += d2st.lexe2fscore(choice);

		double wordpenalty = 0.0;
		for (Dep2StrHypothesis hypo : childHypothesis) {
			wordpenalty += hypo.wordpenalty;
		}
		for (String token : choice.getRight()) {
			if (!token.matches("x[0-9]+")) {
				wordpenalty += 1.0;
			}
		}

		boolean[] state = new boolean[childHypothesis.get(0).history.length];
		for (Dep2StrHypothesis child : childHypothesis) {
			for (int i = 0; i < state.length; i++) {
				if (child.state[i]) {
					state[i] = true;
				}
			}
		}
		state[headindex] = true;

		double score = 0.0;
		score += weights[0] * f2escore;
		score += weights[1] * lexf2escore;
		score += weights[2] * e2fscore;
		score += weights[3] * lexe2fscore;
		score += weights[4] * lmscore;
		score += weights[5] * wordpenalty;
		Dep2StrHypothesis deriv = new Dep2StrHypothesis(state, score, choice, history, headindex, f2escore, lexf2escore, e2fscore, lexe2fscore, lmscore, wordpenalty);
		HyperEdge<Dep2StrHypothesis> bp = new HyperEdge<Dep2StrHypothesis>(deriv, childHypothesis);
		deriv.setbp(bp);
		deriv.setMergedBp(null);

		return deriv;
	}

	/**
	 * 
	 * @param childHypothesis
	 * @param choice
	 * @return
	 */
	private String[] substitute(List<Dep2StrHypothesis> childHypothesis, Dep2StrRule choice) {
		DepNode[] srcRight = choice.getLeft().getRight();
		String[] tar = choice.getRight();

		// subsmap maps target tokens with source-side dependency nodes
		Map<Integer, Integer> subsMap = new HashMap<Integer, Integer>();
		for (int i = 0; i < tar.length; i++) {
			if (tar[i].matches("x[0-9]+")) {
				subsMap.put(i, Integer.valueOf(tar[i].substring(1)));
			}
		}

		// matching happens here, potentially needs to be revised

		// potentially, if child hypothesis contains words with identical head, this might cause confusion
		// but this seems to be rare? do we really have to process this?
		// one possible solution is to augment DepNode with the token index (no that's a bad idea)
		
		// headmap maps the childHypothesis head with the right-hand side of the rule of choice
		Map<Integer, Integer> headMap = new HashMap<Integer, Integer>();
		for (int i = 0; i < srcRight.length; i++) {
			for (int j = 0; j < childHypothesis.size(); j++) {
				DepNode hypoNode = childHypothesis.get(j).choice.getLeft().getLeft();
				if (srcRight[i].lexicalizedEquals(hypoNode)) {
					headMap.put(i, j);
				}
			}
		}

		String res = "";
		for (int i = 0; i < tar.length; i++) {
			if (subsMap.get(i) != null) {
				int srcRightPos = subsMap.get(i);
				int hypoIndex = headMap.get(srcRightPos);
				res += String.join(" ", childHypothesis.get(hypoIndex).history);
				res += " ";
			}
			else {
				res += tar[i];
				res += " ";
			}
		}
		return res.trim().split(" ");
	}

	/**
	 * calculate a hexadecimal point hash value for later searching
	 * 
	 * @param point
	 * @return
	 */
	private int pointHashing(int[] point) {
		int hash = 0;
		for (int digit : point) {
			hash = 16 * hash + digit;
		}
		return hash;
	}

	/**
	 * find the best hypothesis from all applicable rules (the "depth" of the cube) given the child hypothesis combination (a "cell" of the cube)
	 * 
	 * @param childHypothesis
	 * @param choices
	 * @param headindex
	 * @return
	 */
	private Dep2StrHypothesis findBestHypothesis(List<Dep2StrHypothesis> childHypothesis, List<Dep2StrRule> choices, int headindex) {
		Dep2StrHypothesis best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (Dep2StrRule choice : choices) {
			Dep2StrHypothesis candidate = derivHypothesis(childHypothesis, choice, headindex);
			if (candidate.score > bestScore) {
				bestScore = candidate.score;
				best = candidate;
			}
		}
		return best;
	}
}
