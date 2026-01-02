package dev.kuwa.playerTimeLimit.api;

import org.bukkit.entity.Player;

import dev.kuwa.playerTimeLimit.PlayerTimeLimit;
import dev.kuwa.playerTimeLimit.managers.PlayerManager;
import dev.kuwa.playerTimeLimit.model.TimeLimitPlayer;
import dev.kuwa.playerTimeLimit.utils.UtilsTime;

public class PlayerTimeLimitAPI {

	private static PlayerTimeLimit plugin;
	public PlayerTimeLimitAPI(PlayerTimeLimit plugin) {
		this.plugin = plugin;
	}
	
	public static String getTimeLeft(Player player) {
		PlayerManager playerManager = plugin.getPlayerManager();
		TimeLimitPlayer p = playerManager.getPlayerByUUID(player.getUniqueId().toString());
		int timeLimit = playerManager.getTimeLimitPlayer(player);
		return playerManager.getTimeLeft(p, timeLimit);
	}
	
	public static String getTotalTime(Player player) {
		PlayerManager playerManager = plugin.getPlayerManager();
		TimeLimitPlayer p = playerManager.getPlayerByUUID(player.getUniqueId().toString());
		return UtilsTime.getTime(p.getTotalTime(), plugin.getMensajesManager());
	}
}
