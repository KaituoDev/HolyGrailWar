package fun.kaituo.holygrailwar.state;

import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import fun.kaituo.holygrailwar.utils.DrawCareerClass.GameCharacter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class FightState implements GameState, Listener {
    public static final FightState INST = new FightState();
    private Set<Player> players;
    private final HashMap<Player, CharacterBase> playerCharacters = new HashMap<>();
    private final Set<Player> alivePlayers = new HashSet<>();
    private HolyGrailWar game;
    private boolean isGameOver = true;

    public void init() {
        game = HolyGrailWar.inst();
    }


    @Override
    public void enter() {
        HolyGrailWar.disableInvulnerabilityTicks(); // 禁用所有生物的无敌时间
        // 重置抽取记录，确保新一局游戏可以重新抽取
        isGameOver = false;
        DrawCareerClass.getInstance().resetDrawnCharactersAndClasses();

        // 为所有玩家分配角色并初始化存活玩家列表
        alivePlayers.clear();
        for (Player player : game.getPlayers()) {
            alivePlayers.add(player);
            addPlayer(player);
        }
        Bukkit.getPluginManager().registerEvents(this, game);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!alivePlayers.contains(player)) return;

        // 设置玩家为观察者模式
        player.setGameMode(GameMode.SPECTATOR);
        alivePlayers.remove(player);

    }

    private void endGame() {
        isGameOver = true;
        if (alivePlayers.isEmpty()) {
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.sendTitle("§c游戏结束！", "§7没有胜利者...", 10, 70, 20)
            );
        } else {
            Player winner = alivePlayers.iterator().next();
            Bukkit.getOnlinePlayers().forEach(p ->
                    p.sendTitle("§6游戏结束！", "§a" + winner.getName() + "§7赢得了圣杯战争！", 10, 70, 20)
            );
        }
        for (Player p : game.getPlayers())
            removePlayer(p);
        // 延迟3秒后返回等待状态
        Bukkit.getScheduler().runTaskLater(game, () -> {
            game.setState(WaitingState.INST);
        }, 60L); // 60 ticks = 3 seconds
    }

    @EventHandler
    public void onPlayerclickCareerButton(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!block.getType().equals(Material.STONE_BUTTON)){
            return;
        }
        assignCareer(event.getPlayer());
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof LivingEntity) { // 仅对 LivingEntity 生效
            LivingEntity livingEntity = (LivingEntity) event.getEntity();
            livingEntity.setMaximumNoDamageTicks(0); // 禁用新生物的无敌时间
        }
    }

    private void assignCareer(Player player) {
        try {
            playerCharacters.remove(player);

            DrawCareerClass drawSystem = DrawCareerClass.getInstance();
            DrawCareerClass.ClassType classType = drawSystem.drawRandomActiveClass();
            GameCharacter character = drawSystem.drawWeightedUniqueCharacter(classType);


            // 创建角色实例
            CharacterBase characterInstance = character.createCharacterInstance(player, classType);
            characterInstance.setupInventory();

            playerCharacters.put(player, characterInstance);

            String message = String.format(
                    "§a%s §7作为 %s §7回应了你的召唤！",
                    character.getName(),
                    classType.getColoredName()
            );

            player.sendTitle("", message, 10, 70, 20);
        } catch (IllegalStateException e) {
            player.sendMessage("§c角色分配失败: " + e.getMessage());
        }
    }

    @Override
    public void exit() {
        for (Player player : game.getPlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        playerCharacters.clear();
        alivePlayers.clear();
        HandlerList.unregisterAll(this);
    }

    @Override
    public void tick() {
        // 游戏进行中的逻辑
        if (!isGameOver && alivePlayers.size() <= 1) {
            endGame();
        }
    }

    @Override
    public void addPlayer(Player player) {
        assignCareer(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, -1, 19, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 1, false, false));
        alivePlayers.add(player);
    }

    @Override
    public void removePlayer(Player player) {
        playerCharacters.remove(player);
        alivePlayers.remove(player);
        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        player.removePotionEffect(PotionEffectType.SATURATION);
        player.setGameMode(GameMode.SURVIVAL);
    }

    @Override
    public void forceStop() {
        Bukkit.broadcastMessage("§c游戏被强制终止！");
        playerCharacters.clear();
        alivePlayers.clear();
        game.setState(WaitingState.INST);
    }

    // 获取玩家的角色信息
    public CharacterBase getPlayerCharacter(Player player) {
        return playerCharacters.get(player);
    }
}