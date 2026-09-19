package udmodpack;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.BuildVisibility;
import mindustry.type.Liquid;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

/** 减压场建筑。三种变体：
 *  basic: 只耗电
 *  advanced: 耗电 + 铜（物品）
 *  ultimate: 耗电 + 铜（物品） + 水（液体）
 *
 *  净化逻辑：每秒在 depressurizationSize 范围内随机采样 cureTilesPerSecond 格，
 *  每格以 cureChance 概率将感染地板替换为非感染版本（查 InfectionManager.curePair）。
 *  不会一次性净化全部，而是随机渐进。 */
public class UDDepressurization extends Block {

    // ===== 实例字段 =====

    /** 净化范围的完整边长（格数） */
    public int depressurizationSize;

    /** 每秒电力（瓦） */
    public float powerUsePerSecond;

    /** 运行时物品消耗（每秒），null=不消耗 */
    public ItemStack[] itemConsumption;

    /** 运行时液体类型，null=不消耗液体 */
    public Liquid liquidType;

    /** 每秒液体消耗量，0=不消耗液体 */
    public float liquidPerSecond;

    /** 每秒采样多少个格子做净化尝试（值越大净化越快） */
    public int cureTilesPerSecond;

    /** 被采样到的感染格子被净化的概率（0~1） */
    public float cureChance;

    /**
     * @param name                 内容名
     * @param size                 建筑 footprint
     * @param depressurizationSize 净化范围完整边长（格数）
     * @param buildCost            建造物品消耗
     * @param buildTimeTicks       建造时间（ticks）
     * @param powerUsePerSecond    电力（瓦/秒）
     * @param itemConsumption      运行物品消耗（每秒），null=无
     * @param liquidType           运行液体类型，null=无
     * @param liquidPerSecond      每秒液体量，0=无
     * @param cureTilesPerSecond   每秒尝试净化的格子数
     * @param cureChance           每格被净化的概率（0~1）
     */
    public UDDepressurization(
        String name,
        int size,
        int depressurizationSize,
        ItemStack[] buildCost,
        float buildTimeTicks,
        float powerUsePerSecond,
        ItemStack[] itemConsumption,
        Liquid liquidType,
        float liquidPerSecond,
        int cureTilesPerSecond,
        float cureChance
    ) {
        super(name);
        this.size = size;
        this.depressurizationSize = depressurizationSize;
        this.category = Category.effect;
        this.buildVisibility = BuildVisibility.shown;
        this.update = true;
        this.solid = true;
        this.destructible = true;
        this.buildType = UDDepressurizationBuilding::new;
        this.powerUsePerSecond = powerUsePerSecond;
        this.itemConsumption = itemConsumption;
        this.liquidType = liquidType;
        this.liquidPerSecond = liquidPerSecond;
        this.cureTilesPerSecond = cureTilesPerSecond;
        this.cureChance = cureChance;

        // === 建造消耗 ===
        requirements(Category.effect, buildCost);
        this.buildTime = buildTimeTicks;

        // === 电力消耗（自动接电网，效率菱形靠引擎） ===
        if (powerUsePerSecond > 0) {
            this.hasPower = true;
            ConsumePower cp = new ConsumePower(powerUsePerSecond / 60f, 0f, false);
            this.consPower = cp;
            this.consumeBuilder.add(cp);
        }

        // === 物品消耗（引擎自动泵入 + 手动扣减） ===
        if (itemConsumption != null && itemConsumption.length > 0) {
            this.hasItems = true;
            this.itemCapacity = 20;
            ConsumeItems ci = new ConsumeItems(itemConsumption);
            this.consumeBuilder.add(ci);
        }

        // === 液体消耗（引擎自动泵入 + 手动扣减） ===
        if (liquidType != null && liquidPerSecond > 0) {
            this.hasLiquids = true;
            this.liquidCapacity = 20f;
            ConsumeLiquid cl = new ConsumeLiquid(liquidType, liquidPerSecond);
            this.consumeBuilder.add(cl);
        }
    }

