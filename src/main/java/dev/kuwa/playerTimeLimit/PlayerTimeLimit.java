package dev.kuwa.playerTimeLimit;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import dev.kuwa.playerTimeLimit.api.ExpansionPlayerTimeLimit;
import dev.kuwa.playerTimeLimit.api.PlayerTimeLimitAPI;
import dev.kuwa.playerTimeLimit.configs.ConfigsManager;
import dev.kuwa.playerTimeLimit.listeners.PlayerListener;
import dev.kuwa.playerTimeLimit.managers.MensajesManager;
import dev.kuwa.playerTimeLimit.managers.PlayerManager;
import dev.kuwa.playerTimeLimit.managers.ServerManager;
import dev.kuwa.playerTimeLimit.tasks.DataSaveTask;
import dev.kuwa.playerTimeLimit.tasks.PlayerTimeTask;
import dev.kuwa.playerTimeLimit.tasks.ServerTimeResetTask;



public class PlayerTimeLimit extends JavaPlugin {

	private static PlayerTimeLimit instance;
	PluginDescriptionFile pdfFile = getDescription();
	public String version = pdfFile.getVersion();
	public String latestversion;
	
	public String rutaConfig;
	
	private PlayerManager playerManager;
	private ConfigsManager configsManager;
	private MensajesManager mensajesManager;
	private ServerManager serverManager;
	
	private DataSaveTask dataSaveTask;
	
	public static String nombrePlugin = ChatColor.translateAlternateColorCodes('&', "&8[&bPlayerTime&cLimit&8] ");
	
	public void onEnable(){
		instance = this;

	   this.playerManager = new PlayerManager(this);
	   this.serverManager = new ServerManager(this);
	   registerEvents();
	   registerCommands();
	   registerConfig();
	   this.configsManager = new ConfigsManager(this);
	   this.configsManager.configurar();
	   
	   serverManager.executeDataTime();
	   
	   PlayerTimeTask timeTask = new PlayerTimeTask(this);
	   timeTask.start();
	   ServerTimeResetTask serverTask = new ServerTimeResetTask(this);
	   serverTask.start();
	   
	   recargarDataSaveTask();
	   checkMessagesUpdate();
	   
	   PlayerTimeLimitAPI api = new PlayerTimeLimitAPI(this);
	   if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
		   new ExpansionPlayerTimeLimit(this).register();
	   }
	   
	   Bukkit.getConsoleSender().sendMessage(nombrePlugin+ChatColor.YELLOW + "Has been enabled! " + ChatColor.WHITE + "Version: " + version);
	   Bukkit.getConsoleSender().sendMessage(nombrePlugin+ChatColor.YELLOW + "Thanks for using my plugin!  " + ChatColor.WHITE + "~Ajneb97");
	   
