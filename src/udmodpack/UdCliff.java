package udmodpack;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.Vars;
import mindustry.graphics.CacheLayer;
import mindustry.world.Block;
import mindustry.world.Tile;

/** 环境墙（autotile 运行时叠加方案）。
 *
 *  【美术输入】8 张 {name}1.png ~ {name}8.png，每张 64×64，每张代表一个方向的 edge piece。
 *  【运行时渲染】drawBase() 中：
 *    1) 检测 8 邻居是否为同类 UdCliff → 生成 bitmask
 *    2) 对 bitmask 里为 1 的 bit，叠加对应的 piece region（Draw.rect 多次）
 *    3) 64×64 画在 32×32 tile 中心 → 自然溢出 16px 平滑过渡
 *
 *  【方向 → bit → piece 映射】
 *      bit0 = 左上 (-1,  1) → piece 1
 *      bit1 = 上   ( 0, -1) → piece 2
 *      bit2 = 右上 ( 1,  1) → piece 3
 *      bit3 = 左   (-1,  0) → piece 4
 *      bit4 = 右   ( 1,  0) → piece 5
 *      bit5 = 左下 (-1, -1) → piece 6
 *      bit6 = 下   ( 0,  1) → piece 7
 *      bit7 = 右下 ( 1, -1) → piece 8
 *
 *  【检测逻辑】其他 block != this → 记为 1；== this → 记为 0。
 *  【构造函数】单参数 UdCliff(String name)，需与 assets 中 piece 文件名前缀一致。
 */
public class UdCliff extends Block {

    private static final int PIECE_COUNT = 8;

    /** piece 覆盖顺序：先画的在底层，后画的在顶层。下标=绘制次序，值=piece index (0..7)。
     *  默认 0,1,2,3,4,5,6,7 表示 bit0(左上)最底 → bit7(右下)最顶。
     *  修改此数组即可调整叠加层级。 */
    public static int[] drawOrder = {0, 2, 5, 7, 1, 3, 4, 6};

    /** 构造函数传入的原始名（不带 Mindustry 自动加的 mod 前缀） */
    private final String baseName;

    /** 邻居 floor 的 depth 等于此值时视为"相同"（不渲染 edge piece）。 */
    private final byte depthThreshold;

    /** 8 个 piece 的 AtlasRegion，下标=bit 位置 */
    private TextureRegion[] pieceRegions;

    public UdCliff(String name, byte depthThreshold) {
        super(name);
        this.baseName = name;
        this.depthThreshold = depthThreshold;

        this.solid = true;
        this.breakable = false;
        this.alwaysReplace = false;
        this.fillsTile = false;
        this.hasShadow = false;
        this.cacheLayer = CacheLayer.walls;

        this.inEditor = true;
        this.alwaysUnlocked = true;
        this.hideDatabase = false;
        this.placeablePlayer = false;
        this.buildVisibility = mindustry.world.meta.BuildVisibility.hidden;

        this.generateIcons = false; // 避免 Block.createIcons() 内部 ClassCast
    }

    @Override
    public void load() {
        super.load();
        if (Vars.headless) return;

        pieceRegions = new TextureRegion[PIECE_COUNT];
        for (int i = 0; i < PIECE_COUNT; i++) {
            // Mindustry sprite packer 命名规则: "ud-mod-" + baseName + N
            pieceRegions[i] = Core.atlas.find("ud-mod-" + baseName + (i + 1));
        }

        // icon: 用左上 piece
        region = pieceRegions[0];
    }

    @Override
    public void drawBase(Tile tile) {
        if (pieceRegions == null) return;

        int mask = 0;
        // 顺序: 左上、上、右上、左、右、左下、下、右下
        if (!isSameCliff(tile, -1,  1)) mask |= 1;   // bit0
        if (!isSameCliff(tile,  0,  1)) mask |= 2;   // bit1
        if (!isSameCliff(tile,  1,  1)) mask |= 4;   // bit2
        if (!isSameCliff(tile, -1,  0)) mask |= 8;   // bit3
        if (!isSameCliff(tile,  1,  0)) mask |= 16;  // bit4
        if (!isSameCliff(tile, -1, -1)) mask |= 32;  // bit5
        if (!isSameCliff(tile,  0, -1)) mask |= 64;  // bit6
        if (!isSameCliff(tile,  1, -1)) mask |= 128; // bit7

        float wx = tile.worldx();
        float wy = tile.worldy();
        // 按 drawOrder 数组顺序叠加：先画的在底层，后画的在顶层
        for (int bit : drawOrder) {
            if ((mask & (1 << bit)) != 0 && pieceRegions[bit].found()) {
                Draw.rect(pieceRegions[bit], wx, wy);
            }
        }
    }

    /** 邻居是否"相同"（不渲染对应 piece）。
     *  规则：UdCliff 同类 → 相同；相邻 floor 的 depth==0（浅海）→ 也视为相同；其他 block/深度 → 不同。
     *  null 邻居（无墙）→ 视为不同，渲染 piece。 */
    private boolean isSameCliff(Tile tile, int dx, int dy) {
        Tile other = tile.nearby(dx, dy);
        if (other == null) return false;                              // null 邻居 → 不同 → 渲染 piece
        if (other.block() == this) return true;                       // 同类 cliff → 相同
        // 新增：相邻 floor 的 depth 等于 depthThreshold 也视为相同（不渲染边缘 piece）
        byte depth = DepthManager.getDefaultDepth(other.floor());
        return depth == depthThreshold;
    }
}
