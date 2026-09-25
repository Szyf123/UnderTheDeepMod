package udmodpack;

import arc.math.Mathf;
import arc.struct.EnumSet;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.Autotiler;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquids;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BuildVisibility;

/** 卤素发生器：无深度效率，两种液体独立库存。
 *  3 francium + 12 freshWater → 12 atomicHalogen，耗电 1W，耗时 1.6s。
 */
public class UdHalogenatorFactory extends Block implements Autotiler{

    // ---- 生产参数（硬编码） ----
    static final float CRAFT_TIME = 2.8f;
    static final float POWER_USE = 1f / 60f;     // 1W
    static final int   ITEM_CAP = 20;             // francium 物品容量
    static final float WATER_CAP = 40f;           // freshWater 独立容量
    static final float HALOGEN_CAP = 40f;         // atomicHalogen 独立容量
    static final int   SIZE = 3;

    static final int   FRANCIUM_INPUT = 2;        // 一轮吃 2
    static final float WATER_INPUT = 0.4f;         // 一轮吃 24
    static final float HALOGEN_OUTPUT = 15f;      // 一轮产 15

    public UdHalogenatorFactory(String name) {
        super(name);

        this.size = SIZE;
        this.update = true;
        this.solid = true;
        this.category = Category.crafting;
        this.buildVisibility = BuildVisibility.shown;
        this.flags = EnumSet.of(BlockFlag.factory);
        this.buildType = UdHalogenatorBuilding::new;
        this.buildTime = 90f;
        requirements(Category.crafting, new ItemStack[]{
            new ItemStack(mindustry.content.Items.copper, 10),
        });

        // 电力
        this.hasPower = true;
        this.consumeBuilder.add(new ConsumePower(POWER_USE, 0f, false));

        // 物品（francium）
        this.hasItems = true;
        this.itemCapacity = ITEM_CAP;
        this.consumeBuilder.add(new ConsumeItems(new ItemStack[]{
            new ItemStack(UDContent.francium, FRANCIUM_INPUT)
        }));

        // 液体（两种，consumeBuilder 只声明输入）
        this.hasLiquids = true;
        this.liquidCapacity = WATER_CAP + HALOGEN_CAP;
        this.consumeBuilder.add(new ConsumeLiquids(new LiquidStack[]{
            new LiquidStack(UDContent.freshWater, WATER_INPUT)
        }));
        // 产出 atomicHalogen 液体 → 通知原版管道
        this.outputsLiquid = true;
    }

    /** 工厂液体/物品全方向输入，不是只在正面，所以 rotatedOutput=false。 */
    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    /** 原版管道接触本工厂时查此方法决定是否画拐角。 */
    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.hasLiquids;
    }

    // ========================================================================

    public class UdHalogenatorBuilding extends Building {

        float progress;

        @Override
        public void updateTile() {
            // 先 dump 产物腾空间（避免满了就停）
            dumpLiquid(UDContent.atomicHalogen);

            if (!enabled) return;
            if (efficiency <= 0f) return;

            // 输入检查
            if (items.get(UDContent.francium) < FRANCIUM_INPUT) return;
            if (liquids.get(UDContent.freshWater) < WATER_INPUT) return;

            // 输出空间检查
            if (liquids.get(UDContent.atomicHalogen) + HALOGEN_OUTPUT > HALOGEN_CAP) return;

            progress += (delta() / 60f) * efficiency;
            if (progress >= CRAFT_TIME) craft();
        }

        private void craft() {
            progress = 0f;
            items.remove(UDContent.francium, FRANCIUM_INPUT);
            liquids.remove(UDContent.freshWater, WATER_INPUT);
            liquids.add(UDContent.atomicHalogen, HALOGEN_OUTPUT);
        }

        @Override
        public boolean dump() { return false; }

        @Override
        public boolean acceptItem(mindustry.gen.Building source, mindustry.type.Item item) {
            if (item != UDContent.francium) return false;
            return items.get(item) < ITEM_CAP;
        }

        @Override
        public boolean acceptLiquid(mindustry.gen.Building source, Liquid liquid) {
            // 两种液体独立容量检查
            if (liquid == UDContent.freshWater) {
                return liquids.get(liquid) < WATER_CAP;
            }
            if (liquid == UDContent.atomicHalogen) {
                return liquids.get(liquid) < HALOGEN_CAP;
            }
            return false;
        }

        @Override
        public float progress() { return Mathf.clamp(progress / CRAFT_TIME); }

        @Override
        public boolean shouldConsume() {
            if (items.get(UDContent.francium) < FRANCIUM_INPUT) return false;
            if (liquids.get(UDContent.freshWater) < WATER_INPUT) return false;
            // 输出满了 → 停消耗（ConsumeLiquids 会停止自动扣液体）
            // 但 acceptLiquid 不受此影响，管道仍可继续送入液体
            if (liquids.get(UDContent.atomicHalogen) + HALOGEN_OUTPUT > HALOGEN_CAP) return false;
            return enabled;
        }
    }
}
