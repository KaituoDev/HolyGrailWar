package fun.kaituo.holygrailwar.state;

import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import fun.kaituo.holygrailwar.utils.DrawCareerClass.GameCharacter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Set;

public class FightState implements GameState, Listener {
    public static final FightState INST = new FightState();
    private Set<Player> players;
    private final HashMap<Player, CharacterBase> playerCharacters = new HashMap<>();
    private HolyGrailWar game;

    public void init() {
        game = HolyGrailWar.inst();
    }

    @Override
    public void enter() {
        // 重置抽取记录，确保新一局游戏可以重新抽取
        DrawCareerClass.getInstance().resetDrawnCharacters();

        // 为所有玩家分配角色
        for (Player player : game.getPlayers()) {
            assignCareer(player);
        }
        Bukkit.getPluginManager().registerEvents(this, game);
    }

    @EventHandler
    public void onPlayerclickCareerButton(PlayerInteractEvent event) {
        Bukkit.broadcastMessage("1");
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Bukkit.broadcastMessage("2");
            return;
        }
        Block block = event.getClickedBlock();
        if (!block.getType().equals(Material.STONE_BUTTON)){
            Bukkit.broadcastMessage("3");
            return;
        }
        Bukkit.broadcastMessage("4");
        assignCareer(event.getPlayer());
    }



    private void assignCareer(Player player) {
        try {
            playerCharacters.remove(player);

            DrawCareerClass drawSystem = DrawCareerClass.getInstance();
            GameCharacter character = drawSystem.drawWeightedUniqueCharacter();
            DrawCareerClass.ClassType classType = character.getAvailableClasses().iterator().next();

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
        playerCharacters.clear();
        HandlerList.unregisterAll(this);
    }

    @Override
    public void tick() {
        // 游戏进行中的逻辑
    }

    @Override
    public void addPlayer(Player player) {
        assignCareer(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, -1, 19, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 1, false, false));
    }

    @Override
    public void removePlayer(Player player) {
        playerCharacters.remove(player);
        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        player.removePotionEffect(PotionEffectType.SATURATION);
    }

    @Override
    public void forceStop() {
        Bukkit.broadcastMessage("xxx");
        playerCharacters.clear();
        game.setState(WaitingState.INST);
    }

    // 获取玩家的角色信息
    public CharacterBase getPlayerCharacter(Player player) {
        return playerCharacters.get(player);
    }
}