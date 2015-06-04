package edu.jhu.marmota.util;

import java.util.ArrayList;
import java.util.List;

public class Tree<T> {

	/**
	 * the parent is:
	 * 1. optional, you are free to choose whether to use it or not;
	 * 2. does not provide any guarantee -- parent(n) may not include n as a child.
	 */
	protected Tree<T> parent;
	
	protected List<Tree<T>> children;
	private T self;
	protected int index;
	
	public Tree(T self) {
		this.self = self;
		parent = null;
		children = new ArrayList<Tree<T>>();
	}
	
	public Tree(T self, int index) {
		this.self = self;
		this.index = index;
		parent = null;
		children = new ArrayList<Tree<T>>();
	}
	
	public T getSelf() {
		return self;
	}
	
	public Tree<T> getParent() {
		return parent;
	}
	
	public void setParent(Tree<T> parent) {
		this.parent = parent;
	}

	public Tree<T> getChild(int index) {
		return children.get(index);
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
	
	public int getIndex() {
		return index;
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
	
	public List<Tree<T>> preOrderTraverse() {
		List<Tree<T>> res = new ArrayList<Tree<T>>();
		res.add(this);
		if (!children.isEmpty()) {
			for (Tree<T> node: children) {
				res.addAll(node.preOrderTraverse());
			}
		}
		return res;
	}
	
	public List<Tree<T>> postOrderTraverse() {
		List<Tree<T>> res = new ArrayList<Tree<T>>();
		if (!children.isEmpty()) {
			for (Tree<T> node: children) {
				res.addAll(node.postOrderTraverse());
			}
		}
		res.add(this);
		return res;
	}
	
	public List<T> Terminals() {
		List<T> res = new ArrayList<T>();
		if (children.isEmpty()) {
			res.add(self);
		}
		else {
			for (Tree<T> node: children) {
				res.addAll(node.Terminals());
			}
		}
		return res;
	}
	
	public List<T> preTerminals() {
		List<T> res = new ArrayList<T>();
		if (!children.isEmpty()) {
			for (Tree<T> node: children) {
				if (node.children.isEmpty()) {
					res.add(self);
				}
				else {
					res.addAll(node.preTerminals());
				}
			}
		}
		return res;
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		if (this.getChildren().isEmpty()) {
			res.append(this.self.toString());
		}
		else {
			res.append("(" + this.self.toString() + " ");
			for (Tree<T> child: this.getChildren()) {
				res.append(child.toString());
			}
			res.append(")");
		}
		return res.toString();
	}
}
