package udmodpack;

import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.graphics.g3d.PlanetGrid;
import mindustry.type.ItemStack;
import mindustry.type.Sector;

import static udmodpack.UDContent.*;

/**
 * 模组科技树集中构建。
 *
 * 所有 TechNode 在此处一次性创建，父子关系、研究消耗、objectives 等都在这里指定。
 * 构造函数通过 {@code new TechNode(parent.techNode, content, content.researchRequirements())}
 * 创建节点，原版 TechNode 构造函数会自动：
 *   1. parent.children.add(this)   — 挂到父节点
 *   2. content.techNode = this     — 反向引用
 *   3. depth = parent.depth + 1    — 深度自动递增
 *
 * 加载时机：在 UDContent.registerAll() 和 UDPlanet.init() 之后调用。
 * 原版 SerpuloTechTree.load() 先于模组执行，Blocks.coreNucleus.techNode 已存在。
 *
 * 构建顺序按深度从浅到深，保证父节点 techNode 在子节点创建时已赋值。
 * 所有 Item/Liquid 默认 researchRequirements() 返回 ItemStack.empty（零消耗），
 * 通过生产解锁，与原版 Item 行为一致。
 */
public class UDTechTree {

    public static void load() {
        // === 拉莱耶星球根节点 ===
        // UdTechPlaceholder 在 UDContent.registerAll() 里已创建（planet=null, sector=null）。
        // 这里补关联星球和 Sector（PlanetDialog 需要 sector 非空）。
        UDContent.rlyehPlaceholder.initialize(UDPlanet.deep, 0);
        // 给占位节点创建一个 dummy Sector（PlanetDialog.show 需要 preset.sector 不为 null）
        if(UDPlanet.deep.grid != null && UDPlanet.deep.grid.tiles.length > 0) {
            PlanetGrid.Ptile tile = UDPlanet.deep.grid.tiles[0];
            UDContent.rlyehPlaceholder.sector = new Sector(UDPlanet.deep, tile);
        }
        TechNode rlyehRoot = TechTree.nodeRoot("ud-rlyeh", UDContent.rlyehPlaceholder, () -> {});
        rlyehRoot.requiresUnlock = false;
        rlyehRoot.planet = UDPlanet.deep;
        UDPlanet.deep.techTree = rlyehRoot;

        // === 钻头 ===
        TechNode hydraulicDrillNode             = new TechNode(rlyehRoot, hydraulicDrill, ItemStack.empty);
        TechNode electricalDrillNode            = new TechNode(hydraulicDrillNode, electricalDrill, ItemStack.empty);
        TechNode preciseDrillNode               = new TechNode(electricalDrillNode, preciseDrill, ItemStack.empty);

        // === 收割机 ===
        TechNode seaweedHarvesterNode           = new TechNode(hydraulicDrillNode, seaweedHarvester, ItemStack.empty);
        TechNode rootHarvesterNode              = new TechNode(seaweedHarvesterNode, rootHarvester, ItemStack.empty);
        TechNode researchStationNode            = new TechNode(hydraulicDrillNode, researchStation, ItemStack.empty);
        TechNode precCoreFactoryNode            = new TechNode(researchStationNode, precCoreFactory, ItemStack.empty);
        TechNode advancedCoreFactoryNode        = new TechNode(precCoreFactoryNode, advancedCoreFactory, ItemStack.empty);

        // === 工厂 ===
        // === 第1层 ===
        TechNode sorterNode                     = new TechNode(rlyehRoot, sorter, ItemStack.empty);
        // === 第2层 ===
        TechNode gunpowderFactoryNode           = new TechNode(sorterNode, gunpowderFactory, ItemStack.empty);
        TechNode glassKilnNode                  = new TechNode(sorterNode, glassKiln, ItemStack.empty);
        TechNode etchingNode                    = new TechNode(sorterNode, etching, ItemStack.empty);
        TechNode compressorNode                 = new TechNode(sorterNode, compressor, ItemStack.empty);
        // === 第3层 ===
        TechNode ionizationNode                 = new TechNode(glassKilnNode, ionization, ItemStack.empty);
        TechNode advancedEtchingNode            = new TechNode(etchingNode, advancedEtching, ItemStack.empty);
        TechNode chargerNode                    = new TechNode(compressorNode, charger, ItemStack.empty);
        // === 第4层 ===
        TechNode fermentationTankNode           = new TechNode(ionizationNode, fermentationTank, ItemStack.empty);
        TechNode rlyehFoundryNode               = new TechNode(chargerNode, rlyehFoundry, ItemStack.empty);
        // === 第5层 ===
        TechNode bioreactorNode                 = new TechNode(fermentationTankNode, bioreactor, ItemStack.empty);
        TechNode incubatorNode                  = new TechNode(fermentationTankNode, incubator, ItemStack.empty);
        // === 第6层 ===
        TechNode halogenatorNode                = new TechNode(bioreactorNode, halogenator, ItemStack.empty);

        // === 减压场 ===
        TechNode depressurizationBasicNode       = new TechNode(rlyehRoot, depressurizationBasic, ItemStack.empty);
        TechNode depressurizationAdvancedNode    = new TechNode(depressurizationBasicNode, depressurizationAdvanced, ItemStack.empty);
        TechNode depressurizationUltimateNode    = new TechNode(depressurizationAdvancedNode, depressurizationUltimate, ItemStack.empty);
        TechNode infectionLauncherNode           = new TechNode(depressurizationBasicNode, infectionLauncher, ItemStack.empty);

        // === 物品 ===
        // === 第1层 ===
        TechNode ironNode                       = new TechNode(rlyehRoot, iron, ItemStack.empty);
        // === 第2层 ===
        TechNode tuberculosisNode               = new TechNode(ironNode, tuberculosis, ItemStack.empty);
        TechNode silicaSandNode                 = new TechNode(ironNode, silicaSand, ItemStack.empty);
        TechNode nickelNode                     = new TechNode(ironNode, nickel, ItemStack.empty);
        TechNode rootNode                       = new TechNode(ironNode, root, ItemStack.empty);
        TechNode seaweedBundleNode              = new TechNode(ironNode, seaweedBundle, ItemStack.empty);
        // === 第3层 ===
        TechNode manganeseNode                  = new TechNode(tuberculosisNode, manganese, ItemStack.empty);
        TechNode pressedGlassNode               = new TechNode(silicaSandNode, pressedGlass, ItemStack.empty);
        TechNode aurumNode                      = new TechNode(nickelNode, aurum, ItemStack.empty);
        TechNode wreckageAlloyNode              = new TechNode(rootNode, wreckageAlloy, ItemStack.empty);
        TechNode wreckageGlassNode              = new TechNode(rootNode, wreckageGlass, ItemStack.empty);
        TechNode wreckageFiberNode              = new TechNode(rootNode, wreckageFiber, ItemStack.empty);
        TechNode gunpowderChipNode              = new TechNode(seaweedBundleNode, gunpowderChip, ItemStack.empty);
        TechNode gunpowderManganeseSteelNode    = new TechNode(seaweedBundleNode, gunpowderManganeseSteel, ItemStack.empty);
        TechNode gunpowderAdvancedChipNode      = new TechNode(seaweedBundleNode, gunpowderAdvancedChip, ItemStack.empty);
        TechNode gunpowderShieldAlloyNode       = new TechNode(seaweedBundleNode, gunpowderShieldAlloy, ItemStack.empty);
        TechNode gunpowderFranciumNode          = new TechNode(seaweedBundleNode, gunpowderFrancium, ItemStack.empty);
        TechNode gunpowderRlyehAlloyNode        = new TechNode(seaweedBundleNode, gunpowderRlyehAlloy, ItemStack.empty);
        TechNode precCoreFinalNode              = new TechNode(rootNode, precCoreFinal, ItemStack.empty);
        // === 第4层 ===
        TechNode chipNode                       = new TechNode(manganeseNode, chip, ItemStack.empty);
        TechNode manganeseSteelNode             = new TechNode(manganeseNode, manganeseSteel, ItemStack.empty);
        TechNode franciumNode                   = new TechNode(aurumNode, francium, ItemStack.empty);
        TechNode precCoreFightNode              = new TechNode(wreckageAlloyNode, precCoreFight, ItemStack.empty);
        TechNode precCoreMigrateNode            = new TechNode(wreckageGlassNode, precCoreMigrate, ItemStack.empty);
        TechNode precCoreExistNode              = new TechNode(wreckageFiberNode, precCoreExist, ItemStack.empty);
        // === 第5层 ===
        TechNode advancedChipNode               = new TechNode(chipNode, advancedChip, ItemStack.empty);
        TechNode shieldAlloyNode                = new TechNode(manganeseSteelNode, shieldAlloy, ItemStack.empty);
        // === 第6层 ===
        TechNode rlyehAlloyNode                 = new TechNode(shieldAlloyNode, rlyehAlloy, ItemStack.empty);

        // === 液体 ===
        TechNode freshWaterNode                 = new TechNode(rlyehRoot, freshWater, ItemStack.empty);
        TechNode seaweedOilNode                 = new TechNode(freshWaterNode, seaweedOil, ItemStack.empty);
        TechNode atomicHalogenNode              = new TechNode(freshWaterNode, atomicHalogen, ItemStack.empty);
        TechNode rootGelNode                    = new TechNode(atomicHalogenNode, rootGel, ItemStack.empty);

        arc.util.Log.info("[UDTechTree] Tech tree built successfully.");
    }
}
