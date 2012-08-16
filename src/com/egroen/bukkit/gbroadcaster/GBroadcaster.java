package com.egroen.bukkit.gbroadcaster;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GBroadcaster extends JavaPlugin {

    private Iterator<String> messagesIT = null;
    private int scheduleId = -1;
    
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        
        
        loadMessages();
        startSchedule();
    }
    
    public void onDisable() { stopSchedule(); }
    
    private void loadMessages() { messagesIT = getConfig().getStringList("messages").iterator(); }
    private void startSchedule() {
        if (scheduleId != -1) return;   // Already started
        scheduleId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                if (messagesIT == null || !messagesIT.hasNext()) loadMessages();
                
                String message = ChatColor.translateAlternateColorCodes('&', messagesIT.next());
                String prepend = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prepend"));
                Bukkit.broadcastMessage(prepend+" "+message);
            }
            
           
        }, 0, getConfig().getLong("delay")*20L);
    }
    
    private void stopSchedule() {
        if (scheduleId == -1) return;   // Already stopped
        getServer().getScheduler().cancelTask(scheduleId);
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
                    stopSchedule();
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
}
