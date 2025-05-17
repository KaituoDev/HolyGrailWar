package fun.kaituo.holygrailwar.characters.Kyoko;

import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static org.bukkit.Bukkit.getPlayer;

public class KyokoLancer extends CharacterBase implements Listener {
    private boolean isChainSwordMode = false;
    private static final String TRANSFORM_ITEM_NAME = "链枪变形";
    private static final String CHAIN_SWORD_TRIGGER = "截链复微枪";
    private static final String LANCE_TRIGGER = "截链复微枪";
    private static final int COOLDOWN_TICKS = 20 * 3;

    // 攻击段数相关变量
    private int attackCombo = 1;
    private long lastAttackTime = 0;
    private static final int COMBO_RESET_TIME = 3 * 20; // 3秒(60 ticks)
    private boolean isExecutingCombo1 = false; // 新增状态变量
    private boolean isExecutingCombo2 = false;


    // 物品相关常量
    private static final String FLAME_ITEM_NAME = "净罪之大炎";
    private static final String PILLAR_ITEM_NAME = "断罪之刑柱";
    private static final Material FLAME_MATERIAL = Material.BLAZE_POWDER;
    private static final Material PILLAR_MATERIAL = Material.NETHER_BRICK_WALL;

    public KyokoLancer(Player player) {
        super(player, "佐仓杏子", DrawCareerClass.ClassType.LANCER, 1, 0, 0);
        player.getServer().getPluginManager().registerEvents(this, HolyGrailWar.inst());
        startHealthCheckTask();
        startComboResetTask();
    }

