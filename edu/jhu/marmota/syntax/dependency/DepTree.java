package edu.jhu.marmota.syntax.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	DepTree(DepNode self, boolean rooted) {
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
	DepTree(DepNode self, int index, boolean rooted) {
		super(self, index);
		edgeLabels = new ArrayList<String>();
		this.rooted = rooted;
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
