package fun.kaituo.holygrailwar.sign;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AmountSign extends AbstractSignListener {
    @Getter
    private int amount = 7;
    public AmountSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1, "游戏人数：");
        updateAmount();
    }

    private void updateAmount() {
        lines.set(2, "§c" + amount + "人");
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        if (amount >= 7) {
            return;
        }
        amount += 1;
        updateAmount();
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
        if (amount <= 2) {
            return;
        }
        amount -= 1;
        updateAmount();
    }
}
