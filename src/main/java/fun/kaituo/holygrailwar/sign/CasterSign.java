package fun.kaituo.holygrailwar.sign;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CasterSign extends AbstractSignListener {
    @Getter
    private static boolean CasterActive = true;
    public CasterSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1,"Caster:");
        setActive();
    }

    public void setActive() {
        if (CasterActive == true){
            lines.set(2,"§a开");
        }
        if (CasterActive == false){
            lines.set(2,"§c关");
        }
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        CasterActive = !CasterActive;
        setActive();
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
    }
}
