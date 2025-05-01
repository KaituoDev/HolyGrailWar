package fun.kaituo.holygrailwar.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractSkill {
    protected final JavaPlugin plugin;
    protected final Player player;
    private final Material triggerItem;
    private final String skillName;
    private final int cooldownTicks;
    private long lastUsedTime = 0;
    private static final Set<Material> allSkillItems = new HashSet<>();


    public AbstractSkill(JavaPlugin plugin, Player player, Material triggerItem, String skillName, int cooldownTicks) {
        this.plugin = plugin;
        this.player = player;
        this.triggerItem = triggerItem;
        this.skillName = skillName;
        this.cooldownTicks = cooldownTicks;
        // 注册技能物品
        synchronized (allSkillItems) {
            allSkillItems.add(triggerItem);
        }
    }

    public static Set<Material> getAllSkillItems() {
        return new HashSet<>(allSkillItems);
    }

    /**
     * 检查并处理技能触发事件
     * @param event 玩家交互事件
     */
    public void checkAndTrigger(PlayerInteractEvent event) {
        // 检查是否是右键触发
        if (!event.getAction().toString().contains("RIGHT")) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) return;

        // 检查物品类型和名称
        if (item.getType() == triggerItem) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(skillName)) {
                // 检查冷却时间
                if (System.currentTimeMillis() - lastUsedTime < cooldownTicks * 50L) {
                    return;
                }

                // 触发技能
                if (onTrigger(event)) {
                    lastUsedTime = System.currentTimeMillis();
                    player.setCooldown(triggerItem, cooldownTicks);
                }
            }
        }
    }

    /**
     * 技能触发时的逻辑
     * @param event 玩家交互事件
     * @return 是否成功触发技能
     */
    protected abstract boolean onTrigger(PlayerInteractEvent event);

    /**
     * 检查玩家是否正在冷却中
     * @return 是否在冷却中
     */
    public boolean isOnCooldown() {
        return System.currentTimeMillis() - lastUsedTime < cooldownTicks * 50L;
    }

    /**
     * 获取剩余冷却时间(秒)
     * @return 剩余冷却时间(秒)
     */
    public double getRemainingCooldown() {
        long remaining = (lastUsedTime + cooldownTicks * 50L) - System.currentTimeMillis();
        return Math.max(0, remaining) / 1000.0;
    }
}