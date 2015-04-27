package edu.jhu.marmota.syntax.dependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.jhu.marmota.alignment.AlignmentReader;
import edu.jhu.marmota.alignment.WordAlignedSentencePair;
import edu.jhu.marmota.util.Numbers;
import edu.jhu.marmota.util.Tree;
import fig.basic.Pair;

public class Dep2StrRuleExtractor {
	
	DepTreeReader depTreeReader;
	AlignmentReader alignmentReader;
	BufferedWriter ruleWriter;
	
	public Dep2StrRuleExtractor(String align, String fr, String en, String dep, String rule) throws IOException {
		depTreeReader = new DepTreeReader(dep);
		alignmentReader = new AlignmentReader(align, fr, en);
		ruleWriter = new BufferedWriter(new FileWriter(new File(rule)));
	}
	
	public void extract() throws IOException {
		WordAlignedSentencePair alignment = alignmentReader.read();
		DepTree tree = depTreeReader.read();
		while (tree != null && alignment != null) {
			
		}
	}
	
	public List<int[]> headSpan(DepTree tree, WordAlignedSentencePair alignment) {
		List<int[]> headSpan = new ArrayList<int[]>();
		List<Tree<DepNode>> nodes = tree.postOrderTraverse();
		for (Tree<DepNode> node: nodes) {
			List<Integer> span = new ArrayList<Integer>();
			span.addAll(alignment.f2e(node.getIndex()));
			headSpan.add(Numbers.Integer2int(span.toArray(new Integer[0])));
		}
		return headSpan;
	}
	
	public boolean[] consistency(DepTree tree, WordAlignedSentencePair alignment) {
		// TODO
		return null;
	}
	
	public List<Pair<Integer, Integer>> depSpan(DepTree tree, WordAlignedSentencePair alignment) {
		// TODO
		return null;
	}
}
