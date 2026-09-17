package udmodpack;

import mindustry.world.blocks.environment.Floor;

/** 带 depthDefault 的地板基类。
 *  七参数构造: name / variants / depthDefault / drawEdgeOut / drawEdgeIn / isInfected / canBeInfected。
 *  构造函数自动设好 inEditor=true, alwaysUnlocked=true, hideDatabase=false；edge 默认 null。 */
public class UdBasicFloor extends Floor {
    // 深度常量（0=浅海, 1=深海, 2=深渊）
    public static final byte SHALLOW = 0;
    public static final byte DEEP    = 1;
    public static final byte ABYSSAL = 2;

    public final byte depthDefault;
    public final boolean isInfected;
    public final boolean canBeInfected;

    public UdBasicFloor(String name, int variants, byte depthDefault,
                        boolean drawEdgeOut, boolean drawEdgeIn,
                        boolean isInfected, boolean canBeInfected) {
        super(name, variants);
        this.depthDefault = depthDefault;
        this.drawEdgeOut = drawEdgeOut;
        this.drawEdgeIn = drawEdgeIn;
        this.isInfected = isInfected;
        this.canBeInfected = canBeInfected;
        this.edge = null;
        // 缺省值：地图编辑器可见 + 数据库可见
        this.inEditor = true;
        this.alwaysUnlocked = true;
        this.hideDatabase = false;
    }
}
