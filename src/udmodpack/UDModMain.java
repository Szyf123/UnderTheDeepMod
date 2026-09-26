package udmodpack;

import arc.struct.ObjectMap;
import mindustry.mod.*;

/** 模组入口 + 全局数据注册中心。
 *  所有对 infectPair / curePair 的读写都通过本类公开的静态成员进行。 */
public class UDModMain extends Mod {

    // ===== 全局公开数据：地板感染配对 =====
    /** 原地板 → 感染地板（扩散用） */
    public static final ObjectMap<UdBasicFloor, UdBasicFloor> infectPair = new ObjectMap<>();
    /** 感染地板 → 原地板（净化用），与 infectPair 互为反向 */
    public static final ObjectMap<UdBasicFloor, UdBasicFloor> curePair = new ObjectMap<>();

    /** 注册一对地板，同时写入 infectPair + curePair。所有注册都走这里。 */
    public static void addFloorPair(UdBasicFloor clean, UdBasicFloor infected) {
        infectPair.put(clean, infected);
        curePair.put(infected, clean);
    }

    @Override
    public void loadContent() {
        UDContent.registerAll();
        // 地板感染配对
        addFloorPair(UDContent.shallowBasic, UDContent.shallowBasicInfectious);
        addFloorPair(UDContent.shallowSand, UDContent.shallowSandInfectious);
        addFloorPair(UDContent.deepBasic, UDContent.deepBasicInfectious);
        addFloorPair(UDContent.deepSand, UDContent.deepSandInfectious);
        addFloorPair(UDContent.abyssalBasic, UDContent.abyssalBasicInfectious);
        addFloorPair(UDContent.abyssalHole, UDContent.abyssalHoleInfectious);
        InfectionManager.init();
        DepthManager.init();

        // 星球必须在所有 Block/Item/Liquid 之后初始化
        UDPlanet.init();

        // 最后：注册科技树（原版 SerpuloTechTree.load() 已先于模组执行，
        // Blocks.coreNucleus.techNode 此时已存在）
        UDTechTree.load();
    }
}
