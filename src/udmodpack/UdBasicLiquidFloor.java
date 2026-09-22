package udmodpack;

import arc.graphics.g2d.TextureRegion;
import mindustry.graphics.CacheLayer;
import mindustry.type.Liquid;
import mindustry.world.blocks.environment.ShallowLiquid;

/** 模组液体地板：继承原版 ShallowLiquid，加入 depth / 感染 / 液体配置。
 *  ShallowLiquid 原版只做"视觉混合"（createIcons 里把 liquid 贴图半透明叠到底图上），
 *  不设 liquidDrop / liquidMultiplier / isLiquid。本类自动补上这些，
 *  让原版 Pump / SolidPump 能直接识别并抽取。
 *
 *  最简六参数构造: name / variants / depthDefault / liquid / drawEdgeOut / drawEdgeIn
 *  其余液体字段（multiplier / drownTime / shallow / cacheLayer）用默认值，
 *  可通过后链式设置（new UdBasicLiquidFloor(...).liquidMultiplier(1.5f)）调整。 */
public class UdBasicLiquidFloor extends ShallowLiquid {

    // —— 模组特有字段 ——
    public final byte depthDefault;
    public final boolean isInfected;
    public final boolean canBeInfected;

    public UdBasicLiquidFloor(String name, int variants, byte depthDefault,
                              Liquid liquid,
                              boolean drawEdgeOut, boolean drawEdgeIn) {
        super(name);
        // 深度/感染
        this.depthDefault = depthDefault;
        this.isInfected = false;
        this.canBeInfected = false;
        // Floor 基类
        this.variants = variants;
        this.drawEdgeOut = drawEdgeOut;
        this.drawEdgeIn = drawEdgeIn;
        this.edge = null;
        this.inEditor = true;
        this.alwaysUnlocked = true;
        this.hideDatabase = false;
        // —— ShallowLiquid / 液体相关 ——
        this.isLiquid = true;
        this.liquidDrop = liquid;
        this.liquidMultiplier = 1.0f;       // 泵产量倍率，deep 建议 1.5，cryo 建议 0.5
        this.drownTime = 30f;               // 踩上溺水时间（秒），0=不溺水
        this.shallow = false;               // 原版浅海生成标志，deep/abyssal 设 false
        this.cacheLayer = CacheLayer.water; // GPU shader 层，water/slag/tar
        this.liquidOpacity = 0.35f;         // ShallowLiquid 混合透明度
    }

    // —— 链式 setter（返回 this，方便注册时一行写完） ——

    /** 泵产量倍率：deep 建议 1.5，cryo 建议 0.5。默认 1.0。 */
    public UdBasicLiquidFloor liquidMultiplier(float v) { this.liquidMultiplier = v; return this; }

    /** 单位踩上溺水秒数，0=不溺水。默认 30。 */
    public UdBasicLiquidFloor drownTime(float v) { this.drownTime = v; return this; }

    /** 原版浅海生成标志。默认 false。 */
    public UdBasicLiquidFloor shallow(boolean v) { this.shallow = v; return this; }

    /** GPU shader 层。默认 CacheLayer.water。可选 slag / tar。 */
    public UdBasicLiquidFloor cacheLayer(CacheLayer layer) { this.cacheLayer = layer; return this; }

    /** ShallowLiquid 的 liquid 贴图透明度。默认 0.35。 */
    public UdBasicLiquidFloor liquidOpacity(float v) { this.liquidOpacity = v; return this; }

    /** 设 floorBase（底层非液体地板）+ liquidBase（液体层来源），
     *  ShallowLiquid.createIcons() 会把 liquidBase.region 半透明叠到 floorBase.variantRegions 上。
     *  这是**可选**的视觉混合方案——如果你的 liquid 本身就是完整的、不需要从别的 floor 底图叠上来，
     *  可以不调这个，ShallowLiquid.createIcons() 里有 null 判断会跳过混合。 */
    public UdBasicLiquidFloor blendFrom(ShallowLiquid liquidBase, mindustry.world.blocks.environment.Floor floorBase) {
        this.liquidBase = liquidBase;
        this.floorBase = floorBase;
        return this;
    }
}
