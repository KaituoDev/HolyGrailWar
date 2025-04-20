package fun.kaituo.holygrailwar.state;

import fun.kaituo.gameutils.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FightState implements GameState {
    @Override
    public void enter() {

    }

    @Override
    public void exit() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void addPlayer(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, -1, 19, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 1, false, false));
    }

    @Override
    public void removePlayer(Player player) {
        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        player.removePotionEffect(PotionEffectType.SATURATION);
    }

    @Override
    public void forceStop() {

    }
}
