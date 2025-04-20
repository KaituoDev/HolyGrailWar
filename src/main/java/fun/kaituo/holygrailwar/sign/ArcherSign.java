package fun.kaituo.holygrailwar.sign;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ArcherSign extends AbstractSignListener {
    @Getter
    private boolean ArcherActive = true;
    public ArcherSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1,"Archer:");
        setActive();
    }

    public void setActive() {
        if (ArcherActive == true){
            lines.set(2,"§a开");
        }
        if (ArcherActive == false){
            lines.set(2,"§c关");
        }
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        ArcherActive = !ArcherActive;
        setActive();
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
    }
}
