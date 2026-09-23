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
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BuildVisibility;

import static mindustry.Vars.*;
import static udmodpack.UDContent.*;

/** 装药合成厂 - 六配方工厂（2行3列UI）。
 *  配方：3海藻 + 1材料 → 1装药，生产时间 90f。
 */
public class UdGunpowderFactory extends Block {

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

    public UdGunpowderFactory(String name) {
        super(name);
        this.update = true;
        this.solid = true;
        this.hasItems = true;
        this.sync = true;
        this.configurable = true;
        this.category = Category.crafting;
        this.buildVisibility = BuildVisibility.shown;
        this.flags = EnumSet.of(BlockFlag.factory);

        this.size = 2;
        this.itemCapacity = 20;
        this.buildType = UdGunpowderFactoryBuilding::new;
        this.buildTime = 120f;
        requirements(Category.crafting, new ItemStack[]{
            new ItemStack(mindustry.content.Items.copper, 40),
            new ItemStack(mindustry.content.Items.lead, 25),
            new ItemStack(mindustry.content.Items.silicon, 20)
        });

        this.hasPower = true;
        this.consPower = new ConsumePower(6f / 60f, 0f, false);
        this.consumeBuilder.add(consPower);

        // 6个配方：3海藻 + 1材料 → 1装药
        recipes.add(new Recipe("芯片装药合成",
            new ItemStack[]{
                new ItemStack(seaweedBundle, 3),
                new ItemStack(chip, 1)
            },
            new ItemStack[]{ new ItemStack(gunpowderChip, 1) },
            90f
        ));
        recipes.add(new Recipe("锰钢装药合成",
            new ItemStack[]{
                new ItemStack(seaweedBundle, 3),
                new ItemStack(manganeseSteel, 1)
            },
            new ItemStack[]{ new ItemStack(gunpowderManganeseSteel, 1) },
            90f
        ));
        recipes.add(new Recipe("高级芯片装药合成",
            new ItemStack[]{
                new ItemStack(seaweedBundle, 3),
                new ItemStack(advancedChip, 1)
            },
            new ItemStack[]{ new ItemStack(gunpowderAdvancedChip, 1) },
            90f
        ));
        recipes.add(new Recipe("护盾合金装药合成",
            new ItemStack[]{
                new ItemStack(seaweedBundle, 3),
                new ItemStack(shieldAlloy, 1)
            },
            new ItemStack[]{ new ItemStack(gunpowderShieldAlloy, 1) },
            90f
        ));
        recipes.add(new Recipe("钫装药合成",
            new ItemStack[]{
                new ItemStack(seaweedBundle, 3),
                new ItemStack(francium, 1)
            },
            new ItemStack[]{ new ItemStack(gunpowderFrancium, 1) },
            90f
        ));
        recipes.add(new Recipe("克苏鲁合金装药合成",
            new ItemStack[]{
                new ItemStack(seaweedBundle, 3),
                new ItemStack(rlyehAlloy, 1)
            },
            new ItemStack[]{ new ItemStack(gunpowderRlyehAlloy, 1) },
            90f
        ));
    }

    public class UdGunpowderFactoryBuilding extends Building {

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
            return (item == seaweedBundle
                || item == chip
                || item == manganeseSteel
                || item == advancedChip
                || item == shieldAlloy
                || item == francium
                || item == rlyehAlloy)
                && items.get(item) < itemCapacity;
        }

        @Override
        public float progress() {
            return Mathf.clamp(progress);
        }

        @Override
        public boolean shouldConsume() {
            Recipe r = recipes.get(currentRecipe);
            for (ItemStack s : r.itemInputs) {
                if (items.get(s.item) < s.amount) return false;
            }
            for (ItemStack s : r.itemOutputs) {
                if (items.get(s.item) + s.amount > itemCapacity) return false;
            }
            return enabled;
        }

        // ===== UI：配方切换（2行3列） =====

        @Override
        public void buildConfiguration(Table table) {
            table.row();

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

                // 每3个按钮换行，形成2行3列
                if ((i + 1) % 3 == 0) buttons.row();
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
