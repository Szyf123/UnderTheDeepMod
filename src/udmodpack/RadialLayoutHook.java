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
 * 核心策略：
 *   savedChildren 只在第一次 children 完整时填充，永远不清空。
 *   findMaxDepth + RadialLayout.layout + 画直线 → 都用 savedChildren（原始完整结构）。
 *   真正画的时候用空 children（阻止原版折线）。
 */
public class RadialLayoutHook {

    public static int ringCount = 5;
    public static Color ringColor = new Color(0.3f, 0.5f, 0.8f, 0.25f);
    public static float ringStroke = 2.5f;

    static int currentMaxDepth = 0;
    /** node → 原始 children 数组。只在首次（children 完整时）填充，之后永久复用。*/
    static final ObjectMap<TechTreeNode, TechTreeNode[]> savedChildren = new ObjectMap<>();
    static Element bgActor = null;
    /** 标记 savedChildren 是否已填好原始数据 */
    static boolean savedChildrenValid = false;

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
                // Dialog 关掉了，重置标记，下次重新填
                savedChildrenValid = false;
                savedChildren.clear();
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

            // ===== 1. 仅在 children 完整时保存一次原始结构 =====
            // 第一次进来或重置后：root.children 应该还是原版完整数组
            // 用 root.children.length > 0 作为判断（根节点不可能没子节点）
            if(!savedChildrenValid && root.children != null && root.children.length > 0) {
                savedChildren.clear();
                for(TechTreeNode n : Vars.ui.research.nodes) {
                    savedChildren.put(n, n.children);
                }
                savedChildrenValid = true;
                Log.info("[RadialLayoutHook] Saved original children for " + savedChildren.size + " nodes");
            }

            if(!savedChildrenValid) {
                // children 还是空的（原版还没初始化完），等下一帧
                Core.app.post(RadialLayoutHook::tick);
                return;
            }

            // ===== 2. 用 savedChildren 算最大深度（永远正确）=====
            currentMaxDepth = findMaxDepthFromSaved(root);

            // ===== 3. 用 savedChildren 做同心圆布局（RadialLayout 需要遍历完整 children）=====
            RadialLayout.layoutWithSaved(root, savedChildren);

            // ===== 4. 清空 children → 原版 drawChildren 拿不到 children → 折线消失 =====
            for(TechTreeNode n : Vars.ui.research.nodes) {
                if(n.children.length > 0) {
                    n.children = new TechTreeNode[0];
                }
            }

            // ===== 5. bounds =====
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

    /** 从 savedChildren 递归算深度（不依赖 node.children）*/
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
                try {
                    mindustry.ui.dialogs.ResearchDialog.View v =
                        Vars.ui.research == null ? vRef : Vars.ui.research.view;
                    if(v == null) return;

                    float cx = v.panX + v.getWidth() / 2f;
                    float cy = v.panY + v.getHeight() / 2f;

                    // ===== 1. 同心圆环 =====
                    int rings = Math.min(ringCount, currentMaxDepth + 1);
                    Lines.stroke(Scl.scl(ringStroke), ringColor);
                    for(int i = 1; i <= rings; i++) {
                        Lines.circle(cx, cy, i * RadialLayout.DepthDelta);
                    }

                    // ===== 2. 直线连线（用保存的 children 数组）=====
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