    private void startHealthCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                checkAndReplaceItemsBasedOnHealth(player);
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0L, 20L);
    }

    private void startComboResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAttackTime > COMBO_RESET_TIME * 50) {
                    attackCombo = 1;
                }
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0L, 20L);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        // 检查是否是当前角色
        if (!player.equals(this.player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() != Material.TRIDENT || !containsTrigger(weapon, CHAIN_SWORD_TRIGGER)) {
            return;
        }

        // 检查是否是蓄满力的攻击或正在执行连招
        if (player.getAttackCooldown() < 1.0f || isExecutingCombo1 || isExecutingCombo2) {
            attackCombo = 1;
            return;
        }

        event.setCancelled(true);
        lastAttackTime = System.currentTimeMillis();

        switch (attackCombo) {
            case 1:
                isExecutingCombo1 = true;
                executeCombo1Attack(player);
                break;
            case 2:
                isExecutingCombo2 = true;
                executeCombo2Attack(player);
                break;
            case 3:
                executeCombo3Attack(player);
                attackCombo = (attackCombo % 3) + 1;
                break;
        }
    }

    private void executeCombo1Attack(Player player) {
        player.sendMessage("§a执行第一段攻击：突刺斩击");

        // 获取玩家朝向的方向向量
        org.bukkit.util.Vector direction = player.getLocation().getDirection().setY(0).normalize();
        org.bukkit.Location startLoc = player.getLocation().clone().add(0, 1.5, 0); // 从玩家腰部高度开始

        // 使用BukkitRunnable来实现异步移动和伤害
        new BukkitRunnable() {
            int ticks = 0;
            final int moveTicks = 5; // 移动持续时间(ticks)
            final int effectTicks = 8; // 特效持续时间
            final double effectSpeed = 0.4; // 特效移动速度

            @Override
            public void run() {
                if (ticks == 0) {
                    // 初始移动
                    org.bukkit.Location currentLoc = player.getLocation();
                    org.bukkit.Location targetLoc = currentLoc.clone().add(direction.multiply(1.5));

                    if (!hasObstacleBetween(currentLoc, targetLoc)) {
                        player.teleport(targetLoc);
                    } else {
                        org.bukkit.Location safeLoc = findSafeLocation(currentLoc, direction, 1.5);
                        if (safeLoc != null) {
                            player.teleport(safeLoc);
                        }
                    }

                    // 创建水平倒V字箭头特效
                    createHorizontalArrowEffect(startLoc, direction, effectSpeed, effectTicks);
                }

                if (ticks == moveTicks / 2) {
                    // 在移动中途造成伤害
                    damageEntitiesInFront(player, 5, 2, 8);
                }

                if (ticks >= moveTicks) {
                    // 攻击完成
                    isExecutingCombo1 = false;
                    attackCombo = 2;
                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    // 创建水平倒V字箭头特效
    private void createHorizontalArrowEffect(org.bukkit.Location startLoc, org.bukkit.util.Vector direction, double speed, int duration) {
        new BukkitRunnable() {
            int ticks = 0;
            org.bukkit.Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    return;
                }

                // 移动特效位置
                currentLoc.add(direction.clone().multiply(speed));

                // 创建水平倒V字粒子
                createHorizontalInvertedVParticles(currentLoc, direction);

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }
    // 创建水平倒V字粒子（前段细后端粗）
    private void createHorizontalInvertedVParticles(Location center, Vector direction) {
        // 获取垂直于方向的向量（水平方向）
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        // 获取垂直向量（上下方向）
        Vector vertical = new Vector(0, 1, 0);

        // 粒子参数
        float baseSize = 1.5f; // 基础大小（后端）
        Color baseColor = Color.fromRGB(255, 50, 50);
        Color tipColor = Color.fromRGB(255, 100, 100);

        // 创建倒V字的两翼（从粗到细）
        for (double t = 0; t <= 1; t += 0.15) {
            // 计算当前段的大小（从粗到细）
            float currentSize = (float) (baseSize * (1 - t * 0.7)); // 前端比后端细30%

            // 计算两翼偏移量（水平展开）
            double wingSpan = 0.5 + t * 0.3; // 后端比前端略宽

            // 左侧翼
            Vector leftOffset = perpendicular.clone().multiply(wingSpan)
                    .add(vertical.clone().multiply(0.2 * t)); // 轻微向上偏移
            center.getWorld().spawnParticle(
                    Particle.DUST,
                    center.clone().add(leftOffset),
                    1,
                    new Particle.DustOptions(baseColor, currentSize)
            );

            // 右侧翼
            Vector rightOffset = perpendicular.clone().multiply(-wingSpan)
                    .add(vertical.clone().multiply(0.2 * t)); // 轻微向上偏移
            center.getWorld().spawnParticle(
                    Particle.DUST,
                    center.clone().add(rightOffset),
                    1,
                    new Particle.DustOptions(baseColor, currentSize)
            );
        }

        // 创建尖端效果（更亮但更细）
        for (int i = 0; i < 3; i++) {
            center.getWorld().spawnParticle(
                    Particle.DUST,
                    center.clone().add(direction.clone().multiply(0.3)), // 稍微向前突出
                    2,
                    0.1, 0.1, 0.1,
                    new Particle.DustOptions(tipColor, baseSize * 0.7f) // 尖端更细
            );
        }

        // 创建拖尾效果（后端更粗）
        for (double t = 0.2; t <= 0.8; t += 0.15) {
            Location tailLoc = center.clone().subtract(direction.clone().multiply(t));
            float tailSize = (float) (baseSize * (0.8 + t * 0.3)); // 越往后越粗

            center.getWorld().spawnParticle(
                    Particle.DUST,
                    tailLoc,
                    1,
                    new Particle.DustOptions(
                            Color.fromRGB(200, 30, 30), // 暗红色
                            tailSize
                    )
            );
        }
    }
    // 检查两点之间是否有障碍物
    private boolean hasObstacleBetween(org.bukkit.Location from, org.bukkit.Location to) {
        org.bukkit.util.Vector path = to.toVector().subtract(from.toVector());
        double distance = path.length();
        path.normalize();

        for (double d = 0; d <= distance; d += 0.5) {
            org.bukkit.Location checkLoc = from.clone().add(path.clone().multiply(d));
            if (checkLoc.getBlock().getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    // 寻找安全移动位置
    private org.bukkit.Location findSafeLocation(org.bukkit.Location start, org.bukkit.util.Vector direction, double maxDistance) {
        for (double d = 0.1; d <= maxDistance; d += 0.1) {
            org.bukkit.Location checkLoc = start.clone().add(direction.clone().multiply(d));
            if (checkLoc.getBlock().getType().isSolid()) {
                // 找到障碍物，返回前一个位置
                return start.clone().add(direction.clone().multiply(d - 0.1));
            }
        }
        return start.clone().add(direction.clone().multiply(maxDistance));
    }

    // 对前方区域造成伤害（包含后方2格范围）
    private void damageEntitiesInFront(Player player, double length, double width, double damage) {
        org.bukkit.Location playerLoc = player.getLocation();
        org.bukkit.util.Vector direction = playerLoc.getDirection().setY(0).normalize();
        org.bukkit.util.Vector perpendicular = new org.bukkit.util.Vector(-direction.getZ(), 0, direction.getX()).normalize();

        java.util.Set<org.bukkit.entity.LivingEntity> damagedEntities = new java.util.HashSet<>();

        // 获取世界中的所有活着的实体
        for (org.bukkit.entity.LivingEntity entity : player.getWorld().getLivingEntities()) {
            // 排除玩家自己和已死亡的实体
            if (entity.equals(player) || entity.isDead()) {
                continue;
            }

            // 计算实体相对于玩家的位置向量
            org.bukkit.util.Vector relativePos = entity.getLocation().toVector()
                    .subtract(playerLoc.toVector())
                    .setY(0);

            // 计算在前方距离和侧向距离
            double forwardDist = relativePos.dot(direction);
            double sideDist = Math.abs(relativePos.dot(perpendicular));

            // 检查是否在矩形区域内（向后延伸2格）
            if (forwardDist >= -2 && forwardDist <= length && sideDist <= width
                    && !damagedEntities.contains(entity)) {
                damagedEntities.add(entity);
                entity.damage(damage, player);
            }
        }
    }

    private void executeCombo2Attack(Player player) {
        player.sendMessage("§a执行第二段攻击：半月斩");

        new BukkitRunnable() {
            int ticks = 0;
            final int durationTicks = 5; // 将持续时间减半，因为速度加倍了
            final Location playerLoc = player.getLocation().clone().add(0, 1, 0);

            @Override
            public void run() {
                if (ticks == 0) {
                    // 初始伤害
                    damageSemicircleArea(player, 8, 10);
                    // 创建红色刀光特效（速度加倍）
                    createRedSlashEffect(playerLoc, player.getLocation().getYaw());
                }

                if (ticks >= durationTicks) {
                    // 攻击完成
                    isExecutingCombo2 = false;
                    attackCombo = 3;
                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    // 创建红色刀光特效（从左扫到右）
    private void createRedSlashEffect(Location center, float yaw) {
        // 特效参数
        final int segments = 20; // 分段数量
        final int layers = 3; // 垂直层数
        final double radius = 8; // 半径
        final Color baseColor = Color.fromRGB(255, 50, 50); // 基础红色
        final Color brightColor = Color.fromRGB(255, 100, 100); // 高亮红色

        new BukkitRunnable() {
            int progress = 0;
            final int maxProgress = segments / 2; // 进度减半，因为每次处理两个角度

            @Override
            public void run() {
                if (progress >= maxProgress) {
                    this.cancel();
                    return;
                }

                // 同时处理当前角度和下一个角度
                for (int offset = 0; offset < 2; offset++) {
                    int currentSegment = progress * 2 + offset;
                    if (currentSegment >= segments) continue;

                    // 修改角度计算：从-90度(左)到90度(右)
                    double angle = Math.toRadians(180 * currentSegment / (double)segments);                    // 不需要额外旋转90度了，因为我们直接计算了从左到右的角度

                    // 创建当前角度的刀光
                    for (int l = 0; l < layers; l++) {
                        double yOffset = 0.3 * (l + 1); // 垂直偏移
                        float size = 1.2f - 0.2f * l; // 上层粒子稍大

                        // 计算粒子位置
                        double x = radius * Math.cos(angle);
                        double z = radius * Math.sin(angle);

                        // 旋转到玩家面向的方向
                        Vector vec = new Vector(x, yOffset, z);
                        vec = rotateAroundY(vec, yaw);

                        Location particleLoc = center.clone().add(vec);

                        // 使用渐变色，扫过的边缘更亮
                        Color particleColor = currentSegment == segments - 1 ? brightColor : baseColor;

                        particleLoc.getWorld().spawnParticle(
                                Particle.DUST,
                                particleLoc,
                                2,
                                new Particle.DustOptions(particleColor, size)
                        );

                        // 从中心到边缘的连线效果
                        for (double t = 0.1; t <= 0.9; t += 0.1) {
                            Location lineLoc = center.clone().add(vec.clone().multiply(t));
                            lineLoc.getWorld().spawnParticle(
                                    Particle.DUST,
                                    lineLoc,
                                    1,
                                    new Particle.DustOptions(
                                            Color.fromRGB(200, 30, 30),
                                            0.8f
                                    )
                            );
                        }
                    }
                }

                progress++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    // 辅助方法：绕Y轴旋转向量
    private Vector rotateAroundY(Vector vec, double yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        double x = vec.getX() * cos - vec.getZ() * sin;
        double z = vec.getX() * sin + vec.getZ() * cos;

        return new Vector(x, vec.getY(), z);
    }

    // 半圆形区域伤害方法（自定义检测逻辑）
    private void damageSemicircleArea(Player player, double radius, int damage) {
        org.bukkit.Location playerLoc = player.getLocation();
        float yaw = playerLoc.getYaw();
        java.util.Set<org.bukkit.entity.LivingEntity> damagedEntities = new java.util.HashSet<>();

        // 获取世界中的所有活着的实体
        for (org.bukkit.entity.LivingEntity entity : player.getWorld().getLivingEntities()) {
            // 排除玩家自己和已死亡的实体
            if (entity.equals(player) || entity.isDead()) {
                continue;
            }

            // 计算实体与玩家的距离
            double distance = entity.getLocation().distance(playerLoc);
            if (distance > radius) {
                continue;
            }

            // 计算实体相对于玩家的角度
            org.bukkit.util.Vector direction = entity.getLocation().toVector()
                    .subtract(playerLoc.toVector())
                    .setY(0)
                    .normalize();
            org.bukkit.util.Vector playerDirection = playerLoc.getDirection().setY(0).normalize();

            double angle = Math.toDegrees(Math.atan2(
                    direction.getX() * playerDirection.getZ() - direction.getZ() * playerDirection.getX(),
                    direction.getX() * playerDirection.getX() + direction.getZ() * playerDirection.getZ()
            ));

            // 检查是否在半圆形范围内（-90到90度）
            if (angle >= -90 && angle <= 90 && !damagedEntities.contains(entity)) {
                damagedEntities.add(entity);
                entity.damage(damage, player);
            }
        }
    }

    private void executeCombo3Attack(Player player) {
        player.sendMessage("§a执行第三段攻击：上挑击飞");

        new BukkitRunnable() {
            int ticks = 0;
            final int durationTicks = 2; // 攻击持续时间

            @Override
            public void run() {
                if (ticks == 0) {
                    // 初始伤害和击飞效果
                    damageAndLaunchEntities(player, 5, 12, 1);
                }

                if (ticks >= durationTicks) {
                    // 攻击完成
                    attackCombo = 1; // 重置为第一段
                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    // 对前方区域造成伤害并击飞实体
    private void damageAndLaunchEntities(Player player, double length, double damage, double launchPower) {
        org.bukkit.Location playerLoc = player.getLocation();
        org.bukkit.util.Vector direction = playerLoc.getDirection().setY(0).normalize();
        org.bukkit.util.Vector perpendicular = new org.bukkit.util.Vector(-direction.getZ(), 0, direction.getX()).normalize();

        java.util.Set<org.bukkit.entity.LivingEntity> damagedEntities = new java.util.HashSet<>();

        // 获取世界中的所有活着的实体
        for (org.bukkit.entity.LivingEntity entity : player.getWorld().getLivingEntities()) {
            // 排除玩家自己和已死亡的实体
            if (entity.equals(player) || entity.isDead()) {
                continue;
            }

            // 计算实体相对于玩家的位置向量
            org.bukkit.util.Vector relativePos = entity.getLocation().toVector()
                    .subtract(playerLoc.toVector())
                    .setY(0);

            // 计算在前方距离和侧向距离
            double forwardDist = relativePos.dot(direction);
            double sideDist = Math.abs(relativePos.dot(perpendicular));

            // 检查是否在矩形区域内
            if (forwardDist >= 0 && forwardDist <= length && sideDist <= 2
                    && !damagedEntities.contains(entity)) {
                damagedEntities.add(entity);

                // 造成伤害
                entity.damage(damage, player);

                // 击飞效果 - 向上和略微向前的力
                org.bukkit.util.Vector launchVector = new org.bukkit.util.Vector(
                        direction.getX() * 0,  // 略微向前
                        launchPower,              // 主要向上的力
                        direction.getZ() * 0    // 略微向前
                );
                entity.setVelocity(launchVector);


            }
        }
    }

    private void checkAndReplaceItemsBasedOnHealth(Player player) {
        double health = player.getHealth();
        if (health > 5) {
            replaceItemsInInventory(player, FLAME_MATERIAL, FLAME_ITEM_NAME,
                    HolyGrailWar.inst().getInv("KyokoLancerSword").getHotbar(2));
        } else {
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

        player.sendMessage("§c未找到有效的形态切换物品!");
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

    @Override
    public void cleanup() {
        super.cleanup();
        // 清理特定于KyokoLancer的资源
        if (player != null) {
            player.setCooldown(Material.STRING, 0);
        }
    }
}