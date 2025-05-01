// CharacterBase.java - 基础角色抽象类
package fun.kaituo.holygrailwar.characters;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.utils.AbstractSkill;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public abstract class CharacterBase {
    protected final Player player;
    protected final String characterName;
    protected final DrawCareerClass.ClassType classType;
    protected GameInventory ginv;
    protected final List<AbstractSkill> skills = new ArrayList<>();

    // 新增魔力相关属性
    protected int maxMana;
    protected int currentMana;
    protected int manaRegenRate; // 每tick回复的魔力值
    protected int manaRegenDelay; // 上次使用技能后开始回复的延迟ticks
    private int lastManaUseTick = 0;

    // 新增构造方法部分
    public CharacterBase(Player player, String characterName, DrawCareerClass.ClassType classType,
                         int maxMana, int manaRegenRate, int manaRegenDelay) {
        this.player = player;
        this.characterName = characterName;
        this.classType = classType;
        this.maxMana = maxMana;
        this.currentMana = maxMana;
        this.manaRegenRate = manaRegenRate;
        this.manaRegenDelay = manaRegenDelay;
        setupInventory();

        // 启动魔力回复任务
        startManaRegenTask();
    }

    private void updateManaBar() {
        if (player == null || !player.isOnline()) return;

        // 设置经验条显示魔力百分比 (0-1)
        float manaPercent = (float) currentMana / maxMana;
        player.setExp(manaPercent);

        // 设置经验等级为0，这样就不会显示具体数值
        player.setLevel(0);
    }

    private void startManaRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // 检查是否在回复延迟期内
                int currentTick = Bukkit.getServer().getCurrentTick();
                if (currentTick - lastManaUseTick >= manaRegenDelay) {
                    currentMana = Math.min(currentMana + manaRegenRate, maxMana);
                    updateManaBar(); // 更新经验条
                }
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    // 新增魔力相关方法
    public boolean hasEnoughMana(int cost) {
        return currentMana >= cost;
    }

    public boolean consumeMana(int cost) {
        if (hasEnoughMana(cost)) {
            currentMana -= cost;
            lastManaUseTick = Bukkit.getServer().getCurrentTick();
            updateManaBar(); // 更新经验条
            return true;
        }
        return false;
    }

    public int getCurrentMana() {
        return currentMana;
    }

    public int getMaxMana() {
        return maxMana;
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

        // 清理魔力相关状态
        currentMana = maxMana;
        lastManaUseTick = 0;

        // 清理特定技能资源
        for (AbstractSkill skill : skills) {
            // 如果有需要特殊清理的逻辑可以在这里添加
        }
        skills.clear();
        if (player != null && player.isOnline()) {
            player.setExp(0);
            player.setLevel(0);
        }
    }
}