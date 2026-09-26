package udmodpack;

import arc.graphics.Color;
import mindustry.type.Item;

/** 物品基类。构造参数：name / color / hardness / lowPriority / frames / frameTime。
 *  科技树挂接统一在 UDTechTree.load() 中构建，不在此类处理。 */
public class UdBadicItem extends Item {

    public UdBadicItem(String name, Color color, int hardness, boolean lowPriority, int frames, float frameTime) {
        super(name, color);
        this.hardness = hardness;
        this.lowPriority = lowPriority;
        this.frames = frames;
        this.frameTime = frameTime;
    }
}
