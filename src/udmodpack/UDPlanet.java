package udmodpack;

import arc.*;
import arc.graphics.Color;
import mindustry.content.Planets;
import mindustry.graphics.g3d.HexMesh;
import mindustry.type.Planet;

/** 模组自定义星球「拉莱耶」定义。 */
public class UDPlanet {

    /** 全局公开引用，方便其他代码（科技树、区块预设）访问 */
    public static Planet deep;

    /** 创建并配置星球。在所有 Block/Item/Liquid 注册之后、科技树和 SectorPreset 之前调用。 */
    public static void init() {
        // ===== 构造函数：完全对齐 Serpulo =====
        deep = new Planet("ud-rlyeh", Planets.sun, 1f, 3);

        // ===== 让大气能显示的三件事 =====
        // 1. 强制打开大气开关
        Core.settings.put("atmosphere", true);
        // 2. clipRadius（正数才能通过 frustum 检测）
        deep.clipRadius = 4f;
        // 3. 大气参数（完全对齐 Serpulo）
        deep.atmosphereColor = Color.valueOf("4A6880");
        deep.atmosphereRadIn = 0.02f;
        deep.atmosphereRadOut = 0.3f;
        deep.hasAtmosphere = true;

        // ===== 自定义地形 =====
        // 只改 generator 和 meshLoader，其他全对齐 Serpulo
        deep.generator = new UDRlyehPlanetGenerator();
        deep.meshLoader = () -> new HexMesh(deep, 6);

        // ===== 列表 icon =====
        deep.iconColor = Color.valueOf("3399FF");

        // ===== 其他：对齐 Serpulo =====
        deep.alwaysUnlocked = true;
        deep.accessible = true;
        deep.startSector = 0;
    }
}
