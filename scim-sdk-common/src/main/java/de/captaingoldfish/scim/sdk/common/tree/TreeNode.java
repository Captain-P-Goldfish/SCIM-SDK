package de.captaingoldfish.scim.sdk.common.tree;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import lombok.ToString;


/**
 * a simple representation of a tree node
 *
 * @author Pascal Knueppel
 * @since 25.03.2022
 */
@ToString(of = "value")
public class TreeNode<T>
{

  /**
   * we use the tree to modify its root and leaf value nodes if something changes on this node
   */
  private final GenericTree<T> tree;

  /**
   * the list of parents of this node
   */
  private final Set<TreeNode<T>> parents;

  /**
   * this node itself
   */
  @Getter
  private final T value;

  /**
   * the children of this node
   */
  private final Set<TreeNode<T>> children;

  protected TreeNode(GenericTree<T> tree, T value)
  {
    this.tree = tree;
    this.parents = new HashSet<>();
    this.value = Objects.requireNonNull(value);
    this.children = new HashSet<>();
    this.tree.addRoot(this);
    this.tree.addLeaf(this);
    this.tree.addNode(this);
  }

  /**
   * if this node is a root-node or not
   */
  public boolean isRoot()
  {
    return parents.isEmpty();
  }

  /**
   * if this node is a leaf or not
   */
  public boolean isLeaf()
  {
    return children.isEmpty();
  }

  /**
   * add several parents at once
   */
  public void addParents(TreeNode<T>... parents)
  {
    for ( TreeNode<T> parent : parents )
    {
      addParent(parent);
    }
  }

  /**
   * add a parent to this node
   */
  public void addParent(TreeNode<T> parent)
  {
    Objects.requireNonNull(parent).children.add(this);
    // if this node was a root before we need to remove it from the root set in the tree
    if (parents.isEmpty())
    {
      tree.removeRoot(this);
    }
    parents.add(parent);
    // we just added a node as a child so if we got exactly 1 entry now the parent node was a leaf before and thus
    // must be removed from the tree as a leaf node
    if (parent.children.size() == 1)
    {
      tree.removeLeaf(parent);
    }
  }

  /**
   * removes several parents at once
   */
  public void removeParents(TreeNode<T>... parents)
  {
    for ( TreeNode<T> parent : parents )
    {
      removeParent(parent);
    }
  }

  /**
   * remove a parent from this node
   */
  public void removeParent(TreeNode<T> parent)
  {
    Objects.requireNonNull(parent).children.remove(this);
    parents.remove(parent);
    // if this node has no parents anymore it is a root now
    if (parents.isEmpty())
    {
      tree.addRoot(this);
    }
    // if the parent has no children anymore it is now a leaf
    if (parent.children.isEmpty())
    {
      tree.addLeaf(parent);
    }
  }

  /**
   * adds several children at once
   */
  public void addChildren(TreeNode<T>... children)
  {
    for ( TreeNode<T> child : children )
    {
      addChild(child);
    }
  }

  /**
   * add a child to this node
   */
  public void addChild(TreeNode<T> child)
  {
    Objects.requireNonNull(child).parents.add(this);
    // if this node was a leaf before we need to remove it from the leaves in the tree
    if (children.isEmpty())
    {
      tree.removeLeaf(this);
    }
    children.add(child);
    // we just added an entry so if we got exactly one entry now the child was a root node before and thus must be
    // removed from the roots in the tree
    if (child.parents.size() == 1)
    {
      tree.removeRoot(child);
    }
  }

  /**
   * removes several children at once
   */
  public void removeChildren(TreeNode<T>... children)
  {
    for ( TreeNode<T> child : children )
    {
      removeChild(child);
    }
  }

  /**
   * remove a child from this node. If this child has many parents it will not be removed from the tree though
   */
  public void removeChild(TreeNode<T> child)
  {
    Objects.requireNonNull(child).parents.remove(this);
    children.remove(child);
    // if this node has no children anymore we will add it as a leaf node to the tree
    if (children.isEmpty())
    {
      tree.addLeaf(this);
    }
    if (child.parents.isEmpty())
    {
      tree.addRoot(child);
    }
  }

  /**
   * @see #parents
   */
  public Set<TreeNode<T>> getParents()
  {
    return new HashSet<>(parents);
  }

  /**
   * @see #children
   */
  public Set<TreeNode<T>> getChildren()
  {
    return new HashSet<>(children);
  }

  /**
   * @return the nodes from the whole branch of this node
   */
  public Set<TreeNode<T>> getAllBranchNodes()
  {
    Set<TreeNode<T>> branchNodes = getChildren();
    for ( TreeNode<T> child : children )
    {
      branchNodes.addAll(child.getAllBranchNodes());
    }
    return branchNodes;
  }

  /**
   * removes this node from its parents and forgets its own parents
   */
  public void clearParents()
  {
    for ( TreeNode<T> parent : getParents() )
    {
      parent.children.remove(this);
      if (parent.children.isEmpty())
      {
        tree.addLeaf(parent);
      }
    }
    parents.clear();
    tree.addRoot(this);
  }
}
