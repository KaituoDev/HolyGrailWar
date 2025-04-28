package fun.kaituo.holygrailwar.utils;

import fun.kaituo.holygrailwar.characters.CharacterBase;
import fun.kaituo.holygrailwar.characters.Homura.HomuraArcher;
import fun.kaituo.holygrailwar.characters.Homura.HomuraAssassin;
import fun.kaituo.holygrailwar.characters.Kyoko.KyokoCaster;
import fun.kaituo.holygrailwar.characters.Kyoko.KyokoLancer;
import fun.kaituo.holygrailwar.characters.Madoka.MadokaArcher;
import fun.kaituo.holygrailwar.characters.Mami.MamiArcher;
import fun.kaituo.holygrailwar.characters.Mami.MamiCaster;
import fun.kaituo.holygrailwar.characters.Mami.MamiRider;
import fun.kaituo.holygrailwar.characters.Sayaka.SayakaBerserker;
import fun.kaituo.holygrailwar.characters.Sayaka.SayakaRider;
import fun.kaituo.holygrailwar.characters.Sayaka.SayakaSaber;
import fun.kaituo.holygrailwar.sign.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class DrawCareerClass {
    // 职阶枚举
    public enum ClassType {
        LANCER("Lancer", 1, "§b"),
        SABER("Saber", 1, "§e"),
        BERSERKER("Berserker", 1, "§0"),
        RIDER("Rider", 1, "§6"),
        ARCHER("Archer", 1, "§4"),
        ASSASSIN("Assassin", 1, "§8"),
        CASTER("Caster", 1, "§5");

        private final String displayName;
        private final int baseWeight;
        private final String colorCode;  // 新增颜色代码字段

        ClassType(String displayName, int baseWeight, String colorCode) {
            this.displayName = displayName;
            this.baseWeight = baseWeight;
            this.colorCode = colorCode;
        }

        public String getDisplayName() { return displayName; }
        public int getBaseWeight() { return baseWeight; }
        public String getColorCode() { return colorCode; }  // 新增获取颜色代码方法

        // 获取带颜色的职阶名
        public String getColoredName() {
            return colorCode + displayName;
        }
    }

    // 角色类
    public static class GameCharacter {
        private final String name;
        private final Map<ClassType, Class<? extends CharacterBase>> characterClasses;
        private final Map<ClassType, Integer> classWeights; // 各职阶权重

        public GameCharacter(String name) {
            this.name = name;
            this.characterClasses = new EnumMap<>(ClassType.class);
            this.classWeights = new EnumMap<>(ClassType.class);
        }

        public String getName() { return name; }

        // 添加职阶及权重
        public GameCharacter addClass(ClassType classType, int weight, Class<? extends CharacterBase> characterClass) {
            classWeights.put(classType, weight);
            characterClasses.put(classType, characterClass);
            return this;
        }
        public CharacterBase createCharacterInstance(Player player, ClassType classType) {
            try {
                Class<? extends CharacterBase> clazz = characterClasses.get(classType);
                return clazz.getConstructor(Player.class).newInstance(player);
            } catch (Exception e) {
                throw new RuntimeException("无法创建角色实例", e);
            }
        }

        public int getWeightForClass(ClassType classType) {
            return classWeights.getOrDefault(classType, 0);
        }

        public Set<ClassType> getAvailableClasses() {
            return classWeights.keySet();
        }
    }

    // 单例实例
    private static final DrawCareerClass INSTANCE = new DrawCareerClass();

    // 游戏数据
    private final List<GameCharacter> allCharacters;
    private final Map<ClassType, List<GameCharacter>> classToCharacters;
    private final Set<GameCharacter> drawnCharacters = new HashSet<>();
    private final Set<ClassType> availableClasses = new HashSet<>();
    private final Random random = new Random();

    private DrawCareerClass() {
        // 初始化角色数据
        allCharacters = Arrays.asList(
                new GameCharacter("美树沙耶香")
                        .addClass(ClassType.SABER, 1, SayakaSaber.class)
                        .addClass(ClassType.BERSERKER, 2, SayakaBerserker.class)
                        .addClass(ClassType.RIDER, 2, SayakaRider.class),
                new GameCharacter("佐仓杏子")
                        .addClass(ClassType.LANCER, 1, KyokoLancer.class)
                        .addClass(ClassType.CASTER, 2, KyokoCaster.class),
                new GameCharacter("鹿目圆")
                        .addClass(ClassType.ARCHER,1, MadokaArcher.class),
                new GameCharacter("晓美焰")
                        .addClass(ClassType.ASSASSIN,1, HomuraAssassin.class)
                        .addClass(ClassType.ARCHER,2, HomuraArcher.class),
                new GameCharacter("巴麻美")
                        .addClass(ClassType.RIDER,1, MamiRider.class)
                        .addClass(ClassType.ARCHER,2, MamiArcher.class)
                        .addClass(ClassType.CASTER,3, MamiCaster.class)
        );

        // 构建职阶到角色的映射
        classToCharacters = new EnumMap<>(ClassType.class);
        for (ClassType classType : ClassType.values()) {
            classToCharacters.put(classType, new ArrayList<>());
        }

        for (GameCharacter character : allCharacters) {
            for (ClassType classType : character.getAvailableClasses()) {
                classToCharacters.get(classType).add(character);
            }
        }
    }

    private void reset() {

    }

    public static DrawCareerClass getInstance() {
        return INSTANCE;
    }

    public List<ClassType> getActiveClassTypes() {
        List<ClassType> activeClasses = new ArrayList<>();

        if (ArcherSign.isArcherActive()) {
            activeClasses.add(ClassType.ARCHER);
        }
        if (LancerSign.isLancerActive()) {
            activeClasses.add(ClassType.LANCER);
        }
        if (RiderSign.isRiderActive()) {
            activeClasses.add(ClassType.RIDER);
        }
        if (BerserkerSign.isBerserkerActive()) {
            activeClasses.add(ClassType.BERSERKER);
        }
        if (CasterSign.isCasterActive()) {
            activeClasses.add(ClassType.CASTER);
        }
        if (AssassinSign.isAssassinActive()) {
            activeClasses.add(ClassType.ASSASSIN);
        }
        if (SaberSign.isSaberActive()) {
            activeClasses.add(ClassType.SABER);
        }

        return activeClasses;
    }

    // 从激活的职阶中随机抽取一个职阶
    public ClassType drawRandomActiveClass() {
        List<ClassType> activeClasses = new ArrayList<>(availableClasses);
        if (activeClasses.isEmpty()) {
            throw new IllegalStateException("没有激活的职阶可供抽取");
        }
        ClassType drawnClass = activeClasses.get(random.nextInt(activeClasses.size()));
        availableClasses.remove(drawnClass);
        return drawnClass;
    }




    // 核心方法：加权不重复抽取
    public GameCharacter drawWeightedUniqueCharacter(ClassType classType) {
        // 获取可用角色（未抽取过的）
        List<GameCharacter> availableChars = classToCharacters.get(classType).stream()
                .filter(c -> !drawnCharacters.contains(c))
                .collect(Collectors.toList());

        if (availableChars.isEmpty()) {
            throw new IllegalStateException("没有可用的唯一角色用于职阶: " + classType.getDisplayName());
        }

        // 计算总权重（角色权重 × 职阶基础权重）
        int totalWeight = availableChars.stream()
                .mapToInt(c -> c.getWeightForClass(classType) * classType.getBaseWeight())
                .sum();

        // 加权随机选择
        int randomWeight = random.nextInt(totalWeight);
        int accumulatedWeight = 0;

        for (GameCharacter character : availableChars) {
            int charWeight = character.getWeightForClass(classType) * classType.getBaseWeight();
            accumulatedWeight += charWeight;

            if (randomWeight < accumulatedWeight) {
                drawnCharacters.add(character);
                return character;
            }
        }

        // 理论上不会执行到这里
        return availableChars.get(0);
    }

    // 重置抽取记录
    public void resetDrawnCharactersAndClasses() {
        drawnCharacters.clear();
        availableClasses.clear();
        availableClasses.addAll(getActiveClassTypes());
    }


    // 获取已抽取的角色
    public Set<GameCharacter> getDrawnCharacters() {
        return new HashSet<>(drawnCharacters);
    }

    // 检查角色是否已被抽取
    public boolean isCharacterDrawn(GameCharacter character) {
        return drawnCharacters.contains(character);
    }

    // 示例用法
    public static void main(String[] args) {
        DrawCareerClass system = DrawCareerClass.getInstance();

        // 模拟多次抽取
        for (int i = 1; i <= 8; i++) {
            try {
                // 随机选择一个职阶类型
                ClassType[] classes = ClassType.values();
                ClassType randomClass = classes[system.random.nextInt(classes.length)];

                // 加权不重复抽取
                GameCharacter character = system.drawWeightedUniqueCharacter(randomClass);

                System.out.printf("抽取%d: 职阶-%s, 角色-%s (权重: %d×%d)%n",
                        i,
                        randomClass.getDisplayName(),
                        character.getName(),
                        character.getWeightForClass(randomClass),
                        randomClass.getBaseWeight());
            } catch (IllegalStateException e) {
                System.out.println("抽取失败: " + e.getMessage());
            }
        }

        // 显示已抽取的角色
        System.out.println("\n已抽取的角色:");
        system.getDrawnCharacters().forEach(c ->
                System.out.println("- " + c.getName())
        );

        // 重置后可以重新抽取
        system.resetDrawnCharactersAndClasses();
        System.out.println("\n系统重置后，可以重新抽取角色");
    }
}