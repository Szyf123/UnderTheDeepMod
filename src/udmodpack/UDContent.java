package udmodpack;

import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.type.ItemStack;

import static mindustry.Vars.tilesize;
import static udmodpack.UdBasicFloor.*;

/** 所有内容注册集中在此。
 *  UdBasicFloor: (name, variants, depthDefault, drawEdgeOut, drawEdgeIn, isInfected, canBeInfected)
 *  UDDepressurization: (name, size, depressurizationSize, buildCost[], buildTimeTicks,
 *    powerUsePerSecond, itemConsumption[], liquidType, liquidPerSecond) */
public class UDContent {

    // ===== 自定义特效 =====
    /** 净化生成特效：方形轮廓从中心向外扩散并淡出。 */
    public static final Effect cureGenerate = new Effect(22, e -> {
        // fin: 0→1 生命周期进度
        float fin = e.fin();
        // 边长从 tilesize 向外扩到 tilesize * 3
        float half = tilesize * 0.5f * (1f + fin * 4f);
        // 颜色从青→透明
        Color c = Color.cyan.cpy().a(1f - fin);
        // 线宽随时间变细
        float lineWidth = 3f * (1f - fin) + 0.5f;

        // 方形轮廓线（从中心向外扩）
        Drawf.square(e.x, e.y, half, lineWidth, c);
    });

    // ===== 自定义物品（注册引用） =====
    public static UdBadicItem wreckageAlloy;
    public static UdBadicItem wreckageGlass;
    public static UdBadicItem wreckageFiber;
    public static UdBadicItem precCoreFight;
    public static UdBadicItem precCoreMigrate;
    public static UdBadicItem precCoreExist;
    public static UdBadicItem precCoreFinal;

    // ===== 新物品 =====
    public static UdBadicItem manganese;
    public static UdBadicItem iron;
    public static UdBadicItem nickel;
    public static UdBadicItem chip;
    public static UdBadicItem root;
    public static UdBadicItem tuberculosis;
    public static UdBadicItem manganeseSteel;
    public static UdBadicItem rlyehAlloy;
    public static UdBadicItem shieldAlloy;
    public static UdBadicItem advancedChip;
    public static UdBadicItem gunpowderChip;
    public static UdBadicItem gunpowderManganeseSteel;
    public static UdBadicItem gunpowderRlyehAlloy;
    public static UdBadicItem gunpowderAdvancedChip;
    public static UdBadicItem gunpowderShieldAlloy;
    public static UdBadicItem pressedGlass;
    public static UdBadicItem silicaSand;
    public static UdBadicItem aurum;
    public static UdBadicItem francium;
    public static UdBadicItem gunpowderFrancium;

    // 配对用引用
    public static UdBasicFloor shallowBasic;
    public static UdBasicFloor shallowBasicInfectious;
    public static UdBasicFloor shallowSand;
    public static UdBasicFloor shallowSandInfectious;
    public static UdBasicFloor deepBasic;
    public static UdBasicFloor deepBasicInfectious;
    public static UdBasicFloor abyssalBasic;
    public static UdBasicFloor abyssalBasicInfectious;
    public static UdBasicFloor abyssalHole;
    public static UdBasicFloor abyssalHoleInfectious;

