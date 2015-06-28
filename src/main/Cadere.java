package main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import listener.MainListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Cadere extends JavaPlugin implements Listener
{
	
	public static final int MIN_PLAYERS = 2;
	
	public static Location LOBBY;
	public static Location ARENA_CENTER;
	public static Location SPECTATOR_SPAWN;
	public static double ARENA_RADIUS = 10;
	
	public static String GAMESTATUS = "LOBBY";
	
	public ArrayList<Player> playersIngame = new ArrayList<Player>();
	
	public HashMap<Location, Material> destroyedBlocksMaterial = new HashMap<Location, Material>();
	public HashMap<Location, Byte> destroyedBlocksData = new HashMap<Location, Byte>();
	
	public int countdown = 0;
	public int startGameCountdownTaskID;
	public int serverShutdownCountdownTaskID;
	
	public static Cadere plugin;
	public MainListener mainListener;
	
	
	public void onEnable()
	{
		plugin = this;
		
		LOBBY = new Location(Bukkit.getWorlds().get(0), 30.5, 1.0, 0.5, 90.0f, 0.0f);
		ARENA_CENTER = new Location(Bukkit.getWorlds().get(0), 0.5, 1.0, 0.5, -90f, 90f);
		SPECTATOR_SPAWN = ARENA_CENTER.clone().add(0, 20, 0);
		
		this.getServer().getPluginManager().registerEvents(this, this);
		mainListener = new MainListener();
		
		System.out.println("[" + this.getDescription().getName() + "] v" + this.getDescription().getVersion() + " enabled!");
		
		for(Player p : Bukkit.getOnlinePlayers())
			makeLobbyer(p);
		
		tryGameStart();
	}
	
	@Override
	public void onDisable()
	{
		mainListener.disable();
		
		repairMap();
		
		System.out.println("[" + this.getDescription().getName() + "] v" + this.getDescription().getVersion() + " disabled!");
	}
	
	// ---
	// GAME
	// ---
	
	public void tryGameStart()
	{
		if(Bukkit.getOnlinePlayers().size() >= Cadere.MIN_PLAYERS)
		{
			msgToAll(ChatColor.WHITE + "Enough players to start the game. [" + ChatColor.AQUA + Bukkit.getOnlinePlayers().size() + ChatColor.WHITE + "/" + ChatColor.AQUA + MIN_PLAYERS + ChatColor.WHITE + "]");
			startGameCountdown();
		}
		else
			msgToAll(ChatColor.RED + "Not enough players to start the game. [" + ChatColor.AQUA + Bukkit.getOnlinePlayers().size() + ChatColor.RED + "/" + ChatColor.AQUA + MIN_PLAYERS + ChatColor.RED + "]");
	}
	
	public void startGameCountdown()
	{
		GAMESTATUS = "GAMESTART";
		countdown = 11;
		
		startGameCountdownTaskID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			@Override
			public void run()
			{
				if(countdown == 0)
				{
					Bukkit.getScheduler().cancelTask(startGameCountdownTaskID);
					startGameCountdownTaskID = 0;
					
					startGame();
					return;
				}
				if(countdown % 5 == 0 || countdown <= 3)
					msgToAll("Game starting in " + ChatColor.RESET + ChatColor.AQUA + countdown + ChatColor.WHITE + " seconds.");
				
				countdown--;
			}
		}, 5L, 20L);
	}
	
	public void cancelGameStartCountdown()
	{
		Bukkit.getScheduler().cancelTask(startGameCountdownTaskID);
		startGameCountdownTaskID = 0;
		countdown = 0;
		
		GAMESTATUS = "LOBBY";
		
		msgToAll(ChatColor.RED + "The countdown has been canceled. [" + ChatColor.AQUA + (Bukkit.getOnlinePlayers().size() - 1) + ChatColor.RED + "/" + ChatColor.AQUA + MIN_PLAYERS + ChatColor.RED + "]");
	}
	
	public void startGame()
	{
		GAMESTATUS = "INGAME";
		
		msgToAll(ChatColor.GREEN + "Game starting now!");
		
		spawnPlayers();
	}
	
	public void spawnPlayers()
	{
		Collection<?> players = Bukkit.getOnlinePlayers();
		int numberOfPlayers = players.size();
		double distanceToCenter = ARENA_RADIUS * 0.7;
		
		double playerRadianDistance = 2 * Math.PI / numberOfPlayers;
		
		Iterator<?> it = players.iterator();
		int i = 0;
		while(it.hasNext())
		{
			Player p = (Player) it.next();
			
			double dX = Math.sin(i * playerRadianDistance) * distanceToCenter;
			double dY = Math.cos(i * playerRadianDistance) * distanceToCenter;
			
			playersIngame.add(p);
			Location loc = ARENA_CENTER.clone().add(dX, 10, dY);
			loc.setYaw(getYawFromTo(loc, ARENA_CENTER));
			p.teleport(loc);
			
			i++;
		}
	}
	
	
	public void checkForGameEnd()
	{
		if(playersIngame.size() <= 1)
			gameEnd();
	}
	
	public void gameEnd()
	{
		String name = "Nobody";
		if(playersIngame.size() > 0)
			name = playersIngame.get(0).getName();
		
		msgToAll(ChatColor.GRAY + name + ChatColor.RESET + ChatColor.GREEN + " has won Cadere!");
		
		GAMESTATUS = "AFTERGAME";
		
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run()
			{
				resetGame();
			}
		}, 3 * 20L);
	}
	
	
	public void resetGame()
	{
		msgToAll("Resetting Map.");
		
		playersIngame.clear();
		
		repairMap();
		
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run()
			{
				GAMESTATUS = "LOBBY";
				
				for(Player p : Bukkit.getOnlinePlayers())
					makeLobbyer(p);
				
				tryGameStart();
			}
		}, 3 * 20L);
	}
	
	@SuppressWarnings("deprecation")
	public void repairMap()
	{
		for(Entry<Location, Material> entry : destroyedBlocksMaterial.entrySet())
		{
			entry.getKey().getBlock().setType(entry.getValue());
			entry.getKey().getBlock().setData(destroyedBlocksData.get(entry.getKey()));
		}
	}
	
	// ---
	// PLAYER ACTIONS
	// ---
	
	public void playerLeave(Player p)
	{
		// LOBBY and AFTERGAME not important
		
		if(GAMESTATUS.equals("GAMESTART"))
		{
			if(Bukkit.getOnlinePlayers().size() - 1 < MIN_PLAYERS)
				cancelGameStartCountdown();
		}
				
		else if(GAMESTATUS.equals("INGAME"))
			if(playersIngame.contains(p))
				playerOut(p);
	}
	
	public void playerFall(Player p)
	{
		if(GAMESTATUS.equals("LOBBY") || GAMESTATUS.equals("GAMESTART") || GAMESTATUS.equals("AFTERGAME"))
			makeLobbyer(p);
		
		else if(GAMESTATUS.equals("INGAME"))
		{
			if(playersIngame.contains(p))
				playerOut(p);
			else
				makeSpectator(p);
		}
	}
	
	public void playerOut(Player p)
	{
		playersIngame.remove(p);
		makeSpectator(p);
		
		msgToAll(ChatColor.GRAY + p.getName() + ChatColor.RESET + ChatColor.RED + " has dropped out!");
		
		checkForGameEnd();
	}
	
	
	public void makeLobbyer(Player p)
	{
		p.setGameMode(GameMode.ADVENTURE);
		p.teleport(LOBBY);
	}
	
	public void makeSpectator(Player p)
	{
		p.setGameMode(GameMode.SPECTATOR);
		p.teleport(SPECTATOR_SPAWN);
	}
	
	// ---
	// UTIL
	// ---
	
	public void destroyBlock(Location loc)
	{
		loc.add(0, -1, 0);
		Block b = loc.getBlock();
		
		if(b.getType() != Material.AIR)
		{
			addDestroyedBlock(b);
			b.setType(Material.AIR);
			
			//return; // remove comment in front of the return to only destroy one block dropping down, commented out all blocks the player could be standing on are removed
		}
		
		ArrayList<Block> blocksAround = new ArrayList<Block>();
		for(int x = -1; x <= 1; x++)
			for(int z = -1; z <= 1; z++)
				if(!(x == 0 && z == 0))
				{
					Block bn = b.getLocation().clone().add(x, 0, z).getBlock();
					if(bn.getType() != Material.AIR)
						blocksAround.add(bn);
				}
		
		for(Block bl : blocksAround)
		{
			Location center = bl.getLocation().clone().add(0.5, 0, 0.5);
			
			double dX = Math.abs(loc.getX() - center.getX());
			double dZ = Math.abs(loc.getZ() - center.getZ());
			
			if(dX < 0.8 && dZ < 0.8)
			{
				addDestroyedBlock(bl);
				bl.setType(Material.AIR);
				
				//return; // remove comment in front of the return to only destroy one block dropping down, commented out all blocks the player could be standing on are removed
			}
		}
	}
	
	public boolean isBlockUnderneath(Location loc)
	{
		loc.add(0, -1, 0);
		Block b = loc.getBlock();
		
		if(b.getType() != Material.AIR)
			return true;
		
		ArrayList<Block> blocksAround = new ArrayList<Block>();
		for(int x = -1; x <= 1; x++)
			for(int z = -1; z <= 1; z++)
				if(!(x == 0 && z == 0))
				{
					Block bn = b.getLocation().clone().add(x, 0, z).getBlock();
					if(bn.getType() != Material.AIR)
						blocksAround.add(bn);
				}
		
		for(Block bl : blocksAround)
		{
			Location center = bl.getLocation().clone().add(0.5, 0, 0.5);
			
			double dX = Math.abs(loc.getX() - center.getX());
			double dZ = Math.abs(loc.getZ() - center.getZ());
			
			if(dX < 0.8 && dZ < 0.8)
				return true;
		}
		
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public void addDestroyedBlock(Block b)
	{
		//spawn falling block
		b.getLocation().getWorld().spawnFallingBlock(b.getLocation(), b.getType(), b.getData());
		
		destroyedBlocksMaterial.put(b.getLocation(), b.getType());
		destroyedBlocksData.put(b.getLocation(), b.getData());
	}
	
	
	public double distanceTo(Location loc1, Location loc2)
	{
		if(loc1.getWorld() != loc2.getWorld())
			return Double.MAX_VALUE;
		
		double deltaX = Math.abs(loc1.getX() - loc2.getX());
		double deltaY = Math.abs(loc1.getY() - loc2.getY());
		double deltaZ = Math.abs(loc1.getZ() - loc2.getZ());
		
		double distance2d = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
		double distance3d = Math.sqrt(distance2d * distance2d + deltaY * deltaY);
		
		return distance3d;
	}
	
	public float getYawFromTo(Location from, Location to)
	{
		double dX = to.getX() - from.getX();
		double dZ = to.getZ() - from.getZ();
		
		return (float) Math.toDegrees(Math.atan2(dX, dZ));
	}
	
	public void msgToAll(String msg)
	{
		for(Player p : Bukkit.getOnlinePlayers())
			p.sendMessage(msg);
	}
	
}
