package de.captaingoldfish.scim.sdk.common.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Pascal Knueppel
 * @since 25.03.2022
 */
public class GenericTreeTest
{


  /**
   * add three root nodes to the tree
   */
  @Test
  public void testAddRootNodes()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> rootNode1 = tree.addNewNode("hello world 1");
    Assertions.assertEquals("hello world 1", rootNode1.getValue());
    TreeNode<String> rootNode2 = tree.addNewNode("hello world 2");
    TreeNode<String> rootNode3 = tree.addNewNode("hello world 3");

    Assertions.assertEquals(3, tree.getAllNodes().size());
    Assertions.assertEquals(3, tree.getRoots().size());
    Assertions.assertEquals(3, tree.getLeafs().size());

    // @formatter:off
    List<TreeNode<String>> treeNodeList = new ArrayList(){{add(rootNode1); add(rootNode2); add(rootNode3);}};
    // @formatter:on
    for ( TreeNode<String> treeNode : treeNodeList )
    {
      Assertions.assertEquals(0, treeNode.getParents().size());
      Assertions.assertEquals(0, treeNode.getChildren().size());
      Assertions.assertTrue(treeNode.isRoot());
      Assertions.assertTrue(treeNode.isLeaf());
    }
  }

  /**
   * tries to add two parent nodes to a single node
   */
  @Test
  public void testAddParentNodes()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> childNode = tree.addNewNode("hello world 1");
    TreeNode<String> parentNode1 = tree.addNewNode("hello world 2");
    TreeNode<String> parentNode2 = tree.addNewNode("hello world 3");

    childNode.addParents(parentNode1, parentNode2);

    Assertions.assertEquals(3, tree.getAllNodes().size());
    Assertions.assertEquals(2, tree.getRoots().size());
    Assertions.assertEquals(1, tree.getLeafs().size());

    // @formatter:off
    List<TreeNode<String>> treeNodeList = new ArrayList(){{add(parentNode1); add(parentNode2);}};
    // @formatter:on
    for ( TreeNode<String> treeNode : treeNodeList )
    {
      Assertions.assertEquals(0, treeNode.getParents().size());
      Assertions.assertEquals(1, treeNode.getChildren().size());
      Assertions.assertTrue(treeNode.isRoot());
      Assertions.assertFalse(treeNode.isLeaf());
    }

    Assertions.assertEquals(2, childNode.getParents().size());
    Assertions.assertEquals(0, childNode.getChildren().size());
    Assertions.assertFalse(childNode.isRoot());
    Assertions.assertTrue(childNode.isLeaf());
  }

  /**
   * this test removes a parent from a node
   */
  @Test
  public void testRemoveParent()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> childNode = tree.addNewNode("hello world 1");
    TreeNode<String> parentNode1 = tree.addNewNode("hello world 2");
    TreeNode<String> parentNode2 = tree.addNewNode("hello world 3");
    TreeNode<String> parentNode3 = tree.addNewNode("hello world 4");

    childNode.addParents(parentNode1, parentNode2, parentNode3);
    childNode.removeParents(parentNode1, parentNode2);

    Assertions.assertEquals(4, tree.getAllNodes().size());
    Assertions.assertEquals(3, tree.getRoots().size());
    Assertions.assertEquals(3, tree.getLeafs().size());

    // @formatter:off
    List<TreeNode<String>> treeNodeList = new ArrayList(){{add(parentNode1); add(parentNode2);}};
    // @formatter:on
    for ( TreeNode<String> treeNode : treeNodeList )
    {
      Assertions.assertEquals(0, treeNode.getParents().size());
      Assertions.assertEquals(0, treeNode.getChildren().size());
      Assertions.assertTrue(treeNode.isRoot());
      Assertions.assertTrue(treeNode.isLeaf());
    }

    Assertions.assertEquals(1, childNode.getParents().size());
    Assertions.assertEquals(0, childNode.getChildren().size());
    Assertions.assertFalse(childNode.isRoot());
    Assertions.assertTrue(childNode.isLeaf());

    Assertions.assertEquals(0, parentNode3.getParents().size());
    Assertions.assertEquals(1, parentNode3.getChildren().size());
    Assertions.assertTrue(parentNode3.isRoot());
    Assertions.assertFalse(parentNode3.isLeaf());
  }

  /**
   * Removes all parents from a node
   */
  @Test
  public void testRemoveAllParents()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> childNode = tree.addNewNode("hello world 1");
    TreeNode<String> parentNode1 = tree.addNewNode("hello world 2");

    childNode.addParent(parentNode1);
    childNode.removeParent(parentNode1);

    Assertions.assertEquals(2, tree.getAllNodes().size());
    Assertions.assertEquals(2, tree.getRoots().size());
    Assertions.assertEquals(2, tree.getLeafs().size());

    // @formatter:off
    List<TreeNode<String>> treeNodeList = new ArrayList(){{add(parentNode1); add(childNode);}};
    // @formatter:on
    for ( TreeNode<String> treeNode : treeNodeList )
    {
      Assertions.assertEquals(0, treeNode.getParents().size());
      Assertions.assertEquals(0, treeNode.getChildren().size());
      Assertions.assertTrue(treeNode.isRoot());
      Assertions.assertTrue(treeNode.isLeaf());
    }
  }

  /**
   * adds some children to a node
   */
  @Test
  public void testAddChildren()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> parentNode = tree.addNewNode("hello world 1");
    TreeNode<String> childNode1 = tree.addNewNode("hello world 2");
    TreeNode<String> childNode2 = tree.addNewNode("hello world 3");

    parentNode.addChildren(childNode1, childNode2);

    Assertions.assertEquals(3, tree.getAllNodes().size());
    Assertions.assertEquals(1, tree.getRoots().size());
    Assertions.assertEquals(2, tree.getLeafs().size());

    // @formatter:off
    List<TreeNode<String>> treeNodeList = new ArrayList(){{add(childNode1); add(childNode2);}};
    // @formatter:on
    for ( TreeNode<String> treeNode : treeNodeList )
    {
      Assertions.assertEquals(1, treeNode.getParents().size());
      Assertions.assertEquals(0, treeNode.getChildren().size());
      Assertions.assertFalse(treeNode.isRoot());
      Assertions.assertTrue(treeNode.isLeaf());
    }

    Assertions.assertEquals(0, parentNode.getParents().size());
    Assertions.assertEquals(2, parentNode.getChildren().size());
    Assertions.assertTrue(parentNode.isRoot());
    Assertions.assertFalse(parentNode.isLeaf());
  }

  /**
   * removes some children from a parent
   */
  @Test
  public void testRemoveChildren()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> parentNode = tree.addNewNode("hello world 1");
    TreeNode<String> childNode1 = tree.addNewNode("hello world 2");
    TreeNode<String> childNode2 = tree.addNewNode("hello world 3");
    TreeNode<String> childNode3 = tree.addNewNode("hello world 4");

    parentNode.addChildren(childNode1, childNode2, childNode3);
    parentNode.removeChildren(childNode1, childNode2);

    Assertions.assertEquals(4, tree.getAllNodes().size());
    Assertions.assertEquals(3, tree.getRoots().size());
    Assertions.assertEquals(3, tree.getLeafs().size());

    // @formatter:off
    List<TreeNode<String>> treeNodeList = new ArrayList(){{add(childNode1); add(childNode2);}};
    // @formatter:on
    for ( TreeNode<String> treeNode : treeNodeList )
    {
      Assertions.assertEquals(0, treeNode.getParents().size());
      Assertions.assertEquals(0, treeNode.getChildren().size());
      Assertions.assertTrue(treeNode.isRoot());
      Assertions.assertTrue(treeNode.isLeaf());
    }

    Assertions.assertEquals(0, parentNode.getParents().size());
    Assertions.assertEquals(1, parentNode.getChildren().size());
    Assertions.assertTrue(parentNode.isRoot());
    Assertions.assertFalse(parentNode.isLeaf());

    Assertions.assertEquals(1, childNode3.getParents().size());
    Assertions.assertEquals(0, childNode3.getChildren().size());
    Assertions.assertFalse(childNode3.isRoot());
    Assertions.assertTrue(childNode3.isLeaf());
  }

  /**
   * removes all children from a node
   */
  @Test
  public void testRemoveAllChildren()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> parentNode = tree.addNewNode("hello world 1");
    TreeNode<String> childNode1 = tree.addNewNode("hello world 2");

    parentNode.addChild(childNode1);
    parentNode.removeChild(childNode1);

    Assertions.assertEquals(2, tree.getAllNodes().size());
    Assertions.assertEquals(2, tree.getRoots().size());
    Assertions.assertEquals(2, tree.getLeafs().size());

    // @formatter:off
    List<TreeNode<String>> treeNodeList = new ArrayList(){{add(childNode1); add(parentNode);}};
    // @formatter:on
    for ( TreeNode<String> treeNode : treeNodeList )
    {
      Assertions.assertEquals(0, treeNode.getParents().size());
      Assertions.assertEquals(0, treeNode.getChildren().size());
      Assertions.assertTrue(treeNode.isRoot());
      Assertions.assertTrue(treeNode.isLeaf());
    }
  }

  /**
   * checks that all nodes from a specific branch are returned if accessed
   */
  @Test
  public void testGetAllBranchNodes()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> root = tree.addNewNode("root");

    TreeNode<String> a = tree.addNewNode("a");
    TreeNode<String> a1 = tree.addNewNode("a1");
    TreeNode<String> a11 = tree.addNewNode("a11");
    TreeNode<String> a12 = tree.addNewNode("a12");
    TreeNode<String> a2 = tree.addNewNode("a2");
    TreeNode<String> a21 = tree.addNewNode("a21");

    // build the first branch
    a.addChildren(a1, a2);
    a1.addChildren(a11, a12);
    a2.addChildren(a21);

    TreeNode<String> b = tree.addNewNode("b");
    TreeNode<String> b1 = tree.addNewNode("b1");
    TreeNode<String> b11 = tree.addNewNode("b11");
    TreeNode<String> b2 = tree.addNewNode("b2");
    TreeNode<String> b3 = tree.addNewNode("b3");
    TreeNode<String> b31 = tree.addNewNode("b31");

    // build the seconds branch
    b.addChildren(b1, b2, b3);
    b1.addChildren(b11);
    b3.addChildren(b31);

    // add branches to root-node
    root.addChildren(a, b);

    // root-node
    Assertions.assertEquals(1, tree.getRoots().size());
    // a11, a12, a21, b11, b2, b31
    Assertions.assertEquals(6, tree.getLeafs().size(), tree.getLeafs().toString());
    Assertions.assertEquals(13, tree.getAllNodes().size());

    Set<String> a1BranchNodes = a1.getAllBranchNodes().stream().map(TreeNode::getValue).collect(Collectors.toSet());
    MatcherAssert.assertThat(a1BranchNodes, Matchers.containsInAnyOrder("a11", "a12"));

    Set<String> aBranchNodes = a.getAllBranchNodes().stream().map(TreeNode::getValue).collect(Collectors.toSet());
    MatcherAssert.assertThat(aBranchNodes, Matchers.containsInAnyOrder("a1", "a2", "a11", "a12", "a21"));

    Set<String> b2BranchNodes = b2.getAllBranchNodes().stream().map(TreeNode::getValue).collect(Collectors.toSet());
    MatcherAssert.assertThat(b2BranchNodes, Matchers.empty());
  }

  /**
   * verifies that a node can successfully be cleared of all its parents
   */
  @Test
  public void testClearParents()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> childNode = tree.addNewNode("hello world 1");
    TreeNode<String> parentNode1 = tree.addNewNode("hello world 2");
    TreeNode<String> parentNode2 = tree.addNewNode("hello world 3");

    childNode.addParents(parentNode1, parentNode2);

    childNode.clearParents();

    Assertions.assertEquals(3, tree.getAllNodes().size());
    Assertions.assertEquals(3, tree.getRoots().size());
    Assertions.assertEquals(3, tree.getLeafs().size());
  }

  /**
   * verifies that a node can successfully be cleared of all its parents
   */
  @Test
  public void testClearParentsWhereParentHasSeveralChildren()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> childNode = tree.addNewNode("hello world 1");
    TreeNode<String> childNode2 = tree.addNewNode("hello world 1");
    TreeNode<String> parentNode1 = tree.addNewNode("hello world 2");
    TreeNode<String> parentNode2 = tree.addNewNode("hello world 3");

    childNode.addParents(parentNode1, parentNode2);
    childNode2.addParent(parentNode1);

    childNode.clearParents();

    Assertions.assertEquals(4, tree.getAllNodes().size());
    Assertions.assertEquals(3, tree.getRoots().size());
    Assertions.assertEquals(3, tree.getLeafs().size());

    // @formatter:off
    List<TreeNode<String>> treeNodeList = new ArrayList(){{add(childNode); add(parentNode2);}};
    // @formatter:on
    for ( TreeNode<String> treeNode : treeNodeList )
    {
      Assertions.assertEquals(0, treeNode.getParents().size());
      Assertions.assertEquals(0, treeNode.getChildren().size());
      Assertions.assertTrue(treeNode.isRoot());
      Assertions.assertTrue(treeNode.isLeaf());
    }

    Assertions.assertEquals(0, parentNode1.getParents().size());
    Assertions.assertEquals(1, parentNode1.getChildren().size());
    Assertions.assertTrue(parentNode1.isRoot());
    Assertions.assertFalse(parentNode1.isLeaf());

    Assertions.assertEquals(1, childNode2.getParents().size());
    Assertions.assertEquals(0, childNode2.getChildren().size());
    Assertions.assertFalse(childNode2.isRoot());
    Assertions.assertTrue(childNode2.isLeaf());
  }

  /**
   * verifies that a single node can be removed from the tree, while its branch will be preserved
   */
  @Test
  public void testRemoveNodeFromTree()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> root = tree.addNewNode("hello world 1");
    TreeNode<String> a = tree.addNewNode("hello world 1");
    TreeNode<String> a1 = tree.addNewNode("hello world 2");

    root.addChild(a);
    a.addChild(a1);

    tree.removeNodeFromTree(a);

    Assertions.assertEquals(2, tree.getAllNodes().size());
    Assertions.assertEquals(2, tree.getRoots().size());
    Assertions.assertEquals(2, tree.getLeafs().size());

    Assertions.assertFalse(tree.getAllNodes().contains(a));
  }

  /**
   * verifies that a complete branch node can be removed from the tree, while its branch will be preserved
   */
  @Test
  public void testRemoveBranchFromTree()
  {
    GenericTree<String> tree = new GenericTree<>();

    TreeNode<String> root = tree.addNewNode("hello world 1");
    TreeNode<String> a = tree.addNewNode("a");
    TreeNode<String> a1 = tree.addNewNode("a1");
    TreeNode<String> a11 = tree.addNewNode("a11");
    TreeNode<String> a12 = tree.addNewNode("a12");
    TreeNode<String> b = tree.addNewNode("b");
    TreeNode<String> b1 = tree.addNewNode("b2");

    root.addChildren(a, b);
    a.addChild(a1);
    a1.addChildren(a11, a12);
    b.addChildren(b1);

    tree.removeBranchFromTree(a);

    Assertions.assertEquals(3, tree.getAllNodes().size());
    Assertions.assertEquals(1, tree.getRoots().size());
    Assertions.assertEquals(1, tree.getLeafs().size());

    // @formatter:off
    List<TreeNode<String>> treeNodeList = new ArrayList(){{add(a); add(a1);; add(a11);; add(a12);}};
    // @formatter:on
    for ( TreeNode<String> treeNode : treeNodeList )
    {
      Assertions.assertFalse(tree.getAllNodes().contains(treeNode));
    }
  }

}
