package udmodpack;

import arc.math.Mathf;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.meta.BuildVisibility;

import mindustry.content.Items;

/** 深度测试工厂：不耗电，每 5 秒一轮，5铜→1铅。
 *  效率公式：
 *    actualEfficiency = baseEfficiency + depthSum * depthMultiplier
 *    if (actualEfficiency >= minEfficiency) 用 actualEfficiency，否则停工（效率 0）
 *  其中 depthSum = footprint 内所有 tile 的有效深度之和。 */
public class UdDepthTestBlock extends Block {

    /** 基础效率倍率（1.0 = 正常速度） */
    public float baseEfficiency;
    /** 每个深度单位对效率的加成 */
    public float depthMultiplier;
    /** 最低效率阈值：实际效率低于此值则停工（0） */
    public float minEfficiency;

    public UdDepthTestBlock(String name, float baseEfficiency, float depthMultiplier, float minEfficiency) {
        super(name);
        this.baseEfficiency = baseEfficiency;
        this.depthMultiplier = depthMultiplier;
        this.minEfficiency = minEfficiency;

        this.category = Category.crafting;
        this.buildVisibility = BuildVisibility.shown;
        this.update = true;
        this.solid = true;
        this.hasItems = true;
        this.itemCapacity = 20;
        this.buildType = UdDepthTestBuilding::new;
        this.buildTime = 60f;

        requirements(Category.crafting, new ItemStack[]{
            new ItemStack(Items.copper, 30),
            new ItemStack(Items.lead, 15)
        });

        // 声明消耗铜（引擎自动 pathing + 泵入）
        this.consumeBuilder.add(new ConsumeItems(new ItemStack[]{
            new ItemStack(Items.copper, 5)
        }));
    }

    public class UdDepthTestBuilding extends Building {

        float progress;

        @Override
        public void updateTile() {
            // 每轮开始时计算深度总和 → 效率
            float depthSum = 0f;
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    depthSum += DepthManager.getEffectiveDepth(tileX() + dx, tileY() + dy);
                }
            }

            float actualEfficiency = baseEfficiency + depthSum * depthMultiplier;
            if (actualEfficiency < minEfficiency) return; // 低于阈值，停工

            // 原料不够就暂停积累进度（和原版工厂一致）
            if (items.get(Items.copper) < 5) return;

            // 产出没空间也暂停
            if (items.get(Items.lead) + 1 > itemCapacity) return;

            progress += (delta() / 60f) * actualEfficiency;
            if (progress >= 5f) {
                progress = 0;
                items.remove(Items.copper, 5);
                items.add(Items.lead, 1);
            }

            dump(Items.lead);
        }

        /** 只接受铜。 */
        @Override
        public boolean acceptItem(mindustry.gen.Building source, mindustry.type.Item item) {
            return item == Items.copper && items.get(item) < itemCapacity;
        }

        @Override
        public float progress() {
            return Mathf.clamp(progress / 5f);
        }
    }
}
