package fun.kaituo.holygrailwar.state;

import fun.kaituo.gameutils.game.Game;
import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.holygrailwar.HolyGrailWar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class ReadyState implements GameState {

    public static final int COUNTDOWN_SECONDS = 5;
    public static final ReadyState INST = new ReadyState();
    private ReadyState() {}
    private Set<Integer> taskIds = new HashSet<>();

    private HolyGrailWar game;

    public void init() {
        game = HolyGrailWar.inst() ;
    }
    @Override
    public void enter() {
        for (Player p : game.getPlayers()) {
            addPlayer(p);
        }
        taskIds.add(Bukkit.getScheduler().runTaskLater(game, () -> {
            game.setState(FightState.INST);
        }, COUNTDOWN_SECONDS * 20).getTaskId());
    }



    @Override
    public void exit() {
    }

    @Override
    public void tick() {

    }

    @Override
    public void addPlayer(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
    }

    @Override
    public void removePlayer(Player p) {
        p.removePotionEffect(PotionEffectType.RESISTANCE);
        p.removePotionEffect(PotionEffectType.SATURATION);
    }

    @Override
    public void forceStop() {
        game.setState(WaitingState.INST);
    }
}
