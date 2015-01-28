//   RTree.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited
//   Copyright (C) 2008-2010 aled@sourceforge.net
//  
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//  
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//  
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package it.unibo.alchemist.external.com.infomatiq.jsi.rtree;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;
import it.unibo.alchemist.external.com.infomatiq.jsi.BuildProperties;
import it.unibo.alchemist.external.com.infomatiq.jsi.Point;
import it.unibo.alchemist.external.com.infomatiq.jsi.PriorityQueue;
import it.unibo.alchemist.external.com.infomatiq.jsi.Rectangle;
import it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex;

import java.io.Serializable;
import java.util.Properties;
//CHECKSTYLE:OFF

/**
 * <p>
 * This is a lightweight RTree implementation, specifically designed for the
 * following features (in order of importance):
 * <ul>
 * <li>Fast intersection query performance. To achieve this, the RTree uses only
 * main memory to store entries. Obviously this will only improve performance if
 * there is enough physical memory to avoid paging.</li>
 * <li>Low memory requirements.</li>
 * <li>Fast add performance.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The main reason for the high speed of this RTree implementation is the
 * avoidance of the creation of unnecessary objects, mainly achieved by using
 * primitive collections from the trove4j library.
 * </p>
 */
public class RTree implements SpatialIndex, Serializable {

	private static final long serialVersionUID = 3185788131573314351L;
	// parameters of the tree
	private static final int DEFAULT_MAX_NODE_ENTRIES = 50;
	private static final int DEFAULT_MIN_NODE_ENTRIES = 20;
	protected int maxNodeEntries;
	private int minNodeEntries;

	// map of nodeId -> node object
	// TODO eliminate this map - it should not be needed. Nodes
	// can be found by traversing the tree.
	private final TIntObjectHashMap<Node> nodeMap = new TIntObjectHashMap<Node>();

	// used to mark the status of entries during a node split
	private static final int ENTRY_STATUS_ASSIGNED = 0;
	private static final int ENTRY_STATUS_UNASSIGNED = 1;
	private byte[] entryStatus;
	private byte[] initialEntryStatus;

	// stacks used to store nodeId and entry index of each node
	// from the root down to the leaf. Enables fast lookup
	// of nodes when a split is propagated up the tree.
	private final TIntStack parents = new TIntArrayStack();
	private final TIntStack parentsEntry = new TIntArrayStack();

	// initialisation
	private int treeHeight = 1; // leaves are always level 1
	private int rootNodeId;
	private int sizef;

	// Enables creation of new nodes
	private int highestUsedNodeId;

	// Deleted node objects are retained in the nodeMap,
	// so that they can be reused. Store the IDs of nodes
	// which can be reused.
	private final TIntStack deletedNodeIds = new TIntArrayStack();

	// List of nearest rectangles. Use a member variable to
	// avoid recreating the object each time nearest() is called.
	private final TIntArrayList nearestIds = new TIntArrayList();
	private final TIntArrayList savedValues = new TIntArrayList();
	private double savedPriority;

	// List of nearestN rectanges, used in the alternative nearestN
	// implementation.
	private final PriorityQueue distanceQueue = new PriorityQueue(PriorityQueue.SORT_ORDER_ASCENDING);

	/**
	 * Constructor. Use init() method to initialize parameters of the RTree.
	 */
	public RTree() {
		init(null);
	}

