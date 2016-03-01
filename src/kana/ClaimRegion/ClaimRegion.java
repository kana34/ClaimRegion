package kana.ClaimRegion;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class ClaimRegion extends JavaPlugin implements Listener{
	
	private Logger logger = Logger.getLogger("Minecraft");
	public Plugin plugin;
	public ClaimRegionCommand commandL;
	public PluginCommand batchcommand;
	private WorldEditPlugin worldEdit;
	private Selection selection;
	private BlockVector p1;
	private BlockVector p2;
	private Selection blockSelection;
	
	// ------------------
	// ---- onEnable ----
	// ------------------
	public void onEnable(){
		PluginManager pm = this.getServer().getPluginManager();
    	
		pm.registerEvents(this, this);
		
		this.commandL = new ClaimRegionCommand(this);
        this.batchcommand = getCommand("claim");
        batchcommand.setExecutor(commandL);
                
        // On charge vault
        //----------------
    	Vault.load(this);
    	Vault.setupChat();
    	Vault.setupPermissions();
    	Vault.setupEconomy();
    	//if (!Vault.setupEconomy()){
    	 //  logger.info(String.format("[%s] - Necessite Vault pour fonctionner!", getDescription().getName()));
    	  //  getServer().getPluginManager().disablePlugin(this);
    	 //   return;
    	//}    	
    	
    	// On charge les configurations
    	//-----------------------------
    	this.loadConfig();	
    	
		logger.info("[ClaimRegionCommand] Plugin charge parfaitement!");
    }
	
	// -------------------
	// ---- onDisable ----
	// -------------------
    public void onDisable(){
            logger.info("[ClaimRegionCommand] Plugin desactive...");
    }
    
    // --------------------
 	// ---- loadConfig ----
 	// --------------------
    public void loadConfig(){           
    	
    	// On créé le fichier de configuration par default
    	//------------------------------------------------
    	this.getConfig().options().copyDefaults(true);	
    	this.saveConfig();    	    	
    }
    
    WorldGuardPlugin getWorldGuard() {
        this.plugin = getServer().getPluginManager().getPlugin("WorldGuard");   
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) { 
            getServer().getPluginManager().disablePlugin(this);
            return null;
        }     
        return (WorldGuardPlugin) plugin;
    }
    
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerUse(PlayerInteractEvent event){
        Player p = event.getPlayer();
     
        if(p.getItemInHand().getType() == Material.WOOD_AXE){
        	this.worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
    		this.selection = worldEdit.getSelection(p);
    		
    		if(selection != null){
	    		int blocSelection = testSelection(selection, p.getWorld());
	    		int nbrBlocMax = getConfig().getInt("groups." + Vault.permission.getPrimaryGroup(p) + ".bloc");
	    		
	    		p.sendMessage(ChatColor.GREEN + "[ClaimRegion] " + ChatColor.WHITE + "Région: " + ChatColor.YELLOW + blocSelection + "/" + ChatColor.GREEN + nbrBlocMax);
    		}
        }
    }
    
    public int testSelection(Selection selection, World world){
		p1 = new BlockVector(selection.getMaximumPoint().getBlockX(),64 ,selection.getMaximumPoint().getBlockZ());
		p2 = new BlockVector(selection.getMinimumPoint().getBlockX(),64 ,selection.getMinimumPoint().getBlockZ());
		blockSelection = new CuboidSelection(world,p1,p2);
		return blockSelection.getArea();
	}
}
