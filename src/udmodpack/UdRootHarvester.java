package udmodpack;

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
import mindustry.world.meta.BlockGroup;

import static mindustry.Vars.tilesize;

/**
 * 根须收割机（RootHarvester）。绘制方式参考 UnderTheDeepMod 正常版本。
 */
public class UdRootHarvester extends Block implements Autotiler{

    public int drillSize;
    public Item drillItem;
    public float drilltime;
    public float powerConsume;

    public int frontOffset() {
        return (size + drillSize) / 2;
    }

    public UdRootHarvester(String name, int size, int drillSize, float buildTime, float powerConsume,
                           Item drillItem, float drilltime) {
        super(name);
        this.size = size;
        this.drillSize = drillSize;
        this.buildTime = buildTime;
        this.powerConsume = powerConsume;
        this.drillItem = drillItem;
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
                UdRootHarvesterBuild hb = (UdRootHarvesterBuild) build;
                return hb.matchedCount > 0 && hb.items.total() < hb.block.itemCapacity;
            });
        }

        this.buildType = UdRootHarvesterBuild::new;
    }

    public UdRootHarvester(String name, int size, int drillSize, float buildTime, float powerConsume,
                           Item drillItem, float drilltime, ItemStack... reqs) {
        this(name, size, drillSize, buildTime, powerConsume, drillItem, drilltime);
        if(reqs.length > 0) requirements(Category.production, reqs);
    }

    @Override
    public boolean rotatedOutput(int x, int y){ return false; }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.outputsItems() || otherblock.acceptsItems;
    }

    public int[] scanTileBounds(int ax, int ay, int rotation) {
        int d = drillSize;
        int perp = (size - d) / 2;

        int sx0, sy0;
        switch(rotation & 3) {
            case 0: sx0 = ax + size;    sy0 = ay + perp; break;
            case 1: sx0 = ax + perp;    sy0 = ay + size; break;
            case 2: sx0 = ax - d;       sy0 = ay + perp; break;
            default:sx0 = ax + perp;    sy0 = ay - d;
        }
        return new int[]{sx0, sy0, sx0 + d - 1, sy0 + d - 1};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);

        float centerX = x * tilesize + offset;
        float centerY = y * tilesize + offset;
        int f = frontOffset();
        float scanCx = centerX + Geometry.d4x(rotation) * f * tilesize;
        float scanCy = centerY + Geometry.d4y(rotation) * f * tilesize;

        Drawf.dashSquare(Color.white.cpy().a(0.5f), scanCx, scanCy, drillSize * tilesize);
    }

    public class UdRootHarvesterBuild extends Building {

        public int matchedCount;
        public final Seq<Tile> matchedTiles = new Seq<>();
        public float progress;

        public int frontOffset() {
            return (size + drillSize) / 2;
        }

        @Override
        public void onProximityUpdate() {
            rescan();
        }

        public void rescan() {
            matchedTiles.clear();
            matchedCount = 0;

            int ax = tileX() - (size - 1) / 2;
            int ay = tileY() - (size - 1) / 2;
            UdRootHarvester b = (UdRootHarvester) block;
            int[] bounds = b.scanTileBounds(ax, ay, rotation);

            for(int tx = bounds[0]; tx <= bounds[2]; tx++) {
                for(int ty = bounds[1]; ty <= bounds[3]; ty++) {
                    Tile t = Vars.world.tile(tx, ty);
                    if(t != null && t.floor() instanceof UdBasicFloor && ((UdBasicFloor) t.floor()).isInfected) {
                        matchedTiles.add(t);
                        matchedCount++;
                    }
                }
            }
        }

        @Override
        public void updateTile() {
            rescan();

            if(timer(timerDump, dumpTime / timeScale())) {
                dump(items.first());
            }

            if(matchedCount <= 0 || drillItem == null || efficiency <= 0f) return;
            if(items.total() >= itemCapacity) return;

            float interval = drilltime / matchedCount;
            progress += delta() * efficiency;

            if(progress >= interval) {
                int amount = (int)(progress / interval);
                for(int i = 0; i < amount && items.total() < itemCapacity; i++) {
                    offload(drillItem);
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

        @Override
        public void drawSelect() {
            UdRootHarvester block = (UdRootHarvester) this.block;
            int f = block.frontOffset();
            float scanCx = x + Geometry.d4x(rotation) * f * tilesize;
            float scanCy = y + Geometry.d4y(rotation) * f * tilesize;

            Drawf.dashSquare(Color.white.cpy().a(0.5f), scanCx, scanCy, block.drillSize * tilesize);
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
