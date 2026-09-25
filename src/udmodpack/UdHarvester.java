package udmodpack;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.Autotiler;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.BlockGroup;

import static mindustry.Vars.tilesize;

/**
 * 收割机（Harvester）：方向性开采建筑。检测前方 drillsize×drillsize 范围内的 overlay 地块，
 * 匹配 targetblock 类型，每 drilltime/n 帧随机产出一个 drillitem。
 *
 * 扫描区域：以建筑前方 offset = (size + drillsize) / 2 格为中心的 drillsize×drillsize 矩形
 * （要求 size 和 drillsize 奇偶性一致，保证 offset 和中心坐标都是整数）。
 */
public class UdHarvester extends Block implements Autotiler{

    /** 扫描范围边长（tiles），必须和 size 奇偶性一致 */
    public int drillSize;
    /** 要匹配的覆盖层地块（overlay） */
    public Floor targetBlock;
    /** 可产出的物品池（均匀随机） */
    public Item[] drillItems;
    /** 每产出 1 个物品的基础时间（帧）。实际间隔 = drilltime / n（n=匹配地块数） */
    public float drilltime;
    /** 每秒耗电（power），<=0 则不耗电 */
    public float powerConsume;

    /** 扫描区域前方偏移（tiles），= (size + drillSize) / 2 */
    public int frontOffset() {
        return (size + drillSize) / 2;
    }

    public UdHarvester(String name, int size, int drillSize, float buildTime, float powerConsume,
                       Floor targetBlock, Item[] drillItems, float drilltime) {
        super(name);
        this.size = size;
        this.drillSize = drillSize;
        this.buildTime = buildTime;
        this.powerConsume = powerConsume;
        this.targetBlock = targetBlock;
        this.drillItems = drillItems;
        this.drilltime = drilltime;

        this.update = true;
        this.solid = true;
        this.rotate = true;
        this.group = BlockGroup.drills;
        this.hasItems = true;
        this.category = Category.production;

        if(powerConsume > 0f) {
            this.hasPower = true;
            consumePowerCond(powerConsume, build -> {
                UdHarvesterBuild hb = (UdHarvesterBuild) build;
                return hb.matchedCount > 0 && hb.items.total() < hb.block.itemCapacity;
            });
        }

        this.buildType = UdHarvesterBuild::new;
    }

    public UdHarvester(String name, int size, int drillSize, float buildTime, float powerConsume,
                       Floor targetBlock, Item[] drillItems, float drilltime,
                       ItemStack... reqs) {
        this(name, size, drillSize, buildTime, powerConsume, targetBlock, drillItems, drilltime);
        if(reqs.length > 0) requirements(Category.production, reqs);
    }

    /** UdHarvester 物品通过 dump() 全方向输出，不是只朝正面，所以 rotatedOutput=false。 */
    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    /** 原版传送带/物品桥接触 UdHarvester 时会查 UdHarvester.blends() 决定是否画拐角。 */
    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        // 接受任何有物品能力的邻居（传送带、物品桥等）
        return otherblock.outputsItems() || otherblock.acceptsItems;
    }

    /** 鼠标拿起放置预览时，显示前方扫描范围虚线框。 */
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);

        float centerX = x * tilesize + offset;
        float centerY = y * tilesize + offset;
        int f = frontOffset();
        float scanCx = centerX + Geometry.d4x(rotation) * f * tilesize;
        float scanCy = centerY + Geometry.d4y(rotation) * f * tilesize;

        Drawf.dashSquare(Color.white.cpy().a(0.4f), scanCx, scanCy, drillSize * tilesize);
    }

    public class UdHarvesterBuild extends Building {

        /** 前方扫描区域内匹配 targetBlock 的 tile 数 */
        public int matchedCount;
        /** 用于缓存匹配 tile 位置（避免每帧重新扫描） */
        public final Seq<Tile> matchedTiles = new Seq<>();
        /** 累积进度（帧） */
        public float progress;

        /** 扫描区域前方偏移（tiles），= (size + drillSize) / 2 */
        public int frontOffset() {
            return (size + drillSize) / 2;
        }

        @Override
        public void onProximityUpdate() {
            rescan();
        }

        /** 重新扫描前方区域，统计匹配 overlay 的 tile 数 */
        public void rescan() {
            matchedTiles.clear();
            matchedCount = 0;

            // Building.tileX() / tileY() 已经是 multiblock 的中心 tile（挂建筑的那个 tile）
            int cx = tileX();
            int cy = tileY();
            int f = frontOffset();
            int dx = Geometry.d4x(rotation) * f;
            int dy = Geometry.d4y(rotation) * f;
            int sx = cx + dx;
            int sy = cy + dy;

            int half = drillSize / 2;
            for(int ox = -half; ox <= half; ox++) {
                for(int oy = -half; oy <= half; oy++) {
                    Tile t = Vars.world.tile(sx + ox, sy + oy);
                    if(t != null && t.overlay() == targetBlock) {
                        matchedTiles.add(t);
                        matchedCount++;
                    }
                }
            }
        }

        /** 随机选一个 drillitem */
        public Item pickRandomDrillItem() {
            int idx = Mathf.random(drillItems.length - 1);
            return drillItems[idx];
        }

        @Override
        public void updateTile() {
            // 每帧重扫一次前方区域，确保附近 overlay 变化或建筑旋转后能立即反映
            // （Tile.setOverlay 只调本 build 的 onProximityUpdate，不向周围传播）
            rescan();

            // 物品输出
            if(timer(timerDump, dumpTime / timeScale())) {
                dump(items.first());
            }

            if(matchedCount <= 0 || drillItems == null || drillItems.length == 0 || efficiency <= 0f) return;

            if(items.total() >= itemCapacity) return;

            // 每 drilltime / matchedCount 帧产出一个（匹配越多越快）
            float interval = drilltime / matchedCount;
            progress += delta() * efficiency;

            if(progress >= interval) {
                int amount = (int)(progress / interval);
                for(int i = 0; i < amount && items.total() < itemCapacity; i++) {
                    offload(pickRandomDrillItem());
                }
                progress %= interval;
            }
        }

        @Override
        public float progress() {
            if(matchedCount <= 0 || drilltime <= 0f) return 0f;
            float interval = drilltime / matchedCount;
            return Mathf.clamp(progress / interval);
        }

        @Override
        public void draw() {
            if(region.found()) {
                Draw.rect(region, x, y, rotdeg());
            }
        }

        /** 选中已放置的建筑时，显示扫描范围虚线框。 */
        @Override
        public void drawSelect() {
            UdHarvester block = (UdHarvester) this.block;
            int f = block.frontOffset();
            float scanCx = x + Geometry.d4x(rotation) * f * tilesize;
            float scanCy = y + Geometry.d4y(rotation) * f * tilesize;

            Drawf.dashSquare(Color.white.cpy().a(0.4f), scanCx, scanCy, block.drillSize * tilesize);
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            progress = read.f();
        }
    }
}
