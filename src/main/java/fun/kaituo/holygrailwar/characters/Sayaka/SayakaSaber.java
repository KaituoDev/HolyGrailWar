package fun.kaituo.holygrailwar.characters.Sayaka;

import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.utils.AbstractSkill;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bukkit.Bukkit.getPlayer;

public class SayakaSaber extends CharacterBase {
    private int skillCount = 0;
    private final SkillCycle skillCycle;
    private final BlackTideSkill blackTideSkill;
    private static final String SKILL_NAME = "迷惘裹挟之斩";
    private static final String BLACK_TIDE_SKILL_NAME = "黑潮蚀岸之声";
    private org.bukkit.event.Listener listener;
    private boolean isUltimateActive = false;

    // 新增的回复系统字段
    private final Map<Integer, Double> activeHeals = new HashMap<>(); // 存储活跃的回复任务<任务ID, 总回复量>
    private double healingThisSecond = 0; // 当前秒内已回复的生命值
    private long lastHealingSecond = 0; // 上次回复的时间戳(秒)
    private static final double MAX_HEAL_PER_SECOND = 20.0; // 每秒最大回复量

    public SayakaSaber(Player player) {
        this(player, (JavaPlugin) Bukkit.getPluginManager().getPlugin("HolyGrailWar"));
    }