    /** 放置预览时的净化范围虚线方形。 */
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        float centerX = x * tilesize + offset;
        float centerY = y * tilesize + offset;
        float fullSizePx = depressurizationSize * tilesize;
        Drawf.dashSquare(Color.white.cpy().a(0.4f), centerX, centerY, fullSizePx);
    }

    /** 减压场的 Building 实例。 */
    public static class UDDepressurizationBuilding extends Building {

        /** 物品消耗计时器 */
        float itemConsumeTimer = 0f;
        /** 净化周期计时器（累计秒） */
        float cureTimer = 0f;
        /** 深度覆盖轮询计数器（10帧一次） */
        int depthTickCounter = 0;
        /** 上一次轮询时是否在工作（默认 false，读档后第一次轮询自然触发 addCoverage） */
        boolean wasActive = false;

        public UDDepressurizationBuilding() {
            super();
        }

        /** 销毁时清理深度覆盖。 */
        @Override
        public void onRemoved() {
            if (wasActive) {
                UDDepressurization block = (UDDepressurization) this.block;
                DepthManager.removeCoverage(tileX(), tileY(), block.depressurizationSize);
                wasActive = false;
            }
        }

        /** 每帧：电力/液体引擎自动扣，物品手动扣，净化每秒随机采样，10帧轮询深度覆盖状态。 */
        @Override
        public void updateTile() {
            // ===== 深度覆盖轮询（10帧一次） =====
            depthTickCounter++;
            if (depthTickCounter >= 10) {
                depthTickCounter = 0;
                boolean nowActive = efficiency > 0f && enabled;
                if (nowActive != wasActive) {
                    UDDepressurization block = (UDDepressurization) this.block;
                    if (nowActive) {
                        DepthManager.addCoverage(tileX(), tileY(), block.depressurizationSize);
                    } else {
                        DepthManager.removeCoverage(tileX(), tileY(), block.depressurizationSize);
                    }
                    wasActive = nowActive;
                }
            }

            if (efficiency <= 0f || !enabled) return;

            UDDepressurization block = (UDDepressurization) this.block;

            // === 物品消耗（手动每秒一轮） ===
            if (block.itemConsumption != null && block.itemConsumption.length > 0) {
                itemConsumeTimer += delta() / 60f;
                if (itemConsumeTimer >= 1f) {
                    for (ItemStack stack : block.itemConsumption) {
                        items.remove(stack.item, stack.amount);
                    }
                    itemConsumeTimer = 0f;
                }
            }

            // === 净化（每秒一轮，随机采样 N 格，每格概率净化） ===
            cureTimer += delta() / 60f;
            if (cureTimer >= 1f) {
                cureTimer = 0f;
                runCure(block);
            }
        }

        /** 在净化范围内随机采样 block.cureTilesPerSecond 格，每格以 cureChance 概率净化。 */
        private void runCure(UDDepressurization block) {
            int halfSize = block.depressurizationSize / 2;
            // 范围左上角：建筑中心偏移，算清楚边界
            int centerX = tileX();
            int centerY = tileY();
            int rangeMinX = centerX - halfSize;
            int rangeMinY = centerY - halfSize;
            int rangeMaxX = centerX + halfSize;
            int rangeMaxY = centerY + halfSize;

            int worldW = world.width();
            int worldH = world.height();

            for (int i = 0; i < block.cureTilesPerSecond; i++) {
                // 在范围内随机选一个坐标
                int rx = Mathf.random(rangeMinX, rangeMaxX);
                int ry = Mathf.random(rangeMinY, rangeMaxY);

                // 边界保护
                if (rx < 0 || rx >= worldW || ry < 0 || ry >= worldH) continue;

                Tile tile = world.tile(rx, ry);
                if (tile == null) continue;

                if (!(tile.floor() instanceof UdBasicFloor)) continue;
                UdBasicFloor floor = (UdBasicFloor) tile.floor();

                // 不是感染地板，跳过
                if (!floor.isInfected) continue;

                // 概率净化
                if (Mathf.random() < block.cureChance) {
                    UdBasicFloor clean = UDModMain.curePair.get(floor);
                    if (clean != null) {
                        tile.setFloor(clean);
                        UDContent.cureGenerate.at(tile.worldx(), tile.worldy());
                    }
                }
            }
        }

        /** 玩家选中时的净化范围虚线方形。 */
        @Override
        public void drawSelect() {
            UDDepressurization block = (UDDepressurization) this.block;
            float fullSizePx = block.depressurizationSize * tilesize;
            Drawf.dashSquare(Color.white.cpy().a(0.4f), x, y, fullSizePx);
        }

        /** 正常运行时渲染净化范围发光线框（加法混合叠加，逐层外扩+降透明度）。 */
        @Override
        public void draw() {
            super.draw();

            if (efficiency <= 0f || !enabled) return;

            UDDepressurization block = (UDDepressurization) this.block;
            float fullSizePx = block.depressurizationSize * tilesize;
            float half = fullSizePx / 2f;

            Blending.additive.apply();

            // 核心亮线（最内层，100% alpha）
            Lines.stroke(1.2f, Color.cyan);
            Lines.rect(x - half, y - half, fullSizePx, fullSizePx);

            // 外发光 5 层（alpha 10%）
            for (int i = 0; i < 5; i++) {
                float width = 2.0f + i * 0.8f;
                float expand = 0.4f + i * 0.4f;
                float size = fullSizePx + expand * 2f;
                Lines.stroke(width, Color.cyan.cpy().a(0.1f));
                Lines.rect(x - half - expand, y - half - expand, size, size);
            }

            Blending.normal.apply();
        }
    }
}
