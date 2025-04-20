package fun.kaituo.holygrailwar.sign;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AssassinSign extends AbstractSignListener {
    @Getter
    private boolean AssassinActive = true;
    public AssassinSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1,"Assassin:");
        setActive();

    }

    public void setActive() {
        if (AssassinActive == true){
            lines.set(2,"§a开");
        }
        if (AssassinActive == false){
            lines.set(2,"§c关");
        }
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        AssassinActive = !AssassinActive;
        setActive();
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
    }
}
