package me.gorgeousone.tangledmaze.clip;

import me.gorgeousone.tangledmaze.util.BlockUtil;
import me.gorgeousone.tangledmaze.util.Direction;
import me.gorgeousone.tangledmaze.util.MathUtil;
import me.gorgeousone.tangledmaze.util.Vec2;
import org.bukkit.block.Block;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A factory class for creating clip actions for merging, cutting away or expanding clips.
 */
public class ClipActionFactory {
	
	private ClipActionFactory() {}
	
	public static boolean canBeExit(Clip clip, Vec2 loc) {
		ClipAction noChanges = new ClipAction(clip);
		return sealsClipBorder(noChanges, loc, Direction.CARDINALS);
	}
	
	/**
	 * Creates a clip action for merging the given clip into the given maze.
	 * Returns null if the clip and maze are not in the same world.
	 */
	public static ClipAction addClip(Clip maze, Clip clip) {
		if (!maze.getWorld().equals(clip.getWorld())) {
			return null;
		}
		ClipAction addition = new ClipAction(maze);
		addProtrudingClipFill(addition, maze, clip);
		//return null if the clip is totally covered by the maze
		if (addition.getAddedFill().isEmpty()) {
			return addition;
		}
		addProtrudingClipBorder(addition, maze, clip);
		removeEnclosedMazeBorder(addition, maze);
		removeMazeExits(maze.getExits(), addition);
		return addition;
	}
	
	/**
	 * Adds all fill of the clip that is not contained by the maze to the addition
	 */
	private static void addProtrudingClipFill(ClipAction addition, Clip maze, Clip clip) {
		for (Map.Entry<Vec2, Integer> fillBlock : clip.getFill().entrySet()) {
			Vec2 fillLoc = fillBlock.getKey();
			if (!maze.contains(fillLoc)) {
				addition.addFill(fillLoc, fillBlock.getValue());
			}
		}
	}
	
	/**
	 * Adds all border of the clip that is not contained by the maze to the addition
	 */
	private static void addProtrudingClipBorder(ClipAction addition, Clip maze, Clip clip) {
		for (Vec2 clipBorder : clip.getBorder()) {
			if (!maze.contains(clipBorder)) {
				addition.addBorder(clipBorder);
			}
		}
	}
	
	/**
	 * Removes maze border that won't seal the maze after merging with the clip anymore.
	 */
	private static void removeEnclosedMazeBorder(ClipAction addition, Clip maze) {
		for (Vec2 mazeBorder : maze.getBorder()) {
			if (!touchesExternal(addition, mazeBorder, Direction.values())) {
				addition.removeBorder(mazeBorder);
			}
		}
		addition.getAddedBorder().removeIf(newBorder -> !touchesExternal(addition, newBorder, Direction.values()));
	}
	
	/**
	 * Removes maze exits that won't be lying on maze border after the clip action is applied
	 */
	private static void removeMazeExits(List<Vec2> exits, ClipAction changes) {
		for (Vec2 exit : exits) {
			if (!sealsClipBorder(changes, exit, Direction.CARDINALS)) {
				changes.removeExit(exit);
			}
		}
	}
	
	/**
	 * Creates a clip action for cutting away the area inside this clip from the given maze
	 */
	public static ClipAction removeClip(Clip maze, Clip clip) {
		if (!maze.getWorld().equals(clip.getWorld())) {
			return null;
		}
		ClipAction deletion = new ClipAction(maze);
		removeOverlappingClipFill(deletion, maze, clip);
		
		if (deletion.getRemovedFill().isEmpty()) {
			return deletion;
		}
		addIntersectingClipBorder(deletion, maze, clip);
		removeExcludedMazeBorder(deletion, maze, clip);
		removeMazeExits(maze.getExits(), deletion);
		return deletion;
	}
	
	/**
	 * Removes all fill of the clip (without border) that overlaps with the maze from the deletion
	 */
	private static void removeOverlappingClipFill(ClipAction deletion, Clip maze, Clip clip) {
		for (Map.Entry<Vec2, Integer> clipFill : clip.getFill().entrySet()) {
			if (!clip.borderContains(clipFill.getKey()) && maze.contains(clipFill.getKey())) {
				deletion.removeFill(clipFill.getKey(), clipFill.getValue());
			}
		}
	}
	
