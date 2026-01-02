package dev.kuwa.playerTimeLimit.libs.actionbar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import dev.kuwa.playerTimeLimit.PlayerTimeLimit;
import dev.kuwa.playerTimeLimit.managers.MensajesManager;

public class ActionBarAPI
{

	@SuppressWarnings("deprecation")
	public static void sendActionBar(Player player, String message) {
		if (player == null || !player.isOnline()) return;

		// 非同期なら同期に投げ直す
		if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(PlayerTimeLimit.getInstance(), () -> sendActionBar(player, message));
			return;
		}

		// Colorize / message preprocessing
		String colored = MensajesManager.getMensajeColor(message);
		// Try Adventure API first (Paper 1.16+ / modern)
		try {
			// Player#sendActionBar(Component) exists on modern Paper/Spigot with Adventure
			player.sendActionBar(Component.text(colored));
			return;
		} catch (NoSuchMethodError | NoClassDefFoundError | UnsupportedOperationException ignored) {
			// not available - fallback
		} catch (Throwable t) {
			// in case sendActionBar(Component) exists but fails, don't crash plugin
			t.printStackTrace();
		}

		// Try Spigot (1.16 - 1.19) action bar via Bungee ChatMessageType
		try {
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colored));
			return;
		} catch (NoSuchMethodError | NoClassDefFoundError ignored) {
			// not available - fallback
		} catch (Throwable t) {
			t.printStackTrace();
		}

		// Fallback: try legacy NMS packet (only attempt when we can extract v#_#_R# style)
		String nmsver = extractNMSVersion();
		if (nmsver != null && !nmsver.isEmpty()) {
			try {
				// legacy flow (from your original implementation) but guarded
				boolean useOldMethods = (nmsver.equalsIgnoreCase("v1_8_R1") || nmsver.startsWith("v1_7_"));

				// Call the event, if cancelled don't send Action Bar
				try {
					ActionBarMessageEvent actionBarMessageEvent = new ActionBarMessageEvent(player, colored);
					Bukkit.getPluginManager().callEvent(actionBarMessageEvent);
					if (actionBarMessageEvent.isCancelled()) return;
				} catch (IllegalStateException ise) {
					// If for some reason callEvent fails (shouldn't on main thread), skip event but continue sending
				}

				Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsver + ".entity.CraftPlayer");
				Object craftPlayer = craftPlayerClass.cast(player);
				Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + nmsver + ".PacketPlayOutChat");
				Class<?> packetClass = Class.forName("net.minecraft.server." + nmsver + ".Packet");
				Object packet;

				if (useOldMethods) {
					Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + nmsver + ".ChatSerializer");
					Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
					Method m3 = chatSerializerClass.getDeclaredMethod("a", String.class);
					Object cbc = iChatBaseComponentClass.cast(m3.invoke(chatSerializerClass, "{\"text\": \"" + colored + "\"}"));
					packet = packetPlayOutChatClass.getConstructor(new Class<?>[]{iChatBaseComponentClass, byte.class}).newInstance(cbc, (byte) 2);
				} else {
					Class<?> chatComponentTextClass = Class.forName("net.minecraft.server." + nmsver + ".ChatComponentText");
					Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
					try {
						Class<?> chatMessageTypeClass = Class.forName("net.minecraft.server." + nmsver + ".ChatMessageType");
						Object[] chatMessageTypes = chatMessageTypeClass.getEnumConstants();
						Object chatMessageType = null;
						for (Object obj : chatMessageTypes) {
							if (obj.toString().equalsIgnoreCase("GAME_INFO")) {
								chatMessageType = obj;
								break;
							}
						}
						Object chatCompontentText = chatComponentTextClass.getConstructor(new Class<?>[]{String.class}).newInstance(colored);
						if (chatMessageType != null) {
							packet = packetPlayOutChatClass.getConstructor(new Class<?>[]{iChatBaseComponentClass, chatMessageTypeClass}).newInstance(chatCompontentText, chatMessageType);
						} else {
							packet = packetPlayOutChatClass.getConstructor(new Class<?>[]{iChatBaseComponentClass, byte.class}).newInstance(chatCompontentText, (byte) 2);
						}
					} catch (ClassNotFoundException cnfe) {
						Object chatCompontentText = chatComponentTextClass.getConstructor(new Class<?>[]{String.class}).newInstance(colored);
						packet = packetPlayOutChatClass.getConstructor(new Class<?>[]{iChatBaseComponentClass, byte.class}).newInstance(chatCompontentText, (byte) 2);
					}
				}

				Method craftPlayerHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
				Object craftPlayerHandle = craftPlayerHandleMethod.invoke(craftPlayer);
				Field playerConnectionField = craftPlayerHandle.getClass().getDeclaredField("playerConnection");
				Object playerConnection = playerConnectionField.get(craftPlayerHandle);
				Method sendPacketMethod = playerConnection.getClass().getDeclaredMethod("sendPacket", packetClass);
				sendPacketMethod.invoke(playerConnection, packet);
				return;
			} catch (ClassNotFoundException cnf) {
				// NMS classes not found -> skip to last fallback
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		// Last resort: don't crash, send as normal chat message (or nothing)
		player.sendMessage(colored); // 保険：ActionBarが無理なら通常メッセージで通知
	}

	/**
	 * Try to extract the legacy NMS version token (v1_XX_RX).
	 * This returns null when we're running on modern remapped servers (Paper 1.21+), so the caller
	 * will skip NMS reflection in that case.
	 */
	private static String extractNMSVersion() {
		// Try to get package like org.bukkit.craftbukkit.v1_20_R2
		try {
			String pkg = Bukkit.getServer().getClass().getPackage().getName(); // often org.bukkit.craftbukkit.v1_20_R2
			if (pkg != null) {
				// find v1_xx_rx pattern
				Pattern p = Pattern.compile("(v1_\\d+_R\\d+)");
				Matcher m = p.matcher(pkg);
				if (m.find()) {
					return m.group(1);
				}
			}
		} catch (Throwable ignored) {}

		// As a fallback, try to inspect class name for v1_ pattern
		try {
			String className = Bukkit.getServer().getClass().getName(); // fully qualified class name
			Pattern p = Pattern.compile("(v1_\\d+_R\\d+)");
			Matcher m = p.matcher(className);
			if (m.find()) {
				return m.group(1);
			}
		} catch (Throwable ignored) {}

		// If we can't find v1_... token, return null -> indicates modern remapped server, skip NMS
		return null;
	}
	public static void sendActionBar(final Player player, final String message, int duration,PlayerTimeLimit plugin) {
		sendActionBar(player, message);

		if (duration >= 0) {
			// Sends empty message at the end of the duration. Allows messages shorter than 3 seconds, ensures precision.
			new BukkitRunnable() {
				@Override
				public void run() {
					sendActionBar(player, "");
				}
			}.runTaskLater(plugin, duration + 1);
		}

		// Re-sends the messages every 3 seconds so it doesn't go away from the player's screen.
		while (duration > 40) {
			duration -= 40;
			new BukkitRunnable() {
				@Override
				public void run() {
					sendActionBar(player, message);
				}
			}.runTaskLater(plugin, (long) duration);
		}
	}

}
