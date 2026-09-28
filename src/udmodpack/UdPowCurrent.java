package udmodpack;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.WeatherState;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;

/**
 * 洋流发电机（PowCurrent）：当任何方向的 UdCurrentWeather 天气激活时，
 * 以指定速率产电。纯发电机，自身不带电池。
 * -top.png 贴图会随洋流方向旋转。
 *
 * 构造: (name, size, powerPerTick)
 */
public class UdPowCurrent extends Block{

    /** 每帧产电量（激活时） */
    public float powerPerTick;

    /** -top 贴图（朝向洋流） */
    public TextureRegion topRegion;

    /** 粒子发射节流（每几帧发一个）。默认 16 = 每秒约 4 个 */
    public int particleInterval = 40;

    public UdPowCurrent(String name, int size, float powerPerTick) {
        super(name);
        this.size = size;
        this.powerPerTick = powerPerTick;

        this.update = true;
        this.solid = true;
        this.rotate = false;
        this.group = BlockGroup.power;

        this.hasPower = true;
        this.outputsPower = true;
        this.conductivePower = true;
        this.consumesPower = false; // Block 默认 true，显式关掉

        this.buildType = UdPowCurrentBuild::new;
    }

    @Override
    public void load() {
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    public class UdPowCurrentBuild extends Building {

        /** 返回当前第一个激活的 UdCurrentWeather，无则 null */
        public UdCurrentWeather findActiveCurrent() {
            for(WeatherState ws : Groups.weather) {
                if(ws.weather() instanceof UdCurrentWeather uw) return uw;
            }
            return null;
        }

        public boolean hasCurrentWeather() {
            return findActiveCurrent() != null;
        }

        /** 洋流方向对应的角度（度）。Effect 和 Draw.rect 的旋转约定都是顺时针，
         * 而 atan2 是逆时针，所以统一取反。0° = 朝右（+X）。 */
        public float currentAngle() {
            UdCurrentWeather uw = findActiveCurrent();
            if(uw == null) return 0f;
            return -Mathf.atan2(uw.dirY, uw.dirX) * Mathf.radDeg;
        }

        @Override
        public float getPowerProduction() {
            return hasCurrentWeather() ? powerPerTick : 0f;
        }

        @Override
        public void updateTile() {
            // 粒子：节流发射，沿洋流方向
            // timer(id, frames) — 每 particleInterval 帧触发一次
            if(hasCurrentWeather() && timer(0, particleInterval) && Mathf.chance(0.15f)) {
                Fx.shootSmokeSquareSparse.wrap(Color.valueOf("80d4ff").a(0.7f), currentAngle() + 90f).at(x, y);
            }
        }

        @Override
        public void draw() {
            // base
            if(region.found()) {
                Draw.rect(region, x, y);
            }
            // top（有洋流时旋转，否则 0°）
            if(topRegion.found()) {
                float angle = hasCurrentWeather() ? currentAngle() : 0f;
                Draw.rect(topRegion, x, y, angle);
            }
        }
    }
}
