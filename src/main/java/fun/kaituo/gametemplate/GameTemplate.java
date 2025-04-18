package fun.kaituo.gametemplate;

import fun.kaituo.gametemplate.state.TemplateState;
import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.game.Game;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class GameTemplate extends Game {
    @Override
    public void addPlayer(Player player) {

    }

    @Override
    public void removePlayer(Player player) {

    }

    @Override
    public void forceStop() {

    }

    @Override
    public void tick() {

    }

    public void onEnable() {
        super.onEnable();
        setState(new TemplateState());
        updateExtraInfo("§f示例游戏", GameUtils.inst().getMainWorld().getSpawnLocation());
    }

    public void onDisable() {
        super.onDisable();
    }
}