	/**
	 * Adds new border to the deletion where the clips edge cuts into the maze
	 */
	private static void addIntersectingClipBorder(ClipAction deletion, Clip maze, Clip clip) {
		for (Vec2 clipBorder : clip.getBorder()) {
			if (!maze.borderContains(clipBorder) && maze.contains(clipBorder)) {
				deletion.addBorder(clipBorder);
			}
		}
		//remove every part of the new added border not sealing the maze anyway
		Iterator<Vec2> iter = deletion.getAddedBorder().iterator();
		
		while (iter.hasNext()) {
			Vec2 newBorder = iter.next();
			
			if (!touchesFill(deletion, newBorder, Direction.values())) {
				iter.remove();
				deletion.removeFill(newBorder, maze.getY(newBorder));
			}
		}
	}
	
	/**
	 * Removes maze border lying inside the cut away clip area from the deletion
	 */
	private static void removeExcludedMazeBorder(ClipAction deletion, Clip maze, Clip clip) {
		for (Vec2 mazeBorder : maze.getBorder()) {
			if (clip.contains(mazeBorder) && !touchesFill(deletion, mazeBorder, Direction.values())) {
				deletion.removeBorder(mazeBorder);
				deletion.removeFill(mazeBorder, maze.getY(mazeBorder));
			}
		}
	}
	
	public static ClipAction brushBorder(Clip maze, Block block, double playerYaw) {
		if (!maze.isBorderBlock(block)) {
			return null;
		}
		Direction dir = maze.getBorderFacing(new Vec2(block));

		if (dir == null) {
			return null;
		}
		double borderAngle = dir.getVec2().getMcAngle();
		double absDelta = Math.abs(getDeltaAngle(playerYaw, borderAngle));

		if (absDelta < 90) {
			return addClip(maze, create3x3Square(maze, block));
		}
		return removeClip(maze, create3x3Square(maze, block));
	}

	/**
	 * Returns the smallest angle between two angles in degrees, always between -180 and 180.
	 */
	private static double getDeltaAngle(double deg1, double deg2) {
		double delta = deg2 - deg1;
		delta = MathUtil.floorMod(delta + 180, 360) - 180;
		return delta;
	}

	private static Clip create3x3Square(Clip maze, Block block) {
		Clip square = new Clip(null, maze.getWorld());
		int midX = block.getX();
		int midZ = block.getZ();
		int midY = block.getY();
		
		for (int dx = -1; dx <= 1; ++dx) {
			for (int dz = -1; dz <= 1; ++dz) {
				Vec2 loc = new Vec2(midX + dx, midZ + dz);
				int y = maze.getY(loc);
				
				if (-1 == y) {
					y = BlockUtil.getSurfaceY(maze.getWorld(), loc, midY);
				}
				square.add(loc.getX(), loc.getZ(), y);
				
				if (dx != 0 || dz != 0) {
					square.addBorder(loc);
				}
			}
		}
		return square;
	}
	
	public static boolean sealsClipBorder(ClipAction changes, Vec2 loc, Direction[] directions) {
		boolean touchesFill = false;
		boolean touchesExternal = false;
		
		for (Direction dir : directions) {
			Vec2 neighbor = loc.clone().add(dir.getVec2());
			
			if (!changes.clipWillContain(neighbor)) {
				touchesExternal = true;
			} else if (!changes.clipBorderWillContain(neighbor)) {
				touchesFill = true;
			}
			if (touchesFill && touchesExternal) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean touchesFill(ClipAction changes, Vec2 loc, Direction[] directions) {
		for (Direction dir : directions) {
			Vec2 neighbor = loc.clone().add(dir.getVec2());
			
			if (!changes.clipBorderWillContain(neighbor) && changes.clipWillContain(neighbor)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean touchesExternal(ClipAction changes, Vec2 loc, Direction[] directions) {
		for (Direction dir : directions) {
			Vec2 neighbor = loc.clone().add(dir.getVec2());
			
			if (!changes.clipWillContain(neighbor)) {
				return true;
			}
		}
		return false;
	}
}
