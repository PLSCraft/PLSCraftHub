package today.pls.hub;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;
import today.pls.plscore.api.PLSCoreAPI;

import java.util.ArrayList;
import java.util.UUID;

import static today.pls.hub.Utils.*;

public class GenericListener implements Listener {

    private static final int BOOK_SLOT = 0, TOGGLE_PLAYER_SLOT = 8, SERVER_SELECT_SLOT = 4, TRAILS_SLOT = 2;

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
        showPlayersItem = itemStackWithName(Material.LEVER, "§2§lShow Players");

        infoBook = new ItemStack(Material.WRITTEN_BOOK);
        infoBook.addUnsafeEnchantment(Enchantment.THORNS, 1337);
        BookMeta m = (BookMeta) infoBook.getItemMeta();
        m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        m.setTitle("§b§lPLSCraft Handbook");
        m.setAuthor("RaidAndFade");
        m.setGeneration(BookMeta.Generation.ORIGINAL);
        m.addPage("Welcome to PLSCraft");
        infoBook.setItemMeta(m);


        playerInv = Bukkit.createInventory(null, 36, "§c§lServer Selection");
        playerInv.setItem(BOOK_SLOT, infoBook);
        //playerInv.setItem(TRAILS_SLOT, itemStackWithName(Material.BLAZE_POWDER, "§6§lTrails"));
        playerInv.setItem(SERVER_SELECT_SLOT, serverSelectItem);
        playerInv.setItem(TOGGLE_PLAYER_SLOT, hidePlayersItem);

        serverSelectInv = Bukkit.createInventory(null, 9*5, "Server Selection"); //simplistic and hardcoded for now

        ItemStack placeholder = itemStackWithName(Material.STAINED_GLASS_PANE,"§0");
        placeholder.setDurability((short)7);

        int size = serverSelectInv.getSize();
        for(int i=0;i<size;i++){
            if(! ((i < 9) || (i >= size-9) || (i%9==0) || (i%9==8)))
                continue;
            serverSelectInv.setItem(i,placeholder);
        }

    }

    GenericListener(HubPlugin hubPlugin) {
        pl = hubPlugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent pme){
        Player p = pme.getPlayer();
        if(HubPlugin.hasNCP){
            fr.neatmonster.nocheatplus.hooks.NCPExemptionManager.exemptPermanently(p, fr.neatmonster.nocheatplus.checks.CheckType.MOVING_NOFALL);
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

        if(HubPlugin.hasNCP){
            fr.neatmonster.nocheatplus.hooks.NCPExemptionManager.unexempt(p, fr.neatmonster.nocheatplus.checks.CheckType.MOVING_NOFALL);
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

        p.getInventory().clear();

        for(int s=0;s<playerInv.getContents().length;s++){
            if(playerInv.getItem(s)!=null)
                p.getInventory().setItem(s,playerInv.getItem(s).clone());
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerItemUse(PlayerInteractEvent pie){
        if (pie.getAction() == Action.RIGHT_CLICK_AIR || pie.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (pie.getItem() == null) return;

            if (pie.getItem().isSimilar(serverSelectItem)) {
                pie.setCancelled(true);
                final Player p = pie.getPlayer();
                final Inventory pi = Bukkit.createInventory(null, serverSelectInv.getSize(), serverSelectInv.getTitle());
                pi.setContents(serverSelectInv.getContents());

                serverItemStack("survival", Material.DIAMOND_PICKAXE, "§3Mine AND Craft!", (i1) -> {
                    serverItemStack("skyblock", Material.GRASS, "§6Blocks in the Sky!", (i2) -> {
                        serverItemStack("creative", Material.BEACON, "§eCreate things?", (i3) -> {
                            serverItemStack("revelation", Material.IRON_DOOR, "Whitelist FTB Revelations!", (i4) -> {
                                serverItemStack("dev", Material.DEAD_BUSH, "PLSCRAFT DEVELOPMENT SERVER!", (i5) -> {
                                    try {
                                        if (p.hasPermission("bungeecord.server.dev")) {
                                            pi.setItem(5 + 9 * 3, i5);
                                        } else {
                                            pi.setItem(5 + 9 * 3, null);
                                        }
                                        if (p.hasPermission("bungeecord.server.revelation")) {
                                            pi.setItem(3 + 9 * 3, i4);
                                        } else {
                                            pi.setItem(3 + 9 * 3, null);
                                        }

                                        pi.setItem(2 + 9 * 2, i1);
                                        pi.setItem(4 + 9 * 2, i2);
                                        pi.setItem(6 + 9 * 2, i3);
                                        p.openInventory(pi);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            });
                        });
                    });
                });
            }
            if (pie.getItem().isSimilar(hidePlayersItem)) {
                Player p = pie.getPlayer();
                playersHidingOthers.add(p.getUniqueId());

                p.getInventory().setItem(TOGGLE_PLAYER_SLOT, showPlayersItem);
                for (Player tp : pl.getServer().getOnlinePlayers()) {
                    p.hidePlayer(pl, tp);
                }
            }
            if (pie.getItem().isSimilar(showPlayersItem)) {
                Player p = pie.getPlayer();
                playersHidingOthers.remove(p.getUniqueId());

                p.getInventory().setItem(TOGGLE_PLAYER_SLOT, hidePlayersItem);
                for (Player tp : pl.getServer().getOnlinePlayers()) {
                    p.showPlayer(pl, tp);
                }
            }
        }
    }

    private void serverItemStack(String s, Material m, String motd, Callback<ItemStack> i) {
        ItemStack is = new ItemStack(m);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName("§b§lServer: §3§l" + s.toUpperCase());
        ArrayList<String> lore = new ArrayList<>();
        lore.add(motd);
        if (HubPlugin.hasPLSCore && PLSCoreAPI.getInstance().haveServer(s)) {
            PLSCoreAPI.getInstance().getServer(s, (ss, t) -> {
                im.setDisplayName("§b§lServer: §2§l" + s.toUpperCase());
                im.addEnchant(Enchantment.THORNS, 1337, true);
                im.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS);
                lore.add("§aPlayers: §d" + ss.playerCount + "§5/§d" + ss.playerCap);
                im.setLore(lore);
                is.setItemMeta(im);
                i.call(is);
            });
        } else {
            im.addEnchant(Enchantment.THORNS, 1337, true);
            im.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS);
            im.setLore(lore);
            is.setItemMeta(im);
            i.call(is);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent ide){
        ide.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent ice){
        ice.setCancelled(true);

        if(!(ice.getWhoClicked() instanceof Player)) return;
        if(ice.getCurrentItem() == null) return;
        if(ice.getCurrentItem().getItemMeta() == null) return;

        if(ice.getInventory().getName().equals(serverSelectInv.getName())) {
            ItemStack is = ice.getCurrentItem();
            String itemName = ChatColor.stripColor(is.getItemMeta().getDisplayName());
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
    }

    @EventHandler(priority=EventPriority.NORMAL)
    public void onPickup(EntityPickupItemEvent epie){
        epie.setCancelled(true);
        epie.getItem().remove();
    }

    @EventHandler(priority=EventPriority.NORMAL)
    public void onDrop(PlayerDropItemEvent pdie){
        pdie.setCancelled(true);
    }


    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent pqe){
        Player p = pqe.getPlayer();
        HubPlugin.ncsbt.removePlayer(p);

        flyingPlayers.remove(p.getUniqueId());
    }
}
