package fun.kaituo.holygrailwar;

import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.commands.SetCharacterCommand;
import fun.kaituo.holygrailwar.state.FightState;
import fun.kaituo.holygrailwar.state.ReadyState;
import fun.kaituo.holygrailwar.state.WaitingState;
import fun.kaituo.gameutils.game.Game;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("unused")
public class HolyGrailWar extends Game {

    public static final WaitingState INST = new WaitingState();
    private static HolyGrailWar instance;
    public static HolyGrailWar inst() { return instance; }
    private final Map<UUID, CharacterBase> playerCharacters = new HashMap<>();


    public final Set<UUID> playerIds = new HashSet<>();
    @Getter
    private static final Location LobbyLocation = new Location(GameUtils.inst().getMainWorld(), 2000.5, 100, 4000.5);


    public Set<Player> getPlayers() {
        Set<Player> players = new HashSet<>();
        for (UUID id : playerIds) {
            Player p = Bukkit.getPlayer(id);
            assert p != null;
            players.add(p);
        }
        return players;
    }

    // 添加角色管理方法
    public void setPlayerCharacter(Player player, CharacterBase character) {
        // 清除玩家当前角色(如果有)
        if (playerCharacters.containsKey(player.getUniqueId())) {
            playerCharacters.get(player.getUniqueId()).clearInventory();
        }

        // 设置新角色
        playerCharacters.put(player.getUniqueId(), character);
    }

    public CharacterBase getPlayerCharacter(Player player) {
        return playerCharacters.get(player.getUniqueId());
    }

    public void removePlayerCharacter(Player player) {
        if (playerCharacters.containsKey(player.getUniqueId())) {
            CharacterBase character = playerCharacters.get(player.getUniqueId());
            character.cleanup(); // 清理监听器
            character.clearInventory();
            playerCharacters.remove(player.getUniqueId());
        }
    }
    // 禁用所有实体的无敌时间
    // 禁用所有生物的无敌时间
    public static void disableInvulnerabilityTicks() {
        Bukkit.getWorlds().forEach(world -> {
            world.getLivingEntities().forEach(entity -> { // 使用 getLivingEntities() 而不是 getEntities()
                entity.setMaximumNoDamageTicks(0); // 设置无敌时间为0
                entity.setNoDamageTicks(0); // 立即生效
            });
        });
    }

    // 恢复默认无敌时间（默认值：10 ticks = 0.5s）
    public static void restoreDefaultInvulnerabilityTicks() {
        Bukkit.getWorlds().forEach(world -> {
            world.getLivingEntities().forEach(entity -> { // 只对 LivingEntity 生效
                entity.setMaximumNoDamageTicks(10); // 默认值
                entity.setNoDamageTicks(0); // 立即生效
            });
        });
    }

    private void initStates(){
        WaitingState.INST.init();
        ReadyState.INST.init();
        FightState.INST.init();
    }

    @Override
    public void addPlayer(Player p) {
        p.setBedSpawnLocation(LobbyLocation, true);
        playerIds.add(p.getUniqueId());
        p.teleport(LobbyLocation);

        // 初始化经验条
        p.setExp(0);
        p.setLevel(0);

        super.addPlayer(p);
    }

    @Override
    public void removePlayer(Player p) {
        // 清理经验条
        p.setExp(0);
        p.setLevel(0);

        removePlayerCharacter(p);
        playerIds.remove(p.getUniqueId());
        super.removePlayer(p);
    }

    @Override
    public void forceStop() {
        // 清理所有玩家的冷却时间
        for (Player player : getPlayers()) {
            player.setCooldown(Material.DIAMOND_SWORD, 0);
            player.setCooldown(Material.GLOW_INK_SAC, 0);
        }
    }


    @Override
    public void tick() {
        super.tick();
    }

    public void onEnable() {
        super.onEnable();

        instance = this;
        updateExtraInfo("§e圣杯战争", getLoc("lobby"));

        // 注册命令
        getCommand("setcharacter").setExecutor(new SetCharacterCommand());
        getCommand("setcharacter").setTabCompleter(new SetCharacterCommand());

        Bukkit.getScheduler().runTaskLater(this, () -> {
            initStates();
            setState(WaitingState.INST);
        }, 1);
    }

    public void onDisable() {
        restoreDefaultInvulnerabilityTicks(); // 恢复默认无敌时间
        for (Player player : getPlayers()) {
            player.setCooldown(Material.DIAMOND_SWORD, 0);
            player.setCooldown(Material.GLOW_INK_SAC, 0);
        }
        super.onDisable();
    }

}




