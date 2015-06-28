package listener;

import main.Cadere;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.util.Vector;

public class MainListener implements Listener
{
	
	Cadere plugin;
	
	private int playerPositionCheckingTaskID;
	
	
	public MainListener()
	{
		plugin = Cadere.plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		playerPositionChecking();
	}
	
	public void disable()
	{
		Bukkit.getScheduler().cancelTask(playerPositionCheckingTaskID);
		playerPositionCheckingTaskID = 0;
	}
	
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void playerChat(AsyncPlayerChatEvent e)
	{
		e.setFormat(ChatColor.GRAY + e.getPlayer().getName() + ChatColor.WHITE + ": " + e.getMessage());
	}
	
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void playerJoin(PlayerJoinEvent e)
	{
		Player p = e.getPlayer();
		
		e.setJoinMessage(null);
		plugin.msgToAll(ChatColor.GRAY + p.getName() + ChatColor.RESET + ChatColor.YELLOW + " joined the server.");
		plugin.makeLobbyer(p);
		
		if(Cadere.GAMESTATUS.equals("LOBBY"))
		{
			plugin.makeLobbyer(p);
			plugin.tryGameStart();
		}
		
		else if(Cadere.GAMESTATUS.equals("GAMESTART"))
			plugin.makeLobbyer(p);
		
		else if(Cadere.GAMESTATUS.equals("INGAME") || Cadere.GAMESTATUS.equals("AFTERGAME"))
			plugin.makeSpectator(p);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void playerLeave(PlayerQuitEvent e)
	{
		e.setQuitMessage(null); // to ensure order of messages is right, first leave message, then message that countdown has been canceled or message that player has lost
		plugin.msgToAll(ChatColor.GRAY + e.getPlayer().getName() + ChatColor.RESET + ChatColor.YELLOW + " has left the server.");
		
		plugin.playerLeave(e.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void playerKick(PlayerKickEvent e)
	{
		e.setLeaveMessage(null); // to ensure order of messages is right, first leave message, then message that countdown has been canceled or message that player has lost
		plugin.msgToAll(ChatColor.GRAY + e.getPlayer().getName() + ChatColor.RESET + ChatColor.YELLOW + " was kicked from the server.");
		
		plugin.playerLeave(e.getPlayer());
	}
	
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void playerDamageRecieve(EntityDamageEvent e)
	{
		e.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void playerHunger(FoodLevelChangeEvent e)
	{
		e.setCancelled(true);
	}
	
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void weatherChange(WeatherChangeEvent e)
	{
		e.setCancelled(true);
	}
	
	
	public void playerPositionChecking()
	{
		playerPositionCheckingTaskID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Cadere.plugin, new Runnable()
		{
			@Override
			public void run()
			{
				for(Player p : Bukkit.getOnlinePlayers())
				{
					if(p.getLocation().getY() < -5)
					{
						plugin.playerFall(p);
						return;
					}
					
					if(plugin.playersIngame.contains(p) && p.getLocation().getY() < 1.2 && Cadere.GAMESTATUS.equals("INGAME") && plugin.isBlockUnderneath(p.getLocation()))
					{
						Vector v = p.getVelocity();
						p.setVelocity(new Vector(v.getX(), 1.5, v.getZ()));
						plugin.destroyBlock(p.getLocation());
					}
				}
			}
		}, 0L, 1L);
	}
	
}
