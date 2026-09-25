package udmodpack;

import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.graphics.CacheLayer;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;

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

    // ===== 自定义液体 =====
    public static UdBasicLiquid freshWater;
    public static UdBasicLiquid seaweedOil;
    public static UdBasicLiquid atomicHalogen;

    public static UdCellLiquid rootGel;

    // ===== 自定义物品（注册引用） =====
    public static UdBadicItem wreckageAlloy;
    public static UdBadicItem wreckageGlass;
    public static UdBadicItem wreckageFiber;
    public static UdBadicItem root;
    public static UdBadicItem precCoreFight;
    public static UdBadicItem precCoreExist;
    public static UdBadicItem precCoreMigrate;
    public static UdBadicItem precCoreFinal;

    public static UdBadicItem seaweedBundle;
    public static UdBadicItem tuberculosis;
    public static UdBadicItem iron;
    public static UdBadicItem silicaSand;
    public static UdBadicItem manganese;
    public static UdBadicItem aurum;
    public static UdBadicItem nickel;
    public static UdBadicItem francium;

    public static UdBadicItem chip;
    public static UdBadicItem manganeseSteel;
    public static UdBadicItem pressedGlass;
    public static UdBadicItem advancedChip;
    public static UdBadicItem shieldAlloy;
    public static UdBadicItem rlyehAlloy;

    public static UdBadicItem gunpowderChip;
    public static UdBadicItem gunpowderManganeseSteel;
    public static UdBadicItem gunpowderAdvancedChip;
    public static UdBadicItem gunpowderShieldAlloy;
    public static UdBadicItem gunpowderFrancium;
    public static UdBadicItem gunpowderRlyehAlloy;

    // 配对用引用
    public static UdBasicFloor shallowBasic;
    public static UdBasicFloor shallowBasicInfectious;
    public static UdBasicFloor shallowSand;
    public static UdBasicFloor shallowSandInfectious;
    public static UdBasicFloor deepBasic;
    public static UdBasicFloor deepBasicInfectious;
    public static UdBasicFloor deepSand;
    public static UdBasicFloor deepSandInfectious;
    public static UdBasicFloor abyssalBasic;
    public static UdBasicFloor abyssalBasicInfectious;
    public static UdBasicFloor abyssalHole;
    public static UdBasicFloor abyssalHoleInfectious;

    // 矿石叠加块
    public static UdBasicOre ironOre;
    public static UdBasicOre aurumOre;
    public static UdBasicOre tuberculosisOre;
    public static UdBasicOre nickelOre;
    public static UdBasicOre franciumOre;

    public static UdOverlayFloor wreckageOre;
    public static UdOverlayFloor seaweedOre;

    public static UdCustomDrill hydraulicDrill;
    public static UdCustomDrill electricalDrill;
    public static UdCustomDrill preciseDrill;

    public static UdHarvester researchStation;
    public static UdHarvester seaweedHarvester;

    // 边界虚空地板（环境墙）— 复刻 metal-tiles 4方向 autotile
    public static UdVoidFloor udTestVoidFloor;

    // 环境墙
    public static UdCliff shallowCliff;
    public static UdCliff deepCliff;

    // 液体地板
    public static UdBasicLiquidFloor deepHalogenOcean;

    public static void registerAll() {
        // 测试：虚空地板（tileable 平铺，solid=true + drownTime=30f 内置）
        udTestVoidFloor = new UdVoidFloor("ud-test-void-floor");

        // 浅海环境墙：8 张 64×64 piece，depth==SHALLOW(0) 的邻居视为相同 → 不渲染 edge
        shallowCliff = new UdCliff("ud-shallow-cliff", DepthManager.SHALLOW);
        deepCliff = new UdCliff("ud-deep-cliff", DepthManager.DEEP);

        // 深海卤素海洋：atomicHalogen 液体，DEEP 深度
        deepHalogenOcean = new UdBasicLiquidFloor(
            "ud-deep-halogen-ocean",
            4, DEEP,
            atomicHalogen,
            true, true
        );

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
        deepSand = new UdBasicFloor(
            "ud-deep-sand-floor",
            3, DEEP,
            true, true,
            false, true
        );
        // itemDrop 赋值移到 silicaSand 注册之后
        deepSand.playerUnmineable = true;
        deepBasicInfectious = new UdBasicFloor(
            "ud-deep-basic-infecitious-floor",
            6, DEEP,
            true, true,
            true, false
        );
        deepSandInfectious = new UdBasicFloor(
            "ud-deep-sand-infecitious-floor",
            3, DEEP,
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
        // itemDrop 赋值移到 silicaSand 注册之后
        shallowSand.playerUnmineable = true;
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

        // === 自定义液体 ===
        freshWater = new UdBasicLiquid("ud-fresh-water", Color.valueOf("#B2E9FA"), true, 0.5f, 0.4f, 0.5f, true, true);
        seaweedOil = new UdBasicLiquid("ud-seaweed-oil", Color.valueOf("#E5C258"), false, 0.6f, 0.8f, 0.2f, true, true);
        atomicHalogen = new UdBasicLiquid("ud-atomic-halogen", Color.valueOf("#C3F71F"), false, 0.2f, 0.4f, 0.5f, false, true);

        rootGel = new UdCellLiquid("ud-root-gel", Color.valueOf("#9578ED"), 0.6f, 0.2f, 0.8f, false, true, 0.5f, 5, Color.valueOf("#D1ADFF"), Color.valueOf("#472982"));

        // === 自定义物品 ===
        wreckageAlloy = new UdBadicItem("ud-wreckage-alloy", Color.valueOf("#C3F71F"), 1, false, 3, 20f);
        wreckageGlass = new UdBadicItem("ud-wreckage-glass", Color.valueOf("#C3F71F"), 1, false, 3, 20f);
        wreckageFiber = new UdBadicItem("ud-wreckage-fiber", Color.valueOf("#C3F71F"), 1, false, 3, 20f);
        root = new UdBadicItem("ud-root", Color.valueOf("#9578ED"), 1, false, 0, 0f);

        precCoreFight = new UdBadicItem("ud-prec-core-fight", Color.valueOf("#D06B53"), 1, false, 0, 0f);
        precCoreExist = new UdBadicItem("ud-prec-core-exist", Color.valueOf("#62AE7F"), 1, false, 0, 0f);
        precCoreMigrate = new UdBadicItem("ud-prec-core-migrate", Color.valueOf("#665C9F"), 1, false, 0, 0f);
        precCoreFinal = new UdBadicItem("ud-prec-core-final", Color.valueOf("#000000"), 1, false, 0, 0f);

        seaweedBundle = new UdBadicItem("ud-seaweed-bundle", Color.valueOf("#E5C258"), 5, false, 0, 0f);
        tuberculosis = new UdBadicItem("ud-tuberculosis", Color.valueOf("#945C48"), 1, false, 0, 0f);
        iron = new UdBadicItem("ud-iron", Color.valueOf("#7F7F7F"), 1, false, 0, 0f);
        silicaSand = new UdBadicItem("ud-silica-sand", Color.valueOf("#F2EBDB"), 1, true, 0, 0f);
        manganese = new UdBadicItem("ud-manganese", Color.valueOf("#A4DE79"), 5, false, 0, 0f);
        aurum = new UdBadicItem("ud-aurum", Color.valueOf("#F6F742"), 3, false, 0, 0f);
        nickel = new UdBadicItem("ud-nickel", Color.valueOf("#82B482"), 3, false, 0, 0f);
        francium = new UdBadicItem("ud-francium", Color.valueOf("#9EDECC"), 4, false, 0, 0f);

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

        // === Floor 的 itemDrop 赋值（必须在所有 Item 注册之后！） ===
        deepSand.itemDrop = silicaSand;
        shallowSand.itemDrop = silicaSand;

        // === 矿石叠加块 ===
        ironOre = new UdBasicOre("ud-iron-ore", iron, 4);
        tuberculosisOre = new UdBasicOre("ud-tuberculosis-ore", tuberculosis, 4);
        aurumOre = new UdBasicOre("ud-aurum-ore", aurum, 4);
        nickelOre = new UdBasicOre("ud-nickel-ore", nickel, 4);
        franciumOre = new UdBasicOre("ud-francium-ore", francium, 4);

        // === 覆盖装饰块 ===
        wreckageOre = new UdOverlayFloor("ud-wreckage-ore", 4, 5, 12f);
        seaweedOre = new UdOverlayFloor("ud-seaweed-ore", 3, 1, 12f);

        // === 自定义钻头 ===
        hydraulicDrill = new UdCustomDrill("ud-dri-hydraulic", 2, 1, 60f, 90f, 0.5f, 0.05f,
            seaweedOil, 0.8f,
            iron, 800f,
            tuberculosis, 800f,
            silicaSand, 460f,
            new mindustry.type.ItemStack(mindustry.content.Items.copper, 30),
            new mindustry.type.ItemStack(mindustry.content.Items.lead, 20)
        );
        electricalDrill = new UdCustomDrill("ud-dri-electrical", 3, 3, 60f, 90f, 1.2f, 0.12f,
            seaweedOil, 1.8f,
            aurum, 760f,
            nickel, 660f,
            iron, 450f,
            tuberculosis, 450f,
            silicaSand, 280f,
            new mindustry.type.ItemStack(mindustry.content.Items.copper, 30),
            new mindustry.type.ItemStack(mindustry.content.Items.lead, 20)
        );
        preciseDrill = new UdCustomDrill("ud-dri-precise", 4, 4, 60f, 90f, 2f, 0.2f,
            seaweedOil, 3f,
            francium, 680f,
            aurum, 420f,
            nickel, 380f,
            iron, 240f,
            tuberculosis, 240f,
            silicaSand, 180f,
            new mindustry.type.ItemStack(mindustry.content.Items.copper, 30),
            new mindustry.type.ItemStack(mindustry.content.Items.lead, 20)
        );

        // === 收割机（科研工作站） ===
        researchStation = new UdHarvester("ud-research-station", 3, 5, 90f, 1.5f,
            wreckageOre,
            new mindustry.type.Item[]{ wreckageAlloy, wreckageGlass, wreckageFiber },
            180f,
            new ItemStack(mindustry.content.Items.copper, 20),
            new ItemStack(mindustry.content.Items.lead, 15)
        );
        seaweedHarvester = new UdHarvester("ud-seaweed-harvester", 2, 4, 90f, 1.5f,
            seaweedOre,
            new mindustry.type.Item[]{ seaweedBundle },
            180f,
            new ItemStack(mindustry.content.Items.copper, 20),
            new ItemStack(mindustry.content.Items.lead, 15)
        );

        // === 装药合成厂（6配方，2x3 UI） ===
        new UdGunpowderFactory("ud-gunpowder-factory");

        new UdDepthDrivenFactory(
            "ud-fac-Ionization",
            2,
            1.0f, 0f, 0.05f,
            1f,
            1f,
            0, null,
            50f, null,
            null,
            new LiquidStack[]{ new LiquidStack(freshWater, 12f) },
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );
        new UdDepthDrivenFactory(
            "ud-fac-sorter",
            2,
            1.0f, -0.1f, 0.05f,
            2.5f,
            0f,
            10, new ItemStack[]{ new ItemStack(tuberculosis, 2) },
            0f, null,
            new ItemStack[]{ new ItemStack(manganese, 2) },
            null,
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );
        new UdDepthDrivenFactory(
            "ud-fac-etching",
            2,
            1.0f, -0.1f, 0.65f,
            1.5f,
            1f,
            10, new ItemStack[]{ new ItemStack(iron, 1), new ItemStack(manganese, 2) },
            0f, null,
            new ItemStack[]{ new ItemStack(chip, 1) },
            null,
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );
        new UdDepthDrivenFactory(
            "ud-fac-compressor",
            2,
            0.6f, 0.05f, 0.05f,
            2.4f,
            1f,
            10, new ItemStack[]{ new ItemStack(iron, 2), new ItemStack(manganese, 2) },
            20f, new LiquidStack[]{ new LiquidStack(freshWater, 0.1f) },
            new ItemStack[]{ new ItemStack(manganeseSteel, 1) },
            null,
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );
        new UdDepthDrivenFactory(
            "ud-fac-fermentation-tank",
            2,
            0.6f, 0.05f, 0.05f,
            2f,
            1f,
            20, new ItemStack[]{ new ItemStack(seaweedBundle, 6) },
            30f, null,
            null,
            new LiquidStack[]{ new LiquidStack(seaweedOil, 18f) },
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );
        new UdDepthDrivenFactory(
            "ud-fac-glass-kiln",
            2,
            0.35f, 0.08f, 0.05f,
            2f,
            1f,
            20, new ItemStack[]{ new ItemStack(silicaSand, 1) },
            0f, null,
            new ItemStack[]{ new ItemStack(pressedGlass, 1) },
            null,
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );
        new UdDepthDrivenFactory(
            "ud-fac-advanced-etching",
            3,
            1.0f, -0.05f, 0.5f,
            3.6f,
            1f,
            20, new ItemStack[]{ new ItemStack(aurum, 1), new ItemStack(manganese, 3), new ItemStack(silicaSand, 6) },
            0f, null,
            new ItemStack[]{ new ItemStack(advancedChip, 1) },
            null,
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );
        new UdDepthDrivenFactory(
            "ud-fac-charger",
            3,
            1.0f, -0.05f, 0.05f,
            2f,
            1f,
            20, new ItemStack[]{ new ItemStack(manganeseSteel, 4), new ItemStack(aurum, 2) },
            30f, new LiquidStack[]{ new LiquidStack(rootGel, 0.2f) },
            new ItemStack[]{ new ItemStack(shieldAlloy, 1) },
            null,
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );

        // === 培养机 ===
        new UdRootFactory("ud-fac-incubator");

        // === 生物反应器 ===
        new UdBioreactorFactory("ud-fac-bioreactor");

        // === 原子卤化釜 ===
        new UdHalogenatorFactory("ud-fac-halogenator");

        new UdDepthDrivenFactory(
            "ud-fac-rlyeh-foundry",
            4,
            0.2f, 0.05f, 0.9f,
            5.2f,
            1f,
            30, new ItemStack[]{ new ItemStack(nickel, 5), new ItemStack(advancedChip, 3) },
            30f, new LiquidStack[]{ new LiquidStack(atomicHalogen, 0.12f) },
            new ItemStack[]{ new ItemStack(rlyehAlloy, 1) },
            null,
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );

        // === 精准核心合成厂（多配方工厂） ===
        new UdPrecCoreFactory("ud-prec-core-factory");

        new UdDepthDrivenFactory(
            "ud-advanced-core-factory",
            4,
            0.2f, 0.025f, 0.9f,
            10f,
            1f,
            100, new ItemStack[]{ new ItemStack(precCoreFight, 50), new ItemStack(precCoreExist, 50), new ItemStack(precCoreMigrate, 50) },
            50f, new LiquidStack[]{ new LiquidStack(rootGel, 0.25f), new LiquidStack(atomicHalogen, 0.4f) },
            new ItemStack[]{ new ItemStack(precCoreFinal, 1) },
            null,
            new ItemStack[]{ new ItemStack(Items.copper, 10) },
            90f
        );
    }
}
