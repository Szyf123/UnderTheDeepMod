package udmodpack;

import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.mod.*;

/** 模组入口 + 全局数据注册中心。 */
public class UDModMain extends Mod {

    // ===== 全局公开数据：地板感染配对 =====
    public static final ObjectMap<UdBasicFloor, UdBasicFloor> infectPair = new ObjectMap<>();
    public static final ObjectMap<UdBasicFloor, UdBasicFloor> curePair = new ObjectMap<>();

    public static void addFloorPair(UdBasicFloor clean, UdBasicFloor infected) {
        infectPair.put(clean, infected);
        curePair.put(infected, clean);
    }

    @Override
    public void loadContent() {
        UDContent.registerAll();
        addFloorPair(UDContent.shallowBasic, UDContent.shallowBasicInfectious);
        addFloorPair(UDContent.shallowSand, UDContent.shallowSandInfectious);
        addFloorPair(UDContent.deepBasic, UDContent.deepBasicInfectious);
        addFloorPair(UDContent.deepSand, UDContent.deepSandInfectious);
        addFloorPair(UDContent.abyssalBasic, UDContent.abyssalBasicInfectious);
        addFloorPair(UDContent.abyssalHole, UDContent.abyssalHoleInfectious);
        InfectionManager.init();
        DepthManager.init();

        UDPlanet.init();
        UDTechTree.load();

        // 注册同心圆布局事件 hook
        RadialLayoutHook.init();
        Log.info("[UDMod] All content + hooks registered.");
    }
}
