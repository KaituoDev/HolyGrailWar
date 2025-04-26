// CharacterBase.java - 基础角色抽象类
package fun.kaituo.holygrailwar.characters;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class CharacterBase {
    protected final Player player;
    protected final String characterName;
    protected final DrawCareerClass.ClassType classType;
    protected GameInventory ginv;

    public CharacterBase(Player player, String characterName, DrawCareerClass.ClassType classType) {
        this.player = player;
        this.characterName = characterName;
        this.classType = classType;
        setupInventory();
    }

    public void setupInventory(){
        HolyGrailWar.inst().getInv(getClass().getSimpleName()).apply(player);
    };
    public abstract void activateSkill();

    public String getCharacterName() {return characterName;}

    public DrawCareerClass.ClassType getClassType() {
        return classType;
    }

    protected void giveItem(ItemStack item) {
        player.getInventory().addItem(item);
    }

    protected void clearInventory() {
        player.getInventory().clear();
    }
}