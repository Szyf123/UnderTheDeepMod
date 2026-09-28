package udmodpack;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.WeatherState;
import mindustry.gen.Unit;
import mindustry.type.Weather;

/**
 * 自定义洋流天气：
 *  - 构造时烤入角度（度数）→ 归一化方向向量 dirX/dirY
 *  - update()：推进单位 + 更新粒子位置
 *  - drawOver()：绘制半透明圆点粒子（随机速度沿方向运动）
 *
 * Mindustry 坐标系：Y 轴向下为正。
 * 角度约定：0°=向右(+X), 90°=向下(+Y), 180°=向左(-X), 270°=向上(-Y)。
 */
public class UdCurrentWeather extends Weather {

    /** 归一化方向向量 */
    public final float dirX;
    public final float dirY;

    /** 推力强度系数（与 state.intensity 相乘后施加给单位） */
    public final float force;

    /** 粒子颜色 */
    public Color particleColor = new Color(0.4f, 0.7f, 0.9f, 0.5f);

    /** 单帧粒子数量上限 */
    public static final int PARTICLE_COUNT = 220;

    /** 粒子速度范围（世界单位/秒，Mindustry 里 1 tile = 32 wu） */
    public static final float MAX_SPEED = 8f;
    public static final float MIN_SPEED = 2f;

    /** 粒子半径范围（像素） */
    public static final float MAX_RADIUS = 3.5f;
    public static final float MIN_RADIUS = 1.5f;

    /** 推力经验系数（单位加速度 = dir × force × intensity × PUSH_COEF / tick） */
    public static final float PUSH_COEF = 0.01f;

    /** 日志节流：避免刷屏 */
    private static int logCounter = 0;

    // ---- 粒子数据（transient，不序列化） ----

    private transient float[] px;       // 世界坐标 x
    private transient float[] py;       // 世界坐标 y
    private transient float[] pspeed;   // 随机速度
    private transient float[] pradius;  // 随机半径
    private transient float[] pphase;   // 颜色相位（微调用）
    private transient boolean initialized = false;

    // ===== 构造 =====

    /**
     * @param name            Content 名称
     * @param angleDegrees    方向角度（度）：0=向右，90=向下，180=向左，270=向上
     * @param force           推力强度（推荐 0.8f ~ 1.5f）
     */
    public UdCurrentWeather(String name, float angleDegrees, float force) {
        super(name);
        float rad = angleDegrees * Mathf.degRad;
        this.dirX = Mathf.cos(rad);
        this.dirY = Mathf.sin(rad);
        this.force = force;

        // Weather 基类字段
        this.duration = 3000f;  // 单次默认 ~50 秒
        this.hidden = false;    // 出现在编辑器下拉框
        this.opacityMultiplier = 1f;
    }

    // ===== 每帧更新：粒子位置 + 单位推力 =====

    @Override
    public void update(WeatherState state) {
        // 日志节流：每 60 tick 打印一次，确认引擎真的在调
        if (logCounter++ % 60 == 0) {
            Log.info("[UDCurrent] update called: name=" + name +
                     ", intensity=" + state.intensity() +
                     ", dirX=" + dirX + ", dirY=" + dirY);
        }

        // 初始化粒子（首帧）
        if (!initialized) {
            initParticles();
            initialized = true;
            Log.info("[UDCurrent] particles initialized: " + PARTICLE_COUNT +
                     ", worldSize=" + (Vars.world == null ? "null" :
                                        (Vars.world.width() * Vars.tilesize) + "x" +
                                        (Vars.world.height() * Vars.tilesize)));
        }

        // 1. 更新粒子位置
        updateParticles(state.intensity());

        // 2. 推单位
        pushUnits(state.intensity());
    }

    // ===== 粒子渲染 =====

    @Override
    public void drawOver(WeatherState state) {
        if (Vars.headless || !initialized || px == null) return;

        float intensityMul = state.intensity();

        Draw.alpha(1f);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float x = px[i], y = py[i];

            float fade = intensityMul * (0.7f + 0.3f * Mathf.absin(Time.time * 0.03f + pphase[i], 2f));
            float radius = pradius[i];

            Color c = particleColor;
            float a = c.a * fade;

            Draw.color(c.r, c.g, c.b, a);
            Fill.circle(x, y, radius);
        }
        Draw.color();
    }

    // ===== 粒子逻辑 =====

    private void initParticles() {
        if (Vars.world == null) return;
        int w = Vars.world.width() * Vars.tilesize;
        int h = Vars.world.height() * Vars.tilesize;

        px = new float[PARTICLE_COUNT];
        py = new float[PARTICLE_COUNT];
        pspeed = new float[PARTICLE_COUNT];
        pradius = new float[PARTICLE_COUNT];
        pphase = new float[PARTICLE_COUNT];

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            px[i] = Mathf.random(0, w);
            py[i] = Mathf.random(0, h);
            pspeed[i] = Mathf.random(MIN_SPEED, MAX_SPEED);
            pradius[i] = Mathf.random(MIN_RADIUS, MAX_RADIUS);
            pphase[i] = Mathf.random(0, Mathf.PI * 2);
        }
    }

    private void updateParticles(float intensity) {
        if (Vars.world == null) return;
        float w = Vars.world.width() * Vars.tilesize;
        float h = Vars.world.height() * Vars.tilesize;

        float dt = Time.delta;  // 秒
        float dirXd = dirX, dirYd = dirY;

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // pspeed 单位：世界单位/秒
            // dt 单位：秒
            // 所以 delta pos = speed * dt  → 世界单位
            float s = pspeed[i] * intensity;
            px[i] += dirXd * s * dt;
            py[i] += dirYd * s * dt;

            // 世界边界对侧重生
            if (dirXd > 0 && px[i] > w + 10f) px[i] = -10f;
            else if (dirXd < 0 && px[i] < -10f) px[i] = w + 10f;

            if (dirYd > 0 && py[i] > h + 10f) py[i] = -10f;
            else if (dirYd < 0 && py[i] < -10f) py[i] = h + 10f;
        }
    }

    // ===== 单位推力 =====

    private void pushUnits(float intensity) {
        if (intensity <= 0f) return;
        float fx = dirX * force * intensity * PUSH_COEF;
        float fy = dirY * force * intensity * PUSH_COEF;

        for (Team team : Team.all) {
            var data = team.data();
            if (data == null) continue;
            data.units.each((Unit u) -> {
                if (u.dead()) return;
                u.vel().add(fx, fy);
            });
        }
    }
}
