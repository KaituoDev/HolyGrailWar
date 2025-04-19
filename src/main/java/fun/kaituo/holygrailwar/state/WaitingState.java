package fun.kaituo.holygrailwar.state;

import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.gameutils.util.Misc;
import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.sign.AmountSign;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaitingState implements GameState {
    public static final WaitingState INST = new WaitingState();
    public WaitingState() {}
    public HolyGrailWar game;
    @Getter
    private AmountSign amountSign;


    public void init() {
        game = HolyGrailWar.inst();
        amountSign = new AmountSign(game, game.getLoc("AmountSign"));
        Bukkit.getPluginManager().registerEvents(amountSign, game);
    }

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
    public void addPlayer(Player p) {
        p.getInventory().addItem(Misc.getMenu());
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
        p.teleport(game.getLoc("lobby"));
    }

    @Override
    public void removePlayer(Player p) {

    }

    @Override
    public void forceStop() {

    }
}
