package udmodpack;

import arc.graphics.Color;
import mindustry.type.Liquid;

/** 液体基类。构造参数：name / color / gas / temperature / heatCapacity / viscosity / coolant / capPuddles。
 *  默认值参考 Liquid 原版：temperature=0.5f, heatCapacity=0.5f, viscosity=0.5f, coolant=true, capPuddles=true。
 *  可额外设置的公开字段：
 *  - blockReactive (boolean) 是否与方块反应
 *  - moveThroughBlocks (boolean) 是否能穿过方块
 *  - incinerable (boolean) 是否可焚化
 *  - effect (StatusEffect) 状态效果
 *  - particleEffect (Effect) 粒子特效
 *  - particleSpacing (float) 粒子间距
 *  - boilPoint (float) 沸点
 *  - vaporEffect (Effect) 蒸发特效
 *  - hidden (boolean) 是否隐藏
 *  - gasColor (Color) 气体颜色
 *  - barColor (Color) 条块颜色
 *  - lightColor (Color) 发光颜色
 *  - canStayOn (ObjectSet<Liquid>) 可以共存的液体
 */
public class UdBasicLiquid extends Liquid {

    public UdBasicLiquid(String name, Color color, boolean gas, float temperature, float heatCapacity, float viscosity, boolean coolant, boolean capPuddles) {
        super(name, color);
        this.gas = gas;
        this.temperature = temperature;
        this.heatCapacity = heatCapacity;
        this.viscosity = viscosity;
        this.coolant = coolant;
        this.capPuddles = capPuddles;
    }
}
