// CharacterBase.java - 基础角色抽象类
package fun.kaituo.holygrailwar.characters;

import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class CharacterBase {
    protected final Player player;
    protected final String characterName;
    protected final DrawCareerClass.ClassType classType;

    public CharacterBase(Player player, String characterName, DrawCareerClass.ClassType classType) {
        this.player = player;
        this.characterName = characterName;
        this.classType = classType;
    }

    public abstract void setupInventory();
    public abstract void activateSkill();

    public String getCharacterName() {
        return characterName;
    }

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