package me.gorgeousone.tangledmaze.loot;

import me.gorgeousone.tangledmaze.SessionHandler;
import me.gorgeousone.tangledmaze.clip.Clip;
import me.gorgeousone.tangledmaze.cmdframework.command.BaseCommand;
import me.gorgeousone.tangledmaze.data.Message;
import me.gorgeousone.tangledmaze.maze.MazeBackup;
import me.gorgeousone.tangledmaze.util.text.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class LootRespawnCommand extends BaseCommand {

	private final SessionHandler sessionHandler;
	private final LootHandler lootHandler;
	private final boolean isAvailable;

	public LootRespawnCommand(SessionHandler sessionHandler, LootHandler lootHandler, boolean isAvailable) {
		super("respawn");
		this.sessionHandler = sessionHandler;
		this.lootHandler = lootHandler;
		this.isAvailable = isAvailable;
	}

	@Override
	protected void onCommand(CommandSender sender, String[] args) {
		if (!isAvailable) {
			Message.INFO_LOOT_PLUGIN_NOT_FOUND.sendTo(sender);
			return;
		}
		UUID playerId = getSenderId(sender);
		Clip maze = sessionHandler.getMazeClip(playerId);

		if (null == maze) {
			Message.ERROR_MAZE_MISSING.sendTo(sender);
			return;
		}
		if (!sessionHandler.isBuilt(maze)) {
			Message.INFO_MAZE_NOT_BUILT.sendTo(sender);
			return;
		}
		MazeBackup backup = sessionHandler.getBackup(maze);
		lootHandler.respawnChests(backup.getLootLocations().keySet());
		Message.INFO_LOOT_RESPAWN.sendTo(sender, new Placeholder("count", backup.getLootLocations().size()));
	}
}
