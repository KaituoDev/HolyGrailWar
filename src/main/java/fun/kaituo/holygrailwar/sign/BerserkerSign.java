package fun.kaituo.holygrailwar.sign;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BerserkerSign extends AbstractSignListener {
    @Getter
    private boolean BerserkerActive = true;
    public BerserkerSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1,"Berserker:");
        setActive();
    }

    public void setActive() {
        if (BerserkerActive == true){
            lines.set(2,"§a开");
        }
        if (BerserkerActive == false){
            lines.set(2,"§c关");
        }
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        BerserkerActive = !BerserkerActive;
        setActive();
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
    }
}
