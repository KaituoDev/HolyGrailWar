package fun.kaituo.holygrailwar.sign;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class RiderSign extends AbstractSignListener {
    @Getter
    private static boolean RiderActive = true;
    public RiderSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1,"Rider:");
        setActive();
    }

    public void setActive() {
        if (RiderActive == true){
            lines.set(2,"§a开");
        }
        if (RiderActive == false){
            lines.set(2,"§c关");
        }
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        RiderActive = !RiderActive;
        setActive();
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
    }
}