    public static void registerAll() {
        // 金属地板
        new UdBasicFloor(
            "ud-test-basic-floor",
            4, SHALLOW,
            false, false,
            false, false
        );
        
        // 深渊
        abyssalBasic = new UdBasicFloor(
            "ud-abyssal-basic-floor",
            4, ABYSSAL,
            true, true,
            false, true
        );
        abyssalHole = new UdBasicFloor(
            "ud-abyssal-hole-floor",
            3, ABYSSAL,
            false, true,
            false, true
        );
        abyssalBasicInfectious = new UdBasicFloor(
            "ud-abyssal-basic-infecitious-floor",
            3, ABYSSAL,
            true, true,
            true, false
        );
        abyssalHoleInfectious = new UdBasicFloor(
            "ud-abyssal-hole-infecitious-floor",
            3, ABYSSAL,
            true, true,
            true, false
        );

        // 深海
        deepBasic = new UdBasicFloor(
            "ud-deep-basic-floor",
            6, DEEP,
            true, true,
            false, true
        );
        deepBasicInfectious = new UdBasicFloor(
            "ud-deep-basic-infecitious-floor",
            6, DEEP,
            true, true,
            true, false
        );
        
        // 浅海
        shallowBasic = new UdBasicFloor(
            "ud-shallow-basic-floor",
            4, SHALLOW,
            true, true,
            false, true
        );
        shallowSand = new UdBasicFloor(
            "ud-shallow-sand-floor",
            4, SHALLOW,
            true, true,
            false, true
        );
        shallowBasicInfectious = new UdBasicFloor(
            "ud-shallow-basic-infecitious-floor",
            3, SHALLOW,
            true, true,
            true, false
        );
        shallowSandInfectious = new UdBasicFloor(
            "ud-shallow-sand-infecitious-floor",
            3, SHALLOW,
            true, true,
            true, false
        );

        // === 减压场（三种变体） ===

        // 基础减压场：2x2，净化12格，只耗电 3W，每秒尝试 4 格，成功率 60%
        new UDDepressurization(
            "ud-depressurization-basic",
            2, 12,
            new ItemStack[]{
                new ItemStack(Items.copper, 20),
                new ItemStack(Items.lead, 10)
            },
            120f,
            3f,
            null,           // 无物品消耗
            null, 0f,       // 无液体消耗
            12, 0.7f         // cureTilesPerSecond, cureChance
        );

        // 高级减压场：3x3，净化21格，耗电 6W + 每秒铜 1，每秒尝试 8 格，成功率 70%
        new UDDepressurization(
            "ud-depressurization-advanced",
            3, 21,
            new ItemStack[]{
                new ItemStack(Items.copper, 50),
                new ItemStack(Items.silicon, 30),
                new ItemStack(Items.titanium, 15)
            },
            180f,
            6f,
            new ItemStack[]{ new ItemStack(Items.copper, 1) },
            null, 0f,
            24, 0.85f
        );

        // 终极减压场：4x4，净化40格，耗电 12W + 每秒铜 2 + 每秒水 4，每秒尝试 16 格，成功率 80%
        new UDDepressurization(
            "ud-depressurization-ultimate",
            4, 40,
            new ItemStack[]{
                new ItemStack(Items.copper, 100),
                new ItemStack(Items.silicon, 60),
                new ItemStack(Items.titanium, 40),
                new ItemStack(Items.phaseFabric, 10)
            },
            300f,
            12f,
            new ItemStack[]{ new ItemStack(Items.copper, 2) },
            Liquids.water, 4f,
            40, 1.0f
        );

        // === 感染发射器（基础占位，后续扩展） ===
        new UDInfectionLauncher("ud-infection-launcher");

        // === 自定义物品 ===
        wreckageAlloy = new UdBadicItem("ud-wreckage-alloy", Color.valueOf("#C3F71F"), 1, false, 3, 20f);
        wreckageGlass = new UdBadicItem("ud-wreckage-glass", Color.valueOf("#C3F71F"), 1, false, 3, 20f);
        wreckageFiber = new UdBadicItem("ud-wreckage-fiber", Color.valueOf("#C3F71F"), 1, false, 3, 20f);
        root = new UdBadicItem("ud-root", Color.valueOf("#9578ED"), 1, false, 0, 0f);

        precCoreFight = new UdBadicItem("ud-prec-core-fight", Color.valueOf("#D06B53"), 1, false, 0, 0f);
        precCoreExist = new UdBadicItem("ud-prec-core-exist", Color.valueOf("#62AE7F"), 1, false, 0, 0f);
        precCoreMigrate = new UdBadicItem("ud-prec-core-migrate", Color.valueOf("#665C9F"), 1, false, 0, 0f);
        precCoreFinal = new UdBadicItem("ud-prec-core-final", Color.valueOf("#000000"), 1, false, 0, 0f);

        tuberculosis = new UdBadicItem("ud-tuberculosis", Color.valueOf("#945C48"), 1, false, 0, 0f);
        iron = new UdBadicItem("ud-iron", Color.valueOf("#7F7F7F"), 1, false, 0, 0f);
        silicaSand = new UdBadicItem("ud-silica-sand", Color.valueOf("#F2EBDB"), 1, false, 0, 0f);
        manganese = new UdBadicItem("ud-manganese", Color.valueOf("#A4DE79"), 1, false, 0, 0f);
        aurum = new UdBadicItem("ud-aurum", Color.valueOf("#F6F742"), 1, false, 0, 0f);
        nickel = new UdBadicItem("ud-nickel", Color.valueOf("#82B482"), 1, false, 0, 0f);
        francium = new UdBadicItem("ud-francium", Color.valueOf("#9EDECC"), 1, false, 0, 0f);

        chip = new UdBadicItem("ud-chip", Color.valueOf("#FFB143"), 1, false, 0, 0f);
        manganeseSteel = new UdBadicItem("ud-manganese-steel", Color.valueOf("#8282B4"), 1, false, 0, 0f);
        pressedGlass = new UdBadicItem("ud-pressed-glass", Color.valueOf("#D0D17A"), 1, false, 0, 0f);
        advancedChip = new UdBadicItem("ud-advanced-chip", Color.valueOf("#ED5557"), 1, false, 0, 0f);
        shieldAlloy = new UdBadicItem("ud-shield-alloy", Color.valueOf("#D6A16E"), 1, false, 0, 0f);
        rlyehAlloy = new UdBadicItem("ud-rlyeh-alloy", Color.valueOf("#216AEB"), 1, false, 0, 0f);

        gunpowderChip = new UdBadicItem("ud-gunpowder-chip", Color.valueOf("#FFB143"), 1, false, 0, 0f);
        gunpowderManganeseSteel = new UdBadicItem("ud-gunpowder-manganese-steel", Color.valueOf("#8282B4"), 1, false, 0, 0f);
        gunpowderAdvancedChip = new UdBadicItem("ud-gunpowder-advanced-chip", Color.valueOf("#ED5557"), 1, false, 0, 0f);
        gunpowderShieldAlloy = new UdBadicItem("ud-gunpowder-shield-alloy", Color.valueOf("#D6A16E"), 1, false, 0, 0f);
        gunpowderFrancium = new UdBadicItem("ud-gunpowder-francium", Color.valueOf("#9EDECC"), 1, false, 0, 0f);
        gunpowderRlyehAlloy = new UdBadicItem("ud-gunpowder-rlyeh-alloy", Color.valueOf("#216AEB"), 1, false, 0, 0f);
        
        // === 精准核心合成厂（多配方工厂） ===
        new UdPrecCoreFactory("ud-prec-core-factory");

        // === 深度测试工厂（调试用） ===
        new UdDepthTestBlock("ud-depth-test-block", 1f, 0.2f, 0f);
    }
}
