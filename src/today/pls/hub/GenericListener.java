package today.pls.hub;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import net.minecraft.server.v1_12_R1.Position;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;


public class GenericListener implements Listener {

    private Position PKP1 = new Position(-2,107,-13);
    private Position PKP2 = new Position(2,112,-9);
    private ArrayList<Player> flyingPlayers = new ArrayList<>();
    private ArrayList<Player> respawnedPlayers = new ArrayList<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent pme){
        Player p = pme.getPlayer();
        if(HubPlugin.hasNCP){
            NCPExemptionManager.exemptPermanently(p, CheckType.MOVING_NOFALL);
        }

        if(p.isOnGround()){
            if(flyingPlayers.contains(p)){
                flyingPlayers.remove(p);
            }
            if(respawnedPlayers.contains(p)){
                respawnedPlayers.remove(p);
            }
        }else{
            if(respawnedPlayers.contains(p)) {
                Vector v = p.getVelocity();
                v.setX(0); v.setZ(0);
                p.setVelocity(v);
            }else if(p.getLocation().getBlockY()<50){
                Location nl = new Location(p.getWorld(),0.5,120.5,0.5,p.getLocation().getYaw(),p.getLocation().getPitch());
                nl.setDirection(p.getLocation().getDirection());
                p.setFallDistance(999f);
                p.teleport(nl);
                respawnedPlayers.add(p);
            }else if(p.isSneaking()&&!p.isFlying()&&!flyingPlayers.contains(p)&&pme.getTo().getY() > pme.getFrom().getY()){
                flyingPlayers.add(p);
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

        if(!p.hasPermission("plscrafthub.alwaysspawnbypass")){
           p.teleport(new Location(p.getWorld(),0.5,110.5,0.5));
        }
    }

    public void onPlayerLeave(PlayerQuitEvent pqe){
        Player p = pqe.getPlayer();
        HubPlugin.ncsbt.removePlayer(p);

        if(flyingPlayers.contains(p)){
            flyingPlayers.remove(p);
        }
    }
}
