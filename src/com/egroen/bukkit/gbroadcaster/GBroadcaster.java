package com.egroen.bukkit.gbroadcaster;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class GBroadcaster extends JavaPlugin implements Listener {

    private Iterator<String> messagesIT = null;
    private int scheduleId = 0; // 0 = automatic disabled, -1 = manual disabled
    
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        // Register itself as event handler
        getServer().getPluginManager().registerEvents(this,this);
        
        loadMessages();
        // If reload while players are online..
        if (Bukkit.getOnlinePlayers().length > 0 && scheduleId == 0) startSchedule();
    }
    
    public void onDisable() { stopSchedule(true); }
    
    private void loadMessages() { messagesIT = getConfig().getStringList("messages").iterator(); }
    private void startSchedule() {
        if (scheduleId > 0) return;   // Already started
        scheduleId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                // Check if we have something..
                if (messagesIT == null || !messagesIT.hasNext()) loadMessages();
                
                // Get prepend section
                String prepend = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prepend"));
                // Traces multi-line
                boolean takeNextLine;
                
                // Iterate trough multi-lines
                do {
                    takeNextLine=false;
                    String message = messagesIT.next();
                    if (message.endsWith("|")) {
                        takeNextLine=true;
                        message = message.substring(0, message.length()-1);
                    }
                    message = ChatColor.translateAlternateColorCodes('&', message);
                    Bukkit.broadcastMessage(prepend+" "+message);
                } while (takeNextLine && messagesIT.hasNext()); // Go as long as takeNextLine says and the list can give.
            }
            
           
        }, 0, getConfig().getLong("delay")*20L);
    }
    
    private void stopSchedule(boolean auto) {
        if (scheduleId <= 0) return;   // Already stopped
        getServer().getScheduler().cancelTask(scheduleId);
        scheduleId = auto ? 0 : -1;
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (sender instanceof Player && ((Player)sender).hasPermission("gbroadcaster.admin")) {
            sender.sendMessage("You do not have access to this command!");
            return true;
        }
        
        if (cmd.getLabel().equalsIgnoreCase("gbc")) {
            if ("start".equalsIgnoreCase(args[0])) {
                if (scheduleId == -1) {
                    sender.sendMessage("Broadcasting has been enabled!");
                    startSchedule();
                } else {
                    sender.sendMessage("Broadcasting is already enabled!");
                }
                return true;
            }
            if ("stop".equalsIgnoreCase(args[0])) {
                if (scheduleId != -1) {
                    sender.sendMessage("Broadcasting has been disabled!");
                    stopSchedule(false);
                } else {
                    sender.sendMessage("Broadcast is already disabled!");
                }
                return true;
            }
            if ("reload".equalsIgnoreCase(args[0])) {
                reloadConfig();
                loadMessages();
                startSchedule();
                sender.sendMessage("Reload finished.");
                return true;
            }
        }
        
        return false;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {
        // If first player joined and its autostopped, start it
        if (Bukkit.getOnlinePlayers().length == 1 && scheduleId == 0) startSchedule();
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent ev) {
        // If last player quit and schedule is on, stop id.
        if (Bukkit.getOnlinePlayers().length == 0  && scheduleId > 0) stopSchedule(true);
    }
}
