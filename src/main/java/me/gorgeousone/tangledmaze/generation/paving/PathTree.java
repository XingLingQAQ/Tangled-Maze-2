package me.gorgeousone.tangledmaze.generation.paving;

import me.gorgeousone.tangledmaze.generation.GridCell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Class to keep track of connections of maze paths
 */
public class PathTree {
	
	private final List<GridCell> openEnds;
	private final Set<GridCell> cells;
	private final Set<GridCell> junctions;
	
	private int maxExitDist;
	private int size;
	
	public PathTree() {
		openEnds = new ArrayList<>();
		cells = new HashSet<>();
		junctions = new HashSet<>();
	}
	
	public int size() {
		return size;
	}
	
	/**
	 * Returns true if no paths can be added at any path end anymore.
	 */
	public boolean isComplete() {
		return openEnds.isEmpty();
	}


	public void addSegment(GridCell cell, GridCell parent) {
		addSegment(cell, parent, true, true);
	}

	/**
	 * Adds a new path segment to the tree with the previous path segment as parent.
	 * Also adds junction segments to the list of open ends / junctions to explore.
	 * @param canBeJunction set false, if this cell should not be counted as a junction (e.g. inside rooms)
	 * @param incrementSize set false, if this cell should not be counted as part of the tree (e.g. room elements)
	 */
	public void addSegment(GridCell cell, GridCell parent, boolean canBeJunction, boolean incrementSize) {
		cell.setTree(this);
		cell.setParent(parent);
		cells.add(cell);
		maxExitDist = Math.max(cell.getExitDist(), maxExitDist);

		if (canBeJunction && cell.gridX() % 2 == 0 && cell.gridZ() % 2 == 0) {
			junctions.add(cell);
			openEnds.add(0, cell);
		}
		if (incrementSize) {
			++size;
		}
	}
	
	public Set<GridCell> getCells() {
		return cells;
	}
	
	/**
	 * Returns all path cells that are possibly junctions connecting multiple other path cells.
	 */
	public Set<GridCell> getJunctions() {
		return junctions;
	}

	public GridCell getLastEnd() {
		return openEnds.get(0);
	}
	
	public GridCell getRndEnd(Random random) {
		return openEnds.get(random.nextInt(openEnds.size()));
	}
	
	public void removeEnd(GridCell pathEnd) {
		openEnds.remove(pathEnd);
	}
	
	public void mergeTree(PathTree other, GridCell ownSegment, GridCell otherSegment, GridCell linkSegment) {
		for (GridCell cell : other.cells) {
			cell.setTree(this);
		}
		cells.addAll(other.cells);
		junctions.addAll(other.junctions);
		addSegment(linkSegment, ownSegment);
		balanceTree(linkSegment, otherSegment);
	}
	
	/**
	 * Upon merging two trees, this method updates the parents of cells around the merge point
	 * to keep the distance to the nearest exit of the tree as small as possible.
	 */
	private void balanceTree(GridCell seg1, GridCell seg2) {
		int exitDist1 = seg1.getExitDist();
		int exitDist2 = seg2.getExitDist();
		int distDiff = Math.abs(exitDist2 - exitDist1);
		maxExitDist = Math.max(maxExitDist, (int) Math.ceil(exitDist2 + exitDist1 / 20d));
		
		GridCell furtherSeg = exitDist1 > exitDist2 ? seg1 : seg2;
		GridCell closerSeg = exitDist1 <= exitDist2 ? seg1 : seg2;
		
		for (int i = 0; i < distDiff / 2; i++) {
			GridCell oldParent = furtherSeg.getParent();
			furtherSeg.setParent(closerSeg);
			
			closerSeg = furtherSeg;
			furtherSeg = oldParent;
		}
	}
}