    public SayakaSaber(Player player, JavaPlugin plugin) {
        super(player, "美樹沙耶香", DrawCareerClass.ClassType.SABER, 2400, 1, 0);
        player.setExp(1.0f);
        player.setLevel(0);
        this.skillCycle = new SkillCycle(plugin, player, 200);
        this.blackTideSkill = new BlackTideSkill(plugin, player, 1000);
        addSkill(this.skillCycle);
        addSkill(this.blackTideSkill);

        // 注册伤害监听器
        plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onDamage(EntityDamageEvent event) {
                if (event.getEntity().equals(player)) {
                    handleDamage(event.getDamage());
                }
            }
        }, plugin);

        // 启动每秒回复检测
        new BukkitRunnable() {
            @Override
            public void run() {
                applyHealingOverTime();
            }
        }.runTaskTimer(plugin, 0, 1); // 每tick检测一次
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (listener != null) {
            PlayerInteractEvent.getHandlerList().unregister(listener);
        }
        player.setWalkSpeed(0.2f);
        isUltimateActive = false;
        activeHeals.clear();
        healingThisSecond = 0;
        player.setCooldown(Material.GLOW_INK_SAC, 0);
        player.setCooldown(Material.DIAMOND_SWORD, 0);
    }

    // 处理受到的伤害
    private void handleDamage(double damage) {
        if (isUltimateActive) return;

        double healAmount = damage * 0.5; // 计算回复总量
        final int taskId = (int) System.currentTimeMillis(); // 生成唯一任务ID

        // 将回复任务加入活跃列表
        activeHeals.put(taskId, healAmount);

        // 3秒后移除这个回复任务
        new BukkitRunnable() {
            @Override
            public void run() {
                activeHeals.remove(taskId);
            }
        }.runTaskLater(HolyGrailWar.inst(), 60); // 3秒 = 60ticks
    }

    // 应用随时间回复
    private void applyHealingOverTime() {
        if (activeHeals.isEmpty()) return;
        if (player.isDead()) return;

        long currentSecond = System.currentTimeMillis() / 1000;

        // 如果进入了新的一秒，重置计数器
        if (currentSecond != lastHealingSecond) {
            healingThisSecond = 0;
            lastHealingSecond = currentSecond;
        }

        // 计算所有活跃回复任务的总待回复量
        double totalPendingHeal = activeHeals.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalPendingHeal <= 0) return;

        // 计算本次可以回复的量(不超过每秒上限的1/20，因为这是每tick执行)
        double maxHealThisTick = Math.min(MAX_HEAL_PER_SECOND / 20.0, MAX_HEAL_PER_SECOND - healingThisSecond);
        double healThisTick = Math.min(totalPendingHeal * 0.05, maxHealThisTick); // 均匀分配回复量

        if (healThisTick <= 0) return;

        // 应用回复
        double newHealth = Math.min(player.getHealth() + healThisTick, player.getMaxHealth());
        player.setHealth(newHealth);

        // 更新计数器
        healingThisSecond += healThisTick;

        // 按比例减少每个活跃回复任务的待回复量
        double ratio = healThisTick / totalPendingHeal;
        activeHeals.replaceAll((id, amount) -> amount * (1 - ratio));


    }

    public class BlackTideSkill extends AbstractSkill {
        private org.bukkit.event.Listener blackTideListener;

        public BlackTideSkill(JavaPlugin plugin, Player player, int manaCost) {
            super(plugin, player, Material.DIAMOND_SWORD, BLACK_TIDE_SKILL_NAME, 3600, manaCost);

            blackTideListener = new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onPlayerInteract(PlayerInteractEvent event) {
                    if (event.getPlayer().equals(player)) {
                        checkAndTrigger(event);
                    }
                }
            };
            plugin.getServer().getPluginManager().registerEvents(blackTideListener, plugin);
        }

        @Override
        protected boolean onTrigger(PlayerInteractEvent event) {
            isUltimateActive = true;
            final Location initialLocation = player.getLocation().clone(); // 保存初始位置


            // 启动技能效果
            new BukkitRunnable() {
                int duration = 0;
                final Location center = initialLocation.clone(); // 使用初始位置作为中心点
                final double radius = 20;
                final double height = 5;
                int waterDropCount = 0;
                final List<Entity> affectedEntities = new ArrayList<>(); // 记录受影响的实体

                @Override
                public void run() {
                    // 每tick固定玩家位置，防止被移动
                    if (!player.getLocation().equals(initialLocation)) {
                        player.teleport(initialLocation);
                    }
                    if (duration == 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4));
                        player.setWalkSpeed(0);
                        player.setJumping(false);
                    }

                    if (duration < 50) {
                        // 治疗效果
                        if (player.getHealth() < player.getMaxHealth()) {
                            player.setHealth(Math.min(player.getHealth() + 1.5, player.getMaxHealth()));
                        }

                        // 绘制圆形边界
                        drawCircleBoundary(center, radius);

                        // 伤害效果和标记敌人
                        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, height, radius)) {
                            if (entity instanceof LivingEntity && !(entity instanceof ArmorStand) && !entity.equals(player)) {
                                LivingEntity livingEntity = (LivingEntity) entity;

                                // 伤害效果 - 修改为不会击退
                                livingEntity.damage(2, player);



                                // 标记敌人并创建箭头
                                if (!affectedEntities.contains(entity)) {
                                    affectedEntities.add(entity);
                                }
                                createDirectionArrow(livingEntity, center);

                                if (duration % 10 == 0) {
                                    PotionEffect currentSlowness = livingEntity.getPotionEffect(PotionEffectType.SLOWNESS);
                                    int newLevel = (currentSlowness != null) ? currentSlowness.getAmplifier() + 1 : 0;
                                    livingEntity.addPotionEffect(new PotionEffect(
                                            PotionEffectType.SLOWNESS,
                                            20,
                                            newLevel,
                                            false,
                                            true
                                    ));
                                }

                            }
                        }
                        // 清理已离开范围的实体
                        affectedEntities.removeIf(e ->
                                e.getLocation().distance(center) > radius ||
                                        e.isDead() ||
                                        !e.getWorld().equals(center.getWorld())
                        );

                        // 水滴粒子特效
                        if (duration % 5 == 0) {
                            waterDropCount = Math.min(waterDropCount + 10, 1000);
                            for (int i = 0; i < waterDropCount; i++) {
                                double theta = Math.random() * 2 * Math.PI;
                                double phi = Math.random() * Math.PI / 2;
                                double r = Math.random() * radius;

                                double x = r * Math.sin(phi) * Math.cos(theta);
                                double y = r * Math.cos(phi);
                                double z = r * Math.sin(phi) * Math.sin(theta);

                                Location particleLoc = center.clone().add(x, y, z);
                                player.getWorld().spawnParticle(
                                        Particle.DRIPPING_WATER,
                                        particleLoc,
                                        1,
                                        0, 0, 0, 0
                                );

                                if (Math.random() < 0.05) {
                                    player.getWorld().playSound(
                                            particleLoc,
                                            Sound.AMBIENT_UNDERWATER_EXIT,
                                            0.3f,
                                            0.8f + (float)Math.random() * 0.4f
                                    );
                                }
                            }
                        }

                        duration++;
                    } else {
                        // 技能结束
                        player.setWalkSpeed(0.2f);
                        this.cancel();
                        player.setCooldown(Material.DIAMOND_SWORD, 3600);
                        isUltimateActive = false;
                        affectedEntities.clear(); // 清空受影响的实体列表
                    }
                }

                // 绘制圆形边界
                private void drawCircleBoundary(Location center, double radius) {
                    World world = center.getWorld();
                    int points = 72; // 圆的精细度
                    double increment = (2 * Math.PI) / points;

                    for (int i = 0; i < points; i++) {
                        double angle = i * increment;
                        double x = center.getX() + (radius * Math.cos(angle));
                        double z = center.getZ() + (radius * Math.sin(angle));

                        // 在边界位置生成蓝色粒子
                        world.spawnParticle(
                                Particle.DUST,
                                new Location(world, x, center.getY(), z),
                                1,
                                0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(0, 100, 255), 1.2f)
                        );

                        // 在边界上方生成半透明粒子
                        world.spawnParticle(
                                Particle.DUST,
                                new Location(world, x, center.getY() + 1.5, z),
                                1,
                                0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(0, 150, 255), 0.8f)
                        );
                    }
                }

                // 创建水滴形指向箭头
                private void createDirectionArrow(LivingEntity target, Location center) {
                    Location targetLoc = target.getLocation();
                    // 修正方向：从中心（SayakaSaber位置）指向目标
                    Vector direction = targetLoc.toVector().subtract(center.toVector()).normalize();

                    // 箭头基座位置（目标脚下）
                    Location arrowBase = targetLoc.clone().add(0, 0.1, 0);

                    // 箭头长度
                    double arrowLength = 1.5;

                    // 深蓝色颜色定义
                    Color darkBlue = Color.fromRGB(0, 0, 150);

                    // 水滴形箭头生成
                    for (double d = 0; d <= arrowLength; d += 0.2) {
                        // 计算当前位置的比例（0到1）
                        double progress = d / arrowLength;

                        // 根据位置比例计算粒子大小（尾部大，头部小）
                        float size = (float)(0.8f * (1.0 - progress * 0.7));

                        // 计算当前位置（从基座向方向延伸）
                        Location particleLoc = arrowBase.clone().add(direction.clone().multiply(-d));

                        // 生成粒子
                        target.getWorld().spawnParticle(
                                Particle.DUST,
                                particleLoc,
                                1,
                                0, 0, 0, 0,
                                new Particle.DustOptions(darkBlue, size)
                        );

                        // 在尾部添加更多粒子形成粗尾效果
                        if (progress < 0.3) {
                            // 在尾部周围生成环形粒子
                            for (int i = 0; i < 3; i++) {
                                double angle = i * 120;
                                Vector offset = direction.clone()
                                        .rotateAroundY(Math.toRadians(90))
                                        .multiply(0.15 * (1.0 - progress))
                                        .rotateAroundY(Math.toRadians(angle));

                                target.getWorld().spawnParticle(
                                        Particle.DUST,
                                        particleLoc.clone().add(offset),
                                        1,
                                        0, 0, 0, 0,
                                        new Particle.DustOptions(darkBlue, size * 0.8f)
                                );
                            }
                        }
                    }

                    // 在目标脚下生成圆形标记（深蓝色）
                    for (int i = 0; i < 360; i += 45) {
                        double rad = Math.toRadians(i);
                        double x = Math.cos(rad) * 0.5;
                        double z = Math.sin(rad) * 0.5;

                        target.getWorld().spawnParticle(
                                Particle.DUST,
                                arrowBase.clone().add(x, 0, z),
                                1,
                                0, 0, 0, 0,
                                new Particle.DustOptions(darkBlue, 0.6f)
                        );
                    }
                }

            }.runTaskTimer(plugin, 0, 2);

            return true;
        }
    }

    public class SkillCycle extends AbstractSkill {
        public SkillCycle(JavaPlugin plugin, Player player, int manaCost) {
            super(plugin, player, Material.GLOW_INK_SAC, SKILL_NAME, 60, manaCost);
            // 保存监听器引用
            listener = new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onPlayerInteract(PlayerInteractEvent event) {
                    if (event.getPlayer().equals(player)) {
                        checkAndTrigger(event);
                    }
                }
            };
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }

        @Override
        protected boolean onTrigger(PlayerInteractEvent event) {
            if (isUltimateActive) {
                return false;
            }
            switch (skillCount % 4) {
                case 0:
                    executeSkill1();
                    break;
                case 1:
                    executeSkill2();
                    break;
                case 2:
                    executeSkill3();
                    break;
                case 3:
                    executeUltimateSkill();
                    break;
            }
            skillCount++;
            return true;
        }

        private void executeSkill1() {
            Player player = this.player;
            Location eyeLoc = player.getEyeLocation().add(0,-0.2,0);
            Vector direction = eyeLoc.getDirection().setY(0).normalize();

            // 扇形参数
            double radius = 8;
            double angle = 120;
            double halfAngleRad = Math.toRadians(angle / 2);

            // 基础音效与粒子
            player.getWorld().playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
            player.spawnParticle(Particle.SWEEP_ATTACK, eyeLoc, 1);

            // 蓝色剑光特效（动态扫描扇形区域）
            new BukkitRunnable() {
                double currentAngle = -halfAngleRad; // 从扇形左侧开始
                final int steps = 16; // 扫描步数
                final double angleStep = (halfAngleRad * 2) / steps;
                final Color BLUE = Color.fromRGB(0, 100, 255); // 剑光颜色

                @Override
                public void run() {
                    // 三倍速推进（每tick完成3次角度更新）
                    for (int i = 0; i < 3; i++) {
                        Vector scanDir = direction.clone()
                                .rotateAroundY(currentAngle)
                                .multiply(radius);

                        // 使用更粗的粒子线补偿速度
                        drawParticleLine(
                                eyeLoc.clone().add(0, -0.2, 0),
                                eyeLoc.clone().add(scanDir),
                                BLUE,
                                10,           // 粒子数
                                2.0f        // 粒子大小
                        );

                        currentAngle += angleStep / 3; // 分步推进
                        if (currentAngle > halfAngleRad) break;
                    }

                    // 提前触发伤害（扫描到60%时）
                    if (currentAngle >= halfAngleRad * 0.6) {
                        applyDamageInSector(player, eyeLoc, direction, radius, halfAngleRad);
                        this.cancel();
                    }

                    currentAngle += angleStep;
                }
            }.runTaskTimer((JavaPlugin) Bukkit.getPluginManager().getPlugin("HolyGrailWar"), 0, 1);
        }

        // 绘制彩色粒子线
        private void drawParticleLine(Location start, Location end, Color color, int density, float v) {
            World world = start.getWorld();
            Vector path = end.toVector().subtract(start.toVector());
            double length = path.length();
            Vector step = path.normalize().multiply(length / density);

            for (int i = 0; i <= density; i++) {
                Location point = start.clone().add(step.clone().multiply(i));
                world.spawnParticle(Particle.DUST, point, 1,
                        new Particle.DustOptions(color, 1.2f));
            }
        }

        // 扇形区域伤害应用
        private void applyDamageInSector(Player player, Location origin, Vector direction,
                                         double radius, double halfAngleRad) {
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof LivingEntity) ||
                        entity instanceof ArmorStand ||
                        entity.equals(player)) continue;

                Location entityLoc = entity.getLocation();
                Vector toEntity = entityLoc.toVector()
                        .subtract(origin.toVector())
                        .setY(0)
                        .normalize();

                if (direction.dot(toEntity) > Math.cos(halfAngleRad) &&
                        origin.distance(entityLoc) <= radius) {

                    ((LivingEntity) entity).damage(15, player);
                    ((LivingEntity) entity).addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));

                    // 命中特效
                    entity.getWorld().spawnParticle(Particle.CRIT, entityLoc, 15);
                    entity.getWorld().playSound(entityLoc, Sound.ENTITY_PLAYER_HURT, 0.8f, 1.2f);
                }
            }
        }


        private void executeSkill2() {
            Player player = this.player;
            Location startLoc = player.getEyeLocation().add(0,-0.3,0);
            Vector direction = startLoc.getDirection().normalize();

            // 音效与初始特效
            player.getWorld().playSound(startLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 1.5f);
            player.spawnParticle(Particle.SWEEP_ATTACK, startLoc, 3);

            // 月牙刀光运动轨迹
            new BukkitRunnable() {
                double distance = 0;
                final double maxDistance = 12; // 最大飞行距离
                final double speed = 1.2;     // 每tick前进距离
                final List<Entity> hitEntities = new ArrayList<>(); // 已命中实体

                @Override
                public void run() {
                    // 计算当前位置
                    Location currentLoc = startLoc.clone().add(direction.clone().multiply(distance));

                    // 碰撞检测
                    if (currentLoc.getBlock().getType().isSolid()) {
                        spawnImpactEffect(currentLoc);
                        this.cancel();
                        return;
                    }

                    // 生成月牙形粒子
                    spawnCrescentParticles(currentLoc, direction);

                    // 伤害检测
                    for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5)) {
                        if (entity instanceof LivingEntity &&
                                !(entity instanceof ArmorStand) &&
                                !entity.equals(player) &&
                                !hitEntities.contains(entity)) {

                            // 造成伤害
                            ((LivingEntity) entity).damage(25, player);
                            hitEntities.add(entity);

                            // 自身加速效果
                            player.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SPEED,
                                    20, // 1秒 = 20ticks
                                    1    // 速度II
                            ));

                            // 命中特效
                            spawnImpactEffect(entity.getLocation());
                            player.getWorld().playSound(
                                    entity.getLocation(),
                                    Sound.ENTITY_PLAYER_ATTACK_CRIT,
                                    1.0f,
                                    1.8f
                            );
                        }
                    }

                    // 距离控制
                    distance += speed;
                    if (distance >= maxDistance) {
                        this.cancel();
                    }
                }
            }.runTaskTimer((JavaPlugin) Bukkit.getPluginManager().getPlugin("HolyGrailWar"), 0, 1);
        }

        // 生成月牙形粒子
        private void spawnCrescentParticles(Location center, Vector direction) {
            World world = center.getWorld();
            Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

            // 月牙弧线生成
            for (double angle = -30; angle <= 30; angle += 5) {
                double rad = Math.toRadians(angle);
                Vector offset = perpendicular.clone()
                        .multiply(Math.sin(rad) * 1.2)
                        .add(direction.clone().multiply(Math.cos(rad) * 0.8));

                // 蓝色主粒子
                world.spawnParticle(
                        Particle.DUST,
                        center.clone().add(offset),
                        1,
                        new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.5f)
                );

                // 边缘白光
                if (Math.abs(angle) > 20) {
                    world.spawnParticle(
                            Particle.DUST,
                            center.clone().add(offset.multiply(1.1)),
                            1,
                            new Particle.DustOptions(Color.WHITE, 0.8f)
                    );
                }
            }
        }

        // 碰撞/消失特效
        private void spawnImpactEffect(Location loc) {
            World world = loc.getWorld();
            // 蓝色能量爆发
            for (int i = 0; i < 360; i += 36) {
                Vector dir = new Vector(
                        Math.cos(Math.toRadians(i)),
                        0.5,
                        Math.sin(Math.toRadians(i))
                ).normalize();
                world.spawnParticle(
                        Particle.DUST,
                        loc,
                        1,
                        dir.getX(), dir.getY(), dir.getZ(), 0.5,
                        new Particle.DustOptions(Color.fromRGB(0, 100, 255), 2.0f)
                );
            }
        }

        private void executeSkill3() {
            Player player = this.player;
            Location startLoc = player.getLocation();
            Vector direction = player.getEyeLocation().getDirection().setY(0).normalize();

            // 冲刺音效与特效
            player.getWorld().playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.5f);
            player.spawnParticle(Particle.CLOUD, startLoc, 20);

            // 冲刺阶段
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 8; // 增加冲刺持续时间(8ticks=0.4秒)
                final double speed = 0.6; // 提高移动速度
                LivingEntity dashTarget = null;
                Location lastLocation = player.getLocation();

                @Override
                public void run() {
                    // 保存当前位置用于位移计算
                    lastLocation = player.getLocation();

                    // 检查前方碰撞
                    Location nextLoc = lastLocation.clone().add(direction.clone().multiply(speed));

                    // 方块碰撞检测
                    if (nextLoc.getBlock().getType().isSolid()) {
                        scheduleSlashAttack(player, dashTarget, lastLocation);
                        this.cancel();
                        return;
                    }

                    // 实体碰撞检测
                    for (Entity entity : player.getNearbyEntities(1.2, 1.2, 1.2)) { // 扩大检测范围
                        if (entity instanceof LivingEntity &&
                                !(entity instanceof ArmorStand) &&
                                !entity.equals(player)) {

                            if (dashTarget == null) {
                                dashTarget = (LivingEntity) entity;
                                ((LivingEntity) entity).damage(20, player);
                                spawnDashHitEffect(entity.getLocation());
                            }
                            scheduleSlashAttack(player, dashTarget, lastLocation);
                            this.cancel();
                            return;
                        }
                    }

                    // 执行冲刺位移（使用teleport保证流畅性）
                    player.teleport(nextLoc);
                    // 冲刺轨迹粒子（蓝+淡蓝交替）
                    Color[] trailColors = {
                            Color.fromRGB(0, 100, 255), // 深蓝
                            Color.fromRGB(100, 180, 255) // 淡蓝
                    };

                    // 在移动路径上生成粒子
                    for (int i = 0; i <= 3; i++) {
                        Location trailLoc = lastLocation.clone()
                                .add(direction.clone().multiply(i * 0.33))
                                .add(0, 0.5, 0); // 抬高到腰部位置

                        // 交替使用两种颜色
                        Color color = trailColors[i % 2];

                        // 随机添加闪光粒子
                        if (Math.random() > 0.7) {
                            player.getWorld().spawnParticle(
                                    Particle.DUST,
                                    trailLoc,
                                    1,
                                    0, 0, 0, 0.1,
                            new Particle.DustOptions(color, 1.3f)
                            );
                        }
                    }
                    player.spawnParticle(Particle.ELECTRIC_SPARK,
                            player.getLocation().add(0, 0.5, 0), 3);

                    // 结束检查
                    if (++ticks >= maxTicks) {
                        scheduleSlashAttack(player, dashTarget, player.getLocation());
                        this.cancel();
                    }
                }
            }.runTaskTimer((JavaPlugin) Bukkit.getPluginManager().getPlugin("HolyGrailWar"), 0, 1);
        }

        // 安排斩击攻击（延迟执行）
        private void scheduleSlashAttack(Player player, LivingEntity dashTarget, Location dashEndLoc) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    performSlashAttack(player, dashTarget, dashEndLoc);
                }
            }.runTaskLater((JavaPlugin) Bukkit.getPluginManager().getPlugin("HolyGrailWar"), 3); // 增加延迟
        }

        // 执行斩击攻击（修改后版本）
        private void performSlashAttack(Player player, LivingEntity dashTarget, Location center) {
            Vector direction = player.getEyeLocation().getDirection().setY(0).normalize();
            List<Entity> hitEntities = new ArrayList<>(); // 新增：已命中实体列表

            // 强化斩击特效
            player.getWorld().playSound(center, Sound.ITEM_TRIDENT_THROW, 1.5f, 0.7f);
            drawEnhancedSlashArc(center, direction);

            // 扩大斩击检测范围
            double length = 5; // 长度增加到5格
            double width = 3;  // 宽度增加到3格

            for (double d = 0; d <= length; d += 0.6) {
                for (double w = -width; w <= width; w += 0.6) {
                    Vector offset = direction.clone()
                            .multiply(d)
                            .add(new Vector(-direction.getZ(), 0, direction.getX()).multiply(w));

                    Location checkLoc = center.clone().add(offset).add(0, 1, 0); // 提高检测高度

                    // 伤害检测
                    for (Entity entity : checkLoc.getWorld().getNearbyEntities(checkLoc, 1.0, 1.0, 1.0)) {
                        if (entity instanceof LivingEntity &&
                                !(entity instanceof ArmorStand) &&
                                !entity.equals(player) &&
                                (dashTarget == null || !entity.equals(dashTarget)) &&
                                !hitEntities.contains(entity)) { // 新增：检查是否已命中

                            // 无视护甲的伤害
                            ((LivingEntity) entity).setNoDamageTicks(0);
                            ((LivingEntity) entity).damage(10, player);
                            spawnEnhancedSlashHitEffect(entity.getLocation());
                            hitEntities.add(entity); // 新增：添加到已命中列表
                        }
                    }
                }
            }
        }

        // 增强版斩击弧线绘制
        private void drawEnhancedSlashArc(Location center, Vector baseDir) {
            World world = center.getWorld();
            Vector perpendicular = new Vector(-baseDir.getZ(), 0.4, baseDir.getX()).normalize();

            // 更密集的粒子弧
            for (double angle = -50; angle <= 50; angle += 4) { // 扩大角度和密度
                double rad = Math.toRadians(angle);
                Vector dir = baseDir.clone()
                        .multiply(Math.cos(rad) * 4) // 增加长度
                        .add(perpendicular.clone().multiply(Math.sin(rad) * 3)); // 增加宽度

                Location end = center.clone().add(dir).add(0, 1.2, 0); // 提高弧线高度

                // 更醒目的颜色渐变
                double progress = (angle + 50) / 100;
                Color color = Color.fromRGB(
                        (int) (50 + 205 * progress),
                        (int) (100 + 155 * progress),
                        255
                );

                world.spawnParticle(
                        Particle.DUST,
                        end,
                        3, // 增加粒子数量
                        new Particle.DustOptions(color, 2.2f) // 增大粒子尺寸
                );

                // 添加边缘光效
                if (Math.abs(angle) > 35) {
                    world.spawnParticle(
                            Particle.DUST,
                            end,
                            1,
                            new Particle.DustOptions(Color.WHITE, 1.5f)
                    );
                }
            }
        }

        // 强化斩击命中特效
        private void spawnEnhancedSlashHitEffect(Location loc) {
            loc.getWorld().spawnParticle(Particle.CRIT, loc, 25, 0.7, 0.7, 0.7, 0.1);
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.5f);

            // 添加冲击波效果
            for (int i = 0; i < 120; i += 30) {
                Vector dir = new Vector(
                        Math.cos(Math.toRadians(i)),
                        0.2,
                        Math.sin(Math.toRadians(i))
                ).normalize();
                loc.getWorld().spawnParticle(
                        Particle.DUST,
                        loc,
                        1,
                        dir.getX(), dir.getY(), dir.getZ(), 0.7,
                        new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.8f)
                );
            }
        }
        // 冲刺命中特效
        private void spawnDashHitEffect(Location loc) {
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 30, 0, 0, 0, 0.3);
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
        }


        private void executeUltimateSkill() {
            Player player = this.player;
            Location center = player.getLocation().add(0, 0.7, 0); // 提高中心点到腰部位置
            double radius = 5.0; // 伤害半径
            final double swordLength = 4; // 剑刃长度

            // 音效 - 剑刃挥舞
            player.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2.0f, 0.6f);
            player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.8f);

            // 360度旋转特效（带加速度）
            new BukkitRunnable() {
                double angle = 0;
                final int totalSteps = 24; // 增加总帧数以便更平滑的加速
                final double maxSpeed = 100; // 最大角度速度（度/ticks）
                final double acceleration = 1.5; // 加速度
                double currentSpeed = 15; // 初始速度（慢速开始）
                final List<Entity> hitEntities = new ArrayList<>(); // 已命中实体列表

                @Override
                public void run() {
                    // 计算当前角度方向
                    Vector direction = new Vector(Math.cos(Math.toRadians(angle)), 0, Math.sin(Math.toRadians(angle))).normalize();

                    // 生成更长的剑刃粒子效果
                    for (double l = 0; l <= swordLength; l += 0.3) { // 更密集的剑刃粒子
                        Location swordLoc = center.clone().add(direction.clone().multiply(l));

                        // 主剑刃粒子（密集核心）
                        player.getWorld().spawnParticle(
                                Particle.DUST,
                                swordLoc,
                                3, // 每个点生成3个粒子
                                0.05, 0.05, 0.05, 0,
                                new Particle.DustOptions(Color.fromRGB(0, 150, 255),
                                        1.5f + (float)(currentSpeed / maxSpeed * 0.5f))
                        );

                        // 剑刃高光（中心白线）
                        if (l % 0.6 < 0.3) {
                            player.getWorld().spawnParticle(
                                    Particle.DUST,
                                    swordLoc,
                                    1,
                                    0, 0, 0, 0,
                                    new Particle.DustOptions(Color.WHITE, 1.0f)
                            );
                        }
                    }

                    // 生成淡化的拖尾效果（仅在剑刃后方）
                    double trailStart = swordLength * 0.7; // 拖尾从剑刃70%位置开始
                    for (double r = trailStart; r <= radius; r += 0.4) {
                        Location trailLoc = center.clone().add(direction.clone().multiply(r));

                        // 计算拖尾淡化程度（距离剑刃越远越淡）
                        double fade = 1.0 - ((r - trailStart) / (radius - trailStart));

                        // 拖尾粒子（半透明）
                        player.getWorld().spawnParticle(
                                Particle.DUST,
                                trailLoc,
                                1,
                                0, 0, 0, 0,
                                new Particle.DustOptions(
                                        Color.fromRGB(
                                                100,
                                                200,
                                                255
                                        ),
                                        0.5f * (float)fade
                                )
                        );
                    }

                    // 伤害检测（根据速度调整检测频率）
                    if (angle % (45 - (currentSpeed / maxSpeed * 20)) <= currentSpeed) {
                        for (Entity entity : player.getNearbyEntities(radius, 2.0, radius)) {
                            if (entity instanceof LivingEntity &&
                                    !(entity instanceof ArmorStand) &&
                                    !entity.equals(player) &&
                                    !hitEntities.contains(entity)) {

                                // 计算是否在圆形范围内
                                Location entityLoc = entity.getLocation();
                                if (center.distance(entityLoc) <= radius) {
                                    // 真实伤害（无视护甲）
                                    ((LivingEntity) entity).setNoDamageTicks(0);
                                    ((LivingEntity) entity).damage(30, player);
                                    hitEntities.add(entity);

                                    // 命中特效
                                    entity.getWorld().spawnParticle(
                                            Particle.CRIT,
                                            entityLoc.add(0, 1, 0),
                                            15,
                                            0.5, 0.5, 0.5, 0.2
                                    );
                                    entity.getWorld().playSound(
                                            entityLoc,
                                            Sound.ENTITY_PLAYER_ATTACK_CRIT,
                                            1.0f + (float)(currentSpeed / maxSpeed * 0.5f),
                                            0.7f
                                    );
                                }
                            }
                        }
                    }

                    // 应用加速度
                    currentSpeed = Math.min(currentSpeed + acceleration, maxSpeed);
                    angle += currentSpeed;

                    // 中心漩涡特效
                    if (angle % 30 <= currentSpeed) {
                        for (int i = 0; i < 5; i++) {
                            double swirlRadius = (angle % 60) / 60.0 * 2.5;
                            Vector swirlDir = new Vector(
                                    Math.cos(Math.toRadians(angle * 4 + i * 72)),
                                    0.2,
                                    Math.sin(Math.toRadians(angle * 4 + i * 72))
                            ).normalize().multiply(swirlRadius);

                            player.getWorld().spawnParticle(
                                    Particle.DUST,
                                    center.clone().add(swirlDir),
                                    1,
                                    new Particle.DustOptions(Color.fromRGB(180, 220, 255), 0.8f)
                            );
                        }
                    }

                    // 结束检查
                    if (angle >= 360) {
                        this.cancel();
                    }
                }
            }.runTaskTimer((JavaPlugin) Bukkit.getPluginManager().getPlugin("HolyGrailWar"), 0, 1);
        }
    }
}













