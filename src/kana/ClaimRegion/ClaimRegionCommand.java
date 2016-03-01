package kana.ClaimRegion;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;

public class ClaimRegionCommand implements CommandExecutor{
	
	private WorldGuardPlugin worldGuard;
	private WorldEditPlugin worldEdit;
	private Selection selection;
	private Selection blockSelection;
	private BlockVector Pmin;
	private BlockVector Pmax;
	private BlockVector p1;
	private BlockVector p2;
	private RegionManager regionManager;
	private World world;
	private ProtectedCuboidRegion pr;
	private String nomTerrain;
	
	ClaimRegion plugin;
	public ClaimRegionCommand(ClaimRegion plugin){
		this.plugin = plugin;
	}
	
	
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args){
		
		Player player = null;
    	if(sender instanceof Player){
    		player = (Player) sender;
    	}
    	if(commandLabel.equalsIgnoreCase("claim")){
	        if(args.length == 0){
	        	sender.sendMessage(ChatColor.GOLD + "[ClaimRegion] " + ChatColor.WHITE + "Tapez /claim help");
	        	return true;
	        }
	        else if(args.length == 1){
	        	//--------------
	        	//---- HELP ----
	        	//--------------
	        	if(args[0].equalsIgnoreCase("help")){
	        		sender.sendMessage(ChatColor.WHITE + "----------- HELP ClaimRegion -----------");
					sender.sendMessage(ChatColor.WHITE + "/claim help" + ChatColor.GREEN + " Obtenir l'aide");
					sender.sendMessage(ChatColor.WHITE + "/claim create [nom du terrain]" + ChatColor.GREEN + " Créer un terrain");
					sender.sendMessage(ChatColor.WHITE + "/claim delete [nom du terrain]" + ChatColor.GREEN + " Créer un terrain");
					sender.sendMessage(ChatColor.WHITE + "/claim addmember [nom du joueur]" + ChatColor.GREEN + " Ajouter un joueur au terrain");
					sender.sendMessage(ChatColor.WHITE + "/claim delmember [nom du joueur]" + ChatColor.GREEN + " Retirer un joueur au terrain");
					sender.sendMessage(ChatColor.WHITE + "/claim reload" + ChatColor.GREEN + " Recharger la configuration");
		        	return true;
	        	}
	        	//----------------
	        	//---- RELOAD ----
	        	//----------------
	        	else if(args[0].equalsIgnoreCase("reload")){
	        		if(!Vault.permission.has(player, "claimregion.reload")){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'avez pas la permission d'utiliser cette commande !");
	        			return false;
	        		}else{
		        		this.plugin.reloadConfig();
		        		sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Configuration rechargée!");
			        	return true;
	        		}
	        	}
	        }
	        else if(args.length == 2){
	        	//----------------
	        	//---- CREATE ----
	        	//----------------
	        	if(args[0].equalsIgnoreCase("create")){
	        		
	        		// On vérifi la permission
	        		// -----------------------
	        		if(!Vault.permission.has(player, "claimregion.create")){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'avez pas la permission d'utiliser cette commande !");
	        			return false;
	        		}
	        		
	        		// On vérifi que le groupe est dans la config
	        		// ------------------------------------------
	        		if(Vault.permission.getPrimaryGroup(player) == null){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Votre grade n'est pas présent dans la config. Contacter un admin !");
	        			return false;
	        		}
	        		
	        		// On vérifi l'argent
	        		// ------------------
	        		double argentPlayer = Vault.economy.getBalance(player);
	        		double argentTerrain = this.plugin.getConfig().getInt("groups." + Vault.permission.getPrimaryGroup(player) + ".prix");
	        		
	        		if(argentPlayer < argentTerrain){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'avez pas assez d'argent pour créer ce terrain !");
	        			return false;
	        		}	        			        		

	        		this.worldGuard = this.plugin.getWorldGuard();
	        		this.worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		    		this.selection = worldEdit.getSelection(player);
		    		this.world = player.getWorld();
		    		this.regionManager = worldGuard.getRegionManager(world);
		    		LocalPlayer localPlayer = worldGuard.wrapPlayer(player);
		    		
		    		// On vérifi le nombre de terrain
	        		// ------------------------------
	        		int nbrTerrainMax = this.plugin.getConfig().getInt("groups." + Vault.permission.getPrimaryGroup(player) + ".terrain");
	        		int nbrTerrainPlayer = regionManager.getRegionCountOfPlayer(localPlayer);
	        		
	        		if(nbrTerrainPlayer >= nbrTerrainMax){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous avez atteint le nombre maximum de terrain pour votre grade !");
	        			return false;
	        		}
		    		
		    		// On verifi le nombre de bloc
		    		// ----------------------------
		    		int blocSelection = testSelection(selection, player.getWorld());
		    		int nbrBlocMax = this.plugin.getConfig().getInt("groups." + Vault.permission.getPrimaryGroup(player) + ".bloc");
		    		
		    		if(blocSelection > nbrBlocMax){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Votre sélection est trop grande !");
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + blocSelection + "/" + nbrBlocMax);
		    			return false;
		    		}
		    		
		    		// On vérifi si la région est sur une autre region
		    		// -----------------------------------------------		    		
		    		ApplicableRegionSet r = regionManager.getApplicableRegions(p1);
		    		
		    		if(r.getRegions().size() >= 1){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Votre sélection est sur une région déjà existante !");
		    			return false;
		    		}
		    		
		    		this.Pmax = recupBlocVectorMax(selection, "default");
					this.Pmin = recupBlocVectorMin(selection, "default");
					
					// On récupère la priorité, le nom du joueur et on défini le nom du terrain
	    			//-------------------------------------------------------
	    			this.nomTerrain = args[1];
	    			this.pr = new ProtectedCuboidRegion(nomTerrain, Pmin, Pmax);
	    			this.pr.setPriority(10);
	    			DefaultDomain owners = new DefaultDomain();
	    			owners.addPlayer(localPlayer);
	    			this.pr.setOwners(owners);
	    			this.pr.setFlag(DefaultFlag.TNT, StateFlag.State.DENY);
	    			this.pr.setFlag(DefaultFlag.CREEPER_EXPLOSION, StateFlag.State.DENY);
	    			this.pr.setFlag(DefaultFlag.OTHER_EXPLOSION, StateFlag.State.DENY);
	    			this.pr.setFlag(DefaultFlag.FIRE_SPREAD, StateFlag.State.DENY);	    			
	    			
	    			try{
		    			regionManager.addRegion(pr);
		    			regionManager.save();
		    			Vault.economy.withdrawPlayer(player, argentTerrain);
		    			sender.sendMessage(ChatColor.GREEN + "[ClaimRegion] " + ChatColor.WHITE + "La région " + ChatColor.GREEN + nomTerrain + ChatColor.YELLOW + " créé avec succés");
		    			return true;
					} 
			    	catch (Exception exp){
			    		sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Un problème est survenu, contactez un admin !");
			    		sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + exp);
			    		return false;
					}
	        	}
	        	//----------------
	        	//---- DELETE ----
	        	//----------------
	        	else if(args[0].equalsIgnoreCase("delete")){
	        		
	        		// On vérifi la permission
	        		// -----------------------
	        		if(!Vault.permission.has(player, "claimregion.delete")){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'avez pas la permission d'utiliser cette commande !");
	        			return false;
	        		}
	        		
	        		// On vérifi que le groupe est dans la config
	        		// ------------------------------------------
	        		if(Vault.permission.getPrimaryGroup(player) == null){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Votre grade n'est pas présent dans la config. Contacter un admin !");
	        			return false;
	        		}
	        		
	        		// On vérifi l'argent
	        		// ------------------
	        		double argentPlayer = Vault.economy.getBalance(player);
	        		double argentTerrainDelete = this.plugin.getConfig().getInt("groups." + Vault.permission.getPrimaryGroup(player) + ".prixDelete");
	        		
	        		if(argentPlayer < argentTerrainDelete){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'avez pas assez d'argent pour supprimer ce terrain !");
	        			return false;
	        		}
	        		
	        		this.worldGuard = this.plugin.getWorldGuard();
	        		this.world = player.getWorld();
		    		this.regionManager = worldGuard.getRegionManager(world);
		    		
		    		// On vérifi si la région existe
		    		// -----------------------------
		    		if(regionManager.getRegion(args[1]) == null){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Le Terrain " + args[1] + " est introuvable !");
	        			return false;
		    		}
		    		
		    		// On vérifi si le terrain appartient bien au joueur
		    		// -------------------------------------------------
		    		DefaultDomain proprioTerrain = regionManager.getRegion(args[1]).getOwners();
		    		LocalPlayer localPlayer = worldGuard.wrapPlayer(player);
		    		DefaultDomain owners = new DefaultDomain();
	    			owners.addPlayer(localPlayer);
	    			
		    		if(!owners.toPlayersString().equals(proprioTerrain.toPlayersString())){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'êtes pas le propriétaire du térrain !");
	        			return false;
		    		}
		    		
		    		// On supprime la région
		    		// ---------------------
		    		regionManager.removeRegion(args[1]);
		    		try {
						regionManager.save();
						Vault.economy.withdrawPlayer(player, argentTerrainDelete);
						sender.sendMessage(ChatColor.GREEN + "[ClaimRegion] " + ChatColor.WHITE + "Joueur ajouté au terrain");
						return true;
						
					} catch (StorageException e) {
						sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Un problème est survenu, contacter un admin !");
						// TODO Auto-generated catch block
						e.printStackTrace();
						return false;
					}	        		
	        	}	        	
	        	else{
	        		sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Tapez /claim help !");
		    		return false;
	        	}
	        }
	        else if(args.length == 3){
	        	//-------------------
	        	//---- ADDMEMBER ----
	        	//-------------------
	        	if(args[0].equalsIgnoreCase("addmember")){
	        		
	        		// On vérifi la permission
	        		// -----------------------
	        		if(!Vault.permission.has(player, "claimregion.addmember")){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'avez pas la permission d'utiliser cette commande !");
	        			return false;
	        		}
	        		
	        		this.worldGuard = this.plugin.getWorldGuard();
	        		this.world = player.getWorld();
		    		this.regionManager = worldGuard.getRegionManager(world);
		    		
		    		// On vérifi si la région existe
		    		// -----------------------------
		    		if(regionManager.getRegion(args[1]) == null){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Le Terrain " + args[1] + " n'est pas à vous !");
	        			return false;
		    		}

		    		// On vérifi si le terrain appartient bien au joueur
		    		// -------------------------------------------------
		    		DefaultDomain proprioTerrain = regionManager.getRegion(args[1]).getOwners();
		    		LocalPlayer localPlayer = worldGuard.wrapPlayer(player);
		    		DefaultDomain owners = new DefaultDomain();
	    			owners.addPlayer(localPlayer);
	    			
		    		if(!owners.toPlayersString().equals(proprioTerrain.toPlayersString())){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'êtes pas le propriétaire du térrain !");
	        			return false;
		    		}		    		
		    		
		    		@SuppressWarnings("deprecation")
					Player playerMember = Bukkit.getPlayer(args[2]);
		    				    		
		    		// On vérifi si le joueur est connecté
		    		// -----------------------------------
		    		if(playerMember == null){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Le joueur doit etre connecté pour l'ajouter au terrain !");
	        			return false;
		    		}
		    		playerMember.getUniqueId();
		    		
		    		LocalPlayer localPlayer2 = worldGuard.wrapPlayer(playerMember);
		    		DefaultDomain members = regionManager.getRegion(args[1]).getMembers();
		    		members.addPlayer(localPlayer2);
		    		regionManager.getRegion(args[1]).setMembers(members);
		    		try {
						regionManager.save();
						sender.sendMessage(ChatColor.GREEN + "[ClaimRegion] " + ChatColor.WHITE + "Joueur ajouté au terrain");
		    			return true;
					} catch (StorageException e) {						
						// TODO Auto-generated catch block
						e.printStackTrace();
						sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Un problème est survenue contacter un admin !");
	        			return false;
					}
	        	}
	        	//-------------------
	        	//---- DELMEMBER ----
	        	//-------------------
	        	else if(args[0].equalsIgnoreCase("delmember")){
	        		
	        		// On vérifi la permission
	        		// -----------------------
	        		if(!Vault.permission.has(player, "claimregion.delmember")){
	        			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'avez pas la permission d'utiliser cette commande !");
	        			return false;
	        		}
	        		
	        		this.worldGuard = this.plugin.getWorldGuard();
	        		this.world = player.getWorld();
		    		this.regionManager = worldGuard.getRegionManager(world);
		    		
		    		// On vérifi si la région existe
		    		// -----------------------------
		    		if(regionManager.getRegion(args[1]) == null){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Le Terrain " + args[1] + " n'est pas à vous !");
	        			return false;
		    		}

		    		// On vérifi si le terrain appartient bien au joueur
		    		// -------------------------------------------------
		    		DefaultDomain proprioTerrain = regionManager.getRegion(args[1]).getOwners();
		    		LocalPlayer localPlayer = worldGuard.wrapPlayer(player);
		    		DefaultDomain owners = new DefaultDomain();
	    			owners.addPlayer(localPlayer);
	    			
		    		if(!owners.toPlayersString().equals(proprioTerrain.toPlayersString())){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Vous n'êtes pas le propriétaire du térrain !");
	        			return false;
		    		}
		    		
		    		DefaultDomain members = regionManager.getRegion(args[1]).getMembers();
		    		if(!members.contains(args[2])){
		    			sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Aucun joueur du nom de " + args[2] + " n 'est present dans la region !");
		    			sender.sendMessage(members.toPlayersString());
	        			return false;
		    		}
		    		
		    		members.removePlayer(args[2]);
		    		regionManager.getRegion(args[1]).setMembers(members);
		    		
		    		try {
						regionManager.save();
						sender.sendMessage(ChatColor.GREEN + "[ClaimRegion] " + ChatColor.WHITE + "Joueur enlevé au terrain");
		    			return true;
					} catch (StorageException e) {						
						// TODO Auto-generated catch block
						e.printStackTrace();
						sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Un problème est survenue contacter un admin !");
	        			return false;
					}
	        	}
	        }
	        else{
        		sender.sendMessage(ChatColor.RED + "[ClaimRegion] " + ChatColor.WHITE + "Tapez /claim help !");
	    		return false;
        	}
    	}
		return false;	
	}
	
	public BlockVector recupBlocVectorMax(Selection selection, String st){
		int posX = selection.getMaximumPoint().getBlockX();
		int posY = 220;
		int posZ = selection.getMaximumPoint().getBlockZ();
	    p1 = new BlockVector(posX, posY, posZ);	    
	    return p1;
	}
	public BlockVector recupBlocVectorMin(Selection selection, String st){
		int posX = selection.getMinimumPoint().getBlockX();
		int posY = 10;
		int posZ = selection.getMinimumPoint().getBlockZ();
	    p2 = new BlockVector(posX, posY, posZ);	    
	    return p2;
	}
	public int testSelection(Selection selection, World world){
		p1 = new BlockVector(selection.getMaximumPoint().getBlockX(),64 ,selection.getMaximumPoint().getBlockZ());
		p2 = new BlockVector(selection.getMinimumPoint().getBlockX(),64 ,selection.getMinimumPoint().getBlockZ());
		blockSelection = new CuboidSelection(world,p1,p2);
		return blockSelection.getArea();
	}
}
