package com.samuelking.bansystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class BanSystemPlugin extends JavaPlugin implements Listener {

    private final HashMap<UUID, BanInfo> bans = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ban") && args.length >= 3) {
            if (sender.hasPermission("bansystem.ban")) { // Überprüft, ob der Spieler die Berechtigung hat
                Player target = Bukkit.getPlayer(args[0]);
                long duration = parseDuration(args[1]);
                String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                if (target != null) {
                    bans.put(target.getUniqueId(), new BanInfo(System.currentTimeMillis(), duration, reason));
                    target.kickPlayer(getBanMessage(target.getName(), reason, duration));
                    sender.sendMessage(ChatColor.GREEN + "Spieler gebannt.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl auszuführen.");
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("unban") && args.length == 1) {
            if (sender.hasPermission("bansystem.unban")) { // Überprüft, ob der Spieler die Berechtigung hat
                String playerName = args[0];
                Player target = Bukkit.getPlayer(playerName);
                UUID targetUUID = null;

                if (target != null) {
                    targetUUID = target.getUniqueId();
                } else {
                    // Falls der Spieler offline ist, suchen wir ihn mit dem Namen im Ban-System
                    for (UUID uuid : bans.keySet()) {
                        if (Bukkit.getOfflinePlayer(uuid).getName().equalsIgnoreCase(playerName)) {
                            targetUUID = uuid;
                            break;
                        }
                    }
                }

                if (targetUUID != null) {
                    bans.remove(targetUUID);
                    sender.sendMessage(ChatColor.GREEN + "Spieler entbannt.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl auszuführen.");
            }

            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (bans.containsKey(uuid)) {
            BanInfo info = bans.get(uuid);
            if (System.currentTimeMillis() - info.banTime < info.duration || info.duration == -1) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, getBanMessage(event.getPlayer().getName(), info.reason, info.duration));
            } else {
                bans.remove(uuid);
            }
        }
    }

    private String getBanMessage(String name, String reason, long duration) {
        String time = (duration == -1) ? "Permanent" : (duration / 1000 / 60) + " Minuten";
        return ChatColor.RED + "Du bist gebannt!\n" +
               ChatColor.GOLD + "Spieler: " + name + "\n" +
               ChatColor.YELLOW + "Grund: " + reason + "\n" +
               ChatColor.AQUA + "Dauer: " + time + "\n" +
               ChatColor.LIGHT_PURPLE + "Creator: SamuelKing";
    }

    private long parseDuration(String input) {
        if (input.equalsIgnoreCase("permanent")) return -1;
        try {
            if (input.endsWith("m")) return Integer.parseInt(input.replace("m", "")) * 60 * 1000L;
            if (input.endsWith("h")) return Integer.parseInt(input.replace("h", "")) * 60 * 60 * 1000L;
            if (input.endsWith("d")) return Integer.parseInt(input.replace("d", "")) * 24 * 60 * 60 * 1000L;
        } catch (NumberFormatException e) {
            return 5 * 60 * 1000L; // default 5 minutes
        }
        return 5 * 60 * 1000L;
    }

    private static class BanInfo {
        long banTime;
        long duration;
        String reason;

        BanInfo(long banTime, long duration, String reason) {
            this.banTime = banTime;
            this.duration = duration;
            this.reason = reason;
        }
    }
}

