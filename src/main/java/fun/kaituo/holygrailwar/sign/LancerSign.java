package fun.kaituo.holygrailwar.sign;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LancerSign extends AbstractSignListener {
    @Getter
    private static boolean LancerActive = true;
    public LancerSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1,"Lancer:");
        setActive();
    }

    public void setActive() {
        if (LancerActive == true){
            lines.set(2,"§a开");
        }
        if (LancerActive == false){
            lines.set(2,"§c关");
        }
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        LancerActive = !LancerActive;
        setActive();
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
    }
}