	   updateChecker();
	}
	  
	public void onDisable(){
		this.configsManager.getPlayerConfigsManager().guardarJugadores();
		serverManager.saveDataTime();
		Bukkit.getConsoleSender().sendMessage(nombrePlugin+ChatColor.YELLOW + "Has been disabled! " + ChatColor.WHITE + "Version: " + version);
	}
	public void registerCommands(){
		this.getCommand("playertimelimit").setExecutor(new Comando(this));
	}
	
	public void registerEvents(){
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new PlayerListener(this), this);
	}
	
	public void registerConfig(){	
		File config = new File(this.getDataFolder(), "config.yml");
		rutaConfig = config.getPath();
		if(!config.exists()){
			this.getConfig().options().copyDefaults(true);
			saveConfig();  
		}
	}
	
	public void recargarConfigs() {
		this.configsManager.getMensajesConfigManager().reloadMessages();
		this.configsManager.getPlayerConfigsManager().guardarJugadores();
		reloadConfig();
		this.configsManager.getMainConfigManager().configurar();
		
		recargarDataSaveTask();
	}
	
	public void recargarDataSaveTask() {
		if(dataSaveTask != null) {
			dataSaveTask.end();
		}
		dataSaveTask = new DataSaveTask(this);
		dataSaveTask.start(getConfig().getInt("data_save_time"));
	}

	public static PlayerTimeLimit getInstance() {
		return instance;
	}

	public PlayerManager getPlayerManager() {
		return playerManager;
	}

	public MensajesManager getMensajesManager() {
		return mensajesManager;
	}

	public void setMensajesManager(MensajesManager mensajesManager) {
		this.mensajesManager = mensajesManager;
	}
	
	public FileConfiguration getMessages() {
		return this.configsManager.getMensajesConfigManager().getMessages();
	}

	public ConfigsManager getConfigsManager() {
		return configsManager;
	}

	public ServerManager getServerManager() {
		return serverManager;
	}
	
	public void checkMessagesUpdate(){
		  Path archivoConfig = Paths.get(rutaConfig);
		  Path archivoMessages = Paths.get(configsManager.getMensajesConfigManager().getPath());
		  try{
			  String textoConfig = new String(Files.readAllBytes(archivoConfig));
			  String textoMessages = new String(Files.readAllBytes(archivoMessages));
			  FileConfiguration messages = configsManager.getMensajesConfigManager().getMessages();
			  
			  if(!textoMessages.contains("commandResetTimeError:")){
				  messages.set("commandResetTimeError", "&cYou need to use: &7/ptl resettime <player>");
				  messages.set("commandResetTimeCorrect", "&aCurrent time has been reset for player &7%player%&a!");
				  messages.set("commandTakeTimeError", "&cYou need to use: &7/ptl taketime <player> <time>");
				  messages.set("invalidNumber", "&cYou need to use a valid number!");
				  messages.set("commandTakeTimeCorrect", "&aTaken &7%time% seconds &afrom &7%player% &atime!");
				  messages.set("playerNotOnline", "&cThat player is not online.");
				  messages.set("commandAddTimeError", "&cYou need to use: &7/ptl addtime <player> <time>");
				  messages.set("commandAddTimeCorrect", "&aAdded &7%time% seconds &ato &7%player% &atime!");
				  configsManager.getMensajesConfigManager().saveMessages();
			  }
			  if(!textoConfig.contains("world_whitelist_system:")){
				  getConfig().set("world_whitelist_system.enabled", false);
				  List<String> lista = new ArrayList<String>();
				  lista.add("world");lista.add("world_nether");lista.add("world_the_end");
				  getConfig().set("world_whitelist_system.worlds", lista);
				  getConfig().set("world_whitelist_system.teleport_coordinates_on_kick", "spawn;0;60;0;90;0");
				  saveConfig();
			  }
			  if(!textoConfig.contains("update_notification:")){
				  getConfig().set("update_notification", true);
				  saveConfig();
				  messages.set("playerDoesNotExists", "&cThat player doesn't exists.");
				  List<String> lista = new ArrayList<String>();
				  lista.add("&c&m                                          ");
				  lista.add("&7&l%player% Data:");
				  lista.add("&7Time left: &a%time_left%");
				  lista.add("&7Total played time: &a%total_time%");
				  lista.add("&c&m                                          ");
				  messages.set("checkCommandMessage", lista);
				  lista = new ArrayList<String>();
				  lista.add("&c&m                                          ");
				  lista.add("&7Exact time when playtimes will be reset:");
				  lista.add("&e%reset_time%");
				  lista.add("");
				  lista.add("&7Remaining time until reset:");
				  lista.add("&e%remaining%");
				  lista.add("&c&m                                          ");
				  messages.set("infoCommandMessage", lista);
				  configsManager.getMensajesConfigManager().saveMessages();
			  }
		  }catch(IOException e){
			  e.printStackTrace();
		  }
	}
	
