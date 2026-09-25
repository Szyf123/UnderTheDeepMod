package udmodpack;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.PixmapRegion;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.TileOverlayChangeEvent;
import mindustry.graphics.Layer;
import mindustry.graphics.MultiPacker;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OverlayFloor;

import java.util.Iterator;

/**
 * 通用叠加地板：变体 + 可选动画帧 + 底部阴影，不绑定 itemDrop。
 * 产出逻辑由外部建筑（如 UdOreMiner）通过 overlay 身份比对自行处理。
 *
 * 贴图命名约定（由 animFrames 决定寻址规则）：
 * <pre>
 *   animFrames = 1（静态）
 *     name1   name2   name3   name4   ...
 *
 *   animFrames > 1（动画）
 *     name1-1  name1-2  ...  name1-M   ← 变体1 的 M 帧动画
 *     name2-1  name2-2  ...  name2-M   ← 变体2
 *     ...
 *     nameN-1  nameN-2  ...  nameN-M   ← 变体N
 *     （没有裸的 name1/name2，帧 1 就是静态 fallback）
 * </pre>
 */
public class UdOverlayFloor extends OverlayFloor {

    // === 动画 tile 注册表 ===
    // 原版 BlockRenderer.updateFloors 是 private 且只在编辑器的 reload() 里填充，
    // 正常游戏中永远为空。所以自己维护 tile 列表，用 Trigger.draw 每帧渲染。
    // 不能用 Trigger.update（Logic.update 阶段）——那里 Draw 命令会被 Renderer 先 flush，
    // 紧接着 FloorRenderer.drawFloor() 重画 chunk 会覆盖掉。Trigger.draw 在渲染阶段，
    // Draw.z(Layer.floor + 1f) 和 FloorRenderer(Layer.floor) 在同一个 z-sort 批次里，自然排在上层。
    private static final Seq<Tile> animatedTiles = new Seq<>();
    private static boolean registeredTick = false;

    static {
        Events.on(WorldLoadEvent.class, e -> {
            animatedTiles.clear();
            if(Vars.world == null) return;
            for(Tile tile : Vars.world.tiles){
                if(tile.overlay() instanceof UdOverlayFloor uf && uf.updateRender(tile)){
                    animatedTiles.add(tile);
                }
            }
        });

        // 编辑器/运行时动态放矿块：新 overlay 是 UdOverlayFloor 且启用动画 → 加入列表
        Events.on(TileOverlayChangeEvent.class, e -> {
            if(e.overlay instanceof UdOverlayFloor uf && uf.updateRender(e.tile)){
                if(!animatedTiles.contains(e.tile)) animatedTiles.add(e.tile);
            }
        });

        if(!registeredTick){
            registeredTick = true;
            Events.run(mindustry.game.EventType.Trigger.draw, () -> {
                if(animatedTiles.isEmpty()) return;
                Draw.z(Layer.floor + 1f);
                // 用 iterator 遍历，同时安全移除已失效的 tile（overlay 被删除/改为 AirBlock）
                Iterator<Tile> it = animatedTiles.iterator();
                while(it.hasNext()){
                    Tile tile = it.next();
                    if(tile.overlay() instanceof UdOverlayFloor uf){
                        uf.renderUpdateImpl(tile);
                    } else {
                        it.remove();
                    }
                }
            });
        }
    }

    /** 每个变体的动画帧数；=1 静态寻址 nameN，>1 动画寻址 nameN-M */
    public int animFrames = 1;
    /** 每帧切换间隔（帧）；<=0 则不启用动画渲染（仅寻址规则受 animFrames 控制） */
    public float animFrameTime = 0f;

    /** 动画帧纹理 [variant][frame]；animFrames==1 时为 null */
    public TextureRegion[][] animRegions;

    /**
     * @param name           内容名
     * @param variants       哈希变体数量
     * @param animFrames     每个变体的动画帧数；=1 → 静态寻址 nameN，>1 → 动画寻址 nameN-M
     * @param animFrameTime  每帧切换间隔（帧）；<=0 则不启用动画渲染
     */
    public UdOverlayFloor(String name, int variants, int animFrames, float animFrameTime) {
        super(name);
        this.variants = variants;
        this.animFrames = animFrames;
        this.animFrameTime = animFrameTime;
    }

    /** 静态叠加层（name1~nameN 寻址）。 */
    public UdOverlayFloor(String name, int variants) {
        this(name, variants, 1, 0f);
    }

    /** 默认 3 变体，静态。 */
    public UdOverlayFloor(String name) {
        this(name, 3, 1, 0f);
    }

