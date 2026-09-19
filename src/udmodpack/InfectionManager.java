package udmodpack;

import arc.Events;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.world.Tile;

/** 全局感染管理：扩散 + 建筑伤害。
 *  净化由减压场建筑自己完成。
 *  配对数据统一由 UDModMain 维护，本类只读 UDModMain.infectPair。
 *  v160: Events.run(Trigger.afterGameUpdate, ...) 每帧触发 */
public class InfectionManager {

    // ===== 静态参数（后续可改为配置驱动）=====
    /** 扩散半径（曼哈顿距离） */
    public static int INFECTION_STRENGTH  = 3;
    /** 每轮处理坐标数 */
    public static int INFECTION_NUMBER    = 5;
    /** 循环间隔（帧数），60帧≈1秒 */
    public static int INFECTION_FRAMES    = 50;
    /** 已感染地板对建筑的单次伤害 */
    public static float INFECTION_DAMAGE  = 10.0f;

    // ===== 内部状态 =====
    private static int frameCounter = 0;

    /** 在 loadContent() 末尾调用，挂 Trigger.afterGameUpdate 做降频循环。 */
    public static void init() {
        Events.run(Trigger.afterGameUpdate, () -> {
            if (Vars.state == null || Vars.state.isMenu()) return;
            frameCounter++;
            if (frameCounter >= INFECTION_FRAMES) {
                frameCounter = 0;
                runCycle();
            }
        });
    }

    /** 每轮循环入口 */
    private static void runCycle() {
        var world = Vars.world;
        if (world == null || UDModMain.infectPair.isEmpty()) return;

        int w = world.width();
        int h = world.height();

        for (int i = 0; i < INFECTION_NUMBER; i++) {
            Tile tile = world.tile(Mathf.random(w - 1), Mathf.random(h - 1));
            if (tile != null) processTile(tile);
        }
    }

    /** 处理单个随机选中的坐标 */
    private static void processTile(Tile tile) {
        if (!(tile.floor() instanceof UdBasicFloor)) return;
        UdBasicFloor ud = (UdBasicFloor) tile.floor();

        if (ud.isInfected) {
            if (tile.build != null && tile.build.health > 0) {
                tile.build.damage(INFECTION_DAMAGE);
            }
        } else if (ud.canBeInfected) {
            UdBasicFloor infected = UDModMain.infectPair.get(ud);
            if (infected != null && hasInfectedSource(tile)) {
                tile.setFloor(infected);
            }
        }
    }

    /** 曼哈顿距离 INFECTION_STRENGTH 内是否存在感染地块 */
    private static boolean hasInfectedSource(Tile target) {
        int sx = target.x, sy = target.y;
        for (int dy = -INFECTION_STRENGTH; dy <= INFECTION_STRENGTH; dy++) {
            for (int dx = -INFECTION_STRENGTH; dx <= INFECTION_STRENGTH; dx++) {
                if (Math.abs(dx) + Math.abs(dy) > INFECTION_STRENGTH) continue;
                Tile other = Vars.world.tile(sx + dx, sy + dy);
                if (other == null) continue;
                if (other.floor() instanceof UdBasicFloor && ((UdBasicFloor) other.floor()).isInfected) return true;
            }
        }
        return false;
    }
}
