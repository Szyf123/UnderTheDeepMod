package udmodpack;

import mindustry.type.Item;
import mindustry.world.blocks.environment.OreBlock;

/** 深海 Mod 的矿石叠加地块。继承原版 OreBlock，功能完全一致。 */
public class UdBasicOre extends OreBlock {

    public UdBasicOre(String name, Item item, int variants) {
        super(name);
        this.variants = variants;
        setup(item);
    }

    /** 默认 3 变体。 */
    public UdBasicOre(String name, Item item) {
        this(name, item, 3);
    }

    /** 用 item.name 自动生成地块名。 */
    public UdBasicOre(Item item, int variants) {
        this("ore-" + item.name, item, variants);
    }
}
