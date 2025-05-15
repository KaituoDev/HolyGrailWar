package fun.kaituo.holygrailwar.characters.Homura;

import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.utils.AbstractSkill;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class HomuraAssassin extends CharacterBase implements Listener {
    private Location linkedChestLocation;
    private JavaPlugin plugin;
    private static final String SHIELD_NAME = "空间与时间之盾";
    private static final String TIME_STOP_ITEM_NAME = "时间停止";
    private final TimeStopSkill timeStopSkill;

    // 时间停止相关状态
    private boolean isTimeStopped = false;
    private final Map<UUID, Vector> entityMomentums = new HashMap<>();
    private final Map<UUID, Location> entityLocations = new HashMap<>();
    private final List<EntityDamageEvent> delayedDamageEvents = new ArrayList<>();
    private final Set<UUID> frozenEntities = new HashSet<>();
    private final Set<BukkitRunnable> pausedTasks = new HashSet<>();
    private final Map<UUID, Projectile> frozenProjectiles = new HashMap<>();
    private final Map<UUID, Vector> projectileDirections = new HashMap<>(); // 新增：记录投射物方向
    private BukkitTask timeStopManaTask;

    private static final String RIFLE_NAME = "89式突击步枪";
    private static final int RIFLE_MAX_AMMO = 30;
    private boolean isRifleCooldown = false;
    private int riflecurrentAmmo = RIFLE_MAX_AMMO; // 当前弹药量

    private static final String PISTOL_NAME = "柯尔特M1911";
    private static final int PISTOL_MAX_AMMO = 7;
    private boolean isPistolCooldown = false;
    private int pistolcurrentAmmo = PISTOL_MAX_AMMO; // 当前弹药量

    private static final String MACHINEGUN_NAME = "M249班用自动机枪";
    private static final int MACHINEGUN_MAX_AMMO = 80;
    private boolean isMachinegunCooldown = false;
    private int machineguncurrentAmmo = MACHINEGUN_MAX_AMMO; // 当前弹药量
    private BukkitTask machinegunSlowTask;
    private boolean isHoldingRightClick = false;
    private BukkitTask machinegunFireTask;


    private static final String GRENADE_NAME = "M26式手榴弹";
    private static final Material GRENADE_MATERIAL = Material.BLACK_CANDLE;
    private static final int GRENADE_COOLDOWN = 60; // 3 seconds (20 ticks = 1 second)

    private BukkitTask weaponRefreshTask;
    private static final Material[] WEAPON_MATERIALS = {
            Material.WOLF_ARMOR,    // 突击步枪
            Material.LEVER,         // 手枪
            Material.IRON_HORSE_ARMOR, // 机枪
            Material.BLACK_CANDLE    // 手榴弹
    };
    private static final String[] WEAPON_NAMES = {
            RIFLE_NAME,
            PISTOL_NAME,
            MACHINEGUN_NAME,
            GRENADE_NAME
    };

    private final Map<UUID, WindCharge> frozenGrenades = new HashMap<>();

    public HomuraAssassin(Player player) {
        super(player, "晓美焰", DrawCareerClass.ClassType.ASSASSIN, 20*15, 1, 20*5);
        this.plugin = HolyGrailWar.inst();
        this.linkedChestLocation = HolyGrailWar.inst().getLoc("homura_assassin_chest");
        this.timeStopSkill = new TimeStopSkill(plugin, player, 10); // 时间停止不消耗魔力

        if (this.linkedChestLocation == null) {
            this.linkedChestLocation = player.getLocation();
            Bukkit.getLogger().warning("[HomuraAssassin] 未配置箱子位置，使用玩家当前位置: " + linkedChestLocation);
        } else {
            Bukkit.getLogger().info("[HomuraAssassin] 加载箱子位置: " + linkedChestLocation);
            clearLinkedChest();
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        startWeaponRefreshTask();
    }
    private void clearLinkedChest() {
        try {
            if (linkedChestLocation == null || linkedChestLocation.getWorld() == null) {
                return;
            }

            if (!linkedChestLocation.getWorld().isChunkLoaded(linkedChestLocation.getBlockX() >> 4, linkedChestLocation.getBlockZ() >> 4)) {
                linkedChestLocation.getWorld().loadChunk(linkedChestLocation.getBlockX() >> 4, linkedChestLocation.getBlockZ() >> 4);
            }

            Block block = linkedChestLocation.getBlock();
            if (!(block.getState() instanceof Chest)) {
                return;
            }

            Chest chest = (Chest) block.getState();
            Inventory inventory = chest.getInventory();
            inventory.clear(); // 清空箱子内容

            Bukkit.getLogger().info("[HomuraAssassin] 已清空关联箱子");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HomuraAssassin] 清空箱子失败: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) return;

        // 处理突击步枪射击
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item.getType() == Material.WOLF_ARMOR) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(RIFLE_NAME)) {
                    event.setCancelled(true);

                    // 检查弹药
                    if (riflecurrentAmmo <= 0) {
                        player.getInventory().removeItem(item); // 移除当前步枪
                        riflecurrentAmmo = RIFLE_MAX_AMMO; // 给予新的满弹药步枪
                        return;
                    }

                    if (!isRifleCooldown) {
                        shootRifle();
                        isRifleCooldown = true;
                        // 6 tick冷却
                        Bukkit.getScheduler().runTaskLater(plugin, () -> isRifleCooldown = false, 6);


                    }
                    return;
                }
            }
        }
        // 处理手枪射击
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item.getType() == Material.LEVER) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(PISTOL_NAME)) {
                    event.setCancelled(true);

                    // 检查弹药
                    if (pistolcurrentAmmo <= 0) {
                        player.getInventory().removeItem(item); // 移除当前步枪
                        pistolcurrentAmmo = PISTOL_MAX_AMMO; // 给予新的满弹药步枪
                        return;
                    }

                    if (!isPistolCooldown) {
                        shootPistol();
                        isPistolCooldown = true;
                        // 6 tick冷却
                        Bukkit.getScheduler().runTaskLater(plugin, () -> isPistolCooldown = false, 7);


                    }
                    return;
                }
            }
        }
        // 处理机枪射击
        if (item.getType() == Material.IRON_HORSE_ARMOR) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(MACHINEGUN_NAME)) {
                event.setCancelled(true);

                // 检查弹药
                if (machineguncurrentAmmo <= 0) {
                    player.getInventory().removeItem(item);
                    machineguncurrentAmmo = MACHINEGUN_MAX_AMMO;
                    return;
                }

                if (!isMachinegunCooldown) {
                    shootMachinegun();
                    isMachinegunCooldown = true;
                    // 4 tick冷却（比步枪稍快）
                    Bukkit.getScheduler().runTaskLater(plugin, () -> isMachinegunCooldown = false, 4);
                }
                return;
            }
        }
        // 处理手榴弹
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item.getType() == GRENADE_MATERIAL) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(GRENADE_NAME)) {
                    event.setCancelled(true);

                    // Check cooldown
                    if (player.hasCooldown(GRENADE_MATERIAL)) {
                        return;
                    }

                    // Remove one candle
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().removeItem(item);
                    }

                    // Set cooldown
                    player.setCooldown(GRENADE_MATERIAL, GRENADE_COOLDOWN);

                    // Launch grenade
                    launchGrenade();
                    return;
                }
            }
        }

        // 处理时间停止物品
        if (item.getType() == Material.STRUCTURE_VOID) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(TIME_STOP_ITEM_NAME)) {
                event.setCancelled(true);
                timeStopSkill.checkAndTrigger(event);
                return;
            }
        }

        // 处理箱子盾牌
        if (item.getType() == Material.NAUTILUS_SHELL) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(SHIELD_NAME)) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> openLinkedChest(event.getPlayer()));
            }
        }
    }

    private void startWeaponRefreshTask() {
        weaponRefreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                refreshWeaponsInChest();
            }
        }.runTaskTimer(plugin, 100, 100); // 5秒(100 ticks)刷新一次
    }

    private void stopWeaponRefreshTask() {
        if (weaponRefreshTask != null && !weaponRefreshTask.isCancelled()) {
            weaponRefreshTask.cancel();
        }
    }
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (isTimeStopped && !event.getEntity().equals(player)) {
            delayedDamageEvents.add(event);
            event.setCancelled(true);
        }
    }

    private void openLinkedChest(Player player) {
        try {
            if (linkedChestLocation == null || linkedChestLocation.getWorld() == null) {
                throw new IllegalStateException("箱子位置无效");
            }

            if (!linkedChestLocation.getWorld().isChunkLoaded(linkedChestLocation.getBlockX() >> 4, linkedChestLocation.getBlockZ() >> 4)) {
                linkedChestLocation.getWorld().loadChunk(linkedChestLocation.getBlockX() >> 4, linkedChestLocation.getBlockZ() >> 4);
            }

            Block block = linkedChestLocation.getBlock();
            if (!(block.getState() instanceof Chest)) {
                throw new IllegalStateException("指定位置不是箱子");
            }

            Chest chest = (Chest) block.getState();
            player.openInventory(chest.getInventory());
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HomuraAssassin] 打开箱子失败: " + e.getMessage());
            player.sendMessage("§c错误: " + e.getMessage());
            Inventory fallback = Bukkit.createInventory(player, 27, "晓美焰的备用箱子");
            player.openInventory(fallback);
        }
    }
    private void refreshWeaponsInChest() {
        try {
            if (linkedChestLocation == null || linkedChestLocation.getWorld() == null) {
                return;
            }

            Block block = linkedChestLocation.getBlock();
            if (!(block.getState() instanceof Chest)) {
                return;
            }

            Chest chest = (Chest) block.getState();
            Inventory inventory = chest.getInventory();

            // 计算当前箱子中武器的数量
            int weaponCount = 0;
            for (ItemStack item : inventory.getContents()) {
                if (item != null && isWeaponItem(item)) {
                    weaponCount++;
                }
            }

            // 如果武器数量超过2/3容量，则不刷新
            int maxWeapons = (int) (inventory.getSize() * 2.0 / 3.0);
            if (weaponCount >= maxWeapons) {
                return;
            }

            // 寻找随机空位
            List<Integer> emptySlots = new ArrayList<>();
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    emptySlots.add(i);
                }
            }

            if (!emptySlots.isEmpty()) {
                // 随机选择一个空位
                int slot = emptySlots.get(new Random().nextInt(emptySlots.size()));

                // 随机选择一种武器
                int weaponIndex = new Random().nextInt(WEAPON_MATERIALS.length);
                Material material = WEAPON_MATERIALS[weaponIndex];
                String name = WEAPON_NAMES[weaponIndex];

                // 创建武器物品
                ItemStack weapon = new ItemStack(material);
                ItemMeta meta = weapon.getItemMeta();
                meta.setDisplayName(name);
                weapon.setItemMeta(meta);

                // 放入箱子
                inventory.setItem(slot, weapon);

            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HomuraAssassin] 刷新武器失败: " + e.getMessage());
        }
    }

    private boolean isWeaponItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        String displayName = item.getItemMeta().getDisplayName();
        for (String weaponName : WEAPON_NAMES) {
            if (displayName.contains(weaponName)) {
                return true;
            }
        }
        return false;
    }

    private void startTimeStop() {
        isTimeStopped = true;
        entityMomentums.clear();
        entityLocations.clear();
        delayedDamageEvents.clear();
        frozenEntities.clear();
        pausedTasks.clear();
        frozenProjectiles.clear();
        projectileDirections.clear();




        // 记录所有实体的动量和位置
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && !entity.equals(player)) {
                    UUID uuid = entity.getUniqueId();
                    if (entity instanceof Player) {
                        ((Player) entity).addPotionEffect(new PotionEffect(
                                PotionEffectType.BLINDNESS,
                                Integer.MAX_VALUE,
                                0,
                                false,
                                false
                        ));
                    }

                    if (entity instanceof LivingEntity) {
                        entityMomentums.put(uuid, entity.getVelocity());
                        entityLocations.put(uuid, entity.getLocation());
                        frozenEntities.add(uuid);

                        entity.setGravity(false);
                        entity.setInvulnerable(true);
                        entity.setVelocity(new Vector(0, 0, 0));
                        if (entity instanceof LivingEntity) {
                            ((LivingEntity) entity).setAI(false);
                        }
                    }
                } else if (entity instanceof Projectile) {
                    Projectile projectile = (Projectile) entity;
                    // 如果是使用者刚发射的投射物，暂时不处理（会在1 tick后被处理）
                    if (projectile.getShooter() instanceof Player &&
                            ((Player)projectile.getShooter()).equals(player) &&
                            projectile.getTicksLived() <= 1) {
                        continue;
                    }

                    // 处理其他所有投射物
                    frozenProjectiles.put(entity.getUniqueId(), projectile);
                    Vector velocity = entity.getVelocity();
                    entityMomentums.put(entity.getUniqueId(), velocity.clone());
                    entityLocations.put(entity.getUniqueId(), entity.getLocation());

                    projectile.setVelocity(new Vector(0, 0, 0));
                    projectile.setGravity(false);
                }
            }
        }

        // 暂停所有BukkitRunnable任务
        for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
            if (task instanceof BukkitRunnable && !task.isCancelled()) {
                task.cancel();
                pausedTasks.add((BukkitRunnable) task);
            }
        }

        // 视觉效果
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 10, 10, 10);

        // 防止实体移动的任务
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isTimeStopped) {
                    this.cancel();
                    return;
                }
                World world = player.getWorld();

                // 保持实体位置固定
                for (UUID uuid : frozenEntities) {
                    Entity entity = Bukkit.getEntity(uuid);
                    if (entity != null && entity.isValid()) {
                        Location originalLoc = entityLocations.get(uuid);
                        if (originalLoc != null) {
                            entity.teleport(originalLoc);
                            entity.setVelocity(new Vector(0, 0, 0));
                        }
                    }
                }

                // 保持投射物位置固定
                for (Map.Entry<UUID, Projectile> entry : frozenProjectiles.entrySet()) {
                    Projectile projectile = entry.getValue();
                    if (projectile != null && projectile.isValid()) {
                        projectile.setVelocity(new Vector(0, 0, 0));
                        // No need to maintain direction during time stop
                    }
                }
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof WindCharge) { // 改为检测WindCharge而不是TNTPrimed
                        WindCharge grenade = (WindCharge) entity;
                        frozenGrenades.put(grenade.getUniqueId(), grenade);
                        Vector velocity = grenade.getVelocity();
                        entityMomentums.put(grenade.getUniqueId(), velocity.clone());
                        entityLocations.put(grenade.getUniqueId(), grenade.getLocation().clone());

                        grenade.setGravity(false);
                        grenade.setVelocity(new Vector(0, 0, 0));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
        timeStopManaTask = new BukkitRunnable() {
            @Override
            public void run() {
                CharacterBase character = HolyGrailWar.inst().getPlayerCharacter(player);
                if (character == null || !character.hasEnoughMana(1)) {
                    // Mana不足，结束时停
                    endTimeStop();
                    player.sendMessage("§c魔力不足，时间恢复流动");
                    this.cancel();
                    return;
                }

                // 消耗2点mana
                character.consumeMana(2);
            }
        }.runTaskTimer(plugin, 0, 1); // 每tick执行一次
    }

    private void endTimeStop() {
        isTimeStopped = false;
        // 取消mana消耗任务
        if (timeStopManaTask != null && !timeStopManaTask.isCancelled()) {
            timeStopManaTask.cancel();
            timeStopManaTask = null;
        }

        // 恢复实体状态
        for (UUID uuid : frozenEntities) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) {
                entity.setGravity(true);
                entity.setInvulnerable(false);

                if (entity instanceof Player) {
                    ((Player) entity).removePotionEffect(PotionEffectType.BLINDNESS);
                }

                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).setAI(true);
                    Vector momentum = entityMomentums.get(uuid);
                    if (momentum != null) {
                        entity.setVelocity(momentum);
                    }
                }
            }
        }

        // 恢复投射物状态
        for (Map.Entry<UUID, Projectile> entry : frozenProjectiles.entrySet()) {
            Projectile projectile = entry.getValue();
            if (projectile != null && projectile.isValid()) {
                projectile.setGravity(true);
                Vector originalVelocity = entityMomentums.get(entry.getKey());

                if (originalVelocity != null) {
                    projectile.setVelocity(originalVelocity);

                    // 更新方向
                    if (originalVelocity.lengthSquared() > 0) {
                        Location loc = projectile.getLocation();
                        loc.setDirection(originalVelocity);
                        projectile.teleport(loc);
                    }
                }
            }
        }

        // 恢复风弹状态并应用抛物线运动
        for (Map.Entry<UUID, WindCharge> entry : frozenGrenades.entrySet()) {
            WindCharge grenade = entry.getValue();
            if (grenade != null && grenade.isValid()) {
                Vector originalVelocity = entityMomentums.get(entry.getKey());
                Location originalLocation = entityLocations.get(entry.getKey());

                if (originalVelocity != null && originalLocation != null) {
                    grenade.teleport(originalLocation);
                    grenade.setGravity(false); // 禁用原版重力，手动模拟

                    // 手动模拟抛物线运动
                    Vector velocity = originalVelocity.clone();
                    Vector gravity = new Vector(0, -0.05, 0);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!grenade.isValid()) {
                                this.cancel();
                                return;
                            }

                            // 更新速度（应用重力）
                            velocity.add(gravity);
                            // 更新位置
                            grenade.setVelocity(velocity);

                            // 检查是否击中地面（手动检测）
                            if (grenade.getLocation().getY() <= grenade.getWorld().getMinHeight()) {
                                explodeGrenade(grenade);
                                this.cancel();
                                return;
                            }
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
            }
        }

        // 恢复被暂停的任务
        for (BukkitRunnable task : pausedTasks) {
            if (!task.isCancelled()) {
                task.runTask(plugin);
            }
        }

        // 应用延迟的伤害事件
        for (EntityDamageEvent event : delayedDamageEvents) {
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity victim = (LivingEntity) event.getEntity();
                victim.damage(event.getDamage(), player);
            }
        }

        // 视觉效果
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation(), 50, 1, 1, 1);

        // 清除状态
        entityMomentums.clear();
        entityLocations.clear();
        delayedDamageEvents.clear();
        frozenEntities.clear();
        pausedTasks.clear();
        frozenProjectiles.clear();
        projectileDirections.clear();
        frozenGrenades.clear();
    }


    // 新增事件监听器 - 阻止时停期间的攻击行为
    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (isTimeStopped && !event.getEntity().equals(player)) {
            event.setCancelled(true);
        }
    }

    // 修改后的 ProjectileLaunchEvent 处理
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!isTimeStopped) return;

        // 只允许时停使用者发射投射物
        if (!(event.getEntity().getShooter() instanceof Player)) {
            event.setCancelled(true);
            return;
        }

        Player shooter = (Player) event.getEntity().getShooter();
        if (!shooter.equals(player)) {
            event.setCancelled(true);
            return;
        }

        // 如果是风弹，立即冻结
        if (event.getEntity() instanceof WindCharge) {
            WindCharge grenade = (WindCharge) event.getEntity();
            frozenGrenades.put(grenade.getUniqueId(), grenade);
            entityMomentums.put(grenade.getUniqueId(), grenade.getVelocity().clone());
            entityLocations.put(grenade.getUniqueId(), grenade.getLocation().clone());

            grenade.setVelocity(new Vector(0, 0, 0));
            grenade.setGravity(false);
            return;
        }

        // 其他投射物延迟1 tick后冻结
        Projectile projectile = event.getEntity();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isTimeStopped || !projectile.isValid()) return;

                frozenProjectiles.put(projectile.getUniqueId(), projectile);
                entityMomentums.put(projectile.getUniqueId(), projectile.getVelocity().clone());
                entityLocations.put(projectile.getUniqueId(), projectile.getLocation());

                projectile.setVelocity(new Vector(0, 0, 0));
                projectile.setGravity(false);
            }
        }.runTaskLater(plugin, 1);
    }

    private void shootRifle() {
        // 检查弹药
        if (riflecurrentAmmo <= 0) return;

        // 连续发射三发子弹
        for (int i = 0; i < 3; i++) {
            final int shotNumber = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 获取玩家视线方向
                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection().normalize();
                // 减少弹药
                riflecurrentAmmo--;
                // 显示剩余弹药
                showRifleAmmoCount();

                // 创建子弹
                Arrow bullet = player.launchProjectile(Arrow.class);
                bullet.setVelocity(direction.multiply(15)); // 每秒15格速度
                bullet.setGravity(false);
                bullet.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                bullet.setDamage(0); // 设置为0，我们自己处理伤害
                bullet.setInvulnerable(true); // 防止被其他插件干扰
                bullet.setSilent(true);
                // 记录子弹发射位置
                Location startLocation = player.getLocation().clone();
                // 设置子弹命中检测
                Bukkit.getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void onProjectileHit(ProjectileHitEvent event) {
                        if (event.getEntity().equals(bullet)) {
                            ProjectileHitEvent.getHandlerList().unregister(this);
                            if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity) {
                                LivingEntity target = (LivingEntity) event.getHitEntity();
                                // 计算距离
                                double distance = startLocation.distance(target.getLocation());

                                // 根据距离计算伤害 (近距离5点，远距离1点，线性递减)
                                double damage = Math.max(2, 3 - (distance * 0.02)); // 每格距离减少0.1伤害，最低1点
                                target.damage(damage, player);
                            }
                            bullet.remove();
                        }
                    }
                }, plugin);

                // 设置子弹最大飞行距离和粒子效果
                new BukkitRunnable() {
                    private int distance = 0;
                    private final Random random = new Random();

                    @Override
                    public void run() {
                        if (!bullet.isValid() || distance >= 150) {
                            bullet.remove();
                            this.cancel();
                            return;
                        }

                        // 在子弹位置生成灰色微小粒子 - 优化版本
                        Location bulletLoc = bullet.getLocation();
                        for (int j = 0; j < 5; j++) { // 每tick生成5个粒子
                            // 添加轻微随机偏移(0.1格范围内)
                            double offsetX = (random.nextDouble() - 0.5) * 0.1;
                            double offsetY = (random.nextDouble() - 0.5) * 0.1;
                            double offsetZ = (random.nextDouble() - 0.5) * 0.1;

                            bulletLoc.getWorld().spawnParticle(
                                    Particle.DUST,
                                    bulletLoc.clone().add(offsetX, offsetY, offsetZ),
                                    1, // 每个位置1个粒子
                                    0, 0, 0, // 无额外偏移
                                    0, // 额外数据
                                    new Particle.DustOptions(
                                            Color.fromRGB(120, 120, 120), // 浅灰色
                                            0.3f // 更小的粒子大小
                                    )
                            );
                        }

                        distance++;
                    }
                }.runTaskTimer(plugin, 0, 1);
            }, i * 2); // 每发子弹间隔2 tick
        }
    }

    private void shootPistol() {
        // 检查弹药
        if (pistolcurrentAmmo <= 0) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 获取玩家视线方向
            Location eyeLoc = player.getEyeLocation();
            Vector direction = eyeLoc.getDirection().normalize();
            // 减少弹药
            pistolcurrentAmmo--;
            // 显示剩余弹药
            showPistolAmmoCount();

            // 创建子弹
            Arrow bullet = player.launchProjectile(Arrow.class);
            bullet.setVelocity(direction.multiply(15)); // 每秒15格速度
            bullet.setGravity(false);
            bullet.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            bullet.setDamage(0); // 设置为0，我们自己处理伤害
            bullet.setInvulnerable(true); // 防止被其他插件干扰
            bullet.setSilent(true);
            // 记录子弹发射位置
            Location startLocation = player.getLocation().clone();
            // 设置子弹命中检测
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onProjectileHit(ProjectileHitEvent event) {
                    if (event.getEntity().equals(bullet)) {
                        ProjectileHitEvent.getHandlerList().unregister(this);
                        if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity) {
                            LivingEntity target = (LivingEntity) event.getHitEntity();
                            // 计算距离
                            double distance = startLocation.distance(target.getLocation());

                            // 根据距离计算伤害
                            double damage = Math.max(3, 7 - (distance * 0.1)); // 每格距离减少0.1伤害，最低1点
                            target.damage(damage, player);
                        }
                        bullet.remove();
                    }
                }
            }, plugin);

                // 设置子弹最大飞行距离和粒子效果
                new BukkitRunnable() {
                    private int distance = 0;
                    private final Random random = new Random();

                    @Override
                    public void run() {
                        if (!bullet.isValid() || distance >= 50) {
                            bullet.remove();
                            this.cancel();
                            return;
                        }

                        // 在子弹位置生成灰色微小粒子 - 优化版本
                        Location bulletLoc = bullet.getLocation();
                        for (int j = 0; j < 5; j++) { // 每tick生成5个粒子
                            // 添加轻微随机偏移(0.1格范围内)
                            double offsetX = (random.nextDouble() - 0.5) * 0.1;
                            double offsetY = (random.nextDouble() - 0.5) * 0.1;
                            double offsetZ = (random.nextDouble() - 0.5) * 0.1;

                            bulletLoc.getWorld().spawnParticle(
                                    Particle.DUST,
                                    bulletLoc.clone().add(offsetX, offsetY, offsetZ),
                                    1, // 每个位置1个粒子
                                    0, 0, 0, // 无额外偏移
                                    0, // 额外数据
                                    new Particle.DustOptions(
                                            Color.fromRGB(120, 120, 120), // 浅灰色
                                            0.3f // 更小的粒子大小
                                    )
                            );
                        }

                        distance++;
                    }
                }.runTaskTimer(plugin, 0, 1);
            },0);

    }
    private void shootMachinegun() {
        // 检查弹药
        if (machineguncurrentAmmo <= 0) return;

        // 连续发射多发子弹（模拟机枪连射效果）
        for (int i = 0; i < 5; i++) { // 每次射击发射5发子弹
            final int shotNumber = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 获取玩家视线方向
                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection().normalize();

                // 添加随机偏移 (水平±5度，垂直±3度)
                double horizontalOffset = (Math.random() - 0.5) * Math.toRadians(5);
                double verticalOffset = (Math.random() - 0.5) * Math.toRadians(3);

                // 应用偏移
                Vector offsetDirection = rotateVector(direction.clone(), horizontalOffset, verticalOffset);

                // 减少弹药
                machineguncurrentAmmo--;
                // 显示剩余弹药
                showMachinegunAmmoCount();

                // 创建子弹
                Arrow bullet = player.launchProjectile(Arrow.class);
                bullet.setVelocity(offsetDirection.multiply(10)); // 每秒10格速度
                bullet.setGravity(false);
                bullet.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                bullet.setDamage(0); // 设置为0，我们自己处理伤害
                bullet.setInvulnerable(true); // 防止被其他插件干扰
                bullet.setSilent(true);

                // 记录子弹发射位置
                Location startLocation = player.getLocation().clone();

                // 设置子弹命中检测
                Bukkit.getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void onProjectileHit(ProjectileHitEvent event) {
                        if (event.getEntity().equals(bullet)) {
                            ProjectileHitEvent.getHandlerList().unregister(this);
                            if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity) {
                                LivingEntity target = (LivingEntity) event.getHitEntity();
                                // 计算距离
                                double distance = startLocation.distance(target.getLocation());

                                // 根据距离计算伤害
                                double damage = Math.max(0.5, 2 - (distance * 0.08)); // 每格距离减少0.08伤害，最低0.5点
                                target.damage(damage, player);
                            }
                            bullet.remove();
                        }
                    }
                }, plugin);

                // 设置子弹最大飞行距离和粒子效果
                new BukkitRunnable() {
                    private int distance = 0;
                    private final Random random = new Random();

                    @Override
                    public void run() {
                        if (!bullet.isValid() || distance >= 150) {
                            bullet.remove();
                            this.cancel();
                            return;
                        }

                        // 在子弹位置生成灰色微小粒子
                        Location bulletLoc = bullet.getLocation();
                        for (int j = 0; j < 5; j++) {
                            double offsetX = (random.nextDouble() - 0.5) * 0.1;
                            double offsetY = (random.nextDouble() - 0.5) * 0.1;
                            double offsetZ = (random.nextDouble() - 0.5) * 0.1;

                            bulletLoc.getWorld().spawnParticle(
                                    Particle.DUST,
                                    bulletLoc.clone().add(offsetX, offsetY, offsetZ),
                                    1,
                                    0, 0, 0,
                                    0,
                                    new Particle.DustOptions(
                                            Color.fromRGB(120, 120, 120),
                                            0.3f
                                    )
                            );
                        }

                        distance++;
                    }
                }.runTaskTimer(plugin, 0, 1);
            }, i * 2); // 每发子弹间隔2 tick
        }
    }

    private Vector rotateVector(Vector vector, double yaw, double pitch) {
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        // 绕Y轴旋转 (水平)
        double x = vector.getX() * cosYaw - vector.getZ() * sinYaw;
        double z = vector.getX() * sinYaw + vector.getZ() * cosYaw;

        // 绕X轴旋转 (垂直)
        double y = vector.getY() * cosPitch - z * sinPitch;
        z = vector.getY() * sinPitch + z * cosPitch;

        return new Vector(x, y, z).normalize();
    }
    private void showRifleAmmoCount() {
        // 使用ActionBar显示剩余弹药（更小更靠下）
        player.sendActionBar(ChatColor.YELLOW + "弹药: " +
                ChatColor.WHITE + riflecurrentAmmo +
                ChatColor.GRAY + "/" +
                ChatColor.WHITE + RIFLE_MAX_AMMO);
    }

    private void showPistolAmmoCount() {
        // 使用ActionBar显示剩余弹药（更小更靠下）
        player.sendActionBar(ChatColor.YELLOW + "弹药: " +
                ChatColor.WHITE + pistolcurrentAmmo +
                ChatColor.GRAY + "/" +
                ChatColor.WHITE + PISTOL_MAX_AMMO);
    }
    private void showMachinegunAmmoCount() {
        // 使用ActionBar显示剩余弹药（更小更靠下）
        player.sendActionBar(ChatColor.YELLOW + "弹药: " +
                ChatColor.WHITE + machineguncurrentAmmo +
                ChatColor.GRAY + "/" +
                ChatColor.WHITE + MACHINEGUN_MAX_AMMO);
    }
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
            return;
        }

        ItemStack item = event.getItemDrop().getItemStack();
        if (item == null) return;

        // 检查是否是三把武器之一
        if (item.getType() == Material.WOLF_ARMOR ||
                item.getType() == Material.LEVER ||
                item.getType() == Material.IRON_HORSE_ARMOR) {

            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                // 取消丢弃事件
                event.setCancelled(true);

                // 延迟1 tick后移除手中的物品
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.getInventory().setItemInMainHand(null);

                    // 发送提示消息
                    if (meta.getDisplayName().contains(RIFLE_NAME)) {
                        player.sendMessage(ChatColor.YELLOW + "已丢弃突击步枪");
                    } else if (meta.getDisplayName().contains(PISTOL_NAME)) {
                        player.sendMessage(ChatColor.YELLOW + "已丢弃手枪");
                    } else if (meta.getDisplayName().contains(MACHINEGUN_NAME)) {
                        player.sendMessage(ChatColor.YELLOW + "已丢弃机枪");

                        // 如果丢弃的是机枪，取消缓慢效果任务
                        if (machinegunSlowTask != null && !machinegunSlowTask.isCancelled()) {
                            machinegunSlowTask.cancel();
                            player.removePotionEffect(PotionEffectType.SLOWNESS);
                        }
                    }
                }, 1);

                // 更新弹药显示
                updateAmmoDisplay();
            }
        }
    }

    private void updateAmmoDisplay() {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName.contains(RIFLE_NAME)) {
                showRifleAmmoCount();
            } else if (displayName.contains(PISTOL_NAME)) {
                showPistolAmmoCount();
            } else if (displayName.contains(MACHINEGUN_NAME)) {
                showMachinegunAmmoCount();
            }
        }
    }
    // 修改手榴弹发射方法
    private void launchGrenade() {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        // 创建风弹（Wind Charge）
        WindCharge grenade = player.getWorld().spawn(eyeLoc, WindCharge.class);
        grenade.setShooter(player);

        // 初始速度
        Vector velocity = direction.multiply(1.5);
        grenade.setVelocity(velocity);

        // 如果不是时停状态，手动模拟抛物线运动
        if (!isTimeStopped) {
            grenade.setGravity(false); // 禁用原版重力，手动模拟
            Vector gravity = new Vector(0, -0.05, 0);

            // 手动模拟抛物线运动
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!grenade.isValid()) {
                        this.cancel();
                        return;
                    }

                    // 更新速度（应用重力）
                    velocity.add(gravity);
                    // 更新位置
                    grenade.setVelocity(velocity);

                    // 检查是否击中地面（手动检测）
                    if (grenade.getLocation().getY() <= grenade.getWorld().getMinHeight()) {
                        explodeGrenade(grenade);
                        this.cancel();
                        return;
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }

        // 监听风弹碰撞事件
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onProjectileHit(ProjectileHitEvent event) {
                if (event.getEntity().equals(grenade)) {
                    ProjectileHitEvent.getHandlerList().unregister(this);
                    explodeGrenade(grenade);
                }
            }
        }, plugin);
    }

    // 爆炸逻辑封装
    private void explodeGrenade(WindCharge grenade) {
        if (!grenade.isValid()) return;

        // 产生等级4的爆炸（不破坏方块）
        grenade.getWorld().createExplosion(
                grenade.getLocation(),
                4.0f,
                false,
                false,
                player
        );

        // 播放爆炸音效
        grenade.getWorld().playSound(
                grenade.getLocation(),
                Sound.ENTITY_GENERIC_EXPLODE,
                4.0f,
                1.0f
        );

        // 移除风弹
        grenade.remove();
    }
    @Override
    public void cleanup() {
        super.cleanup();
        if (isTimeStopped) {
            endTimeStop();
        }
        if (machinegunSlowTask != null && !machinegunSlowTask.isCancelled()) {
            machinegunSlowTask.cancel();
        }
        stopWeaponRefreshTask(); // 新增：停止武器刷新任务
        PlayerInteractEvent.getHandlerList().unregister(this);
        EntityDamageEvent.getHandlerList().unregister(this);
        PlayerDropItemEvent.getHandlerList().unregister(this);
        if (timeStopManaTask != null && !timeStopManaTask.isCancelled()) {
            timeStopManaTask.cancel();
        }
    }

    private class TimeStopSkill extends AbstractSkill {
        public TimeStopSkill(JavaPlugin plugin, Player player, int manaCost) {
            super(plugin, player, Material.STRUCTURE_VOID, TIME_STOP_ITEM_NAME, 0, manaCost);
        }

        @Override
        protected boolean onTrigger(PlayerInteractEvent event) {
            if (isTimeStopped) {
                endTimeStop();
                player.sendMessage("§b时间恢复流动");
            } else {
                // 检查是否有足够mana启动时停
                CharacterBase character = HolyGrailWar.inst().getPlayerCharacter(player);
                if (character != null && character.hasEnoughMana(1)) {
                    startTimeStop();
                    player.sendMessage("§b时间停止！");
                } else {
                    player.sendMessage("§c魔力不足，无法停止时间");
                    return false;
                }
            }
            return true;
        }
    }
}