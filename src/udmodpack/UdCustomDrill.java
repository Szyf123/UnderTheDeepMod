package udmodpack;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectFloatMap;
import arc.struct.ObjectSet;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.consumers.ConsumeLiquid;

/**
 * 完全自定义的钻头，继承 Block（而非 Drill），绕过原版 Drill 硬编码的 liquidBoost/warmup 逻辑。
 *
 * 构造参数:
 * <pre>
 * new UdCustomDrill(name, size, tier, drillTime, powerConsume, boostLiquidUse,
 *     // Item + Float:  指定物品的挖掘时间（帧/个），不写的默认用 drillTime
 *     itemA, 30f,
 *     itemB, 120f,
 *     // Liquid + Float:  液体 boost，倍率（>=1 加速）
 *     Liquids.water, 1.6f,
 *     // ItemStack:  建造需求
 *     new ItemStack(Items.copper, 30),
 *     new ItemStack(Items.lead, 20)
 * );
 * </pre>
 */
public class UdCustomDrill extends Block {

    /** 可挖的最大 hardness（Item.hardness <= tier） */
    public int tier;
    /** 默认挖掘时间，帧/个（没有在 itemDrillTimes 里单独指定的物品用这个） */
    public float drillTime;
    /** 每个物品单独指定的挖掘时间（帧/个）。不在此 Map 里的物品用 drillTime。 */
    public ObjectFloatMap<Item> itemDrillTimes = new ObjectFloatMap<>();
    /** boost 液体 → 速度倍率（>=1 加速，1f 等于不加速） */
    public ObjectFloatMap<Liquid> boostMultipliers = new ObjectFloatMap<>();
    /** boost 液体消耗速率（每秒多少单位液体） */
    public float boostLiquidUse;
    /** 钻头旋转速度（弧度/帧，默认 2） */
    public float rotateSpeed = 2f;
    /** 中心菱形（itemRegion）是否显示 */
    public boolean drawMineItem = true;
    /** 可挖 tile 之间做 dominantItem 决策时排除的物品 */
    public ObjectSet<Item> blockedItems;

    // 纹理：原版 drill-item-N 菱形
    public TextureRegion itemRegion;
    public TextureRegion rotatorRegion;
    public TextureRegion topRegion;

    public UdCustomDrill(String name, int size, int tier, float drillTime,
                         float buildTime, float powerConsume, float boostLiquidUse,
                         Object... rest) {
        super(name);
        this.size = size;
        this.tier = tier;
        this.drillTime = drillTime;
        this.buildTime = buildTime;
        this.boostLiquidUse = boostLiquidUse;

        this.update = true;
        this.solid = true;
        this.group = BlockGroup.drills;
        this.hasItems = true;
        this.category = Category.production;
        this.buildVisibility = BuildVisibility.shown;

        if(powerConsume > 0f) {
            this.hasPower = true;
            consumePower(powerConsume);
        }

        // 解析 rest: Item+Float 效率对, Liquid+Float boost 对, ItemStack 需求
        ItemStack[] reqs = parseRest(rest);
        if(reqs.length > 0) {
            requirements(Category.production, reqs);
        }

        // 注册 boost liquid consumers
        for(var entry : boostMultipliers) {
            if(boostLiquidUse > 0f) {
                consumeLiquid(entry.key, boostLiquidUse).boost();
            }
        }

        this.buildType = UdCustomDrillBuild::new;
    }

    /** 统一解析 rest 混合数组，返回 ItemStack 需求数组；同时填充 itemDrillTimes 和 boostMultipliers。 */
    private ItemStack[] parseRest(Object... rest) {
        int reqCount = 0;
        for(Object o : rest) if(o instanceof ItemStack) reqCount++;
        ItemStack[] reqs = new ItemStack[reqCount];
        int ri = 0;
        for(int i = 0; i < rest.length; i++) {
            Object o = rest[i];
            if(o instanceof Item item && i + 1 < rest.length && rest[i + 1] instanceof Float f) {
                itemDrillTimes.put(item, f);
                i++;
            } else if(o instanceof Liquid liq && i + 1 < rest.length && rest[i + 1] instanceof Float f) {
                boostMultipliers.put(liq, f);
                i++;
            } else if(o instanceof ItemStack stack) {
                reqs[ri++] = stack;
            }
        }
        return reqs;
    }

    @Override
    public void init() {
        super.init();
        // 加载原版 Drill 的菱形纹理（带 fallback）
        itemRegion = Core.atlas.find(name + "-item", "drill-item-" + size);
        rotatorRegion = Core.atlas.find(name + "-rotator", "drill-rotator");
        topRegion = Core.atlas.find(name + "-top", "drill-top");
    }

    /** overlay 优先，floor 兜底。只返回 tier 能挖动的物品。 */
    public Item resolveDrop(Tile tile) {
        Floor overlay = tile.overlay();
        if(overlay != null && overlay != Blocks.air && overlay.itemDrop != null) {
            Item d = overlay.itemDrop;
            if(d.hardness <= tier && (blockedItems == null || !blockedItems.contains(d))) return d;
        }
        Floor floor = tile.floor();
        if(floor != null && floor.itemDrop != null) {
            Item d = floor.itemDrop;
            if(d.hardness <= tier && (blockedItems == null || !blockedItems.contains(d))) return d;
        }
        return null;
    }

    public boolean canMine(Tile tile) {
        if(tile == null || tile.block().isStatic()) return false;
        return resolveDrop(tile) != null;
    }