	// -------------------------------------------------------------------------
	// public implementation of SpatialIndex interface:
	// init(Properties)
	// add(Rectangle, int)
	// delete(Rectangle, int)
	// nearest(Point, TIntProcedure, double)
	// intersects(Rectangle, TIntProcedure)
	// contains(Rectangle, TIntProcedure)
	// size()
	// -------------------------------------------------------------------------
	/**
	 * <p>
	 * Initialize implementation dependent properties of the RTree. Currently
	 * implemented properties are:
	 * <ul>
	 * <li>MaxNodeEntries</li> This specifies the maximum number of entries in a
	 * node. The default value is 10, which is used if the property is not
	 * specified, or is less than 2.
	 * <li>MinNodeEntries</li> This specifies the minimum number of entries in a
	 * node. The default value is half of the MaxNodeEntries value (rounded
	 * down), which is used if the property is not specified or is less than 1.
	 * </ul>
	 * </p>
	 * 
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#init(Properties)
	 */
	private void init(final Properties props) {
		if (props == null) {
			// use sensible defaults if null is passed in.
			maxNodeEntries = DEFAULT_MAX_NODE_ENTRIES;
			minNodeEntries = DEFAULT_MIN_NODE_ENTRIES;
		} else {
			maxNodeEntries = Integer.parseInt(props.getProperty("MaxNodeEntries", "0"));
			minNodeEntries = Integer.parseInt(props.getProperty("MinNodeEntries", "0"));

			// Obviously a node with less than 2 entries cannot be split.
			// The node splitting algorithm will work with only 2 entries
			// per node, but will be inefficient.
			if (maxNodeEntries < 2) {
				maxNodeEntries = DEFAULT_MAX_NODE_ENTRIES;
			}

			// The MinNodeEntries must be less than or equal to (int)
			// (MaxNodeEntries / 2)
			if (minNodeEntries < 1 || minNodeEntries > maxNodeEntries / 2) {
				minNodeEntries = maxNodeEntries / 2;
			}
		}

		entryStatus = new byte[maxNodeEntries];
		initialEntryStatus = new byte[maxNodeEntries];

		for (int i = 0; i < maxNodeEntries; i++) {
			initialEntryStatus[i] = ENTRY_STATUS_UNASSIGNED;
		}

		final Node root = new Node(rootNodeId, 1, maxNodeEntries);
		nodeMap.put(rootNodeId, root);

	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#add(Rectangle, int)
	 */
	public void add(final Rectangle r, final int id) {
		add(r.minX, r.minY, r.maxX, r.maxY, id, 1);

		sizef++;
	}

	/**
	 * Adds a new entry at a specified level in the tree
	 */
	private void add(final double minX, final double minY, final double maxX, final double maxY, final int id, final int level) {
		// I1 [Find position for new record] Invoke ChooseLeaf to select a
		// leaf node L in which to place r
		final Node n = chooseNode(minX, minY, maxX, maxY, level);
		Node newLeaf = null;

		// I2 [Add record to leaf node] If L has room for another entry,
		// install E. Otherwise invoke SplitNode to obtain L and LL containing
		// E and all the old entries of L
		if (n.entryCount < maxNodeEntries) {
			n.addEntry(minX, minY, maxX, maxY, id);
		} else {
			newLeaf = splitNode(n, minX, minY, maxX, maxY, id);
		}

		// I3 [Propagate changes upwards] Invoke AdjustTree on L, also passing
		// LL
		// if a split was performed
		final Node newNode = adjustTree(n, newLeaf);

		// I4 [Grow tree taller] If node split propagation caused the root to
		// split, create a new root whose children are the two resulting nodes.
		if (newNode != null) {
			final int oldRootNodeId = rootNodeId;
			final Node oldRoot = getNode(oldRootNodeId);

			rootNodeId = getNextNodeId();
			treeHeight++;
			final Node root = new Node(rootNodeId, treeHeight, maxNodeEntries);
			root.addEntry(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY, newNode.nodeId);
			root.addEntry(oldRoot.mbrMinX, oldRoot.mbrMinY, oldRoot.mbrMaxX, oldRoot.mbrMaxY, oldRoot.nodeId);
			nodeMap.put(rootNodeId, root);
		}
	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#delete(Rectangle, int)
	 */
	public boolean delete(final Rectangle r, final int id) {
		// FindLeaf algorithm inlined here. Note the "official" algorithm
		// searches all overlapping entries. This seems inefficient to me,
		// as an entry is only worth searching if it contains (NOT overlaps)
		// the rectangle we are searching for.
		//
		// Also the algorithm has been changed so that it is not recursive.

		// FL1 [Search subtrees] If root is not a leaf, check each entry
		// to determine if it contains r. For each entry found, invoke
		// findLeaf on the node pointed to by the entry, until r is found or
		// all entries have been checked.
		parents.clear();
		parents.push(rootNodeId);

		parentsEntry.clear();
		parentsEntry.push(-1);
		Node n = null;
		int foundIndex = -1; // index of entry to be deleted in leaf

		while (foundIndex == -1 && parents.size() > 0) {
			n = getNode(parents.peek());
			final int startIndex = parentsEntry.peek() + 1;

			if (!n.isLeaf()) {
				boolean contains = false;
				for (int i = startIndex; i < n.entryCount; i++) {
					if (Rectangle.contains(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], r.minX, r.minY, r.maxX, r.maxY)) {
						parents.push(n.ids[i]);
						parentsEntry.pop();
						parentsEntry.push(i); // this becomes the start index
												// when the child has been
												// searched
						parentsEntry.push(-1);
						contains = true;
						break; // ie go to next iteration of while()
					}
				}
				if (contains) {
					continue;
				}
			} else {
				foundIndex = n.findEntry(r.minX, r.minY, r.maxX, r.maxY, id);
			}

			parents.pop();
			parentsEntry.pop();
		} // while not found

		if (foundIndex != -1) {
			n.deleteEntry(foundIndex);
			condenseTree(n);
			sizef--;
		}

		// shrink the tree if possible (i.e. if root node has exactly one
		// entry,and that
		// entry is not a leaf node, delete the root (it's entry becomes the new
		// root)
		Node root = getNode(rootNodeId);
		while (root.entryCount == 1 && treeHeight > 1) {
			deletedNodeIds.push(rootNodeId);
			root.entryCount = 0;
			rootNodeId = root.ids[0];
			treeHeight--;
			root = getNode(rootNodeId);
		}

		// if the tree is now empty, then set the MBR of the root node back to
		// it's original state
		// (this is only needed when the tree is empty, as this is the only
		// state where an empty node
		// is not eliminated)
		if (sizef == 0) {
			root.mbrMinX = Float.MAX_VALUE;
			root.mbrMinY = Float.MAX_VALUE;
			root.mbrMaxX = -Float.MAX_VALUE;
			root.mbrMaxY = -Float.MAX_VALUE;
		}
		return (foundIndex != -1);
	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#nearest(Point, TIntProcedure, double)
	 */
	public void nearest(final Point p, final TIntProcedure v, final double furthestDistance) {
		final Node rootNode = getNode(rootNodeId);

		final double furthestDistanceSq = furthestDistance * furthestDistance;
		nearest(p, rootNode, furthestDistanceSq);

		nearestIds.forEach(v);
		nearestIds.reset();
	}

	private void createNearestNDistanceQueue(final Point p, final int count, final double furthestDistance) {
		distanceQueue.reset();
		distanceQueue.setSortOrder(PriorityQueue.SORT_ORDER_DESCENDING);

		// return immediately if given an invalid "count" parameter
		if (count <= 0) {
			return;
		}

		parents.clear();
		parents.push(rootNodeId);

		parentsEntry.clear();
		parentsEntry.push(-1);
		double furthestDistanceSq = furthestDistance * furthestDistance;

		while (parents.size() > 0) {
			final Node n = getNode(parents.peek());
			final int startIndex = parentsEntry.peek() + 1;

			if (!n.isLeaf()) {
				boolean near = false;
				for (int i = startIndex; i < n.entryCount; i++) {
					if (Rectangle.distanceSq(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], p.x, p.y) <= furthestDistanceSq) {
						parents.push(n.ids[i]);
						parentsEntry.pop();
						parentsEntry.push(i);
						parentsEntry.push(-1);
						near = true;
						break;
					}
				}
				if (near) {
					continue;
				}
			} else {
				for (int i = 0; i < n.entryCount; i++) {
					final double entryDistanceSq = Rectangle.distanceSq(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], p.x, p.y);
					final int entryId = n.ids[i];

					if (entryDistanceSq <= furthestDistanceSq) {
						distanceQueue.insert(entryId, entryDistanceSq);

						while (distanceQueue.size() > count) {
							final int value = distanceQueue.getValue();
							final double distanceSq = distanceQueue.getPriority();
							distanceQueue.pop();
							if (distanceSq == distanceQueue.getPriority()) {
								savedValues.add(value);
								savedPriority = distanceSq;
							} else {
								savedValues.reset();
							}
						}
						if (savedValues.size() > 0 && savedPriority == distanceQueue.getPriority()) {
							for (int svi = 0; svi < savedValues.size(); svi++) {
								distanceQueue.insert(savedValues.get(svi), savedPriority);
							}
							savedValues.reset();
						}
						if (distanceQueue.getPriority() < furthestDistanceSq && distanceQueue.size() >= count) {
							furthestDistanceSq = distanceQueue.getPriority();
						}
					}
				}
			}
			parents.pop();
			parentsEntry.pop();
		}
	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#nearestNUnsorted(Point,
	 *      TIntProcedure, int, double)
	 */
	public void nearestNUnsorted(final Point p, final TIntProcedure v, final int count, final double furthestDistance) {
		// This implementation is designed to give good performance
		// where
		// o N is high (100+)
		// o The results do not need to be sorted by distance.
		//
		// Uses a priority queue as the underlying data structure.
		//
		// The behaviour of this algorithm has been carefully designed to
		// return exactly the same items as the the original version
		// (nearestN_orig), in particular,
		// more than N items will be returned if items N and N+x have the
		// same priority.
		createNearestNDistanceQueue(p, count, furthestDistance);

		while (distanceQueue.size() > 0) {
			v.execute(distanceQueue.getValue());
			distanceQueue.pop();
		}
	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#nearestN(Point, TIntProcedure, int,
	 *      double)
	 */
	public void nearestN(final Point p, final TIntProcedure v, final int count, final double furthestDistance) {
		createNearestNDistanceQueue(p, count, furthestDistance);

		distanceQueue.setSortOrder(PriorityQueue.SORT_ORDER_ASCENDING);
		while (distanceQueue.size() > 0) {
			v.execute(distanceQueue.getValue());
			distanceQueue.pop();
		}
	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#intersects(Rectangle, TIntProcedure)
	 */
	public void intersects(final Rectangle r, final TIntProcedure v) {
		final Node rootNode = getNode(rootNodeId);
		intersects(r, v, rootNode);
	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#contains(Rectangle, TIntProcedure)
	 */
	public void contains(final Rectangle r, final TIntProcedure v) {
		// find all rectangles in the tree that are contained by the passed
		// rectangle
		// written to be non-recursive (should model other searches on this?)

		parents.clear();
		parents.push(rootNodeId);

		parentsEntry.clear();
		parentsEntry.push(-1);

		// TODO: possible shortcut here - could test for intersection with the
		// MBR of the root node. If no intersection, return immediately.

		while (parents.size() > 0) {
			final Node n = getNode(parents.peek());
			final int startIndex = parentsEntry.peek() + 1;

			if (!n.isLeaf()) {
				// go through every entry in the index node to check
				// if it intersects the passed rectangle. If so, it
				// could contain entries that are contained.
				boolean intersects = false;
				for (int i = startIndex; i < n.entryCount; i++) {
					if (Rectangle.intersects(r.minX, r.minY, r.maxX, r.maxY, n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i])) {
						parents.push(n.ids[i]);
						parentsEntry.pop();
						parentsEntry.push(i); // this becomes the start index
												// when the child has been
												// searched
						parentsEntry.push(-1);
						intersects = true;
						break; // ie go to next iteration of while()
					}
				}
				if (intersects) {
					continue;
				}
			} else {
				// go through every entry in the leaf to check if
				// it is contained by the passed rectangle
				for (int i = 0; i < n.entryCount; i++) {
					if (Rectangle.contains(r.minX, r.minY, r.maxX, r.maxY, n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]) && !v.execute(n.ids[i])) {
						return;
					}
				}
			}
			parents.pop();
			parentsEntry.pop();
		}
	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#size()
	 */
	public int size() {
		return sizef;
	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#getBounds()
	 */
	public Rectangle getBounds() {
		Rectangle bounds = null;

		final Node n = getNode(getRootNodeId());
		if (n != null && n.entryCount > 0) {
			bounds = new Rectangle();
			bounds.minX = n.mbrMinX;
			bounds.minY = n.mbrMinY;
			bounds.maxX = n.mbrMaxX;
			bounds.maxY = n.mbrMaxY;
		}
		return bounds;
	}

	/**
	 * @see it.unibo.alchemist.external.com.infomatiq.jsi.SpatialIndex#getVersion()
	 */
	public String getVersion() {
		return "RTree-" + BuildProperties.getVersion();
	}

	// -------------------------------------------------------------------------
	// end of SpatialIndex methods
	// -------------------------------------------------------------------------

	/**
	 * Get the next available node ID. Reuse deleted node IDs if possible
	 */
	private int getNextNodeId() {
		int nextNodeId = 0;
		if (deletedNodeIds.size() > 0) {
			nextNodeId = deletedNodeIds.pop();
		} else {
			nextNodeId = 1 + highestUsedNodeId++;
		}
		return nextNodeId;
	}

	/**
	 * Get a node object, given the ID of the node.
	 */
	public Node getNode(final int id) {
		return nodeMap.get(id);
	}

	/**
	 * Get the highest used node ID
	 */
	public int getHighestUsedNodeId() {
		return highestUsedNodeId;
	}

	/**
	 * Get the root node ID
	 */
	public int getRootNodeId() {
		return rootNodeId;
	}

	/**
	 * Split a node. Algorithm is taken pretty much verbatim from Guttman's
	 * original paper.
	 * 
	 * @return new node object.
	 */
	private Node splitNode(final Node n, final double newRectMinX, final double newRectMinY, final double newRectMaxX, final double newRectMaxY, final int newId) {
		// [Pick first entry for each group] Apply algorithm pickSeeds to
		// choose two entries to be the first elements of the groups. Assign
		// each to a group.

		System.arraycopy(initialEntryStatus, 0, entryStatus, 0, maxNodeEntries);

		Node newNode = null;
		newNode = new Node(getNextNodeId(), n.level, maxNodeEntries);
		nodeMap.put(newNode.nodeId, newNode);

		pickSeeds(n, newRectMinX, newRectMinY, newRectMaxX, newRectMaxY, newId, newNode);
		// [Check if done] If all entries have been assigned, stop. If one
		// group has so few entries that all the rest must be assigned to it in
		// order for it to have the minimum number m, assign them and stop.
		while (n.entryCount + newNode.entryCount < maxNodeEntries + 1) {
			if (maxNodeEntries + 1 - newNode.entryCount == minNodeEntries) {
				// assign all remaining entries to original node
				for (int i = 0; i < maxNodeEntries; i++) {
					if (entryStatus[i] == ENTRY_STATUS_UNASSIGNED) {
						entryStatus[i] = ENTRY_STATUS_ASSIGNED;

						if (n.entriesMinX[i] < n.mbrMinX) {
							n.mbrMinX = n.entriesMinX[i];
						}
						if (n.entriesMinY[i] < n.mbrMinY) {
							n.mbrMinY = n.entriesMinY[i];
						}
						if (n.entriesMaxX[i] > n.mbrMaxX) {
							n.mbrMaxX = n.entriesMaxX[i];
						}
						if (n.entriesMaxY[i] > n.mbrMaxY) {
							n.mbrMaxY = n.entriesMaxY[i];
						}

						n.entryCount++;
					}
				}
				break;
			}
			if (maxNodeEntries + 1 - n.entryCount == minNodeEntries) {
				// assign all remaining entries to new node
				for (int i = 0; i < maxNodeEntries; i++) {
					if (entryStatus[i] == ENTRY_STATUS_UNASSIGNED) {
						entryStatus[i] = ENTRY_STATUS_ASSIGNED;
						newNode.addEntry(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], n.ids[i]);
						n.ids[i] = -1; // an id of -1 indicates the entry is not
										// in use
					}
				}
				break;
			}

			// [Select entry to assign] Invoke algorithm pickNext to choose the
			// next entry to assign. Add it to the group whose covering
			// rectangle
			// will have to be enlarged least to accommodate it. Resolve ties
			// by adding the entry to the group with smaller area, then to the
			// the one with fewer entries, then to either. Repeat from S2
			pickNext(n, newNode);
		}

		n.reorganize(this);

		return newNode;
	}

	/**
	 * Pick the seeds used to split a node. Select two entries to be the first
	 * elements of the groups
	 */
	private void pickSeeds(final Node n, final double newRectMinX, final double newRectMinY, final double newRectMaxX, final double newRectMaxY, final int newId, final Node newNode) {
		// Find extreme rectangles along all dimension. Along each dimension,
		// find the entry whose rectangle has the highest low side, and the one
		// with the lowest high side. Record the separation.
		double maxNormalizedSeparation = -1; // initialize to -1 so that even
											// overlapping rectangles will be
											// considered for the seeds
		int highestLowIndex = -1;
		int lowestHighIndex = -1;

		// for the purposes of picking seeds, take the MBR of the node to
		// include
		// the new rectangle aswell.
		if (newRectMinX < n.mbrMinX) {
			n.mbrMinX = newRectMinX;
		}
		if (newRectMinY < n.mbrMinY) {
			n.mbrMinY = newRectMinY;
		}
		if (newRectMaxX > n.mbrMaxX) {
			n.mbrMaxX = newRectMaxX;
		}
		if (newRectMaxY > n.mbrMaxY) {
			n.mbrMaxY = newRectMaxY;
		}

		final double mbrLenX = n.mbrMaxX - n.mbrMinX;
		final double mbrLenY = n.mbrMaxY - n.mbrMinY;

		double tempHighestLow = newRectMinX;
		int tempHighestLowIndex = -1; // -1 indicates the new rectangle is the
										// seed

		double tempLowestHigh = newRectMaxX;
		int tempLowestHighIndex = -1; // -1 indicates the new rectangle is the
										// seed

		for (int i = 0; i < n.entryCount; i++) {
			final double tempLow = n.entriesMinX[i];
			if (tempLow >= tempHighestLow) {
				tempHighestLow = tempLow;
				tempHighestLowIndex = i;
			} else { // ensure that the same index cannot be both lowestHigh and
						// highestLow
				final double tempHigh = n.entriesMaxX[i];
				if (tempHigh <= tempLowestHigh) {
					tempLowestHigh = tempHigh;
					tempLowestHighIndex = i;
				}
			}

			// PS2 [Adjust for shape of the rectangle cluster] Normalize the
			// separations
			// by dividing by the widths of the entire set along the
			// corresponding
			// dimension
			final double normalizedSeparation = mbrLenX == 0 ? 1 : (tempHighestLow - tempLowestHigh) / mbrLenX;

			// PS3 [Select the most extreme pair] Choose the pair with the
			// greatest
			// normalized separation along any dimension.
			// Note that if negative it means the rectangles overlapped. However
			// still include
			// overlapping rectangles if that is the only choice available.
			if (normalizedSeparation >= maxNormalizedSeparation) {
				highestLowIndex = tempHighestLowIndex;
				lowestHighIndex = tempLowestHighIndex;
				maxNormalizedSeparation = normalizedSeparation;
			}
		}

		// Repeat for the Y dimension
		tempHighestLow = newRectMinY;
		tempHighestLowIndex = -1; // -1 indicates the new rectangle is the seed

		tempLowestHigh = newRectMaxY;
		tempLowestHighIndex = -1; // -1 indicates the new rectangle is the seed

		for (int i = 0; i < n.entryCount; i++) {
			final double tempLow = n.entriesMinY[i];
			if (tempLow >= tempHighestLow) {
				tempHighestLow = tempLow;
				tempHighestLowIndex = i;
			} else { // ensure that the same index cannot be both lowestHigh and
						// highestLow
				final double tempHigh = n.entriesMaxY[i];
				if (tempHigh <= tempLowestHigh) {
					tempLowestHigh = tempHigh;
					tempLowestHighIndex = i;
				}
			}

			// PS2 [Adjust for shape of the rectangle cluster] Normalize the
			// separations
			// by dividing by the widths of the entire set along the
			// corresponding
			// dimension
			final double normalizedSeparation = mbrLenY == 0 ? 1 : (tempHighestLow - tempLowestHigh) / mbrLenY;

			// PS3 [Select the most extreme pair] Choose the pair with the
			// greatest
			// normalized separation along any dimension.
			// Note that if negative it means the rectangles overlapped. However
			// still include
			// overlapping rectangles if that is the only choice available.
			if (normalizedSeparation >= maxNormalizedSeparation) {
				highestLowIndex = tempHighestLowIndex;
				lowestHighIndex = tempLowestHighIndex;
				maxNormalizedSeparation = normalizedSeparation;
			}
		}

		// At this point it is possible that the new rectangle is both
		// highestLow and lowestHigh.
		// This can happen if all rectangles in the node overlap the new
		// rectangle.
		// Resolve this by declaring that the highestLowIndex is the lowest Y
		// and,
		// the lowestHighIndex is the largest X (but always a different
		// rectangle)
		if (highestLowIndex == lowestHighIndex) {
			highestLowIndex = -1;
			double tempMinY = newRectMinY;
			lowestHighIndex = 0;
			double tempMaxX = n.entriesMaxX[0];

			for (int i = 1; i < n.entryCount; i++) {
				if (n.entriesMinY[i] < tempMinY) {
					tempMinY = n.entriesMinY[i];
					highestLowIndex = i;
				} else if (n.entriesMaxX[i] > tempMaxX) {
					tempMaxX = n.entriesMaxX[i];
					lowestHighIndex = i;
				}
			}
		}

		// highestLowIndex is the seed for the new node.
		if (highestLowIndex == -1) {
			newNode.addEntry(newRectMinX, newRectMinY, newRectMaxX, newRectMaxY, newId);
		} else {
			newNode.addEntry(n.entriesMinX[highestLowIndex], n.entriesMinY[highestLowIndex], n.entriesMaxX[highestLowIndex], n.entriesMaxY[highestLowIndex], n.ids[highestLowIndex]);
			n.ids[highestLowIndex] = -1;

			// move the new rectangle into the space vacated by the seed for the
			// new node
			n.entriesMinX[highestLowIndex] = newRectMinX;
			n.entriesMinY[highestLowIndex] = newRectMinY;
			n.entriesMaxX[highestLowIndex] = newRectMaxX;
			n.entriesMaxY[highestLowIndex] = newRectMaxY;

			n.ids[highestLowIndex] = newId;
		}

		// lowestHighIndex is the seed for the original node.
		if (lowestHighIndex == -1) {
			lowestHighIndex = highestLowIndex;
		}

		entryStatus[lowestHighIndex] = ENTRY_STATUS_ASSIGNED;
		n.entryCount = 1;
		n.mbrMinX = n.entriesMinX[lowestHighIndex];
		n.mbrMinY = n.entriesMinY[lowestHighIndex];
		n.mbrMaxX = n.entriesMaxX[lowestHighIndex];
		n.mbrMaxY = n.entriesMaxY[lowestHighIndex];
	}

	/**
	 * Pick the next entry to be assigned to a group during a node split.
	 * 
	 * [Determine cost of putting each entry in each group] For each entry not
	 * yet in a group, calculate the area increase required in the covering
	 * rectangles of each group
	 */
	private int pickNext(final Node n, final Node newNode) {
		double maxDifference = Float.NEGATIVE_INFINITY;
		int next = 0;
		int nextGroup = 0;
		for (int i = 0; i < maxNodeEntries; i++) {
			if (entryStatus[i] == ENTRY_STATUS_UNASSIGNED) {

				final double nIncrease = Rectangle.enlargement(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY, n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]);
				final double newNodeIncrease = Rectangle.enlargement(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY, n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]);

				final double difference = Math.abs(nIncrease - newNodeIncrease);

				if (difference > maxDifference) {
					next = i;

					if (nIncrease < newNodeIncrease) {
						nextGroup = 0;
					} else if (newNodeIncrease < nIncrease) {
						nextGroup = 1;
					} else if (Rectangle.area(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY) < Rectangle.area(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY)) {
						nextGroup = 0;
					} else if (Rectangle.area(newNode.mbrMinX, newNode.mbrMinY, newNode.mbrMaxX, newNode.mbrMaxY) < Rectangle.area(n.mbrMinX, n.mbrMinY, n.mbrMaxX, n.mbrMaxY)) {
						nextGroup = 1;
					} else if (newNode.entryCount < maxNodeEntries / 2) {
						nextGroup = 0;
					} else {
						nextGroup = 1;
					}
					maxDifference = difference;
				}
			}
		}

