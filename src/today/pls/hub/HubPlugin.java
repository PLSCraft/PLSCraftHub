package today.pls.hub;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class HubPlugin extends JavaPlugin {

    public static Scoreboard nocollidescb;
    public static Team ncsbt;

    public static boolean hasNCP = false;

    @Override
    public void onEnable(){
        super.onEnable();



        if(getServer().getPluginManager().isPluginEnabled("NoCheatPlus")){
            hasNCP = true;
        }

        nocollidescb = getServer().getScoreboardManager().getMainScoreboard();
        if((ncsbt = nocollidescb.getTeam("ncsb")) == null){
            ncsbt = HubPlugin.nocollidescb.registerNewTeam("ncsb");
        }
        ncsbt.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

        GenericListener gl = new GenericListener(this);
        getServer().getPluginManager().registerEvents(gl, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this,"BungeeCord");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
