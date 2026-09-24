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

/** 生物反应器：无深度效率，两种液体独立库存。
 *  3 root + 12 atomicHalogen → 12 rootGel，耗电 1W，耗时 1.6s。
 */
public class UdBioreactorFactory extends Block {

    // ---- 生产参数（硬编码） ----
    static final float CRAFT_TIME = 1.6f;       // 1.6 秒一轮
    static final float POWER_USE = 1f / 60f;     // 1W
    static final int   ITEM_CAP = 20;            // root 物品容量
    static final float HALOGEN_CAP = 40f;        // atomicHalogen 独立容量
    static final float GEL_CAP = 40f;            // rootGel 独立容量
    static final int   SIZE = 2;

    static final int   ROOT_INPUT = 3;           // 一轮吃 3
    static final float HALOGEN_INPUT = 0.2f;      // 一轮吃 12
    static final float GEL_OUTPUT = 12f;         // 一轮产 12

    public UdBioreactorFactory(String name) {
        super(name);

        this.size = SIZE;
        this.update = true;
        this.solid = true;
        this.category = Category.crafting;
        this.buildVisibility = BuildVisibility.shown;
        this.flags = EnumSet.of(BlockFlag.factory);
        this.buildType = UdBioreactorBuilding::new;
        this.buildTime = 90f;
        requirements(Category.crafting, new ItemStack[]{
            new ItemStack(mindustry.content.Items.copper, 10),
        });

        // 电力
        this.hasPower = true;
        this.consumeBuilder.add(new ConsumePower(POWER_USE, 0f, false));

        // 物品（root）
        this.hasItems = true;
        this.itemCapacity = ITEM_CAP;
        this.consumeBuilder.add(new ConsumeItems(new ItemStack[]{
            new ItemStack(UDContent.root, ROOT_INPUT)
        }));

        // 液体（两种，consumeBuilder 只声明输入）
        this.hasLiquids = true;
        // 物理总容量 = 两种独立容量之和（各自独立软上限在 accept/检查里）
        this.liquidCapacity = HALOGEN_CAP + GEL_CAP;
        this.consumeBuilder.add(new ConsumeLiquids(new LiquidStack[]{
            new LiquidStack(UDContent.atomicHalogen, HALOGEN_INPUT)
        }));
    }

    // ========================================================================

    public class UdBioreactorBuilding extends Building {

        float progress;

        @Override
        public void updateTile() {
            // 无条件输出 rootGel，不受生产状态影响
            dumpLiquid(UDContent.rootGel);

            if (!enabled) return;
            if (efficiency <= 0f) return;

            // 输入检查
            if (items.get(UDContent.root) < ROOT_INPUT) return;
            if (liquids.get(UDContent.atomicHalogen) < HALOGEN_INPUT) return;

            // 输出空间检查（独立容量：gel 不能超 GEL_CAP）
            if (liquids.get(UDContent.rootGel) + GEL_OUTPUT > GEL_CAP) return;

            progress += (delta() / 60f) * efficiency;
            if (progress >= CRAFT_TIME) craft();
        }

        private void craft() {
            progress = 0f;
            items.remove(UDContent.root, ROOT_INPUT);
            liquids.remove(UDContent.atomicHalogen, HALOGEN_INPUT);
            liquids.add(UDContent.rootGel, GEL_OUTPUT);
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
            // 两种液体独立容量检查
            if (liquid == UDContent.atomicHalogen) {
                return liquids.get(liquid) < HALOGEN_CAP;
            }
            if (liquid == UDContent.rootGel) {
                return liquids.get(liquid) < GEL_CAP;
            }
            return false;
        }

        @Override
        public float progress() { return Mathf.clamp(progress / CRAFT_TIME); }

        @Override
        public boolean shouldConsume() {
            if (items.get(UDContent.root) < ROOT_INPUT) return false;
            if (liquids.get(UDContent.atomicHalogen) < HALOGEN_INPUT) return false;
            if (liquids.get(UDContent.rootGel) + GEL_OUTPUT > GEL_CAP) return false;
            return enabled;
        }
    }
}
