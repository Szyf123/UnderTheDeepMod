package udmodpack;

import arc.Core;
import mindustry.type.ItemStack;
import mindustry.type.Planet;
import mindustry.type.SectorPreset;

/**
 * 纯科技树占位节点。
 *
 * 继承 SectorPreset 以满足原版星球选择界面的强转要求
 * （MenuFragment.loadDeskTop 假设 Planet.techTree.root.content 是 SectorPreset）。
 *
 * 构造函数传 planet=null，在 UDTechTree.load() 里补赋值。
 * 因为 UDPlanet.deep 在 ContentLoader 之后才创建，不能在构造时传入。
 *
 * 重要：ContentLoader 会给模组 Content 的 name 自动加 mod 前缀
 * （如 "ud-rlyeh" → "ud-mod-ud-rlyeh"）。但 Mindustry atlas 打包用的是
 * 原始 name，bundle key 也用原始 name。所以我们存一份 originalName，
 * loadIcon() 和 localizedName() 都用它，而不是被改过的 this.name。
 */
public class UdTechPlaceholder extends SectorPreset {

    /** ContentLoader 改了 this.name 之前的原始名。用于 atlas key 和 bundle key。 */
    public final String originalName;

    /** 自定义研究消耗。为 null 时返回 ItemStack.empty（零消耗）。 */
    public ItemStack[] customResearchCost;

    /** 解锁时触发的回调。 */
    public Runnable onUnlockAction;

    /** 自定义图标贴图名（完整 key，跳过自动拼接）。 */
    public String iconName;

    /**
     * @param name           内部名称。bundle key：sector.{name}.name / .description
     * @param alwaysUnlocked 是否始终解锁（根节点建议 true）
     */
    public UdTechPlaceholder(String name, boolean alwaysUnlocked) {
        super(name, (Planet)null, 0);
        this.originalName = name;
        this.alwaysUnlocked = alwaysUnlocked;
        this.hideDatabase = true;
        this.hideDetails = true;
        this.customResearchCost = null;
        this.onUnlockAction = null;
        this.iconName = null;
    }

    /** 默认 alwaysUnlocked = false。 */
    public UdTechPlaceholder(String name) {
        this(name, false);
    }

    /**
     * 重写 initialize 跳过父类 FileMapGenerator 创建。
     * 父类 SectorPreset.initialize() 内部调 FileMapGenerator，
     * 它需要 preset.planet.name——如果 planet=null 就会 NPE。
     *
     * 我们的占位节点不需要地图生成/规则/目标，只需 planet 字段。
     */
    @Override
    public void initialize(Planet planet, int sectorIndex) {
        this.planet = planet;
    }

    @Override
    public void initialize(Planet planet, int sectorIndex, boolean add) {
        this.planet = planet;
    }

    /** 带自定义研究消耗的便捷构造。 */
    public UdTechPlaceholder(String name, boolean alwaysUnlocked, ItemStack[] researchCost) {
        this(name, alwaysUnlocked);
        this.customResearchCost = researchCost;
    }

    /** 带研究消耗 + 解锁回调的完整构造。 */
    public UdTechPlaceholder(String name, boolean alwaysUnlocked,
                             ItemStack[] researchCost, Runnable onUnlockAction) {
        this(name, alwaysUnlocked, researchCost);
        this.onUnlockAction = onUnlockAction;
    }

    @Override
    public ItemStack[] researchRequirements() {
        return customResearchCost != null ? customResearchCost : ItemStack.empty;
    }

    @Override
    public void onUnlock() {
        super.onUnlock();
        if(onUnlockAction != null) {
            onUnlockAction.run();
        }
    }

    @Override
    public void loadIcon() {
        // Mindustry atlas key 格式：
        //   对于模组内容：{modName}-{contentTypeName}-{originalName}
        //   对于原版内容：{contentTypeName}-{name}
        // 我们需要用 originalName（没被 ContentLoader 加前缀的那个）
        if(iconName != null) {
            // 直接用完整 key
            if(Core.atlas.has(iconName)) {
                fullIcon = Core.atlas.find(iconName);
            } else {
                fullIcon = Core.atlas.find("ud-mod-" + iconName);
            }
            String uiKey = iconName + "-ui";
            uiIcon = Core.atlas.has(uiKey) ? Core.atlas.find(uiKey)
                   : Core.atlas.has("ud-mod-" + uiKey) ? Core.atlas.find("ud-mod-" + uiKey)
                   : fullIcon;
        } else {
            // 按标准格式拼，用 originalName
            String stdKey = "sector-" + originalName;
            String modKey = "ud-mod-" + stdKey;
            fullIcon = Core.atlas.has(stdKey) ? Core.atlas.find(stdKey)
                     : Core.atlas.has(modKey) ? Core.atlas.find(modKey)
                     : Core.atlas.find(modKey); // 兜底返回 error 贴图

            String uiStd = stdKey + "-ui";
            String uiMod = modKey + "-ui";
            uiIcon = Core.atlas.has(uiStd) ? Core.atlas.find(uiStd)
                   : Core.atlas.has(uiMod) ? Core.atlas.find(uiMod)
                   : fullIcon;
        }
    }

    @Override
    public boolean isHidden() {
        return true;
    }
}
