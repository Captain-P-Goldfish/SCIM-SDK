package de.captaingoldfish.scim.sdk.common.tree;

import java.util.HashSet;
import java.util.Set;


/**
 * this implementation represents a tree with several root-nodes and each node might have several parents and
 * several children. This tree implementation is not thread-safe!
 *
 * @author Pascal Knueppel
 * @since 25.03.2022
 */
public class GenericTree<T>
{

  /**
   * this set contains all nodes within this tree
   */
  private Set<TreeNode<T>> allNodes;

  /**
   * all root nodes represented within this tree
   */
  private Set<TreeNode<T>> roots;

  /**
   * all leaf nodes represented within this tree
   */
  private Set<TreeNode<T>> leafs;


  public GenericTree()
  {
    allNodes = new HashSet<>();
    this.roots = new HashSet<>();
    this.leafs = new HashSet<>();
  }

  /**
   * creates a new tree node with this tree as its parent
   *
   * @param parents the parents of the new node. May be null
   * @param value the actual value of the node
   * @param children the children of the node. May be null
   * @return a new tree-node that is a child of this tree
   */
  public TreeNode<T> addNewNode(T value)
  {
    return new TreeNode<>(this, value);
  }

  /**
   * method is only called by tree nodes
   */
  protected void addLeaf(TreeNode<T> treeNode)
  {
    leafs.add(treeNode);
  }

  protected void removeLeaf(TreeNode<T> treeNode)
  {
    leafs.remove(treeNode);
  }

  protected void addRoot(TreeNode<T> treeNode)
  {
    roots.add(treeNode);
  }

  protected void removeRoot(TreeNode<T> treeNode)
  {
    roots.remove(treeNode);
  }

  protected void addNode(TreeNode<T> treeNode)
  {
    allNodes.add(treeNode);
  }

  /**
   * removes a single node from the tree. If the node was somewhere in the middle of the tree we will basically
   * get a second tree because its branch was cut off from the first tree
   */
  public void removeNodeFromTree(TreeNode<T> treeNode)
  {
    roots.remove(treeNode);
    leafs.remove(treeNode);
    allNodes.remove(treeNode);
    treeNode.clearParents();
  }

  /**
   * removes the branch from the tree represented by the given treenode
   *
   * @param treeNode the branch to remove
   */
  public void removeBranchFromTree(TreeNode<T> treeNode)
  {
    Set<TreeNode<T>> branchNodes = treeNode.getAllBranchNodes();
    branchNodes.add(treeNode);
    for ( TreeNode<T> node : branchNodes )
    {
      node.clearParents();
    }
    for ( TreeNode<T> node : branchNodes )
    {
      roots.remove(node);
      leafs.remove(node);
      allNodes.remove(node);
    }
  }

  /**
   * creates a new node if a node with the same value does not exist yet and returns the existing node if a node
   * with an identical value does already exist
   */
  public TreeNode<T> addDistinctNode(T value)
  {
    return allNodes.stream().filter(node -> node.getValue().equals(value)).findAny().orElseGet(() -> addNewNode(value));
  }

  /**
   * if this tree still has any nodes left
   */
  public boolean hasNodes()
  {
    return !getAllNodes().isEmpty();
  }

  /**
   * @see #allNodes
   */
  public Set<TreeNode<T>> getAllNodes()
  {
    return new HashSet<>(allNodes);
  }

  /**
   * @see #roots
   */
  public Set<TreeNode<T>> getRoots()
  {
    return new HashSet<>(roots);
  }

  /**
   * @see #leafs
   */
  public Set<TreeNode<T>> getLeafs()
  {
    return new HashSet<>(leafs);
  }
}
