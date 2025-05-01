// CharacterBase.java - 基础角色抽象类
package fun.kaituo.holygrailwar.characters;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.utils.AbstractSkill;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class CharacterBase {
    protected final Player player;
    protected final String characterName;
    protected final DrawCareerClass.ClassType classType;
    protected GameInventory ginv;
    protected final List<AbstractSkill> skills = new ArrayList<>();

    public CharacterBase(Player player, String characterName, DrawCareerClass.ClassType classType) {
        this.player = player;
        this.characterName = characterName;
        this.classType = classType;
        setupInventory();
    }

    public void setupInventory(){
        HolyGrailWar.inst().getInv(getClass().getSimpleName()).apply(player);
    };

    public String getCharacterName() {return characterName;}

    public DrawCareerClass.ClassType getClassType() {
        return classType;
    }

    protected void giveItem(ItemStack item) {
        player.getInventory().addItem(item);
    }

    public void clearInventory() {
        player.getInventory().clear();
    }

    // 添加技能到角色
    protected void addSkill(AbstractSkill skill) {
        skills.add(skill);
    }

    public void cleanup() {
        // 清理所有技能物品的冷却时间
        for (Material item : AbstractSkill.getAllSkillItems()) {
            player.setCooldown(item, 0);
        }

        // 清理特定技能资源
        for (AbstractSkill skill : skills) {
            // 如果有需要特殊清理的逻辑可以在这里添加
        }
        skills.clear();
    }
}