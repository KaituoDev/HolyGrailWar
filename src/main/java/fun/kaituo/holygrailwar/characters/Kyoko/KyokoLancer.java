package fun.kaituo.holygrailwar.characters.Kyoko;

import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.utils.AbstractSkill;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static final String SKILL_ITEM_NAME = "截链复微枪";
    private static final int SKILL_COOLDOWN_TICKS = 20 * 10; // 10秒冷却
    private static final int SKILL_MANA_COST = 20*20; // 技能消耗的蓝量
    private boolean isUsingSkill = false; // 标记是否正在使用技能

    private static final String SWORD_SKILL_NAME = "截链复微枪";
    private static final int SWORD_SKILL_COOLDOWN = 20 * 10; // 10秒冷却
    private static final int SWORD_SKILL_MANA_COST = 300; // 消耗300魔力

    private boolean isLockingView = false;
    private float lockedYaw;
    private float lockedPitch;

    private static final int FLAME_SKILL_MANA_COST = 0; // 魔力消耗
    private static final int FLAME_SKILL_COOLDOWN = 20 ; //冷却


    // 物品相关常量
    private static final String FLAME_ITEM_NAME = "净罪之大炎";
    private static final String PILLAR_ITEM_NAME = "断罪之刑柱";
    private static final Material FLAME_MATERIAL = Material.BLAZE_POWDER;
    private static final Material PILLAR_MATERIAL = Material.NETHER_BRICK_WALL;

    public KyokoLancer(Player player) {
        super(player, "佐仓杏子", DrawCareerClass.ClassType.LANCER, 20*80, 1, 0);
        player.getServer().getPluginManager().registerEvents(this, HolyGrailWar.inst());
        startHealthCheckTask();
        startComboResetTask();
        startFlameSkillCheckTask();
        // 确保物品栏中有地狱岩砖墙物品
        checkAndReplaceItemsBasedOnHealth(player);
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwordSkillUse(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT")) return;
        if (!event.getPlayer().equals(this.player)) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.IRON_SWORD) return;

        if (!containsTrigger(item, SWORD_SKILL_NAME)) return;

        // 检查冷却和魔力
        CharacterBase character = HolyGrailWar.inst().getPlayerCharacter(player);
        if (character == null || !character.hasEnoughMana(SWORD_SKILL_MANA_COST)) {
            return;
        }

        // 执行技能
        executeSwordSkill(player);
        character.consumeMana(SWORD_SKILL_MANA_COST);
        player.setCooldown(Material.IRON_SWORD, SWORD_SKILL_COOLDOWN);
        event.setCancelled(true);
    }

    // 修改剑形态特殊攻击方法
    private void executeSwordSkill(Player player) {
        // 锁定玩家视角
        isLockingView = true;
        lockedYaw = player.getLocation().getYaw();
        lockedPitch = player.getLocation().getPitch();

        // 禁用跳跃
        player.setAllowFlight(false);
        player.setFlying(false);

        // 给予抗性提升和不可移动效果
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                16, // 0.8秒(16 ticks)
                2   // 抗性提升III
        ));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                16, // 0.8秒(16 ticks)
                255 // 最大减速效果(不可移动)
        ));

        // 0.8秒后解锁视角
        new BukkitRunnable() {
            @Override
            public void run() {
                isLockingView = false;
                player.setAllowFlight(false);
            }
        }.runTaskLater(HolyGrailWar.inst(), 16);

        // 0.8秒后执行伤害和特效
        new BukkitRunnable() {
            @Override
            public void run() {
                Location startLoc = player.getLocation().add(0, 1.0, 0);
                Vector direction = startLoc.getDirection().normalize();

                // 计算最大距离(10格)并检查是否有障碍物
                double maxDistance = 10.0;
                boolean hitBlock = false;

                for (double d = 0; d <= maxDistance; d += 0.5) {
                    Location checkLoc = startLoc.clone().add(direction.clone().multiply(d));
                    if (checkLoc.getBlock().getType().isSolid()) {
                        maxDistance = d;
                        hitBlock = true;
                        break;
                    }
                }

                createCrescentMoonEffect(startLoc, direction, maxDistance, hitBlock);
                damageEntitiesInLine(player, startLoc, direction, 15, maxDistance);
            }
        }.runTaskLater(HolyGrailWar.inst(), 16);
    }

    // 创建月牙状剑气特效(添加maxDistance和hitBlock参数)
    private void createCrescentMoonEffect(Location startLoc, Vector direction, double maxDistance, boolean hitBlock) {
        new BukkitRunnable() {
            int ticks = 0;
            final double speed = 1.2; // 移动速度
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (ticks * speed >= maxDistance) {
                    this.cancel();
                    return;
                }

                // 移动剑气位置
                currentLoc.add(direction.clone().multiply(speed));

                // 创建月牙形状
                createCrescentShape(currentLoc, direction, ticks);

                // 创建直线拖尾
                createTrailEffect(startLoc, currentLoc);

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    // 创建月牙形状
    private void createCrescentShape(Location center, Vector direction, int progress) {
        // 获取垂直于方向的向量
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

        // 月牙参数
        double radius = 2.5; // 基础半径
        double thickness = 0.3; // 厚度
        int segments = 12; // 分段数

        // 颜色渐变(从亮到暗)
        Color outerColor = Color.fromRGB(255, 100, 100);
        Color innerColor = Color.fromRGB(200, 30, 30);

        // 创建月牙形状
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * i / (segments - 1); // 0到π

            // 外弧
            double outerX = radius * Math.cos(angle);
            double outerY = radius * 0.6 * Math.sin(angle); // 垂直方向压缩

            // 内弧
            double innerX = (radius - thickness) * Math.cos(angle);
            double innerY = (radius - thickness) * 0.6 * Math.sin(angle);

            // 旋转到移动方向
            Vector outerVec = perpendicular.clone().multiply(outerX)
                    .add(new Vector(0, outerY, 0));
            Vector innerVec = perpendicular.clone().multiply(innerX)
                    .add(new Vector(0, innerY, 0));

            // 应用玩家面向方向
            outerVec = rotateAroundY(outerVec, center.getYaw());
            innerVec = rotateAroundY(innerVec, center.getYaw());

            // 生成粒子
            Location outerLoc = center.clone().add(outerVec);
            Location innerLoc = center.clone().add(innerVec);

            // 外弧粒子(亮色)
            outerLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    outerLoc,
                    1,
                    new Particle.DustOptions(outerColor, 1.2f)
            );

            // 内弧粒子(暗色)
            innerLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    innerLoc,
                    1,
                    new Particle.DustOptions(innerColor, 0.8f)
            );

            // 连接内外弧的线
            if (i % 2 == 0) {
                for (double t = 0.1; t <= 0.9; t += 0.2) {
                    Location lineLoc = outerLoc.clone().add(
                            innerLoc.clone().subtract(outerLoc).toVector().multiply(t)
                    );
                    lineLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            lineLoc,
                            1,
                            new Particle.DustOptions(
                                    Color.fromRGB(220, 60, 60),
                                    0.7f
                            )
                    );
                }
            }
        }

        // 中心亮点
        center.getWorld().spawnParticle(
                Particle.DUST,
                center,
                3,
                0.2, 0.2, 0.2,
                new Particle.DustOptions(Color.fromRGB(255, 150, 150), 1.5f)
        );
    }

    // 创建直线拖尾效果
    private void createTrailEffect(Location start, Location end) {
        Vector path = end.toVector().subtract(start.toVector());
        double distance = path.length();
        path.normalize();

        // 拖尾粒子数基于距离
        int particles = (int) (distance * 2);
        if (particles < 3) particles = 3;

        for (int i = 0; i < particles; i++) {
            double ratio = (double) i / particles;
            Location particleLoc = start.clone().add(path.clone().multiply(distance * ratio));

            // 颜色渐变(从亮到暗)
            int red = 200 + (int)(55 * (1 - ratio));
            Color color = Color.fromRGB(red, 30, 30);

            // 大小渐变(从大到小)
            float size = 1.0f - 0.6f * (float)ratio;

            particleLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    new Particle.DustOptions(color, size)
            );
        }
    }

    // 对直线上的所有敌人造成伤害(使用maxDistance参数)
    private void damageEntitiesInLine(Player player, Location start, Vector direction, double damage, double maxDistance) {
        // 临时禁用事件监听以避免递归
        HandlerList.unregisterAll(KyokoLancer.this);

        try {
            Set<LivingEntity> damagedEntities = new HashSet<>();

            // 射线检测，寻找直线上的敌人
            for (double d = 0; d <= maxDistance; d += 0.5) {
                Location checkLoc = start.clone().add(direction.clone().multiply(d));

                // 检查是否有方块阻挡
                if (checkLoc.getBlock().getType().isSolid()) {
                    break; // 遇到方块就停止检测
                }

                // 检查这个位置是否有实体
                for (Entity entity : player.getWorld().getNearbyEntities(checkLoc, 1.0, 1.0, 1.0)) {
                    if (entity instanceof LivingEntity &&
                            !(entity instanceof ArmorStand) &&
                            !entity.equals(player) &&
                            !entity.isDead() &&
                            !damagedEntities.contains(entity)) {

                        // 造成伤害
                        ((LivingEntity) entity).damage(damage, player);
                        damagedEntities.add((LivingEntity)entity);

                        // 添加击退效果
                        Vector knockback = direction.clone().multiply(0.5).setY(0.2);
                        entity.setVelocity(knockback);

                        // 添加命中特效
                        Location hitLoc = entity.getLocation().add(0, 1, 0);
                        hitLoc.getWorld().spawnParticle(
                                Particle.DUST,
                                hitLoc,
                                10,
                                0.5, 0.5, 0.5,
                                new Particle.DustOptions(Color.RED, 1.5f)
                        );
                    }
                }
            }
        } finally {
            // 重新注册事件监听
            player.getServer().getPluginManager().registerEvents(KyokoLancer.this, HolyGrailWar.inst());
        }
    }
    //三叉戟命中
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player player = (Player) event.getEntity().getShooter();

        // 检查是否是当前角色
        if (!player.equals(this.player)) return;

        // 检查是否是三叉戟
        if (event.getEntity() instanceof org.bukkit.entity.Trident) {
            // 设置10秒冷却时间
            player.setCooldown(Material.TRIDENT, 20 * 10);

            // 对命中的非盔甲架实体造成持续伤害
            if (event.getHitEntity() != null && !(event.getHitEntity() instanceof ArmorStand) && event.getHitEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getHitEntity();
                applyBleedEffect(target, 15, 5 * 20); // 5秒内共15点伤害
            }
        }
    }

    private void applyBleedEffect(LivingEntity target, int totalDamage, int durationTicks) {
        final int damagePerTick = 1; // 每次伤害量
        final int ticksBetweenDamage = durationTicks / (totalDamage / damagePerTick); // 计算伤害间隔

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= durationTicks || target.isDead()) {
                    this.cancel();
                    return;
                }

                // 每隔一定ticks造成伤害
                if (ticksElapsed % ticksBetweenDamage == 0) {
                    target.damage(damagePerTick, player);

                    // 添加流血粒子效果
                    Location loc = target.getLocation().add(0, 1, 0);
                    target.getWorld().spawnParticle(
                            Particle.DUST,
                            loc,
                            10,
                            0.5, 0.5, 0.5,
                            new Particle.DustOptions(Color.RED, 1.0f)
                    );
                }

                ticksElapsed++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }


    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        // 检查是否是当前角色
        if (!player.equals(this.player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null) return;

        // 检查是否是三叉戟或铁剑，并且包含触发字符
        boolean isTridentAttack = weapon.getType() == Material.TRIDENT && containsTrigger(weapon, CHAIN_SWORD_TRIGGER);
        boolean isSwordAttack = weapon.getType() == Material.IRON_SWORD && containsTrigger(weapon, LANCE_TRIGGER);

        if (!isTridentAttack && !isSwordAttack) {
            return;
        }

        // 检查是否是蓄满力的攻击或正在执行连招
        if (player.getAttackCooldown() < 1.0f || isExecutingCombo1 || isExecutingCombo2) {
            attackCombo = 1;
            return;
        }

        event.setCancelled(true);
        lastAttackTime = System.currentTimeMillis();

        if (isTridentAttack) {
            // 三叉戟(枪形态)攻击逻辑
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
                    break;
            }
        } else {
            // 铁剑(剑形态)攻击逻辑
            switch (attackCombo) {
                case 1:
                    isExecutingCombo1 = true;
                    executeSwordCombo1Attack(player);
                    break;
                case 2:
                    isExecutingCombo2 = true;
                    executeSwordCombo2Attack(player);
                    break;
                case 3:
                    executeSwordCombo3Attack(player);
                    break;
            }
        }
    }

    // 剑形态第一段攻击
    private void executeSwordCombo1Attack(Player player) {
        player.sendMessage("§a执行剑形态第一段攻击：圆环斩击");

        // 创建圆形斩击特效（只在外圈）
        createCircularSlashEffect(player.getLocation(), 4, 8); // 只显示4-8格范围的圆环特效

        // 获取玩家所在位置
        Location center = player.getLocation();

        // 获取世界中的所有活着的实体
        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            // 排除玩家自己和已死亡的实体和盔甲架
            if (entity.equals(player) || entity.isDead() || entity instanceof ArmorStand) {
                continue;
            }

            // 计算实体与玩家的水平距离
            double distance = Math.sqrt(Math.pow(entity.getLocation().getX() - center.getX(), 2) +
                    Math.pow(entity.getLocation().getZ() - center.getZ(), 2));

            // 检查是否在攻击范围内
            if (distance <= 8) {
                if (distance <= 4) {
                    // 内圈伤害
                    entity.damage(8, player);
                } else {
                    // 外圈伤害和减速效果
                    entity.damage(12, player);
                    entity.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            20, // 1秒(20 ticks)
                            1 // 缓慢II
                    ));
                }
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isExecutingCombo1 = false;
                attackCombo = 2;
            }
        }.runTaskLater(HolyGrailWar.inst(), 1);
    }

    // 创建圆形斩击特效（只在外圈） - 修改后的版本
    private void createCircularSlashEffect(Location center, double innerRadius, double outerRadius) {
        new BukkitRunnable() {
            int progress = 0;
            final int totalSteps = 12; // 分段数量
            final double angleStep = 2 * Math.PI / totalSteps;
            final double ringWidth = outerRadius - innerRadius; // 圆环宽度
            final double angleOffset = Math.toRadians(-15); // 新增的-15度偏移量

            @Override
            public void run() {
                if (progress >= totalSteps) {
                    this.cancel();
                    return;
                }

                // 每次处理3个角度，加快动画速度
                for (int i = 0; i < 3; i++) {
                    int currentSegment = progress + i;
                    if (currentSegment >= totalSteps) continue;

                    // 主特效角度
                    double mainAngle = angleStep * currentSegment;
                    // 复制体特效角度（主角度+15度偏移）
                    double offsetAngle = mainAngle + angleOffset;

                    // 处理两个角度（0度和15度）
                    for (double angle : new double[]{mainAngle, offsetAngle}) {
                        // 计算外圈粒子位置
                        double outerX = outerRadius * Math.cos(angle);
                        double outerZ = outerRadius * Math.sin(angle);
                        Location outerParticleLoc = center.clone().add(outerX, 0.1, outerZ);

                        // 计算内圈粒子位置
                        double innerX = innerRadius * Math.cos(angle);
                        double innerZ = innerRadius * Math.sin(angle);

                        // 生成外圈粒子（亮红色） - 增加粒子数量
                        outerParticleLoc.getWorld().spawnParticle(
                                Particle.DUST,
                                outerParticleLoc,
                                5,
                                new Particle.DustOptions(Color.fromRGB(255, 80, 80), 1.5f)
                        );

                        // 在圆环范围内生成更密集的连接线特效
                        for (double r = 0; r <= ringWidth; r += 0.5) {
                            double currentRadius = innerRadius + r;
                            double x = currentRadius * Math.cos(angle);
                            double z = currentRadius * Math.sin(angle);
                            Location lineLoc = center.clone().add(x, 0.1, z);

                            // 根据半径比例计算颜色（从内到外渐变）
                            float ratio = (float) (r / ringWidth);
                            int red = 200 + (int)(55 * ratio);
                            Color color = Color.fromRGB(red, 30, 30);

                            // 增加每个位置的粒子数量
                            lineLoc.getWorld().spawnParticle(
                                    Particle.DUST,
                                    lineLoc,
                                    2,
                                    new Particle.DustOptions(color, 1.0f - 0.5f * ratio)
                            );
                        }
                    }
                }

                progress += 3; // 每次前进3步
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    // 剑形态第二段攻击
    private void executeSwordCombo2Attack(Player player) {
        player.sendMessage("§a执行剑形态第二段攻击：链枪缠绕");

        // 获取玩家前方直线上的第一个实体
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection();
        double maxDistance = 5.0; // 最大检测距离

        // 射线检测寻找第一个命中的实体
        LivingEntity target = null;
        for(double d = 0; d <= maxDistance; d += 0.5) {
            Location checkLoc = startLoc.clone().add(direction.clone().multiply(d));
            for(Entity entity : player.getWorld().getNearbyEntities(checkLoc, 0.5, 0.5, 0.5)) {
                if(entity instanceof LivingEntity && !(entity instanceof ArmorStand) && !entity.equals(player)) {
                    target = (LivingEntity) entity;
                    break;
                }
            }
            if(target != null) break;
        }

        if(target != null) {
            // 造成10点伤害
            target.damage(10, player);

            // 施加缓慢5效果，持续1.5秒(30 ticks)
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    30, // 1.5秒(30 ticks)
                    4   // 缓慢V
            ));

            // 创建链枪缠绕特效
            createChainWrapEffect(player, target);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                isExecutingCombo2 = false;
                attackCombo = 3;
            }
        }.runTaskLater(HolyGrailWar.inst(), 1);
    }

    // 创建链枪缠绕特效
    private void createChainWrapEffect(Player player, LivingEntity target) {
        // 初始连接特效 - 红色链条
        createChainLink(player.getEyeLocation(), target.getLocation().add(0, 1, 0), 10, true);

        // 持续缠绕特效
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 30; // 1.5秒(30 ticks)

            @Override
            public void run() {
                if(ticks >= duration || target.isDead()) {
                    this.cancel();
                    return;
                }

                // 在目标周围创建旋转链条特效
                Location center = target.getLocation().add(0, 1, 0);
                double radius = 0.8;
                double angle = Math.toRadians(ticks * 12); // 每tick旋转12度

                // 水平旋转链条 - 红/深红交替
                for(int i = 0; i < 3; i++) {
                    double currentAngle = angle + (i * Math.PI * 2 / 3);
                    double x = radius * Math.cos(currentAngle);
                    double z = radius * Math.sin(currentAngle);
                    Location particleLoc = center.clone().add(x, 0, z);

                    // 交替使用红色和深红色
                    boolean isRed = (ticks + i) % 2 == 0;
                    Color color = isRed ? Color.fromRGB(255, 50, 50) : Color.fromRGB(150, 0, 0);

                    particleLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            particleLoc,
                            1,
                            new Particle.DustOptions(color, 0.8f)
                    );
                }

                // 每隔5ticks创建连接玩家的链条
                if(ticks % 5 == 0) {
                    createChainLink(player.getEyeLocation(), center, 5, false);
                }

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    // 创建链条连接特效
    private void createChainLink(Location from, Location to, int particles, boolean alternateColors) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        for(int i = 0; i < particles; i++) {
            double ratio = (double)i / (particles - 1);
            Location particleLoc = from.clone().add(direction.clone().multiply(distance * ratio));

            // 添加一些随机偏移模拟链条自然摆动
            double offsetX = (Math.random() - 0.5) * 0.2;
            double offsetY = (Math.random() - 0.5) * 0.2;
            double offsetZ = (Math.random() - 0.5) * 0.2;
            particleLoc.add(offsetX, offsetY, offsetZ);

            // 颜色选择
            Color color;
            if(alternateColors) {
                color = i % 2 == 0 ? Color.fromRGB(255, 50, 50) : Color.fromRGB(150, 0, 0);
            } else {
                color = Color.fromRGB(200, 0, 0); // 固定深红色
            }

            particleLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    new Particle.DustOptions(color, 0.7f)
            );
        }
    }



    // 修改后的剑形态第三段攻击
    private void executeSwordCombo3Attack(Player player) {
        player.sendMessage("§a执行剑形态第三段攻击：烈焰扇形斩");

        // 创建扇形斩击特效（120度范围）
        createFanShapedEffect(player.getLocation(), player.getLocation().getYaw(), 6, 120);

        // 使用BukkitRunnable延迟执行伤害，避免事件递归
        new BukkitRunnable() {
            @Override
            public void run() {
                // 对扇形区域内实体造成伤害
                damageFanShapedArea(player, 6, 120, 17);
            }
        }.runTaskLater(HolyGrailWar.inst(), 1);

        attackCombo = 1; // 重置为第一段
    }

    // 修改后的扇形特效创建方法
    private void createFanShapedEffect(Location center, float yaw, double radius, double angleRange) {
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 10; // 特效持续时间
            final double startAngle = -angleRange/2; // 从左侧开始
            final double endAngle = angleRange/2;    // 到右侧结束

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    return;
                }

                // 计算当前进度(0-1)
                double progress = ticks / (double)duration;

                // 当前扫过的角度范围
                double currentAngle = startAngle + (endAngle - startAngle) * progress;

                // 生成扇形边缘粒子（从左向右扫过）
                for (double r = 0; r <= radius; r += 0.5) {
                    // 计算粒子位置（相对玩家）
                    double angleRad = Math.toRadians(currentAngle);
                    double x = r * Math.sin(angleRad);
                    double z = r * Math.cos(angleRad);

                    // 旋转到玩家面向的方向
                    Vector vec = new Vector(x, 0, z);
                    vec = rotateAroundY(vec, yaw);

                    Location particleLoc = center.clone().add(vec);
                    particleLoc.add(0, 0.1, 0); // 稍微抬高一点

                    // 计算颜色渐变（从中心到边缘）
                    float ratio = (float)(r / radius);
                    int red = 255;
                    int green = (int)(100 * (1 - ratio));
                    Color color = Color.fromRGB(red, green, 30);

                    // 生成粒子
                    particleLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            particleLoc,
                            1,
                            new Particle.DustOptions(color, 1.2f - 0.7f * ratio)
                    );

                    // 在扫过的边缘创建更亮的特效
                    if (Math.abs(currentAngle - startAngle) < 5 || Math.abs(currentAngle - endAngle) < 5) {
                        particleLoc.getWorld().spawnParticle(
                                Particle.DUST,
                                particleLoc,
                                2,
                                new Particle.DustOptions(Color.fromRGB(255, 80, 30), 1.5f)
                        );
                    }
                }

                // 生成中心冲击波特效（跟随扫过的方向）
                if (ticks % 2 == 0) {
                    Vector direction = new Vector(
                            Math.sin(Math.toRadians(currentAngle)),
                            0,
                            Math.cos(Math.toRadians(currentAngle))
                    );
                    direction = rotateAroundY(direction, yaw);

                    for (int i = 0; i < 3; i++) {
                        Location waveLoc = center.clone().add(direction.clone().multiply(i * 0.5));
                        waveLoc.add(0, 0.1, 0);

                        // 冲击波粒子（亮红色）
                        waveLoc.getWorld().spawnParticle(
                                Particle.DUST,
                                waveLoc,
                                3,
                                0.2, 0, 0.2,
                                new Particle.DustOptions(Color.fromRGB(255, 50, 50), 1.5f)
                        );
                    }
                }

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    // 修改后的扇形区域伤害方法（120度范围）
    private void damageFanShapedArea(Player player, double radius, double angleRange, double damage) {
        Location playerLoc = player.getLocation();
        float yaw = playerLoc.getYaw();

        // 临时禁用事件监听
        HandlerList.unregisterAll(KyokoLancer.this);

        try {
            // 获取世界中的所有活着的实体
            for (LivingEntity entity : player.getWorld().getLivingEntities()) {
                // 排除玩家自己、盔甲架和已死亡的实体
                if (entity.equals(player) || entity instanceof ArmorStand || entity.isDead()) {
                    continue;
                }

                // 计算实体与玩家的距离
                double distance = entity.getLocation().distance(playerLoc);
                if (distance > radius) {
                    continue;
                }

                // 计算实体相对于玩家的角度
                Vector direction = entity.getLocation().toVector()
                        .subtract(playerLoc.toVector())
                        .setY(0)
                        .normalize();
                Vector playerDirection = playerLoc.getDirection().setY(0).normalize();

                double angle = Math.toDegrees(Math.atan2(
                        direction.getX() * playerDirection.getZ() - direction.getZ() * playerDirection.getX(),
                        direction.getX() * playerDirection.getX() + direction.getZ() * playerDirection.getZ()
                ));

                // 检查是否在扇形范围内（120度）
                if (Math.abs(angle) <= angleRange/2) {
                    entity.damage(damage, player);

                    // 添加击退效果（远离玩家）
                    Vector knockback = entity.getLocation().toVector()
                            .subtract(playerLoc.toVector())
                            .setY(0.3)
                            .normalize()
                            .multiply(0.5);
                    entity.setVelocity(knockback);

                    // 添加命中特效
                    Location hitLoc = entity.getLocation().add(0, 1, 0);
                    hitLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            hitLoc,
                            10,
                            0.5, 0.5, 0.5,
                            new Particle.DustOptions(Color.RED, 1.0f)
                    );
                }
            }
        } finally {
            // 重新注册事件监听
            player.getServer().getPluginManager().registerEvents(KyokoLancer.this, HolyGrailWar.inst());
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
                        direction.getX() * 0.3,  // 略微向前
                        launchPower,              // 主要向上的力
                        direction.getZ() * 0.3    // 略微向前
                );
                entity.setVelocity(launchVector);

                // 添加击飞拖尾特效
                createLaunchTrailEffect(entity);
            }
        }
    }

    // 创建击飞拖尾特效
    private void createLaunchTrailEffect(org.bukkit.entity.LivingEntity entity) {
        new BukkitRunnable() {
            int ticks = 0;
            final int durationTicks = 10; // 0.5秒(10 ticks)
            final java.util.List<org.bukkit.Location> previousLocations = new java.util.ArrayList<>();

            @Override
            public void run() {
                if (ticks >= durationTicks || entity.isDead()) {
                    this.cancel();
                    return;
                }

                // 获取实体当前位置
                org.bukkit.Location currentLoc = entity.getLocation().clone().add(0, -0.5, 0); // 略微下方

                // 记录位置用于创建流畅的拖尾
                previousLocations.add(currentLoc);
                if (previousLocations.size() > 5) {
                    previousLocations.remove(0);
                }

                // 创建拖尾粒子
                if (previousLocations.size() >= 2) {
                    for (int i = 0; i < previousLocations.size() - 1; i++) {
                        org.bukkit.Location start = previousLocations.get(i);
                        org.bukkit.Location end = previousLocations.get(i + 1);

                        // 计算两点之间的粒子
                        double distance = start.distance(end);
                        int particles = (int) (distance * 5);
                        if (particles < 1) particles = 1;

                        org.bukkit.util.Vector direction = end.toVector().subtract(start.toVector()).normalize();
                        for (int p = 0; p < particles; p++) {
                            double ratio = (double) p / particles;
                            org.bukkit.Location particleLoc = start.clone().add(direction.clone().multiply(ratio * distance));

                            // 粒子颜色从亮红到暗红渐变
                            int red = 255 - (int) (100 * (double) i / previousLocations.size());
                            Color color = Color.fromRGB(red, 30, 30);

                            // 粒子大小逐渐减小
                            float size = 0.8f - (0.3f * (float) i / previousLocations.size());

                            particleLoc.getWorld().spawnParticle(
                                    Particle.DUST,
                                    particleLoc,
                                    1,
                                    new Particle.DustOptions(color, size)
                            );
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onPillarSkillUse(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT")) return;
        if (!event.getPlayer().equals(this.player)) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != PILLAR_MATERIAL) return;

        if (!containsTrigger(item, PILLAR_ITEM_NAME)) return;

        // 检查冷却和魔力
        CharacterBase character = HolyGrailWar.inst().getPlayerCharacter(player);
        if (character == null || !character.hasEnoughMana(500)) {
            return;
        }

        // 执行技能
        executePillarSkill(player);
        character.consumeMana(500);
        player.setCooldown(PILLAR_MATERIAL, 20 * 120); // 120秒冷却
        event.setCancelled(true);
    }

    private void executePillarSkill(Player player) {
        // 锁定玩家移动和跳跃
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 255, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20, 128, false, false));

        // 创建准备区域特效
        createPillarPreparationEffect(player.getLocation());

        // 1秒后执行审判效果
        new BukkitRunnable() {
            @Override
            public void run() {
                executePillarJudgmentEffect(player.getLocation());
            }
        }.runTaskLater(HolyGrailWar.inst(), 20);
    }
    private void createPillarPreparationEffect(Location center) {
        Location playerLoc = player.getLocation();
        Vector direction = playerLoc.getDirection().setY(0).normalize();
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

        // 矩形区域参数
        double length = 7.0;
        double width = 5.0;
        double halfWidth = width / 2;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 20) { // 1秒持续时间
                    this.cancel();
                    return;
                }

                // 在矩形区域内生成红色粒子
                for (double l = 0; l <= length; l += 0.5) {
                    for (double w = -halfWidth; w <= halfWidth; w += 0.5) {
                        // 计算粒子位置
                        Vector offset = direction.clone().multiply(l)
                                .add(perpendicular.clone().multiply(w));
                        Location particleLoc = center.clone().add(offset).add(0, 0.1, 0);

                        // 生成红色粒子
                        particleLoc.getWorld().spawnParticle(
                                Particle.DUST,
                                particleLoc,
                                1,
                                new Particle.DustOptions(Color.RED, 1.0f)
                        );

                        // 每隔0.5格生成一个更亮的中心粒子
                        if (l % 1 == 0 && w % 1 == 0) {
                            particleLoc.getWorld().spawnParticle(
                                    Particle.DUST,
                                    particleLoc,
                                    1,
                                    new Particle.DustOptions(Color.fromRGB(255, 100, 100), 1.5f)
                            );
                        }
                    }
                }

                // 在边缘生成更亮的粒子
                if (ticks % 5 == 0) {
                    for (double l = 0; l <= length; l += 0.2) {
                        // 两侧边缘
                        for (double w : new double[]{-halfWidth, halfWidth}) {
                            Vector offset = direction.clone().multiply(l)
                                    .add(perpendicular.clone().multiply(w));
                            Location particleLoc = center.clone().add(offset).add(0, 0.1, 0);

                            particleLoc.getWorld().spawnParticle(
                                    Particle.DUST,
                                    particleLoc,
                                    1,
                                    new Particle.DustOptions(Color.fromRGB(255, 50, 50), 1.2f)
                            );
                        }
                    }

                    // 前端边缘
                    for (double w = -halfWidth; w <= halfWidth; w += 0.2) {
                        Vector offset = direction.clone().multiply(length)
                                .add(perpendicular.clone().multiply(w));
                        Location particleLoc = center.clone().add(offset).add(0, 0.1, 0);

                        particleLoc.getWorld().spawnParticle(
                                Particle.DUST,
                                particleLoc,
                                1,
                                new Particle.DustOptions(Color.fromRGB(255, 80, 80), 1.5f)
                        );
                    }
                }

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    private void executePillarJudgmentEffect(Location center) {
        Location playerLoc = player.getLocation();
        Vector direction = playerLoc.getDirection().setY(0).normalize();
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

        // 矩形区域参数
        double length = 7.0;
        double width = 5.0;
        double halfWidth = width / 2;

        // 收集区域内的实体
        Set<LivingEntity> targets = new HashSet<>();
        for (double l = 0; l <= length; l += 0.5) {
            for (double w = -halfWidth; w <= halfWidth; w += 0.5) {
                // 计算检测位置
                Vector offset = direction.clone().multiply(l)
                        .add(perpendicular.clone().multiply(w));
                Location checkLoc = center.clone().add(offset);

                // 检查这个位置是否有实体
                for (Entity entity : player.getWorld().getNearbyEntities(checkLoc, 0.5, 1.0, 0.5)) {
                    if (entity instanceof LivingEntity &&
                            !(entity instanceof ArmorStand) &&
                            !entity.equals(player)) {
                        targets.add((LivingEntity) entity);
                    }
                }
            }
        }

        // 对每个目标执行审判效果
        for (LivingEntity target : targets) {
            // 造成伤害
            target.damage(25, player);

            // 着火效果
            target.setFireTicks(20 * 8); // 8秒着火

            // 禁止移动和跳跃
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 2, 255, false, false));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 2, 128, false, false));

            // 创建十字架效果
            createPillarCrossEffect(target.getLocation());
        }
    }
    private void createPillarCrossEffect(Location baseLoc) {
        // 找到地面位置
        Block block = baseLoc.getBlock();
        while (block.getY() > 0 && !block.getType().isSolid()) {
            block = block.getRelative(BlockFace.DOWN);
        }
        Location groundLoc = block.getLocation().add(0.5, 0, 0.5);

        World world = groundLoc.getWorld();
        int centerX = groundLoc.getBlockX();
        int centerY = groundLoc.getBlockY();
        int centerZ = groundLoc.getBlockZ();

        // 保存原始方块
        List<Block> originalBlocks = new ArrayList<>();

        // 创建垂直部分 (3格高)
        for (int y = 1; y <= 4; y++) {
            Block verticalBlock = world.getBlockAt(centerX, centerY + y, centerZ);
            originalBlocks.add(verticalBlock);
            verticalBlock.setType(Material.NETHER_BRICK_WALL);
        }

        // 创建水平部分 (左右各1格)
        Block leftBlock = world.getBlockAt(centerX - 1, centerY + 2, centerZ);
        Block rightBlock = world.getBlockAt(centerX + 1, centerY + 2, centerZ);
        originalBlocks.add(leftBlock);
        originalBlocks.add(rightBlock);
        leftBlock.setType(Material.NETHER_BRICK_WALL);
        rightBlock.setType(Material.NETHER_BRICK_WALL);

        // 创建粒子效果
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) { // 2秒持续时间
                    // 恢复方块
                    for (Block block : originalBlocks) {
                        block.setType(Material.AIR);
                    }
                    this.cancel();
                    return;
                }

                // 在十字架位置生成火焰粒子
                for (double y = 0; y <= 3; y += 0.5) {
                    Location particleLoc = groundLoc.clone().add(0, y, 0);
                    particleLoc.getWorld().spawnParticle(
                            Particle.FLAME,
                            particleLoc,
                            2,
                            0.1, 0.1, 0.1,
                            0.02
                    );
                }

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    private void startFlameSkillCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                // 检查玩家是否持有净罪之大炎物品
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && containsTrigger(item, FLAME_ITEM_NAME)) {
                        player.setCooldown(FLAME_MATERIAL, 0); // 确保可以立即使用
                        break;
                    }
                }
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 20L);
    }

    // 添加火焰技能事件处理器
    @EventHandler(priority = EventPriority.HIGH)
    public void onFlameSkillUse(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT")) return;
        if (!event.getPlayer().equals(this.player)) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != FLAME_MATERIAL) return;

        if (!containsTrigger(item, FLAME_ITEM_NAME)) return;

        // 检查冷却和魔力
        CharacterBase character = HolyGrailWar.inst().getPlayerCharacter(player);
        if (character == null || !character.hasEnoughMana(FLAME_SKILL_MANA_COST)) {
            player.sendMessage("§c魔力不足，无法发动净罪之大炎！");
            return;
        }

        // 执行技能
        executeFlameSkill(player);
        character.consumeMana(FLAME_SKILL_MANA_COST);
        player.setCooldown(FLAME_MATERIAL, FLAME_SKILL_COOLDOWN);
        event.setCancelled(true);
    }

    // 实现净罪之大炎技能
    private void executeFlameSkill(Player player) {
        // Lock player's movement and view
        isLockingView = true;
        lockedYaw = player.getLocation().getYaw();
        lockedPitch = player.getLocation().getPitch();

        // Prevent jumping
        player.setAllowFlight(false);
        player.setFlying(false);

        // Add resistance effect to prevent knockback
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                100, // 5 seconds
                4    // Resistance V
        ));

        // Start the flame aura effect
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 100; // 5 seconds (100 ticks)
            final Location center = player.getLocation();
            final List<Location> lavaParticles = new ArrayList<>();

            @Override
            public void run() {
                if (ticks >= duration || player.isDead()) {
                    // Time's up, launch the red spear
                    launchRedSpear(player);
                    // Kill player after 1 second
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.setHealth(0);
                        }
                    }.runTaskLater(HolyGrailWar.inst(), 20); // 1 second delay
                    this.cancel();
                    return;
                }

                // Create burning boundary effect (red dust particles)
                for (int i = 0; i < 36; i++) {
                    double angle = Math.toRadians(i * 10);
                    double x = 10 * Math.cos(angle);
                    double z = 10 * Math.sin(angle);
                    Location boundaryLoc = center.clone().add(x, 0.1, z);

                    boundaryLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            boundaryLoc,
                            1,
                            new Particle.DustOptions(Color.RED, 1.5f)
                    );
                }

                // Every 2 ticks (0.1 seconds), damage and burn nearby entities
                if (ticks % 2 == 0) {
                    for (Entity entity : player.getWorld().getNearbyEntities(center, 10, 10, 10)) {
                        if (entity instanceof LivingEntity &&
                                !(entity instanceof ArmorStand) &&
                                !entity.equals(player)) {

                            LivingEntity target = (LivingEntity) entity;
                            // Apply damage
                            target.damage(0.5, player);
                            // Apply 3 seconds of burning (60 ticks)
                            target.setFireTicks(60);

                            // Create flame particle effect
                            Location particleLoc = target.getLocation().add(0, 1, 0);
                            particleLoc.getWorld().spawnParticle(
                                    Particle.FLAME,
                                    particleLoc,
                                    5,
                                    0.3, 0.3, 0.3,
                                    0.05
                            );
                        }
                    }
                }

                // Create floor effects - more lava and flame particles over time
                int lavaCount = 5 + (int)(ticks * 0.2); // Increase over time
                for (int i = 0; i < lavaCount; i++) {
                    // Random position within radius
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * 10;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);

                    Location lavaLoc = center.clone().add(x, 0.1, z);
                    lavaLoc.getWorld().spawnParticle(
                            Particle.DRIPPING_LAVA,
                            lavaLoc,
                            1
                    );

                    // Add flame particles near lava
                    if (Math.random() < 0.3) {
                        lavaLoc.getWorld().spawnParticle(
                                Particle.FLAME,
                                lavaLoc,
                                2,
                                0.2, 0, 0.2,
                                0
                        );
                    }

                    // Remember some lava locations for persistent effects
                    if (lavaParticles.size() < 20 && Math.random() < 0.1) {
                        lavaParticles.add(lavaLoc);
                    }
                }

                // Create persistent effects at remembered lava locations
                for (Location loc : lavaParticles) {
                    loc.getWorld().spawnParticle(
                            Particle.SMOKE,
                            loc,
                            1,
                            0.1, 0.1, 0.1,
                            0.02
                    );

                    if (Math.random() < 0.2) {
                        loc.getWorld().spawnParticle(
                                Particle.FLAME,
                                loc,
                                1,
                                0.1, 0, 0.1,
                                0
                        );
                    }
                }

                ticks++;
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
    }

    private void launchRedSpear(Player player) {
        // Unlock player's view (though they'll die soon anyway)
        isLockingView = false;

        // Create and launch the red spear (using a trident as projectile)
        Location spawnLoc = player.getEyeLocation();
        org.bukkit.entity.Trident spear = player.launchProjectile(org.bukkit.entity.Trident.class);

        // Make spear uncollectable and non-pickupable
        spear.setPickupStatus(org.bukkit.entity.Trident.PickupStatus.DISALLOWED);
        spear.setInvulnerable(true);

        // Set spear properties
        spear.setVelocity(spawnLoc.getDirection().multiply(2.5)); // Faster speed
        spear.setGravity(false); // No gravity
        spear.setCritical(false);
        spear.setDamage(0); // We'll handle damage manually

        // Add spear trail effect
        new BukkitRunnable() {
            @Override
            public void run() {
                if (spear.isDead() || !spear.isValid()) {
                    this.cancel();
                    return;
                }

                // Create red trail particles
                spear.getWorld().spawnParticle(
                        Particle.DUST,
                        spear.getLocation(),
                        3,
                        0.1, 0.1, 0.1,
                        new Particle.DustOptions(Color.RED, 1.5f)
                );

                // Add flame trail
                spear.getWorld().spawnParticle(
                        Particle.FLAME,
                        spear.getLocation(),
                        2,
                        0.1, 0.1, 0.1,
                        0.02
                );
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);

        // Handle spear hit
        new BukkitRunnable() {
            @Override
            public void run() {
                if (spear.isDead() || !spear.isValid()) {
                    // Spear hit something
                    Location hitLoc = spear.getLocation();


                    // Damage and burn entities at hit location
                    for (Entity entity : spear.getNearbyEntities(2, 2, 2)) {
                        if (entity instanceof LivingEntity &&
                                !(entity instanceof ArmorStand) &&
                                !entity.equals(player)) { // Don't damage self

                            LivingEntity target = (LivingEntity) entity;
                            // Apply 35 damage
                            target.damage(35, player);
                            // Apply 10 seconds of burning (200 ticks)
                            target.setFireTicks(200);

                            // Create hit effect
                            Location particleLoc = target.getLocation().add(0, 1, 0);
                            particleLoc.getWorld().spawnParticle(
                                    Particle.FLAME,
                                    particleLoc,
                                    20,
                                    0.5, 0.5, 0.5,
                                    0.1
                            );
                        }
                    }

                    this.cancel();
                }
            }
        }.runTaskTimer(HolyGrailWar.inst(), 0, 1);
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
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.getPlayer().equals(this.player)) return;

        if (isLockingView) {
            // 完全禁止移动和视角转动
            Location from = event.getFrom();
            Location to = event.getTo();

            // 如果位置变化，传送回原位置
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                Location newLoc = from.clone();
                newLoc.setYaw(lockedYaw);
                newLoc.setPitch(lockedPitch);
                event.setTo(newLoc);
            } else {
                // 锁定视角
                Location newLoc = to.clone();
                newLoc.setYaw(lockedYaw);
                newLoc.setPitch(lockedPitch);
                event.setTo(newLoc);
            }
        }
    }

    @EventHandler
    public void onPlayerJump(PlayerToggleFlightEvent event) {
        if (event.getPlayer().equals(this.player) ){
            if (isLockingView) {
                event.setCancelled(true);
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
        if (player != null) {
            player.setCooldown(Material.STRING, 0);
            isLockingView = false;
            player.setAllowFlight(false);
        }
    }
}