    /** 挖掘时间（帧/个）。未在 itemDrillTimes 里指定的物品用默认 drillTime。 */
    public float getMineTime(Item item) {
        return itemDrillTimes.get(item, drillTime);
    }

    @Override
    public boolean canPlaceOn(Tile tile, mindustry.game.Team team, int rotation) {
        if(isMultiblock()) {
            for(Tile other : tile.getLinkedTilesAs(this, tempTiles)) {
                if(canMine(other)) return true;
            }
            return false;
        } else {
            return canMine(tile);
        }
    }

    @Override
    public void drawPlanConfig(BuildPlan plan, arc.util.Eachable<BuildPlan> list) {
        if(!plan.worldContext || !drawMineItem) return;
        Tile tile = plan.tile();
        if(tile == null) return;
        // 简单版：显示 dominantItem 颜色的菱形
        Item dom = null;
        int cnt = 0;
        for(Tile t : tile.getLinkedTilesAs(this, tempTiles)) {
            Item d = resolveDrop(t);
            if(d != null) { dom = d; cnt++; }
        }
        if(dom != null && cnt > 0) {
            arc.graphics.g2d.Draw.color(dom.color);
            arc.graphics.g2d.Draw.rect(itemRegion, plan.drawx(), plan.drawy());
            arc.graphics.g2d.Draw.color();
        }
    }

    public class UdCustomDrillBuild extends Building {

        public Item dominantItem;
        public int dominantCount;
        public float progress;
        public float timeDrilled;

        /** 当前 boost 倍率（1f = 无 boost） */
        public float currentBoost = 1f;

        @Override
        public void onProximityUpdate() {
            dominantItem = null;
            dominantCount = 0;
            int sx = tileX(), sy = tileY();
            for(int dx = 0; dx < size; dx++) {
                for(int dy = 0; dy < size; dy++) {
                    Tile t = Vars.world.tile(sx + dx, sy + dy);
                    if(t == null) continue;
                    Item d = resolveDrop(t);
                    if(d == null) continue;
                    if(dominantItem == null) {
                        dominantItem = d;
                        dominantCount = 1;
                    } else if(d == dominantItem) {
                        dominantCount++;
                    } else if(!d.lowPriority && dominantItem.lowPriority) {
                        dominantItem = d;
                        dominantCount = 1;
                    }
                }
            }
        }

        /** 更新 boost 倍率：遍历 boostMultipliers，检查对应 liquid 的实际效率 */
        public void updateBoost() {
            if(boostMultipliers.size == 0) {
                currentBoost = 1f;
                return;
            }
            // boostMultipliers 里每个条目对应一个 boost consumer
            // optionalEfficiency 已经包含了所有 optional consumer（包括 boost）的状态
            // 但我们需要自己算倍率，因为 Drill 里的 lerp 逻辑我们不用
            float totalBoost = 1f;
            for(var entry : boostMultipliers) {
                Liquid liq = entry.key;
                float mult = entry.value;
                // 查这个 liquid 对应的 consumer 效率
                for(var cons : block.consumers) {
                    if(cons instanceof ConsumeLiquid cl && cl.liquid == liq && cl.optional && cl.booster) {
                        if(cl.efficiency(self()) > 0f) {
                            totalBoost *= mult;
                        }
                    }
                }
            }
            currentBoost = totalBoost;
        }

        @Override
        public void updateTile() {
            // 液体输出/物品输出处理
            if(timer(timerDump, dumpTime / timeScale())) {
                dump(dominantItem != null && items.has(dominantItem) ? dominantItem : null);
            }

            if(dominantItem == null) return;

            timeDrilled += delta();
            updateBoost();

            if(items.total() < itemCapacity && dominantCount > 0 && efficiency > 0f) {
                float mineTime = getMineTime(dominantItem);
                float speed = efficiency * currentBoost; // 干净！没有 warmup，没有 lerp
                progress += delta() * dominantCount * speed;

                if(progress >= mineTime) {
                    int amount = (int)(progress / mineTime);
                    for(int i = 0; i < amount; i++) {
                        offload(dominantItem);
                    }
                    progress %= mineTime;
                }
            }
        }

        @Override
        public float progress() {
            if(dominantItem == null) return 0f;
            return arc.math.Mathf.clamp(progress / getMineTime(dominantItem));
        }

        @Override
        public void draw() {
            // 建筑主纹理
            if(region.found()) Draw.rect(region, x, y);
            else Draw.rect(Core.atlas.find(name), x, y);

            // 旋转钻头
            if(rotatorRegion != null && rotatorRegion.found()) {
                Drawf_spinSprite(rotatorRegion, x, y, timeDrilled * rotateSpeed);
            }

            // 顶层
            if(topRegion != null && topRegion.found()) {
                Draw.rect(topRegion, x, y);
            }

            // 中心菱形（挖掘物品颜色）
            if(dominantItem != null && drawMineItem) {
                Draw.color(dominantItem.color);
                Draw.rect(itemRegion, x, y);
                Draw.color();
            }
        }

        // —— Drawf.spinSprite 的等价实现，避免额外 import ——
        private void Drawf_spinSprite(TextureRegion region, float x, float y, float rot) {
            // 这个方法在 mindustry.graphics.Drawf 里，但我们避免依赖它
            // 直接用 Draw.rect 即可（原版也是这样实现的）
            Draw.rect(region, x, y, rot);
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);
            write.f(timeDrilled);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            progress = read.f();
            timeDrilled = read.f();
        }
    }
}
