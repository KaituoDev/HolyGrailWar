package fun.kaituo.holygrailwar.commands;

import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.command.GameUtilsCommand;
import fun.kaituo.gameutils.util.Misc;
import fun.kaituo.holygrailwar.HolyGrailWar;
import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.utils.DrawCareerClass;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SetCharacterCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此命令");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("用法: /setcharacter <角色名> <职阶>");
            player.sendMessage("可用角色: 美树沙耶香, 佐仓杏子, 鹿目圆, 晓美焰, 巴麻美");
            player.sendMessage("可用职阶: SABER, ARCHER, LANCER, RIDER, CASTER, ASSASSIN, BERSERKER");
            return true;
        }

        String characterName = args[0];
        String className = args[1].toUpperCase();

        try {
            // 获取职阶枚举
            DrawCareerClass.ClassType classType = DrawCareerClass.ClassType.valueOf(className);

            // 获取角色管理实例
            DrawCareerClass characterSystem = DrawCareerClass.getInstance();

            // 查找匹配的角色
            DrawCareerClass.GameCharacter targetCharacter = null;
            for (DrawCareerClass.GameCharacter character : characterSystem.getAllCharacters()) {
                if (character.getName().equals(characterName)) {
                    targetCharacter = character;
                    break;
                }
            }

            if (targetCharacter == null) {
                player.sendMessage("无效的角色名");
                return true;
            }

            if (!targetCharacter.getAvailableClasses().contains(classType)) {
                player.sendMessage("该角色不支持此职阶");
                return true;
            }



            // 先完全移除现有角色
            HolyGrailWar.inst().removePlayerCharacter(player);

            // 创建新角色实例
            CharacterBase newCharacter = targetCharacter.createCharacterInstance(player, classType);

            // 设置新角色给玩家
            HolyGrailWar.inst().setPlayerCharacter(player, newCharacter);

            player.sendMessage("已成功将你的角色设置为: " + characterName + " - " + classType.getColoredName());



        } catch (IllegalArgumentException e) {
            player.sendMessage("无效的职阶名称");
        } catch (Exception e) {
            player.sendMessage("设置角色时出错: " + e.getMessage());
            HolyGrailWar.inst().getLogger().log(Level.SEVERE,
                    "为玩家 " + player.getName() + " 设置角色时出错", e);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        DrawCareerClass characterSystem = DrawCareerClass.getInstance();

        if (args.length == 1) {
            // 返回所有角色名称的补全
            return GameUtilsCommand.getMatchingCompletions(args[0],
                    characterSystem.getAllCharacters().stream()
                            .map(DrawCareerClass.GameCharacter::getName)
                            .collect(Collectors.toList()));
        }

        if (args.length == 2) {
            // 查找第一个参数对应的角色
            String characterName = args[0];
            DrawCareerClass.GameCharacter targetCharacter = characterSystem.getAllCharacters().stream()
                    .filter(c -> c.getName().equals(characterName))
                    .findFirst()
                    .orElse(null);

            if (targetCharacter != null) {
                // 返回该角色可用职阶的补全
                return GameUtilsCommand.getMatchingCompletions(args[1],
                        targetCharacter.getAvailableClasses().stream()
                                .map(Enum::name)
                                .map(String::toLowerCase)
                                .collect(Collectors.toList()));
            }
        }

        return new ArrayList<>();
    }
}