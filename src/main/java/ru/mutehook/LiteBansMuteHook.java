package ru.mutehook;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class LiteBansMuteHook extends JavaPlugin implements Listener {

    private String muteMessage;
    private List<String> blockedCommands;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadMessages();

        if (getServer().getPluginManager().getPlugin("LiteBans") == null) {
            getLogger().severe("LiteBans не найден! Плагин отключён.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("LiteBansMuteHook включён. Муты будут работать.");
    }

    private void reloadMessages() {
        reloadConfig();
        muteMessage = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("mute-message",
                        "&cВы заглушены и не можете писать в чат."));
        blockedCommands = getConfig().getStringList("blocked-commands");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (isMuted(player)) {
            event.setCancelled(true);
            player.sendMessage(muteMessage);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isMuted(player)) return;

        String message = event.getMessage();
        if (!message.startsWith("/")) return;

        String cmd = message.substring(1).split(" ")[0].toLowerCase();
        if (cmd.contains(":")) {
            cmd = cmd.substring(cmd.indexOf(':') + 1);
        }

        if (blockedCommands.contains(cmd)) {
            event.setCancelled(true);
            player.sendMessage(muteMessage);
        }
    }

    private boolean isMuted(Player player) {
        try {
            Class<?> dbClass = Class.forName("litebans.api.Database");
            Object db = dbClass.getMethod("get").invoke(null);
            UUID uuid = player.getUniqueId();

            try {
                Method m = dbClass.getMethod("isPlayerMuted", UUID.class, String.class);
                Object result = m.invoke(db, uuid, null);
                if (result instanceof Boolean) return (Boolean) result;
            } catch (NoSuchMethodException ignored) {}

            try {
                Method m = dbClass.getMethod("isPlayerMuted", UUID.class);
                Object result = m.invoke(db, uuid);
                if (result instanceof Boolean) return (Boolean) result;
            } catch (NoSuchMethodException ignored) {}

            try {
                Method m = dbClass.getMethod("isMuted", UUID.class);
                Object result = m.invoke(db, uuid);
                if (result instanceof Boolean) return (Boolean) result;
            } catch (NoSuchMethodException ignored) {}

            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
