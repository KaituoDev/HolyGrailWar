package fun.kaituo.holygrailwar;

import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.holygrailwar.state.ReadyState;
import fun.kaituo.holygrailwar.state.WaitingState;
import fun.kaituo.gameutils.game.Game;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("unused")
public class HolyGrailWar extends Game {

    public static final WaitingState INST = new WaitingState();
    private static HolyGrailWar instance;
    public static HolyGrailWar inst() { return instance; }

    public final Set<UUID> playerIds = new HashSet<>();
    @Getter
    private static final Location LobbyLocation = new Location(GameUtils.inst().getMainWorld(), 2000.5, 100, 4000.5);


    public Set<Player> getPlayers() {
        Set<Player> players = new HashSet<>();
        for (UUID id : playerIds) {
            Player p = Bukkit.getPlayer(id);
            assert p != null;
            players.add(p);
        }
        return players;
    }

    private void initStates(){
        WaitingState.INST.init();
        ReadyState.INST.init();
    }

    @Override
    public void addPlayer(Player p) {
        p.setBedSpawnLocation(LobbyLocation, true);
        playerIds.add(p.getUniqueId());
        p.teleport(LobbyLocation);
        super.addPlayer(p);
    }

    @Override
    public void removePlayer(Player p) {
        playerIds.remove(p.getUniqueId());
        super.removePlayer(p);
    }

    @Override
    public void forceStop() {

    }

    @Override
    public void tick() {
        super.tick();
    }

    public void onEnable() {
        super.onEnable();

        instance = this;
        updateExtraInfo("§e圣杯战争", getLoc("lobby"));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            initStates();
            setState(WaitingState.INST);
        }, 1);
    }

    public void onDisable() {
        super.onDisable();
    }

}




