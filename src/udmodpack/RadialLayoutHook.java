package udmodpack;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.ui.layout.Scl;
import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.ResearchDialog.TechTreeNode;

/**
 * 同心圆布局 + 背景圆环 + 直线连线 hook。
 *
 * 核心策略（每帧循环）：
 *   1. 只要 node.children 非空 → 存入 savedChildren（捕获 vanilla 初始化时机）
 *   2. 用 savedChildren 做所有事（算深度、布局、画直线）
 *   3. 清空 node.children（阻止原版折线）
 *   不做任何 close/reopen 状态管理——vanilla 重建树时 children 会重新变非空，下一帧自动捕获。
 */
public class RadialLayoutHook {

    public static int ringCount = 6;
    public static Color ringColor = new Color(0.3f, 0.5f, 0.8f, 0.25f);
    public static float ringStroke = 2.5f;

    static int currentMaxDepth = 0;
    static final ObjectMap<TechTreeNode, TechTreeNode[]> savedChildren = new ObjectMap<>();
    static Element bgActor = null;
    /** 完整初始化过至少一帧 → bgActor 才真正画东西 */
    static boolean ready = false;

    public static void init() {
        Core.app.post(RadialLayoutHook::tick);
        Log.info("[RadialLayoutHook] Initialized");
    }

    static void tick() {
        try {
            if(Vars.ui == null || Vars.ui.research == null) {
                Core.app.post(RadialLayoutHook::tick);
                return;
            }
            if(!Vars.ui.research.isShown()) {
                ready = false;
                if(bgActor != null && bgActor.parent != null) {
                    bgActor.remove();
                }
                bgActor = null;
                Core.app.post(RadialLayoutHook::tick);
                return;
            }
            TechTreeNode root = Vars.ui.research.root;
            if(root == null || root.node == null) {
                Core.app.post(RadialLayoutHook::tick);
                return;
            }
            if(root.node.planet != UDPlanet.deep) {
                Core.app.post(RadialLayoutHook::tick);
                return;
            }

            // ===== 每帧：把当前非空的 children 存入 savedChildren =====
            // 不管是首次打开还是 close→reopen 后 vanilla 重建的 fresh children
            for(TechTreeNode n : Vars.ui.research.nodes) {
                if(n.children != null && n.children.length > 0) {
                    savedChildren.put(n, n.children);
                }
            }

            // ===== 没有可用的 savedChildren？等 vanilla 初始化完 =====
            if(!savedChildren.containsKey(root)) {
                Core.app.post(RadialLayoutHook::tick);
                return;
            }

            // ===== 用 savedChildren 算最大深度 =====
            currentMaxDepth = findMaxDepthFromSaved(root);

            // ===== 用 savedChildren 做同心圆布局 =====
            RadialLayout.layoutWithSaved(root, savedChildren);

            // ===== 清空 children → 原版 drawChildren 折线消失 =====
            for(TechTreeNode n : Vars.ui.research.nodes) {
                n.children = new TechTreeNode[0];
            }

            ready = true;

            // ===== bounds =====
            float minx = 0f, miny = 0f, maxx = 0f, maxy = 0f;
            for(TechTreeNode n : Vars.ui.research.nodes) {
                if(!n.visible) continue;
                minx = Math.min(n.x - n.width/2f, minx);
                maxx = Math.max(n.x + n.width/2f, maxx);
                miny = Math.min(n.y - n.height/2f, miny);
                maxy = Math.max(n.y + n.height/2f, maxy);
            }
            Vars.ui.research.bounds.set(minx, miny, maxx - minx, maxy - miny);
            Vars.ui.research.bounds.y += Vars.ui.research.nodeSize * 1.5f;

            ensureBgActor();

        } catch(Exception e) {
            Log.err("[RadialLayoutHook] Error: " + e.getMessage());
        }

        Core.app.post(RadialLayoutHook::tick);
    }

    static int findMaxDepthFromSaved(TechTreeNode node) {
        TechTreeNode[] children = savedChildren.get(node);
        if(children == null || children.length == 0) return 0;
        int max = 0;
        for(TechTreeNode c : children) max = Math.max(max, findMaxDepthFromSaved(c));
        return max + 1;
    }

    static void ensureBgActor() {
        if(Vars.ui.research.view == null) return;
        if(bgActor != null && bgActor.parent == Vars.ui.research.view) return;

        final mindustry.ui.dialogs.ResearchDialog.View vRef = Vars.ui.research.view;

        bgActor = new Element() {
            { setPosition(0, 0); setSize(0, 0); setOrigin(0, 0); }

            @Override
            public void draw() {
                if(!ready) return;
                try {
                    mindustry.ui.dialogs.ResearchDialog.View v =
                        Vars.ui.research == null ? vRef : Vars.ui.research.view;
                    if(v == null) return;

                    float cx = v.panX + v.getWidth() / 2f;
                    float cy = v.panY + v.getHeight() / 2f;

                    // 同心圆环
                    int rings = Math.min(ringCount, currentMaxDepth + 1);
                    Lines.stroke(Scl.scl(ringStroke), ringColor);
                    for(int i = 1; i <= rings; i++) {
                        Lines.circle(cx, cy, RadialLayout.ringBaseRadius + i * RadialLayout.DepthDelta);
                    }

                    // 直线连线
                    for(TechTreeNode node : Vars.ui.research.nodes) {
                        if(!node.visible) continue;
                        TechTreeNode[] origChildren = savedChildren.get(node);
                        if(origChildren == null) continue;
                        for(TechTreeNode child : origChildren) {
                            if(!child.visible) continue;
                            boolean lock = !child.node.content.unlockedHost();
                            Lines.stroke(Scl.scl(5f), lock ? Pal.gray : Pal.accent);
                            Lines.line(
                                node.x + cx, node.y + cy,
                                child.x + cx, child.y + cy
                            );
                        }
                    }
                } catch(Exception ignored) {}
            }
        };

        Vars.ui.research.view.addChildAt(0, bgActor);
    }
}