//	public void updateChecker(){
//		try {
//			HttpURLConnection con = (HttpURLConnection) new URL(
//					"https://api.spigotmc.org/legacy/update.php?resource=96577").openConnection();
//			int timed_out = 1250;
//			con.setConnectTimeout(timed_out);
//			con.setReadTimeout(timed_out);
//			latestversion = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
//			if (latestversion.length() <= 7) {
//				if(!version.equals(latestversion)){
//					Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"There is a new version available. "+ChatColor.YELLOW+
//							"("+ChatColor.GRAY+latestversion+ChatColor.YELLOW+")");
//					Bukkit.getConsoleSender().sendMessage(ChatColor.RED+"You can download it at: "+ChatColor.WHITE+"https://www.spigotmc.org/resources/96577/");
//				}
//			}
//		} catch (Exception ex) {
//			Bukkit.getConsoleSender().sendMessage(nombrePlugin + ChatColor.RED +"Error while checking update.");
//		}
//	}

	public void updateChecker() {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			try {
				// --- 設定: GitHub のユーザー / リポジトリ / resourceId（spigot 旧チェック用） ---
				final String GITHUB_USER = "kuwacom";
				final String GITHUB_REPO = "PlayerTimeLimit";
				final String[] branches = new String[] {"main", "development"};
				final String[] targets = new String[] { "build.gradle", "plugin.yml" };
				final int TIMEOUT_MS = 2500;
				String remoteVersion = null;

				// try raw.githubusercontent URLs: build.gradle then plugin.yml; try branches main/master
				for (String branch : branches) {
					if (remoteVersion != null) break;
					for (String target : targets) {
						if (remoteVersion != null) break;
						String rawUrl = String.format("https://raw.githubusercontent.com/%s/%s/%s/%s",
								GITHUB_USER, GITHUB_REPO, branch, target);
						String content = fetchURL(rawUrl, TIMEOUT_MS);
						if (content == null) continue;
						// try parse
						if ("plugin.yml".equalsIgnoreCase(target)) {
							remoteVersion = parseVersionFromPluginYml(content);
						} else {
							remoteVersion = parseVersionFromBuildGradle(content);
						}
					}
				}

				// fallback: spigot legacy API (original behaviour) - フォーク元の検証
				if (remoteVersion == null) {
					try {
						String spigotUrl = "https://api.spigotmc.org/legacy/update.php?resource=96577"; // resource id
						String fallback = fetchURL(spigotUrl, TIMEOUT_MS);
						if (fallback != null && !fallback.isBlank()) {
							remoteVersion = fallback.trim();
						}
					} catch (Exception ignored) {}
				}

				latestversion = remoteVersion;
				if (latestversion == null) {
					// couldn't detect; log a friendly message on main thread
					Bukkit.getScheduler().runTask(this, () ->
							Bukkit.getConsoleSender().sendMessage(nombrePlugin + ChatColor.YELLOW + "Update check: could not detect remote version.")
					);
					return;
				}

				// compare versions semantically (non-numeric parts tolerated)
				int cmp = compareVersions(this.version, latestversion);

				Bukkit.getScheduler().runTask(this, () -> {
					if (cmp < 0) {
						// remote is newer
						Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "There is a new version available: " +
								ChatColor.GRAY + latestversion + ChatColor.RESET + ChatColor.YELLOW +
								" (you have " + ChatColor.GRAY + this.version + ChatColor.YELLOW + ")");
						Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Download: " + ChatColor.WHITE + "https://www.spigotmc.org/resources/96577/");
					} else if (cmp == 0) {
						// up to date (optional log)
						Bukkit.getConsoleSender().sendMessage(nombrePlugin + ChatColor.GREEN + "Plugin is up to date. (" + latestversion + ")");
					} else {
						// local is newer (dev build)
						Bukkit.getConsoleSender().sendMessage(nombrePlugin + ChatColor.AQUA + "You are running a newer version (" + this.version + ") than remote (" + latestversion + ").");
					}
				});

			} catch (Exception ex) {
				// network or parse error - log lightly on main thread
				Bukkit.getScheduler().runTask(this, () ->
						Bukkit.getConsoleSender().sendMessage(nombrePlugin + ChatColor.RED + "Error while checking update.")
				);
			}
		});
	}

	/** Fetches URL content as single String (timeout in ms). Returns null on failure. */
	private String fetchURL(String urlStr, int timeoutMs) {
		HttpURLConnection con = null;
		BufferedReader reader = null;
		try {
			URL url = new URL(urlStr);
			con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(timeoutMs);
			con.setReadTimeout(timeoutMs);
			con.setRequestProperty("User-Agent", "PlayerTimeLimit-UpdateChecker/1.0 (+https://github.com/)");
			int code = con.getResponseCode();
			if (code / 100 != 2) return null;
			InputStream is = con.getInputStream();
			reader = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
			return sb.toString();
		} catch (Exception e) {
			return null;
		} finally {
			try { if (reader != null) reader.close(); } catch (Exception ignored) {}
			if (con != null) con.disconnect();
		}
	}

	/** Parse version from plugin.yml content (simple regex "version: <value>") */
	private String parseVersionFromPluginYml(String content) {
		if (content == null) return null;
		// match lines like: version: 1.6.1
		Pattern p = Pattern.compile("^\\s*version\\s*:\\s*([\\w\\.-]+)\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(content);
		if (m.find()) return m.group(1).trim();
		return null;
	}

	/** Parse version from build.gradle content.
	 *  Handles:
	 *   version = '1.6.1'
	 *   version = "1.6.1"
	 *   version '1.6.1'
	 */
	private String parseVersionFromBuildGradle(String content) {
		if (content == null) return null;
		Pattern p = Pattern.compile("version\\s*[=\\s]*['\"]([\\w\\.-]+)['\"]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(content);
		if (m.find()) return m.group(1).trim();

		// fallback: a line like: version '1.6.1'
		p = Pattern.compile("version\\s+['\"]?([\\w\\.-]+)['\"]?", Pattern.CASE_INSENSITIVE);
		m = p.matcher(content);
		if (m.find()) return m.group(1).trim();

		return null;
	}

	/** Compare version strings loosely but numerically when possible.
	 *  returns -1 if a < b, 0 if equal, +1 if a > b
	 */
	private int compareVersions(String a, String b) {
		if (a == null && b == null) return 0;
		if (a == null) return -1;
		if (b == null) return 1;
		a = a.trim();
		b = b.trim();
		if (a.equals(b)) return 0;

		// split into numeric parts and text parts
		String[] apos = a.split("[^0-9]+");
		String[] bpos = b.split("[^0-9]+");
		int len = Math.max(apos.length, bpos.length);
		for (int i = 0; i < len; i++) {
			int ai = 0;
			int bi = 0;
			try { if (i < apos.length && !apos[i].isEmpty()) ai = Integer.parseInt(apos[i]); } catch (NumberFormatException ex) { ai = 0; }
			try { if (i < bpos.length && !bpos[i].isEmpty()) bi = Integer.parseInt(bpos[i]); } catch (NumberFormatException ex) { bi = 0; }
			if (ai < bi) return -1;
			if (ai > bi) return 1;
		}

		// numeric parts equal - fallback to lexicographical compare
		return a.compareTo(b);
	}
}
