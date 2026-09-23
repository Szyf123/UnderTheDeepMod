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

import static mindustry.Vars.*;

/** 受深度影响的单配方工厂（通用参数化实现）。
 *
 *  效率公式：actualEfficiency = baseEfficiency + depthSum * depthMultiplier
 *            if (actualEfficiency < minEfficiency) 停工
 *            progress += (delta()/60f) * actualEfficiency * efficiency
 *
 *  电力消耗用原版 ConsumePower（consumeBuilder）；
 *  物品、液体的消耗产出全部在 updateTile 里手动处理（避免 consumeBuilder 的 efficiency 联动干扰深度效率）。
 *
 *  构造参数（共 14 个）：
 *    name, size,
 *    baseEfficiency, depthMultiplier, minEfficiency,    // 深度效率三参数
 *    craftTime,                                         // 一轮生产耗时（秒）
 *    powerConsumePerSec,                                // 耗电速度，0 = 无耗电
 *    itemCapacity,                                      // 物品容量，0 = 无物品存储
 *    itemInputs,                                        // 消耗物品（一轮），null = 无
 *    liquidCapacity,                                    // 液体容量，0 = 无液体存储
 *    liquidInputs,                                      // 消耗液体（一轮），null = 无
 *    itemOutputs,                                       // 产出物品，null = 无
 *    liquidOutputs,                                     // 产出液体，null = 无
 *    buildCost, buildTime                               // 建造需求
 */
public class UdDepthDrivenFactory extends Block {

    // ---- 深度效率参数 ----
    public final float baseEfficiency;
    public final float depthMultiplier;
    public final float minEfficiency;

    // ---- 生产参数 ----
    public final float craftTime;
    public final float powerConsumePerSec;
    public final int itemCapacityVal;
    public final float liquidCapacityVal;
    public final ItemStack[] itemInputs;
    public final LiquidStack[] liquidInputs;
    public final ItemStack[] itemOutputs;
    public final LiquidStack[] liquidOutputs;

    // ---- 输入白名单（acceptItem / acceptLiquid 用） ----
    public final mindustry.type.Item[] inputItems;
    public final Liquid[] inputLiquids;

    public UdDepthDrivenFactory(
        String name,
        int size,
        float baseEfficiency,
        float depthMultiplier,
        float minEfficiency,
        float craftTime,
        float powerConsumePerSec,
        int itemCapacity,
        ItemStack[] itemInputs,
        float liquidCapacity,
        LiquidStack[] liquidInputs,
        ItemStack[] itemOutputs,
        LiquidStack[] liquidOutputs,
        ItemStack[] buildCost,
        float buildTime
    ) {
        super(name);

        this.baseEfficiency = baseEfficiency;
        this.depthMultiplier = depthMultiplier;
        this.minEfficiency = minEfficiency;
        this.craftTime = craftTime;
        this.powerConsumePerSec = powerConsumePerSec;
        this.itemCapacityVal = itemCapacity;
        this.liquidCapacityVal = liquidCapacity;
        this.itemInputs = itemInputs;
        this.liquidInputs = liquidInputs;
        this.itemOutputs = itemOutputs;
        this.liquidOutputs = liquidOutputs;

        // ---- Block 基础属性 ----
        this.size = size;
        this.update = true;
        this.solid = true;
        this.category = Category.crafting;
        this.buildVisibility = BuildVisibility.shown;
        this.flags = EnumSet.of(BlockFlag.factory);
        this.buildType = UdDepthDrivenFactoryBuilding::new;
        this.buildTime = buildTime;
        requirements(Category.crafting, buildCost);

        // ---- 电力 ----
        if (powerConsumePerSec > 0f) {
            this.hasPower = true;
            this.consumeBuilder.add(new ConsumePower(powerConsumePerSec / 60f, 0f, false));
        } else {
            this.consumesPower = false;
        }

        // ---- 物品 ----
        if (itemCapacity > 0) {
            this.hasItems = true;
            this.itemCapacity = itemCapacity;
            // 必须加入 consumeBuilder，否则引擎的效率检查会导致 updateTile 不被调用
            if (itemInputs != null && itemInputs.length > 0) {
                this.consumeBuilder.add(new ConsumeItems(itemInputs));
            }
        }

        // ---- 液体 ----
        if (liquidCapacity > 0f) {
            this.hasLiquids = true;
            this.liquidCapacity = liquidCapacity;
            if (liquidInputs != null && liquidInputs.length > 0) {
                this.consumeBuilder.add(new ConsumeLiquids(liquidInputs));
            }
        }

        // ---- 输入白名单 ----
        if (itemInputs != null && itemInputs.length > 0) {
            this.inputItems = new mindustry.type.Item[itemInputs.length];
            for (int i = 0; i < itemInputs.length; i++) inputItems[i] = itemInputs[i].item;
        } else {
            this.inputItems = new mindustry.type.Item[0];
        }
        if (liquidInputs != null && liquidInputs.length > 0) {
            this.inputLiquids = new Liquid[liquidInputs.length];
            for (int i = 0; i < liquidInputs.length; i++) inputLiquids[i] = liquidInputs[i].liquid;
        } else {
            this.inputLiquids = new Liquid[0];
        }
    }

