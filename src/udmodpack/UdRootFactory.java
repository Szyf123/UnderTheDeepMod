package udmodpack;

import arc.math.Mathf;
import arc.struct.EnumSet;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquids;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BuildVisibility;

/** 培养机：每秒一轮，耗电、耗淡水、耗1 ud-root → 产15 ud-root。 */
public class UdRootFactory extends Block {

    // ---- 生产参数（硬编码） ----
    static final float CRAFT_TIME = 1f;          // 1 秒一轮
    static final float POWER_USE = 5f / 60f;     // 5W
    static final int   ITEM_CAP = 50;
    static final float LIQUID_CAP = 30f;
    static final float WATER_PER_CRAFT = 6f;      // 一轮消耗 6 淡水
    static final int   ROOT_INPUT = 1;            // 一轮吃 1
    static final int   ROOT_OUTPUT = 15;          // 一轮产 15
    static final int   SIZE = 2;

    public UdRootFactory(String name) {
        super(name);

        this.size = SIZE;
        this.update = true;
        this.solid = true;
        this.category = Category.production;
        this.buildVisibility = BuildVisibility.shown;
        this.flags = EnumSet.of(BlockFlag.factory);
        this.buildType = UdRootFactoryBuilding::new;
        this.buildTime = 120f;
        requirements(Category.production, new ItemStack[]{
            new ItemStack(mindustry.content.Items.copper, 40),
            new ItemStack(mindustry.content.Items.lead, 20),
            new ItemStack(mindustry.content.Items.silicon, 15),
        });

        // 电力
        this.hasPower = true;
        this.consumeBuilder.add(new ConsumePower(POWER_USE, 0f, false));

        // 物品
        this.hasItems = true;
        this.itemCapacity = ITEM_CAP;
        this.consumeBuilder.add(new ConsumeItems(new ItemStack[]{
            new ItemStack(UDContent.root, ROOT_INPUT)
        }));

        // 液体
        this.hasLiquids = true;
        this.liquidCapacity = LIQUID_CAP;
        this.consumeBuilder.add(new ConsumeLiquids(new LiquidStack[]{
            new LiquidStack(UDContent.freshWater, WATER_PER_CRAFT)
        }));
    }

    // ========================================================================

    public class UdRootFactoryBuilding extends Building {

        float progress;

        @Override
        public void updateTile() {
            // ✅ 无条件输出，不受生产状态影响 — 让槽内 root 持续流出
            dump(UDContent.root);
            dumpLiquid(UDContent.freshWater);

            if (!enabled) return;
            if (efficiency <= 0f) return;

            // 输入检查
            if (items.get(UDContent.root) < ROOT_INPUT) return;
            if (liquids.get(UDContent.freshWater) < WATER_PER_CRAFT) return;

            // 输出空间检查
            if (items.get(UDContent.root) + ROOT_OUTPUT > ITEM_CAP) return;

            progress += (delta() / 60f) * efficiency;
            if (progress >= CRAFT_TIME) craft();
        }

        private void craft() {
            progress = 0f;
            items.remove(UDContent.root, ROOT_INPUT);
            liquids.remove(UDContent.freshWater, WATER_PER_CRAFT);
            items.add(UDContent.root, ROOT_OUTPUT);
        }

        @Override
        public boolean dump() { return false; }

        @Override
        public boolean acceptItem(mindustry.gen.Building source, mindustry.type.Item item) {
            if (item != UDContent.root) return false;
            return items.get(item) < ITEM_CAP;
        }

        @Override
        public boolean acceptLiquid(mindustry.gen.Building source, Liquid liquid) {
            if (liquid != UDContent.freshWater) return false;
            return liquids.get(liquid) < LIQUID_CAP;
        }

        @Override
        public float progress() { return Mathf.clamp(progress / CRAFT_TIME); }

        @Override
        public boolean shouldConsume() {
            if (items.get(UDContent.root) < ROOT_INPUT) return false;
            if (liquids.get(UDContent.freshWater) < WATER_PER_CRAFT) return false;
            if (items.get(UDContent.root) + ROOT_OUTPUT > ITEM_CAP) return false;
            return enabled;
        }
    }
}
