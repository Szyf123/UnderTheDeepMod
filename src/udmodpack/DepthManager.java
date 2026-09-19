package udmodpack;

import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

/** 深度数组管理器。
 *  originalDepth[]：WorldLoad 时从 floor.depthDefault 读一次，永不修改。
 *  coverCount[]：待扩展，记录运行中减压场覆盖计数（默认全 0）。 */
public class DepthManager {

    public static final byte SHALLOW = 0;
    public static final byte DEEP    = 1;
    public static final byte ABYSSAL = 2;

    /** 地板原始深度（静态，永不修改） */
    public static byte[] originalDepth;
    /** 被运行中减压场覆盖的计数（默认全 0，后续扩展） */
    public static int[] coverCount;

    private static int worldWidth;
    private static int worldHeight;

    public static void init() {
        Events.on(EventType.WorldLoadEvent.class, e -> rebuild());
    }

    private static void rebuild() {
        if (Vars.world == null) return;
        worldWidth = Vars.world.width();
        worldHeight = Vars.world.height();
        int size = worldWidth * worldHeight;

        originalDepth = new byte[size];
        coverCount = new int[size]; // 默认全 0

        for (int y = 0; y < worldHeight; y++) {
            for (int x = 0; x < worldWidth; x++) {
                Tile tile = Vars.world.tile(x, y);
                originalDepth[pos(x, y)] = getDefaultDepth(tile.floor());
            }
        }
    }

    /** UdBasicFloor 读 depthDefault，原版默认 ABYSSAL(2)。 */
    public static byte getDefaultDepth(Floor floor) {
        if (floor instanceof UdBasicFloor) return ((UdBasicFloor) floor).depthDefault;
        return ABYSSAL;
    }

    /** 获取某坐标当前有效深度（coverCount>0 视为 0，否则用 originalDepth）。O(1)。 */
    public static byte getEffectiveDepth(int x, int y) {
        if (originalDepth == null || coverCount == null) return ABYSSAL;
        int p = pos(x, y);
        if (p < 0 || p >= originalDepth.length) return ABYSSAL;
        return coverCount[p] > 0 ? 0 : originalDepth[p];
    }

    /** 方形范围 coverCount++（减压场开始工作时调用）。 */
    public static void addCoverage(int cx, int cy, int fullSize) {
        if (coverCount == null) return;
        int half = fullSize / 2;
        int minX = cx - half, maxX = cx + half;
        int minY = cy - half, maxY = cy + half;
        for (int y = minY; y <= maxY; y++) {
            if (y < 0 || y >= worldHeight) continue;
            for (int x = minX; x <= maxX; x++) {
                if (x < 0 || x >= worldWidth) continue;
                coverCount[pos(x, y)]++;
            }
        }
    }

    /** 方形范围 coverCount--（减压场停止工作或销毁时调用）。 */
    public static void removeCoverage(int cx, int cy, int fullSize) {
        if (coverCount == null) return;
        int half = fullSize / 2;
        int minX = cx - half, maxX = cx + half;
        int minY = cy - half, maxY = cy + half;
        for (int y = minY; y <= maxY; y++) {
            if (y < 0 || y >= worldHeight) continue;
            for (int x = minX; x <= maxX; x++) {
                if (x < 0 || x >= worldWidth) continue;
                int p = pos(x, y);
                if (coverCount[p] > 0) coverCount[p]--;
            }
        }
    }

    private static int pos(int x, int y) {
        return x + y * worldWidth;
    }
}