    // ========================================================================

    public class UdDepthDrivenFactoryBuilding extends Building {

        float progress;

        @Override
        public void updateTile() {
            // ---- 深度效率 ----
            float depthSum = 0f;
            for (int dx = 0; dx < size; dx++)
                for (int dy = 0; dy < size; dy++)
                    depthSum += DepthManager.getEffectiveDepth(tileX() + dx, tileY() + dy);

            float actualEfficiency = baseEfficiency + depthSum * depthMultiplier;
            if (actualEfficiency < minEfficiency) return;

            // ---- 启用检查 ----
            if (!enabled) return;
            if (powerConsumePerSec > 0f && efficiency <= 0f) return;

            // ---- 输入够不够 ----
            if (itemInputs != null) {
                for (ItemStack s : itemInputs) {
                    if (items.get(s.item) < s.amount) return;
                }
            }
            if (liquidInputs != null) {
                for (LiquidStack s : liquidInputs) {
                    if (liquids.get(s.liquid) < s.amount) return;
                }
            }

            // ---- 输出有没有空间 ----
            if (itemOutputs != null) {
                for (ItemStack s : itemOutputs) {
                    if (items.get(s.item) + s.amount > itemCapacity) return;
                }
            }
            if (liquidOutputs != null) {
                for (LiquidStack s : liquidOutputs) {
                    if (liquids.get(s.liquid) + s.amount > liquidCapacity) return;
                }
            }

            // ---- 累计进度 ----
            float effMul = (powerConsumePerSec > 0f) ? efficiency : 1f;
            progress += (delta() / 60f) * actualEfficiency * effMul;
            if (progress >= craftTime) craft();

            // ---- 自动导出 ----
            if (itemOutputs != null) for (ItemStack s : itemOutputs) dump(s.item);
            if (liquidOutputs != null) for (LiquidStack s : liquidOutputs) dumpLiquid(s.liquid);
        }

        private void craft() {
            progress = 0f;
            if (itemInputs != null)  for (ItemStack s : itemInputs)   items.remove(s.item, s.amount);
            if (liquidInputs != null) for (LiquidStack s : liquidInputs) liquids.remove(s.liquid, s.amount);
            if (itemOutputs != null) for (ItemStack s : itemOutputs)  items.add(s.item, s.amount);
            if (liquidOutputs != null) for (LiquidStack s : liquidOutputs) liquids.add(s.liquid, s.amount);
        }

        @Override
        public boolean dump() { return false; }

        @Override
        public boolean acceptItem(mindustry.gen.Building source, mindustry.type.Item item) {
            if (itemCapacity <= 0 || items.get(item) >= itemCapacity) return false;
            for (mindustry.type.Item allowed : inputItems) if (allowed == item) return true;
            return false;
        }

        @Override
        public boolean acceptLiquid(mindustry.gen.Building source, Liquid liquid) {
            if (liquidCapacity <= 0f || liquids.get(liquid) >= liquidCapacity) return false;
            for (Liquid allowed : inputLiquids) if (allowed == liquid) return true;
            return false;
        }

        @Override
        public float progress() { return Mathf.clamp(progress / craftTime); }

        @Override
        public boolean shouldConsume() {
            if (itemInputs != null)
                for (ItemStack s : itemInputs) if (items.get(s.item) < s.amount) return false;
            if (liquidInputs != null)
                for (LiquidStack s : liquidInputs) if (liquids.get(s.liquid) < s.amount) return false;
            if (itemOutputs != null)
                for (ItemStack s : itemOutputs) if (items.get(s.item) + s.amount > itemCapacity) return false;
            if (liquidOutputs != null)
                for (LiquidStack s : liquidOutputs) if (liquids.get(s.liquid) + s.amount > liquidCapacity) return false;
            return enabled;
        }
    }
}
