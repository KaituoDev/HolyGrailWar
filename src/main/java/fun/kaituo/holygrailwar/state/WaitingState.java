package fun.kaituo.holygrailwar.state;

import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.gameutils.util.Misc;
import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.sign.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.awt.*;
import java.util.UUID;

public class WaitingState implements GameState, Listener {
    public static final WaitingState INST = new WaitingState();
    public WaitingState() {}
    public HolyGrailWar game;
    @Getter
    private AmountSign amountSign;
    private SaberSign saberSign;
    private LancerSign lancerSign;
    private RiderSign riderSign;
    private ArcherSign archerSign;
    private BerserkerSign berserkerSign;
    private CasterSign casterSign;
    private AssassinSign assassinSign;


    public void init() {
        game = HolyGrailWar.inst();
        amountSign = new AmountSign(game, game.getLoc("AmountSign"));
        saberSign = new SaberSign(game, game.getLoc("SaberSign"));
        lancerSign = new LancerSign(game, game.getLoc("LancerSign"));
        riderSign = new RiderSign(game, game.getLoc("RiderSign"));
        archerSign = new ArcherSign(game, game.getLoc("ArcherSign"));
        berserkerSign = new BerserkerSign(game, game.getLoc("BerserkerSign"));
        casterSign = new CasterSign(game, game.getLoc("CasterSign"));
        assassinSign = new AssassinSign(game, game.getLoc("AssassinSign"));

        Bukkit.getPluginManager().registerEvents(amountSign, game);
        Bukkit.getPluginManager().registerEvents(saberSign, game);
        Bukkit.getPluginManager().registerEvents(lancerSign, game);
        Bukkit.getPluginManager().registerEvents(riderSign, game);
        Bukkit.getPluginManager().registerEvents(archerSign, game);
        Bukkit.getPluginManager().registerEvents(berserkerSign, game);
        Bukkit.getPluginManager().registerEvents(casterSign, game);
        Bukkit.getPluginManager().registerEvents(assassinSign, game);
    }


    @EventHandler
    public void onPlayerClickStartButton(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Block block = event.getClickedBlock();
        assert block != null;
        Player player = event.getPlayer();
        if (!block.getType().equals(Material.OAK_BUTTON)) {
            return;
        }
        if (!game.playerIds.contains(player.getUniqueId())){
            return;
        }
        if (!block.getLocation().equals(game.getLoc("StartButton"))) {
            return;
        }

        if (game.getPlayers().size() != getAmountSign().getAmount()) {
            for (Player p : game.getPlayers()) {
                p.sendMessage("§c游戏人数与现有人数不符！");
            }
            return;
        }
        game.setState(ReadyState.INST);
    }



    @Override
    public void enter() {
        for (Player p : game.getPlayers()) {
            addPlayer(p);
        }
        Bukkit.getPluginManager().registerEvents(this,game);
    }

    @Override
    public void exit() {
        for (Player p : game.getPlayers()){
            removePlayer(p);
        }
        HandlerList.unregisterAll(this);
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
        p.getInventory().removeItem(Misc.getMenu());
        p.removePotionEffect(PotionEffectType.RESISTANCE);
        p.removePotionEffect(PotionEffectType.SATURATION);
    }

    @Override
    public void forceStop() {

    }
}
