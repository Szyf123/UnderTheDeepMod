package udmodpack;

import mindustry.world.blocks.environment.Floor;

/** 带 depthDefault 的地板基类。
 *  只保留五参数构造，强制调用方显式填写 name / variants / depthDefault / drawEdgeOut / drawEdgeIn。
 *  构造函数自动设好 inEditor=true, alwaysUnlocked=true, hideDatabase=false；edge 默认 null。 */
public class UdBasicFloor extends Floor {
    // 深度常量（0=浅海, 1=深海, 2=深渊）
    public static final byte SHALLOW = 0;
    public static final byte DEEP    = 1;
    public static final byte ABYSSAL = 2;

    public final byte depthDefault;

    public UdBasicFloor(String name, int variants, byte depthDefault, boolean drawEdgeOut, boolean drawEdgeIn) {
        super(name, variants);
        this.depthDefault = depthDefault;
        this.drawEdgeOut = drawEdgeOut; 
        this.drawEdgeIn = drawEdgeIn;
        this.edge = null;
        // 缺省值：地图编辑器可见 + 数据库可见
        this.inEditor = true;
        this.alwaysUnlocked = true;
        this.hideDatabase = false;
    }
}
