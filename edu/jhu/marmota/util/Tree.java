package edu.jhu.marmota.util;

import java.util.ArrayList;
import java.util.List;

public class Tree<T> {

	/**
	 * the parent is:
	 * 1. optional, you are free to choose whether to use it or not;
	 * 2. does not provide any guarantee -- parent(n) may not include n as a child.
	 */
	private Tree<T> parent;
	
	private List<Tree<T>> children;
	private T self;
	
	public Tree(T self) {
		this.self = self;
		parent = null;
		children = new ArrayList<Tree<T>>();
	}
	
	public Tree<T> getParent() {
		return parent;
	}
	
	public void setParent(Tree<T> parent) {
		this.parent = parent;
	}

	/**
	 * only does shallow copy
	 * 
	 * @return
	 */
	public List<Tree<T>> getChildren() {
		List<Tree<T>> res = new ArrayList<Tree<T>>();
		res.addAll(children);
		return res;
	}
	
	/**
	 * create new children and add (top-down construction)
	 * 
	 * @param child
	 */
	public void addChildren(T child) {
		children.add(new Tree<T>(child));
	}
	
	/**
	 * add constructed child (bottom-up construction)
	 * 
	 * @param child
	 */
	public void addChildren(Tree<T> child) {
		children.add(child);
	}
	
	public List<T> preOrderTraverse() {
		List<T> res = new ArrayList<T>();
		res.add(self);
		for (Tree<T> node: children) {
			res.addAll(node.preOrderTraverse());
		}
		return res;
	}
	
	public List<T> postOrderTraverse() {
		List<T> res = new ArrayList<T>();
		for (Tree<T> node: children) {
			res.addAll(node.postOrderTraverse());
		}
		res.add(self);
		return res;
	}
	
	@Override
	public String toString() {
		String res = "(";
		res += self.toString();
		for (Tree<T> node: children) {
			if (node.children.isEmpty()) {
				res += (" " + node.toString());
			}
			else {
				res += " (" + node.toString() + ")";
			}
		}
		return res;
	}
}