		entryStatus[next] = ENTRY_STATUS_ASSIGNED;

		if (nextGroup == 0) {
			if (n.entriesMinX[next] < n.mbrMinX) {
				n.mbrMinX = n.entriesMinX[next];
			}
			if (n.entriesMinY[next] < n.mbrMinY) {
				n.mbrMinY = n.entriesMinY[next];
			}
			if (n.entriesMaxX[next] > n.mbrMaxX) {
				n.mbrMaxX = n.entriesMaxX[next];
			}
			if (n.entriesMaxY[next] > n.mbrMaxY) {
				n.mbrMaxY = n.entriesMaxY[next];
			}
			n.entryCount++;
		} else {
			// move to new node.
			newNode.addEntry(n.entriesMinX[next], n.entriesMinY[next], n.entriesMaxX[next], n.entriesMaxY[next], n.ids[next]);
			n.ids[next] = -1;
		}

		return next;
	}

	/**
	 * Recursively searches the tree for the nearest entry. Other queries call
	 * execute() on an IntProcedure when a matching entry is found; however
	 * nearest() must store the entry Ids as it searches the tree, in case a
	 * nearer entry is found. Uses the member variable nearestIds to store the
	 * nearest entry IDs.
	 * 
	 * TODO rewrite this to be non-recursive?
	 */
	private double nearest(final Point p, final Node n, double furthestDistanceSq) {
		for (int i = 0; i < n.entryCount; i++) {
			final double tempDistanceSq = Rectangle.distanceSq(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i], p.x, p.y);
			if (n.isLeaf()) { // for leaves, the distance is an actual nearest
								// distance
				if (tempDistanceSq < furthestDistanceSq) {
					furthestDistanceSq = tempDistanceSq;
					nearestIds.reset();
				}
				if (tempDistanceSq <= furthestDistanceSq) {
					nearestIds.add(n.ids[i]);
				}
			} else { // for index nodes, only go into them if they potentially
						// could have
						// a rectangle nearer than actualNearest
				if (tempDistanceSq <= furthestDistanceSq) {
					// search the child node
					furthestDistanceSq = nearest(p, getNode(n.ids[i]), furthestDistanceSq);
				}
			}
		}
		return furthestDistanceSq;
	}

	/**
	 * Recursively searches the tree for all intersecting entries. Immediately
	 * calls execute() on the passed IntProcedure when a matching entry is
	 * found.
	 * 
	 * TODO rewrite this to be non-recursive? Make sure it doesn't slow it down.
	 */
	private boolean intersects(final Rectangle r, final TIntProcedure v, final Node n) {
		for (int i = 0; i < n.entryCount; i++) {
			if (Rectangle.intersects(r.minX, r.minY, r.maxX, r.maxY, n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i])) {
				if (n.isLeaf()) {
					if (!v.execute(n.ids[i])) {
						return false;
					}
				} else {
					final Node childNode = getNode(n.ids[i]);
					if (!intersects(r, v, childNode)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Used by delete(). Ensures that all nodes from the passed node up to the
	 * root have the minimum number of entries.
	 * 
	 * Note that the parent and parentEntry stacks are expected to contain the
	 * nodeIds of all parents up to the root.
	 */
	private void condenseTree(final Node l) {
		// CT1 [Initialize] Set n=l. Set the list of eliminated
		// nodes to be empty.
		Node n = l;
		Node parent = null;
		int parentEntry = 0;

		final TIntStack eliminatedNodeIds = new TIntArrayStack();

		// CT2 [Find parent entry] If N is the root, go to CT6. Otherwise
		// let P be the parent of N, and let En be N's entry in P
		while (n.level != treeHeight) {
			parent = getNode(parents.pop());
			parentEntry = parentsEntry.pop();

			// CT3 [Eliminiate under-full node] If N has too few entries,
			// delete En from P and add N to the list of eliminated nodes
			if (n.entryCount < minNodeEntries) {
				parent.deleteEntry(parentEntry);
				eliminatedNodeIds.push(n.nodeId);
			} else {
				// CT4 [Adjust covering rectangle] If N has not been eliminated,
				// adjust EnI to tightly contain all entries in N
				if (n.mbrMinX != parent.entriesMinX[parentEntry] || n.mbrMinY != parent.entriesMinY[parentEntry] || n.mbrMaxX != parent.entriesMaxX[parentEntry] || n.mbrMaxY != parent.entriesMaxY[parentEntry]) {
					final double deletedMinX = parent.entriesMinX[parentEntry];
					final double deletedMinY = parent.entriesMinY[parentEntry];
					final double deletedMaxX = parent.entriesMaxX[parentEntry];
					final double deletedMaxY = parent.entriesMaxY[parentEntry];
					parent.entriesMinX[parentEntry] = n.mbrMinX;
					parent.entriesMinY[parentEntry] = n.mbrMinY;
					parent.entriesMaxX[parentEntry] = n.mbrMaxX;
					parent.entriesMaxY[parentEntry] = n.mbrMaxY;
					parent.recalculateMBRIfInfluencedBy(deletedMinX, deletedMinY, deletedMaxX, deletedMaxY);
				}
			}
			// CT5 [Move up one level in tree] Set N=P and repeat from CT2
			n = parent;
		}

		// CT6 [Reinsert orphaned entries] Reinsert all entries of nodes in set
		// Q.
		// Entries from eliminated leaf nodes are reinserted in tree leaves as
		// in
		// Insert(), but entries from higher level nodes must be placed higher
		// in
		// the tree, so that leaves of their dependent subtrees will be on the
		// same
		// level as leaves of the main tree
		while (eliminatedNodeIds.size() > 0) {
			final Node e = getNode(eliminatedNodeIds.pop());
			for (int j = 0; j < e.entryCount; j++) {
				add(e.entriesMinX[j], e.entriesMinY[j], e.entriesMaxX[j], e.entriesMaxY[j], e.ids[j], e.level);
				e.ids[j] = -1;
			}
			e.entryCount = 0;
			deletedNodeIds.push(e.nodeId);
		}
	}

	/**
	 * Used by add(). Chooses a leaf to add the rectangle to.
	 */
	private Node chooseNode(final double minX, final double minY, final double maxX, final double maxY, final int level) {
		// CL1 [Initialize] Set N to be the root node
		Node n = getNode(rootNodeId);
		parents.clear();
		parentsEntry.clear();

		// CL2 [Leaf check] If N is a leaf, return N
		while (true) {
			if (n.level == level) {
				return n;
			}

			// CL3 [Choose subtree] If N is not at the desired level, let F be
			// the entry in N
			// whose rectangle FI needs least enlargement to include EI. Resolve
			// ties by choosing the entry with the rectangle of smaller area.
			double leastEnlargement = Rectangle.enlargement(n.entriesMinX[0], n.entriesMinY[0], n.entriesMaxX[0], n.entriesMaxY[0], minX, minY, maxX, maxY);
			int index = 0; // index of rectangle in subtree
			for (int i = 1; i < n.entryCount; i++) {
				final double tempMinX = n.entriesMinX[i];
				final double tempMinY = n.entriesMinY[i];
				final double tempMaxX = n.entriesMaxX[i];
				final double tempMaxY = n.entriesMaxY[i];
				final double tempEnlargement = Rectangle.enlargement(tempMinX, tempMinY, tempMaxX, tempMaxY, minX, minY, maxX, maxY);
				if ((tempEnlargement < leastEnlargement) || ((tempEnlargement == leastEnlargement) && (Rectangle.area(tempMinX, tempMinY, tempMaxX, tempMaxY) < Rectangle.area(n.entriesMinX[index], n.entriesMinY[index], n.entriesMaxX[index], n.entriesMaxY[index])))) {
					index = i;
					leastEnlargement = tempEnlargement;
				}
			}

			parents.push(n.nodeId);
			parentsEntry.push(index);

			// CL4 [Descend until a leaf is reached] Set N to be the child node
			// pointed to by Fp and repeat from CL2
			n = getNode(n.ids[index]);
		}
	}

	/**
	 * Ascend from a leaf node L to the root, adjusting covering rectangles and
	 * propagating node splits as necessary.
	 */
	private Node adjustTree(Node n, Node nn) {
		// AT1 [Initialize] Set N=L. If L was split previously, set NN to be
		// the resulting second node.

		// AT2 [Check if done] If N is the root, stop
		while (n.level != treeHeight) {

			// AT3 [Adjust covering rectangle in parent entry] Let P be the
			// parent
			// node of N, and let En be N's entry in P. Adjust EnI so that it
			// tightly
			// encloses all entry rectangles in N.
			Node parent = getNode(parents.pop());
			final int entry = parentsEntry.pop();

			if (parent.entriesMinX[entry] != n.mbrMinX || parent.entriesMinY[entry] != n.mbrMinY || parent.entriesMaxX[entry] != n.mbrMaxX || parent.entriesMaxY[entry] != n.mbrMaxY) {

				parent.entriesMinX[entry] = n.mbrMinX;
				parent.entriesMinY[entry] = n.mbrMinY;
				parent.entriesMaxX[entry] = n.mbrMaxX;
				parent.entriesMaxY[entry] = n.mbrMaxY;

				parent.recalculateMBR();
			}

			// AT4 [Propagate node split upward] If N has a partner NN resulting
			// from
			// an earlier split, create a new entry Enn with Ennp pointing to NN
			// and
			// Enni enclosing all rectangles in NN. Add Enn to P if there is
			// room.
			// Otherwise, invoke splitNode to produce P and PP containing Enn
			// and
			// all P's old entries.
			Node newNode = null;
			if (nn != null) {
				if (parent.entryCount < maxNodeEntries) {
					parent.addEntry(nn.mbrMinX, nn.mbrMinY, nn.mbrMaxX, nn.mbrMaxY, nn.nodeId);
				} else {
					newNode = splitNode(parent, nn.mbrMinX, nn.mbrMinY, nn.mbrMaxX, nn.mbrMaxY, nn.nodeId);
				}
			}

			// AT5 [Move up to next level] Set N = P and set NN = PP if a split
			// occurred. Repeat from AT2
			n = parent;
			nn = newNode;

			parent = null;
			newNode = null;
		}

		return nn;
	}

	/**
	 * Check the consistency of the tree.
	 * 
	 * @return false if an inconsistency is detected, true otherwise.
	 */
	public boolean checkConsistency() {
		return checkConsistency(rootNodeId, treeHeight, null);
	}

	private boolean checkConsistency(final int nodeId, final int expectedLevel, final Rectangle expectedMBR) {
		// go through the tree, and check that the internal data structures of
		// the tree are not corrupted.
		final Node n = getNode(nodeId);

		// if tree is empty, then there should be exactly one node, at level 1
		// TODO: also check the MBR is as for a new node
		if (nodeId == rootNodeId && size() == 0 && n.level != 1) {
			return false;
		}

		if (n.level != expectedLevel) {
			return false;
		}

		final Rectangle calculatedMBR = calculateMBR(n);
		final Rectangle actualMBR = new Rectangle();
		actualMBR.minX = n.mbrMinX;
		actualMBR.minY = n.mbrMinY;
		actualMBR.maxX = n.mbrMaxX;
		actualMBR.maxY = n.mbrMaxY;
		if (!actualMBR.equals(calculatedMBR)) {
			return false;
		}

		if (expectedMBR != null && !actualMBR.equals(expectedMBR)) {
			return false;
		}

		// Check for corruption where a parent entry is the same object as the
		// child MBR
		if (expectedMBR != null && actualMBR.sameObject(expectedMBR)) {
			return false;
		}

		for (int i = 0; i < n.entryCount; i++) {
			if (n.ids[i] == -1) {
				return false;
			}

			if (n.level > 1 && !checkConsistency(n.ids[i], n.level - 1, new Rectangle(n.entriesMinX[i], n.entriesMinY[i], n.entriesMaxX[i], n.entriesMaxY[i]))) { // if not a leaf
				return false;
			}
		}
		return true;
	}

	/**
	 * Given a node object, calculate the node MBR from it's entries. Used in
	 * consistency checking
	 */
	private Rectangle calculateMBR(final Node n) {
		final Rectangle mbr = new Rectangle();

		for (int i = 0; i < n.entryCount; i++) {
			if (n.entriesMinX[i] < mbr.minX) {
				mbr.minX = n.entriesMinX[i];
			}
			if (n.entriesMinY[i] < mbr.minY) {
				mbr.minY = n.entriesMinY[i];
			}
			if (n.entriesMaxX[i] > mbr.maxX) {
				mbr.maxX = n.entriesMaxX[i];
			}
			if (n.entriesMaxY[i] > mbr.maxY) {
				mbr.maxY = n.entriesMaxY[i];
			}
		}
		return mbr;
	}
	
	private static class Node implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2823316966528817396L;
		private final int nodeId;
		private double mbrMinX = Float.MAX_VALUE;
		private double mbrMinY = Float.MAX_VALUE;
		private double mbrMaxX = -Float.MAX_VALUE;
		private double mbrMaxY = -Float.MAX_VALUE;

		private double[] entriesMinX;
		private double[] entriesMinY;
		private double[] entriesMaxX;
		private double[] entriesMaxY;

		private int[] ids;
		private final int level;
		private int entryCount;

		Node(final int nodeId, final int level, final int maxNodeEntries) {
			this.nodeId = nodeId;
			this.level = level;
			entriesMinX = new double[maxNodeEntries];
			entriesMinY = new double[maxNodeEntries];
			entriesMaxX = new double[maxNodeEntries];
			entriesMaxY = new double[maxNodeEntries];
			ids = new int[maxNodeEntries];
		}

		private void addEntry(final double minX, final double minY, final double maxX, final double maxY, final int id) {
			ids[entryCount] = id;
			entriesMinX[entryCount] = minX;
			entriesMinY[entryCount] = minY;
			entriesMaxX[entryCount] = maxX;
			entriesMaxY[entryCount] = maxY;

			if (minX < mbrMinX) {
				mbrMinX = minX;
			}
			if (minY < mbrMinY) {
				mbrMinY = minY;
			}
			if (maxX > mbrMaxX) {
				mbrMaxX = maxX;
			}
			if (maxY > mbrMaxY) {
				mbrMaxY = maxY;
			}

			entryCount++;
		}

		// Return the index of the found entry, or -1 if not found
		private int findEntry(final double minX, final double minY, final double maxX, final double maxY, final int id) {
			for (int i = 0; i < entryCount; i++) {
				if (id == ids[i] && entriesMinX[i] == minX && entriesMinY[i] == minY && entriesMaxX[i] == maxX && entriesMaxY[i] == maxY) {
					return i;
				}
			}
			return -1;
		}

		// delete entry. This is done by setting it to null and copying the last
		// entry into its space.
		private void deleteEntry(final int i) {
			final int lastIndex = entryCount - 1;
			final double deletedMinX = entriesMinX[i];
			final double deletedMinY = entriesMinY[i];
			final double deletedMaxX = entriesMaxX[i];
			final double deletedMaxY = entriesMaxY[i];

			if (i != lastIndex) {
				entriesMinX[i] = entriesMinX[lastIndex];
				entriesMinY[i] = entriesMinY[lastIndex];
				entriesMaxX[i] = entriesMaxX[lastIndex];
				entriesMaxY[i] = entriesMaxY[lastIndex];
				ids[i] = ids[lastIndex];
			}
			entryCount--;

			// adjust the MBR
			recalculateMBRIfInfluencedBy(deletedMinX, deletedMinY, deletedMaxX, deletedMaxY);
		}

		// deletedMin/MaxX/Y is a rectangle that has just been deleted or made
		// smaller.
		// Thus, the MBR is only recalculated if the deleted rectangle influenced
		// the old MBR
		private void recalculateMBRIfInfluencedBy(final double deletedMinX, final double deletedMinY, final double deletedMaxX, final double deletedMaxY) {
			if (mbrMinX == deletedMinX || mbrMinY == deletedMinY || mbrMaxX == deletedMaxX || mbrMaxY == deletedMaxY) {
				recalculateMBR();
			}
		}

		private void recalculateMBR() {
			mbrMinX = entriesMinX[0];
			mbrMinY = entriesMinY[0];
			mbrMaxX = entriesMaxX[0];
			mbrMaxY = entriesMaxY[0];

			for (int i = 1; i < entryCount; i++) {
				if (entriesMinX[i] < mbrMinX) {
					mbrMinX = entriesMinX[i];
				}
				if (entriesMinY[i] < mbrMinY) {
					mbrMinY = entriesMinY[i];
				}
				if (entriesMaxX[i] > mbrMaxX) {
					mbrMaxX = entriesMaxX[i];
				}
				if (entriesMaxY[i] > mbrMaxY) {
					mbrMaxY = entriesMaxY[i];
				}
			}
		}

		/**
		 * eliminate null entries, move all entries to the start of the source node
		 */
		private void reorganize(final RTree rtree) {
			int countdownIndex = rtree.maxNodeEntries - 1;
			for (int index = 0; index < entryCount; index++) {
				if (ids[index] == -1) {
					while (ids[countdownIndex] == -1 && countdownIndex > index) {
						countdownIndex--;
					}
					entriesMinX[index] = entriesMinX[countdownIndex];
					entriesMinY[index] = entriesMinY[countdownIndex];
					entriesMaxX[index] = entriesMaxX[countdownIndex];
					entriesMaxY[index] = entriesMaxY[countdownIndex];
					ids[index] = ids[countdownIndex];
					ids[countdownIndex] = -1;
				}
			}
		}

		private boolean isLeaf() {
			return (level == 1);
		}

	}
}
