package udmodpack;

import arc.math.Mathf;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.EnumSet;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BuildVisibility;

import static mindustry.Vars.*;
import static udmodpack.UDContent.*;

/** 精准核心合成厂 - 多配方工厂。 */
public class UdPrecCoreFactory extends Block {

    public static class Recipe {
        public final String name;
        public final ItemStack[] itemInputs;
        public final ItemStack[] itemOutputs;
        public final float craftTime;

        public Recipe(String name, ItemStack[] itemInputs, ItemStack[] itemOutputs, float craftTime) {
            this.name = name;
            this.itemInputs = itemInputs;
            this.itemOutputs = itemOutputs;
            this.craftTime = craftTime;
        }
    }

    public final Seq<Recipe> recipes = new Seq<>();

    public UdPrecCoreFactory(String name) {
        super(name);
        this.update = true;
        this.solid = true;
        this.hasItems = true;
        this.sync = true;
        this.configurable = true;
        this.category = Category.crafting;
        this.buildVisibility = BuildVisibility.shown;
        this.flags = EnumSet.of(BlockFlag.factory);

        this.size = 3;
        this.itemCapacity = 600;  // 单个原料的独立容量上限
        this.buildType = UdPrecCoreFactoryBuilding::new;
        this.buildTime = 180f;
        requirements(Category.crafting, new ItemStack[]{
            new ItemStack(mindustry.content.Items.copper, 60),
            new ItemStack(mindustry.content.Items.lead, 40),
            new ItemStack(mindustry.content.Items.silicon, 30)
        });

        this.hasPower = true;
        this.consPower = new ConsumePower(10f / 60f, 0f, false);
        this.consumeBuilder.add(consPower);

        recipes.add(new Recipe("战斗核心合成",
            new ItemStack[]{
                new ItemStack(wreckageAlloy, 300),
                new ItemStack(wreckageGlass, 100),
                new ItemStack(wreckageFiber, 100)
            },
            new ItemStack[]{ new ItemStack(precCoreFight, 1) },
            900f
        ));
        recipes.add(new Recipe("迁移核心合成",
            new ItemStack[]{
                new ItemStack(wreckageAlloy, 100),
                new ItemStack(wreckageGlass, 300),
                new ItemStack(wreckageFiber, 100)
            },
            new ItemStack[]{ new ItemStack(precCoreMigrate, 1) },
            900f
        ));
        recipes.add(new Recipe("生存核心合成",
            new ItemStack[]{
                new ItemStack(wreckageAlloy, 100),
                new ItemStack(wreckageGlass, 100),
                new ItemStack(wreckageFiber, 300)
            },
            new ItemStack[]{ new ItemStack(precCoreExist, 1) },
            900f
        ));
    }

    public class UdPrecCoreFactoryBuilding extends Building {

        public int currentRecipe = 0;
        public float progress;

        @Override
        public void updateTile() {
            if (efficiency <= 0 || !enabled) return;

            Recipe recipe = recipes.get(currentRecipe);

            for (ItemStack s : recipe.itemInputs) {
                if (items.get(s.item) < s.amount) return;
            }
            for (ItemStack s : recipe.itemOutputs) {
                if (items.get(s.item) + s.amount > itemCapacity) return;
            }

            progress += getProgressIncrease(recipe.craftTime);
            if (progress >= 1f) {
                craft(recipe);
            }

            for (ItemStack s : recipe.itemOutputs) {
                dump(s.item);
            }
        }

        @Override
        public boolean dump() {
            return false;
        }

        private void craft(Recipe recipe) {
            for (ItemStack s : recipe.itemInputs) {
                items.remove(s.item, s.amount);
            }
            for (ItemStack s : recipe.itemOutputs) {
                items.add(s.item, s.amount);
            }
            progress = 0;
        }

        @Override
        public boolean acceptItem(mindustry.gen.Building source, mindustry.type.Item item) {
            return (item == wreckageAlloy || item == wreckageGlass || item == wreckageFiber)
                && items.get(item) < itemCapacity;
        }

        @Override
        public float progress() {
            return Mathf.clamp(progress);
        }

        @Override
        public boolean shouldConsume() {
            Recipe r = recipes.get(currentRecipe);
            // 原料够不够
            for (ItemStack s : r.itemInputs) {
                if (items.get(s.item) < s.amount) return false;
            }
            // 输出有没有空间
            for (ItemStack s : r.itemOutputs) {
                if (items.get(s.item) + s.amount > itemCapacity) return false;
            }
            return enabled;
        }

        // ===== UI：配方切换 =====

        @Override
        public void buildConfiguration(Table table) {
            table.row();

            // 选中=深灰八边形(flatDown，边框清晰)，未选中=黄色八边形(flatOver)
            Seq<ImageButton> buttonsRefs = new Seq<>();
            Table buttons = new Table();
            float size = 44f;

            for (int i = 0; i < recipes.size; i++) {
                Recipe r = recipes.get(i);
                int idx = i;
                mindustry.type.Item output = r.itemOutputs[0].item;

                ImageButton btn = buttons.button(
                    new TextureRegionDrawable(output.fullIcon),
                    () -> {
                        currentRecipe = idx;
                        progress = 0;
                        for (int j = 0; j < buttonsRefs.size; j++) {
                            arc.scene.ui.ImageButton.ImageButtonStyle st = buttonsRefs.get(j).getStyle();
                            arc.scene.style.Drawable bg = (j == idx) ? Styles.flatDown : Styles.flatOver;
                            st.up = bg;
                            st.over = bg;
                            st.down = bg;
                        }
                    }
                ).size(size).margin(4f).get();

                arc.scene.style.Drawable initBg = (i == currentRecipe) ? Styles.flatDown : Styles.flatOver;
                btn.getStyle().up = initBg;
                btn.getStyle().over = initBg;
                btn.getStyle().down = initBg;

                buttonsRefs.add(btn);
            }

            table.add(buttons).row();
            super.buildConfiguration(table);
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(currentRecipe);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            currentRecipe = read.i();
            progress = read.f();
        }
    }
}
