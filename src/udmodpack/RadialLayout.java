package udmodpack;

import arc.struct.ObjectMap;
import mindustry.ui.dialogs.ResearchDialog.TechTreeNode;

import static arc.math.Mathf.*;

/**
 * 同心圆辐射布局。
 *
 * 算法：
 *  1. 后序遍历算 Count（子树叶子数），存到 Map<TechTreeNode, Integer>
 *  2. 前序遍历分配扇形：
 *     - 根节点 start=-90°（正上方），arc=360°，depth=0 → (0,0)
 *     - 每个子节点 arc = childCount / parentCount * parentArc
 *     - 子节点起始角 = parentStart + Σ(前面兄弟的 arc)
 *     - 中心角 = 自身起始 + 自身arc/2
 *  3. 径向转直角：x = depth*DepthDelta*cos(θ), y = depth*DepthDelta*sin(θ)
 *
 *  父子扇形包含：子节点的 arc 是它在父节点 arc 中按 Count 比例分到的部分。
 */
public class RadialLayout {

    /** 每层向外的距离增量（像素），默认 100，用户可调。 */
    public static float DepthDelta = 100f;

    /** Count 临时缓存——每次 layout 时创建。 */
    static final ObjectMap<TechTreeNode, Integer> counts = new ObjectMap<>();

    /** 执行布局，结果直接写回 node.x / node.y。依赖 node.children。 */
    public static void layout(TechTreeNode root) {
        counts.clear();
        computeCount(root);
        place(root, -90f, 360f, 0);
    }

    /**
     * 执行布局，从 savedChildren 读原始 children 结构（不依赖 node.children 字段）。
     * 用于 node.children 已被清空的情况。
     */
    public static void layoutWithSaved(TechTreeNode root, ObjectMap<TechTreeNode, TechTreeNode[]> savedChildren) {
        counts.clear();
        computeCountSaved(root, savedChildren);
        placeSaved(root, -90f, 360f, 0, savedChildren);
    }

    /** 后序：从 node.children 读。 */
    static int computeCount(TechTreeNode node) {
        if(node.children == null || node.children.length == 0) {
            counts.put(node, 1);
            return 1;
        }
        int sum = 0;
        for(TechTreeNode child : node.children) {
            sum += computeCount(child);
        }
        counts.put(node, sum);
        return sum;
    }

    /** 后序：从 savedChildren 读。 */
    static int computeCountSaved(TechTreeNode node, ObjectMap<TechTreeNode, TechTreeNode[]> saved) {
        TechTreeNode[] children = saved.get(node);
        if(children == null || children.length == 0) {
            counts.put(node, 1);
            return 1;
        }
        int sum = 0;
        for(TechTreeNode child : children) {
            sum += computeCountSaved(child, saved);
        }
        counts.put(node, sum);
        return sum;
    }

    /** 前序放置，从 node.children 读。 */
    static void place(TechTreeNode node, float startAngle, float arcAngle, int depth) {
        if(depth == 0) {
            node.x = 0;
            node.y = 0;
        } else {
            float centerAngle = startAngle + arcAngle / 2f;
            float radius = depth * DepthDelta;
            float rad = centerAngle * degreesToRadians;
            node.x = radius * cos(rad);
            node.y = radius * sin(rad);
        }

        if(node.children != null && node.children.length > 0) {
            int parentCount = counts.get(node);
            float childStart = startAngle;
            for(TechTreeNode child : node.children) {
                int childCount = counts.get(child);
                float childArc = (float)childCount / parentCount * arcAngle;
                place(child, childStart, childArc, depth + 1);
                childStart += childArc;
            }
        }
    }

    /** 前序放置，从 savedChildren 读。 */
    static void placeSaved(TechTreeNode node, float startAngle, float arcAngle, int depth, ObjectMap<TechTreeNode, TechTreeNode[]> saved) {
        if(depth == 0) {
            node.x = 0;
            node.y = 0;
        } else {
            float centerAngle = startAngle + arcAngle / 2f;
            float radius = depth * DepthDelta;
            float rad = centerAngle * degreesToRadians;
            node.x = radius * cos(rad);
            node.y = radius * sin(rad);
        }

        TechTreeNode[] children = saved.get(node);
        if(children != null && children.length > 0) {
            int parentCount = counts.get(node);
            float childStart = startAngle;
            for(TechTreeNode child : children) {
                int childCount = counts.get(child);
                float childArc = (float)childCount / parentCount * arcAngle;
                placeSaved(child, childStart, childArc, depth + 1, saved);
                childStart += childArc;
            }
        }
    }
}
