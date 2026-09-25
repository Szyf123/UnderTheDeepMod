package udmodpack;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.util.noise.Simplex;
import mindustry.maps.generators.PlanetGenerator;

/** 拉莱耶星球程序化生成器（3D 地表颜色用）。
 *  按纬度硬切三段纯色，加噪声混合制造不规则边界 + zone 内随机替换。
 *
 *  参考 SerpuloPlanetGenerator 做法：
 *  1) position 是单位向量 (|x|,|y|,|z| <= 1)，必须乘 scale 放大后再喂噪声
 *  2) 把纬度值和噪声 lerp 混合，而非加偏移量
 */
public class UDRlyehPlanetGenerator extends PlanetGenerator {

    // ===== 三段纯色 =====
    /** 赤道（0~20°） */
    private static final Color COLOR_EQUATOR = Color.valueOf("99D9EA");
    /** 中纬（20~60°） */
    private static final Color COLOR_MID = Color.valueOf("B8AD90");
    /** 高纬（60~90°） */
    private static final Color COLOR_POLAR = Color.valueOf("282B34");
    /** 赤道带紫色变体（原 #99D9EA 浅蓝 → #827AAB 浅紫蓝） */
    private static final Color COLOR_PURPLE_EQ = Color.valueOf("#827AAB");
    /** 中纬带紫色变体（原 #B8AD90 褐 → #806577 灰紫） */
    private static final Color COLOR_PURPLE_MID = Color.valueOf("#806577");
    /** 极地带紫色变体（原 #282B34 深蓝 → #16131F 深紫黑） */
    private static final Color COLOR_PURPLE_POLAR = Color.valueOf("#16131F");

    // ===== 纬度边界（归一化 0~1）=====
    private static final float EQ_BOUND = 30f / 90f;     // 0.222
    private static final float POLAR_BOUND = 60f / 90f;  // 0.667

    // ===== 噪声参数 =====
    /** 噪声 scale（把单位向量放大多少倍再喂噪声） */
    private static final float SCL = 4f;

    /** 边界噪声 lerp 权重（越大=越不规则的纬度带） */
    private static final float BOUNDARY_LERP = 0.5f;
    /** 边界噪声尺度（越小=越大块的不规则） */
    private static final float BOUNDARY_SCALE = 1f / 3f;
    /** 边界噪声 octaves */
    private static final int BOUNDARY_OCT = 5;

    /** zone 内随机替换概率 */
    private static final float INTRA_CHANCE = 0.22f;
    /** zone 内噪声尺度 */
    private static final float INTRA_SCALE = 1f / 2.5f;
    private static final int INTRA_OCT = 3;

    /** 紫色斑块概率（噪声值归一化后低于此值→变紫） */
    private static final float PURPLE_CHANCE = 0.65f;
    /** 紫色噪声尺度（越小=越大块的紫色斑块） */
    private static final float PURPLE_SCALE = 1f / 2f;
    private static final int PURPLE_OCT = 2;

    // ===== 高度参数 =====
    /** 高度噪声 scale */
    private static final float HEIGHT_SCL = 3f;
    /** 赤道 baseline（归一化参考帧：sector层=1.0，球心=0.0） */
    private static final float BASELINE_EQUATOR = 0.7f;
    /** 极地 baseline（降低 = 极地更深凹，高差更大） */
    private static final float BASELINE_POLE = 0.45f;
    /** 噪声幅度（±ROUGHNESS，加大 = 高差更大） */
    private static final float ROUGHNESS = 0.9f;
    /** 高度噪声尺度（越小=越大块） */
    private static final float HEIGHT_SCALE = 1f / 2.5f;
    private static final int HEIGHT_OCT = 5;

    @Override
    public void getColor(Vec3 position, Color out) {
        // 1) 纬度归一化（0=赤道, 1=极点）
        float latN = Math.abs(position.y);

        // 2) 放大坐标后算噪声
        float px = position.x * SCL;
        float py = position.y * SCL;
        float pz = position.z * SCL;

        // 3) 边界噪声 → 和纬度 lerp 混合（Serpulo 做法）
        float tNoise = Simplex.noise3d(seed, BOUNDARY_OCT, 0.5f, BOUNDARY_SCALE,
            px, py + 999f, pz);
        float t = Mathf.lerp(latN, tNoise, BOUNDARY_LERP);
        t = Mathf.clamp(t);

        // 4) 硬切三段（无渐变）
        Color base;
        if (t < EQ_BOUND) {
            base = COLOR_EQUATOR;
        } else if (t < POLAR_BOUND) {
            base = COLOR_MID;
        } else {
            base = COLOR_POLAR;
        }

        // 5) zone 内随机替换（纯色斑块）
        float nI = Simplex.noise3d(seed + 1, INTRA_OCT, 0.5f, INTRA_SCALE,
            px, py + 370f, pz);
        if ((nI + 1f) * 0.5f < INTRA_CHANCE) {
            Color target;
            if (latN < EQ_BOUND) {
                target = COLOR_MID;
            } else if (latN < POLAR_BOUND) {
                target = Mathf.chance(0.5f) ? COLOR_EQUATOR : COLOR_POLAR;
            } else {
                target = COLOR_MID;
            }
            base = target;
        }

        // 6) 紫色体素：根据当前原色（base）选对应紫色变体
        float nP = Simplex.noise3d(seed + 2, PURPLE_OCT, 0.5f, PURPLE_SCALE,
            px, py + 99f, pz);
        if ((nP + 1f) * 0.5f < PURPLE_CHANCE) {
            if (base.equals(COLOR_EQUATOR)) {
                base = COLOR_PURPLE_EQ;
            } else if (base.equals(COLOR_POLAR)) {
                base = COLOR_PURPLE_POLAR;
            } else {
                // COLOR_MID 或被 zone 替换后的中间态
                base = COLOR_PURPLE_MID;
            }
        }

        out.set(base).a(1f);
    }

    @Override
    public float getHeight(Vec3 position) {
        // 1) 纬度 → baseline 线性插值（0°→0.9, 90°→0.75）
        float latN = Math.abs(position.y);
        float baseline = Mathf.lerp(BASELINE_EQUATOR, BASELINE_POLE, latN);

        // 2) 放大坐标后算噪声
        float px = position.x * HEIGHT_SCL;
        float py = position.y * HEIGHT_SCL;
        float pz = position.z * HEIGHT_SCL;
        float noise = Simplex.noise3d(seed + 100, HEIGHT_OCT, 0.5f, HEIGHT_SCALE,
            px, py + 500f, pz);

        // 3) 用户期望归一化距离 = baseline + noise × ROUGHNESS
        //    Mindustry 公式：归一化距离 = 1 + getHeight × intensity
        //    所以：getHeight = (baseline + noise × ROUGHNESS - 1) / intensity
        float normalizedDist = baseline + noise * ROUGHNESS;
        float height = (normalizedDist - 1f);   // intensity=1.0 时直接是偏移量

        return height;
    }
}
