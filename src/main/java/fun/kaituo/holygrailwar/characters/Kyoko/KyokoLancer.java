package fun.kaituo.holygrailwar.characters.Kyoko;

import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import static org.bukkit.Bukkit.getPlayer;

public class KyokoLancer extends CharacterBase implements Listener {
    private boolean isChainSwordMode = false;
    private static final String TRANSFORM_ITEM_NAME = "链枪变形";
    private static final String CHAIN_SWORD_TRIGGER = "截链复微枪";
    private static final String LANCE_TRIGGER = "截链复微枪";
    private static final int COOLDOWN_TICKS = 20 * 3;

    // 新增物品相关常量
    private static final String FLAME_ITEM_NAME = "净罪之大炎";
    private static final String PILLAR_ITEM_NAME = "断罪之刑柱";
    private static final Material FLAME_MATERIAL = Material.BLAZE_POWDER;
    private static final Material PILLAR_MATERIAL = Material.NETHER_BRICK_WALL;

    public KyokoLancer(Player player) {
        super(player, "佐仓杏子", DrawCareerClass.ClassType.LANCER, 1, 0, 0);
        player.getServer().getPluginManager().registerEvents(this, HolyGrailWar.inst());
        startHealthCheckTask();
    }

    private void startHealthCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {

                checkAndReplaceItemsBasedOnHealth(player);
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0L, 20L); // 每秒检查一次
    }

    private void checkAndReplaceItemsBasedOnHealth(Player player) {
        double health = player.getHealth();

        if (health > 5) {
            // 血量高于5时，替换烈焰粉
            replaceItemsInInventory(player, FLAME_MATERIAL, FLAME_ITEM_NAME,
                    HolyGrailWar.inst().getInv("KyokoLancerSword").getHotbar(2));
        } else {
            // 血量小于等于5时，替换地狱砖墙
            replaceItemsInInventory(player, PILLAR_MATERIAL, PILLAR_ITEM_NAME,
                    HolyGrailWar.inst().getInv("KyokoLancerSword").getHotbar(3));
        }
    }

    private void replaceItemsInInventory(Player player, Material sourceMaterial, String nameContains, ItemStack replacement) {
        if (replacement == null) return;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == sourceMaterial && containsName(item, nameContains)) {
                player.getInventory().setItem(i, replacement.clone());
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null || !event.getAction().toString().contains("RIGHT")) return;

        ItemStack item = event.getItem();
        if (item.getType() != Material.STRING) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().contains(TRANSFORM_ITEM_NAME)) return;

        if (player.hasCooldown(Material.STRING)) {
            event.setCancelled(true);
            return;
        }

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack triggerItem = player.getInventory().getItem(i);
            if (triggerItem == null) continue;

            ItemStack newItem = null;

            if (triggerItem.getType() == Material.TRIDENT && containsTrigger(triggerItem, CHAIN_SWORD_TRIGGER)) {
                newItem = HolyGrailWar.inst().getInv("KyokoLancerSword").getHotbar(0);
                isChainSwordMode = true;
            } else if (triggerItem.getType() == Material.IRON_SWORD && containsTrigger(triggerItem, LANCE_TRIGGER)) {
                newItem = HolyGrailWar.inst().getInv("KyokoLancerLance").getHotbar(0);
                isChainSwordMode = false;
            }

            if (newItem != null) {
                player.getInventory().setItem(i, newItem);
                event.setCancelled(true);
                player.setCooldown(Material.STRING, COOLDOWN_TICKS);
                return;
            }
        }

        player.sendMessage("未找到有效的形态切换物品!");
    }

    private boolean containsTrigger(ItemStack item, String trigger) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().contains(trigger);
    }

    private boolean containsName(ItemStack item, String name) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().contains(name);
    }

    public boolean isChainSwordMode() {
        return isChainSwordMode;
    }
}