package fun.kaituo.holygrailwar.characters.Sayaka;

import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.utils.AbstractSkill;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class SayakaBerserker extends CharacterBase {
    private boolean isBerserkMode = false;
    private int berserkDuration = 0;
    private final int MAX_BERSERK_DURATION = 200; // 10 seconds (20 ticks per second)
    private final int cooldownTicks = 450;
    private BukkitRunnable particleTask;

    public SayakaBerserker(Player player) {
        super(player, "美树沙耶香", DrawCareerClass.ClassType.BERSERKER, 3000, 1, 40);
        BerserkSkill skill = new BerserkSkill(HolyGrailWar.inst(), player);
        addSkill(skill);
        HolyGrailWar.inst().getServer().getPluginManager().registerEvents(skill, HolyGrailWar.inst());
        setupBerserkListener();
        setupDamageReductionListener();
    }

    private void setupDamageReductionListener() {
        HolyGrailWar.inst().getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerDamaged(EntityDamageEvent event) {
                if (!(event.getEntity() instanceof Player)) return;
                Player damaged = (Player) event.getEntity();

                if (damaged.equals(player)) {
                    double originalDamage = event.getDamage();
                    double damageReduction = originalDamage * 0.3; // 30% damage reduction
                    int manaCost = (int) (damageReduction * 10); // 10 times the reduced amount

                    if (hasEnoughMana(manaCost)) {
                        consumeMana(manaCost);
                        event.setDamage(originalDamage - damageReduction);
                    }
                }
            }
        }, HolyGrailWar.inst());
    }

    private void setupBerserkListener() {
        HolyGrailWar.inst().getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerAttack(EntityDamageByEntityEvent event) {
                if (!(event.getDamager() instanceof Player)) return;
                Player attacker = (Player) event.getDamager();

                if (attacker.equals(player) && isBerserkMode) {
                    // Check if attacker is holding a sword in main hand
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand.getType().toString().contains("SWORD")) {
                        // Increase damage during berserk mode
                        event.setDamage(0); // 先清零，然后使用玩家造成伤害
                        LivingEntity target = (LivingEntity) event.getEntity();
                        target.damage(event.getDamage() * 1.5, player);
                        // Life steal effect
                        double healAmount = event.getDamage() * 0.5; // Increased from 0.4 to 0.5
                        double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                        player.setHealth(newHealth);
                    }
                }
            }
        }, HolyGrailWar.inst());
    }
    private class BerserkSkill extends AbstractSkill implements Listener {
        public BerserkSkill(JavaPlugin plugin, Player player) {
            super(plugin, player, Material.DISC_FRAGMENT_5, "随身听中仅剩的碎片", cooldownTicks, 20);
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getPlayer().equals(player)) {
                checkAndTrigger(event);
            }
        }
        @Override
        protected boolean onTrigger(PlayerInteractEvent event) {
            // Check if player is holding the correct item
            ItemStack item = event.getItem();
            if (item == null || !item.hasItemMeta()) return false;

            ItemMeta meta = item.getItemMeta();
            if (!meta.getDisplayName().contains("随身听中仅剩的碎片")) return false;

            // Damage self
            player.damage(12.0);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);

            // Restore mana
            currentMana = Math.min(currentMana + 600, maxMana); // Increased from 500 to 600
            updateManaBar();

            // Enter berserk mode
            isBerserkMode = true;
            berserkDuration = MAX_BERSERK_DURATION;
            player.setCooldown(Material.DISC_FRAGMENT_5, cooldownTicks);

            // 启动粒子效果任务
            startParticleEffects();


            // Visual and sound effects
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation(), 50, 0.5, 0.5, 0.5, 0.1);

            // Start countdown for berserk mode
            new BukkitRunnable() {
                @Override
                public void run() {
                    berserkDuration--;
                    if (berserkDuration <= 0) {
                        isBerserkMode = false;
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
            return true;
        }
        private void startParticleEffects() {
            // 取消现有的粒子任务（如果有）
            if (particleTask != null && !particleTask.isCancelled()) {
                particleTask.cancel();
            }
            particleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isBerserkMode || berserkDuration <= 0) {
                        this.cancel();
                        return;
                    }

                    Location loc = player.getLocation();
                    World world = player.getWorld();

                    // 1. 蓝色药水粒子环绕效果
                    for (int i = 0; i < 10; i++) {
                        double angle = 2 * Math.PI * i / 10;
                        double x = Math.cos(angle) * 0.8;
                        double z = Math.sin(angle) * 0.8;
                        player.spawnParticle(
                                Particle.DUST,
                                loc.clone().add(x, 1.2, z),
                                1,
                                0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(0, 100, 255), 1.0f)
                        );
                    }

                    // 尾迹粒子
                    if (player.isSprinting() || player.isSneaking()) {
                        player.spawnParticle(
                                Particle.DUST,
                                loc.clone().add(0, 0.2, 0),
                                3,
                                0.2, 0.1, 0.2,
                                new Particle.DustOptions(Color.fromRGB(0, 100, 255), 1.5f)
                        );
                    }
                }
            };
            particleTask.runTaskTimer(plugin, 0, 5); // 每5 ticks运行一次
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        isBerserkMode = false;
        berserkDuration = 0;
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
    }
}