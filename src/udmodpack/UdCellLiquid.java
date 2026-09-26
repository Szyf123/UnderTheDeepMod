package udmodpack;

import arc.graphics.Color;
import mindustry.type.CellLiquid;
import mindustry.type.Liquid;

/** 瘤液类液体基类。继承 CellLiquid，构造参数：name / color / temperature / heatCapacity / viscosity / coolant / capPuddles / spreadDamage / cells / colorFrom / colorTo。
 *  CellLiquid 额外字段（有默认值）：
 *  - spreadTarget (Liquid) 扩散/转化的目标液体
 *  - maxSpread (float) 每 tick 最大扩散量，默认 0.75f
 *  - spreadConversion (float) 转化率，默认 1.2f
 *  - removeScaling (float) 吸走目标液体的比例，默认 0.25f
 *  还继承 Liquid 的公开字段：blockReactive / moveThroughBlocks / incinerable / effect /
 *  particleEffect / particleSpacing / boilPoint / vaporEffect / hidden / barColor / lightColor / canStayOn
 *
 * 科技树挂接统一在 UDTechTree.load() 中构建，不在此类处理。 */
public class UdCellLiquid extends CellLiquid {

    public UdCellLiquid(String name, Color color,
                         float temperature, float heatCapacity, float viscosity,
                         boolean coolant, boolean capPuddles,
                         float spreadDamage, int cells, Color colorFrom, Color colorTo) {
        super(name, color);
        this.temperature = temperature;
        this.heatCapacity = heatCapacity;
        this.viscosity = viscosity;
        this.coolant = coolant;
        this.capPuddles = capPuddles;
        this.spreadDamage = spreadDamage;
        this.cells = cells;
        this.colorFrom = colorFrom;
        this.colorTo = colorTo;
    }
}
