package today.pls.hub;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import net.minecraft.server.v1_12_R1.Position;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.UUID;

import static today.pls.hub.Utils.*;

public class GenericListener implements Listener {

    private static final int BOOK_SLOT = 0, TOGGLE_PLAYER_SLOT = 8, SERVER_SELECT_SLOT = 4;

    private HubPlugin pl;

    private static Position PKP1 = new Position(-2,107,-13);
    private static Position PKP2 = new Position(2,112,-9);

    private static ArrayList<UUID> flyingPlayers = new ArrayList<>();
    private static ArrayList<UUID> respawnedPlayers = new ArrayList<>();

    private static ArrayList<UUID> playersHidingOthers = new ArrayList<>();

    private static Inventory playerInv, serverSelectInv;
    private static ItemStack serverSelectItem, hidePlayersItem, showPlayersItem, infoBook;

    static{
        serverSelectItem = itemStackWithName(Material.COMPASS,"§c§lServer Selection");
        hidePlayersItem = itemStackWithName(Material.TORCH, "§4§lHide Players");
        showPlayersItem = itemStackWithName(Material.LEVER, "§4§lShow Players");

        infoBook = new ItemStack(Material.WRITTEN_BOOK);
        infoBook.addUnsafeEnchantment(Enchantment.THORNS, 1337);
        BookMeta m = (BookMeta) infoBook.getItemMeta();
        m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        m.setTitle("PLSCraft Handbook");
        m.setAuthor("RaidAndFade");
        m.setGeneration(BookMeta.Generation.ORIGINAL);
        m.addPage("Welcome to PLSCraft");
        infoBook.setItemMeta(m);


        playerInv = Bukkit.createInventory(null, 36, "§c§lServer Selection");
        playerInv.setItem(BOOK_SLOT, infoBook);
        playerInv.setItem(SERVER_SELECT_SLOT, serverSelectItem);
        playerInv.setItem(TOGGLE_PLAYER_SLOT, hidePlayersItem);

        serverSelectInv = Bukkit.createInventory(null, 9); //simplistic and hardcoded for now
        serverSelectInv.setItem(0, itemStackWithNameAndLore(Material.DIAMOND_PICKAXE, "§1§lServer: §2§lSURVIVAL", "§3Mine & Craft!"));
        serverSelectInv.setItem(4, itemStackWithNameAndLore(Material.GRASS, "§1§lServer: §6§lSKYBLOCK","§3Blocks in the Sky!"));
        serverSelectInv.setItem(8, itemStackWithNameAndLore(Material.BEACON, "§1§lServer: §e§lCREATIVE","§3Create things?"));
    }

    GenericListener(HubPlugin hubPlugin) {
        pl = hubPlugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent pme){
        Player p = pme.getPlayer();
        if(HubPlugin.hasNCP){
            NCPExemptionManager.exemptPermanently(p, CheckType.MOVING_NOFALL);
        }

        if(p.isOnGround()){
            flyingPlayers.remove(p.getUniqueId());
            respawnedPlayers.remove(p.getUniqueId());
        }else{
            if(respawnedPlayers.contains(p.getUniqueId())) {
                Vector v = p.getVelocity();
                v.setX(0); v.setZ(0);
                p.setVelocity(v);
            }else if(p.getLocation().getBlockY()<50){
                Location nl = new Location(p.getWorld(),0.5,120.5,0.5,p.getLocation().getYaw(),p.getLocation().getPitch());
                nl.setDirection(p.getLocation().getDirection());
                p.setFallDistance(999f);
                p.teleport(nl);
                respawnedPlayers.add(p.getUniqueId());
            }else if(p.isSneaking()&&!p.isFlying()&&!flyingPlayers.contains(p.getUniqueId())&&pme.getTo().getY() > pme.getFrom().getY()){
                flyingPlayers.add(p.getUniqueId());
                Vector v = p.getLocation().getDirection().normalize().multiply(.75);
                v.setY(pme.getTo().getY() - pme.getFrom().getY());
                p.setVelocity(v);
                p.setFallDistance(999f);
            }
        }

        Location l = p.getLocation();
        if(l.getX() > PKP1.getX() && l.getX() < PKP2.getX()){
            if(l.getY() > PKP1.getY() && l.getY() < PKP2.getY()) {
                if (l.getZ() > PKP1.getZ() && l.getZ() < PKP2.getZ()) {
                    p.setFallDistance(999f);
                    p.teleport(new Location(p.getWorld(),1001.5,102,1001.5,p.getLocation().getYaw(),p.getLocation().getPitch()));
                }
            }
        }
    }
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerBurn(EntityCombustEvent ece) {
        if (ece.getEntity() instanceof Player) {
            ece.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent ede) {
        if (ede.getEntity() instanceof Player) {
            ede.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent pje){
        Player p = pje.getPlayer();
        HubPlugin.ncsbt.addPlayer(p);

        for (UUID u:playersHidingOthers) {
            pl.getServer().getPlayer(u).hidePlayer(pl,p);
        }

        if(!p.hasPermission("plscrafthub.alwaysspawnbypass")){
           p.teleport(new Location(p.getWorld(),0.5,110.5,0.5));
        }

        p.getInventory().setContents(playerInv.getContents());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerItemUse(PlayerInteractEvent pie){
        if(pie.getAction() == Action.RIGHT_CLICK_AIR || pie.getAction() == Action.RIGHT_CLICK_BLOCK){
            if(pie.getItem()==null) return;

            if(pie.getItem().isSimilar(serverSelectItem)){
                pie.setCancelled(true);

                pie.getPlayer().openInventory(serverSelectInv);
            }
            if(pie.getItem().isSimilar(hidePlayersItem)){
                Player p = pie.getPlayer();
                playersHidingOthers.add(p.getUniqueId());

                p.getInventory().setItem(TOGGLE_PLAYER_SLOT, showPlayersItem);
                for (Player tp: pl.getServer().getOnlinePlayers()) {
                    p.hidePlayer(pl, tp);
                }
            }
            if(pie.getItem().isSimilar(showPlayersItem)){
                Player p = pie.getPlayer();
                playersHidingOthers.remove(p.getUniqueId());

                p.getInventory().setItem(TOGGLE_PLAYER_SLOT, hidePlayersItem);
                for (Player tp: pl.getServer().getOnlinePlayers()) {
                    p.showPlayer(pl, tp);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent ide){
        ide.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent ice){
        if(!(ice.getWhoClicked() instanceof Player)) return;

        if(ice.getInventory().getName().equals(serverSelectInv.getName())) {
            ItemStack is = ice.getCurrentItem();
            String itemName = ChatColor.stripColor(is.getItemMeta().getDisplayName());
            System.out.println(itemName);
            if(itemName.startsWith("Server: ")){
                Player p = (Player)ice.getWhoClicked();
                String serverName = itemName.substring(8).toLowerCase();

                System.out.println("Sending " + p.getDisplayName() + " to " + serverName);

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(serverName);
                p.sendPluginMessage(pl,"BungeeCord",out.toByteArray());
            }
        }

        ice.setCancelled(true);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent pqe){
        Player p = pqe.getPlayer();
        HubPlugin.ncsbt.removePlayer(p);

        if(flyingPlayers.contains(p)){
            flyingPlayers.remove(p);
        }
    }
}