    /**
     * Floor.load() 在 ContentLoader.load() 阶段被调（晚于 init()），此时才会
     * 创建 variantRegions 数组。animFrames > 1 时，在 super.load() 之后
     * 覆盖每个 variantRegions[v] 为 animRegions[v][0]（动画第 1 帧作静态 fallback）。
     *
     * 不在 init() 里设 variantRegions——Floor.load() 会重新 new 整个数组覆盖掉。
     */
    @Override
    public void load() {
        super.load();  // Floor.load() 创建 variantRegions 数组并填 Core.atlas.find(name + N)

        if (animFrames > 1) {
            animRegions = new TextureRegion[variants][animFrames];
            for (int v = 0; v < variants; v++) {
                for (int f = 0; f < animFrames; f++) {
                    animRegions[v][f] = Core.atlas.find(name + (v + 1) + "-" + (f + 1));
                }
                // 覆盖 Floor.load() 填的裸 nameN（用户没提供裸文件），改为 nameN-1
                variantRegions[v] = animRegions[v][0];
            }
        }

        // loadIcon() 早于 load() 执行，找不到裸 name / name1 贴图。
        // 用 variantRegions[0]（= animRegions[0][0]）作为 fullIcon / uiIcon 兜底，
        // 否则编辑器 MapEditorDialog.rebuildBlockSelection() 里 Core.atlas.isFound(region)
        // 返回 false 会把这个块跳过。
        if (variantRegions != null && variantRegions.length > 0 && variantRegions[0].found()) {
            fullIcon = variantRegions[0];
            uiIcon = variantRegions[0];
        }
    }

    /**
     * chunk 缓存里永远画静态 fallback（variantRegions）。
     * animFrames==1：走 Floor.load() 默认的 nameN
     * animFrames>1 且 animFrameTime>0（启用动画）：重写为空——动画帧完全由 Trigger.draw
     *   在 Layer.floor+1f 绘制，不让帧 1 常驻 chunk FBO 形成"两层叠加"的视觉问题。
     * animFrames>1 但 animFrameTime<=0（有动画贴图但不启用动画）：画帧 1 兜底。
     */
    @Override
    public void drawBase(Tile tile) {
        // 启用动画时让 chunk FBO 的 overlay 层留空，完全由 Trigger.draw 接管
        if(animFrames > 1 && animFrameTime > 0f) return;

        int variant = Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1));
        Draw.rect(variantRegions[variant], tile.worldx(), tile.worldy());
    }

    /** 有动画帧 & 帧间隔有效时，让每帧更新接管渲染。 */
    @Override
    public boolean updateRender(Tile tile) {
        return animRegions != null && animFrameTime > 0f;
    }

    /**
     * 原版 Floor.updateRender / renderUpdate 接口——理论上被 BlockRenderer 调。
     * 但 BlockRenderer.updateFloors 列表是 private 且只在编辑器 reload() 时填充，
     * 所以这两个方法在游戏里实际上不会被 BlockRenderer 调用。
     * renderUpdateImpl() 才是真正被 Trigger.draw 每帧调的实现。
     */
    @Override
    public void renderUpdate(Floor.UpdateRenderState state) {
        renderUpdateImpl(state.tile);
    }

    /**
     * 每帧执行的动态渲染。画在 Layer.floor+1，覆盖 chunk 里 drawBase 留下的静态图。
     */
    private void renderUpdateImpl(Tile tile) {
        if(animRegions == null || animFrameTime <= 0f) return;
        int variant = Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1));

        int phase = tile.pos() % animFrames;
        int frame = (int) (Time.globalTime / animFrameTime + phase) % animFrames;
        TextureRegion region = animRegions[variant][frame];
        if (region == null || !region.found()) return;

        Draw.rect(region, tile.worldx(), tile.worldy());
    }

    /**
     * 为贴图添加底部阴影（复刻 OreBlock.createIcons 的阴影逻辑）。
     * 先调 super（处理 edge blend 贴图等 Floor 基类需要的东西），再叠加阴影。
     */
    @Override
    public void createIcons(MultiPacker packer) {
        super.createIcons(packer);

        if (animFrames > 1) {
            // 动画寻址：遍历 nameN-M
            for (int v = 0; v < variants; v++) {
                for (int f = 0; f < animFrames; f++) {
                    applyShadow(packer, name + (v + 1) + "-" + (f + 1));
                }
            }
        } else {
            // 静态寻址：遍历 nameN
            for (int i = 0; i < variants; i++) {
                applyShadow(packer, name + (i + 1));
            }
        }
    }

    /** 对 packer 中指定名字的贴图做阴影处理并写回。不存在则跳过。 */
    private void applyShadow(MultiPacker packer, String texName) {
        PixmapRegion shadow = packer.get(texName);
        if (shadow == null) return;

        Pixmap image = shadow.crop();
        int offset = image.width / Vars.tilesize - 1;
        int shadowColor = Color.rgba8888(0, 0, 0, 0.3f);

        for (int x = 0; x < image.width; x++) {
            for (int y = offset; y < image.height; y++) {
                if (shadow.getA(x, y) == 0 && shadow.getA(x, y - offset) != 0) {
                    image.setRaw(x, y, shadowColor);
                }
            }
        }

        packer.add(MultiPacker.PageType.environment, texName, image);
        image.dispose();
    }
}
