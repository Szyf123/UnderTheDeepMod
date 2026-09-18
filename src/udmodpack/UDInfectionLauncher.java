package udmodpack;

import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.meta.BuildVisibility;

/** 感染发射器 — 持续检测脚下地板，每 1 秒尝试消耗 1 铜将可感染地板转为感染版。 */
public class UDInfectionLauncher extends Block {

    public UDInfectionLauncher(String name) {
        super(name);
        this.size = 1;
        this.itemCapacity = 1;
        this.hasItems = true;
        this.category = Category.effect;
        this.buildVisibility = BuildVisibility.shown;
        this.solid = true;
        this.destructible = true;
        this.update = true;
        this.buildType = UDInfectionLauncherBuilding::new;

        requirements(Category.effect, new ItemStack[]{
            new ItemStack(Items.copper, 30)
        });
        this.buildTime = 120f;

        // 声明需要铜，引擎自动 pathing + 每秒消耗 1 铜
        ConsumeItems ci = new ConsumeItems(new ItemStack[]{
            new ItemStack(Items.copper, 1)
        });
        this.consumeBuilder.add(ci);
    }

    /** 内部 Building 类。 */
    public static class UDInfectionLauncherBuilding extends Building {

        /** 1 秒冷却计时器（累计秒） */
        float infectTimer = 0f;

        public UDInfectionLauncherBuilding() {
            super();
        }

        @Override
        public void updateTile() {
            infectTimer += delta() / 60f;
            if (infectTimer < 1f) return;
            infectTimer = 0f;

            Tile t = tile;
            if (t == null) return;
            if (!(t.floor() instanceof UdBasicFloor)) return;

            UdBasicFloor floor = (UdBasicFloor) t.floor();
            if (floor.isInfected) return;
            if (!floor.canBeInfected) return;

            UdBasicFloor infected = UDModMain.infectPair.get(floor);
            if (infected == null) return;

            // 有铜才感染；铜由 ConsumeItems 引擎自动消耗（1/s）
            if (items.get(Items.copper) < 1) return;

            t.setFloor(infected);
        }
    }
}
