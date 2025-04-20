package fun.kaituo.holygrailwar.sign;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SaberSign extends AbstractSignListener {
    @Getter
    private boolean SaberActive = true;
    public SaberSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1,"Saber:");
        setActive();
    }

    public void setActive() {
    if (SaberActive == true){
        lines.set(2,"§a开");
    }
    if (SaberActive == false){
        lines.set(2,"§c关");
    }
    update();
}

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
    SaberActive = !SaberActive;
    setActive();
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
    }
}
