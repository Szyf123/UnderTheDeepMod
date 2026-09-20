package udmodpack;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.Floor;

/** 边界虚空地板（环境墙）— 自定义 8 方向 autotile，根据金属瓦片（metal-tiles）方案重写。
 *
 *  【8 方向邻居 → 贴图坐标 + 旋转】
 *    GetSurround: 扫描 LU,U,RU,L,R,LD,D,RD 8 方向，填 bool 字段
 *    GetTempPoint: 用 U/L/R/D + 4 对角 → tempX, tempY（4×4 grid）+ tempR（逆时针旋转 ×90°）
 *    Draw: regions[splitRow][splitCol], 旋转 -tempR*90（负号 = 逆时针，Arc 正值为顺时针）
 *
 *  【贴图布局】4×4 grid (tempX=col, tempY=row, row=0 顶部):
 *    ┌────────┬────────┬────────┬────────┐
 *    │ [0,0]  │ [1,0]  │ [2,0]  │ [3,0]  │
 *    │ isolated│  填充   │  T形   │  十字  │
 *    ├────────┼────────┼────────┼────────┤
 *    │ [0,1]  │ [1,1]  │ [2,1]  │ [3,1]  │
 *    │  edge   │  T形   │  十字   │ 纵向   │
 *    ├────────┼────────┼────────┼────────┤
 *    │ [0,2]  │ [1,2]  │ [2,2]  │ [3,2]  │
 *    │ corner  │  T形   │  十字   │ 纵向   │
 *    ├────────┼────────┼────────┼────────┤
 *    │ [0,3]  │ [1,3]  │ [2,3]  │ [3,3]  │
 *    │ corner  │  横向   │  十字   │  十字  │
 *    └────────┴────────┴────────┴────────┘
 */
public class UdVoidFloor extends Floor {

    // ===== 8 方向邻居 bool 字段（GetSurround 填充） =====
    private boolean LU, U, RU, L, R, LD, D, RD;

    // ===== GetTempPoint 输出 =====
    private int tempX, tempY, tempR;

    // ===== 贴图 =====
    protected TextureRegion[][] regions;

    // ===== 边界墙配置 =====

    /**
     * @param name      Floor 注册名 + 贴图前缀，贴图必须叫 {name}.png 尺寸 128×128 (4×4 tile)
     * @param solid     是否阻挡单位寻路
     * @param drownTime 溺水秒数（0 = 不溺水，建议 30~60；空军无视）
     */
    public UdVoidFloor(String name, boolean solid, float drownTime) {
        super(name, 0);

        this.autotile = false;
        this.tilingVariants = 0;
        this.variants = 0;

        this.drawEdgeIn = true;
        this.drawEdgeOut = false;
        this.canShadow = false;
        this.solid = solid;
        this.drownTime = drownTime;
        this.placeableOn = false;
        this.edge = null;

        this.inEditor = true;
        this.alwaysUnlocked = true;
        this.hideDatabase = false;
    }

    // ===== 贴图加载 =====

    @Override
    public void load() {
        super.load();
        if(Vars.headless) return;

        TextureRegion sheet = Core.atlas.find(name);

        int tsize = (int)(Vars.tilesize / Draw.scl);
        regions = sheet.split(tsize, tsize);

        if(regions.length != 4 || regions[0].length != 4){
            Log.warn("Block: @: UdVoidFloor 贴图必须是 4×4 tile 网格 (128×128px @ 32px/tile), 实际 @×@。",
                name, regions.length, regions[0].length);
        }

        // icon: tempX=1, tempY=1（填充中心） → splitRow=3-1=2, splitCol=1
        region = regions[2][1];
    }

    // ===== GetSurround — 扫描 8 方向邻居 =====

    private void getSurround(Tile tile) {
        U  = checkSame(tile,  0, -1);
        D  = checkSame(tile,  0,  1);
        L  = checkSame(tile, -1,  0);
        R  = checkSame(tile,  1,  0);
        LU = checkSame(tile, -1, -1);
        RU = checkSame(tile,  1, -1);
        LD = checkSame(tile, -1,  1);
        RD = checkSame(tile,  1,  1);
    }

    // ===== GetTempPoint — 根据 8 方向 bool 选贴图位置 + 旋转 =====

    private void getTempPoint() {
        if(!U & !L & !R & !D){ tempX=0; tempY=0; tempR=0; return; }

        // ---- 单方向 ----
        if(U & !L & !R & !D){ tempX=0; tempY=1; tempR=2; return; }
        if(!U & L & !R & !D){ tempX=0; tempY=1; tempR=3; return; }
        if(!U & !L & R & !D){ tempX=0; tempY=1; tempR=1; return; }
        if(!U & !L & !R & D){ tempX=0; tempY=1; tempR=0; return; }

        // ---- 两相邻方向（corner）----
        if(U & L & !R & !D){
            if(LU){ tempX=0; tempY=3; tempR=2; return; }
            else{ tempX=0; tempY=2; tempR=2; return; }
        }
        if(U & !L & R & !D){
            if(RU){ tempX=0; tempY=3; tempR=1; return; }
            else{ tempX=0; tempY=2; tempR=1; return; }
        }
        // 纵向 N+D
        if(U & !L & !R & D){ tempX=3; tempY=2; tempR=0; return; }
        // 横向 L+R（伪代码修复后新增）
        if(!U & L & R & !D){ tempX=3; tempY=2; tempR=1; return; }
        if(!U & L & !R & D){
            if(LD){ tempX=0; tempY=3; tempR=3; return; }
            else{ tempX=0; tempY=2; tempR=3; return; }
        }
        if(!U & !L & R & D){
            if(RD){ tempX=0; tempY=3; tempR=0; return; }
            else{ tempX=0; tempY=2; tempR=0; return; }
        }

        // ---- 三方向（T 形）----
        if(U & L & R & !D){
            if(RU){
                if(LU){ tempX=1; tempY=3; tempR=1; return; }
                else{ tempX=1; tempY=1; tempR=1; return; }
            }else{
                if(LU){ tempX=1; tempY=2; tempR=1; return; }
                else{ tempX=1; tempY=0; tempR=1; return; }
            }
        }
        if(U & L & !R & D){
            if(LU){
                if(LD){ tempX=1; tempY=3; tempR=2; return; }
                else{ tempX=1; tempY=1; tempR=2; return; }
            }else{
                if(LD){ tempX=1; tempY=2; tempR=2; return; }
                else{ tempX=1; tempY=0; tempR=2; return; }
            }
        }
        if(U & !L & R & D){
            if(RD){
                if(RU){ tempX=1; tempY=3; tempR=0; return; }
                else{ tempX=1; tempY=1; tempR=0; return; }
            }else{
                if(RU){ tempX=1; tempY=2; tempR=0; return; }
                else{ tempX=1; tempY=0; tempR=0; return; }
            }
        }
        if(!U & L & R & D){
            if(LD){
                if(RD){ tempX=1; tempY=3; tempR=3; return; }
                else{ tempX=1; tempY=1; tempR=3; return; }
            }else{
                if(RD){ tempX=1; tempY=2; tempR=3; return; }
                else{ tempX=1; tempY=0; tempR=3; return; }
            }
        }

        // ---- 四方向全有（十字 + 各种缺对角）----
        if(U & L & R & D){
            if( LU &  RU &  LD &  RD){ tempX=3; tempY=1; tempR=0; return; }
            if(!LU &  RU &  LD &  RD){ tempX=3; tempY=0; tempR=3; return; }
            if( LU & !RU &  LD &  RD){ tempX=3; tempY=0; tempR=2; return; }
            if( LU &  RU & !LD &  RD){ tempX=3; tempY=0; tempR=0; return; }
            if( LU &  RU &  LD & !RD){ tempX=3; tempY=0; tempR=1; return; }
            if(!LU & !RU &  LD &  RD){ tempX=2; tempY=2; tempR=3; return; }
            if(!LU &  RU & !LD &  RD){ tempX=2; tempY=2; tempR=0; return; }
            if(!LU &  RU &  LD & !RD){ tempX=2; tempY=3; tempR=1; return; }
            if( LU & !RU & !LD &  RD){ tempX=2; tempY=3; tempR=0; return; }
            if( LU & !RU &  LD & !RD){ tempX=2; tempY=2; tempR=2; return; }
            if( LU &  RU & !LD & !RD){ tempX=2; tempY=2; tempR=1; return; }
            if(!LU & !RU & !LD &  RD){ tempX=2; tempY=1; tempR=0; return; }
            if(!LU & !RU &  LD & !RD){ tempX=2; tempY=1; tempR=3; return; }
            if(!LU &  RU & !LD & !RD){ tempX=2; tempY=1; tempR=1; return; }
            if( LU & !RU & !LD & !RD){ tempX=2; tempY=1; tempR=2; return; }
            if(!LU & !RU & !LD & !RD){ tempX=2; tempY=0; tempR=0; return; }
        }

        // 兜底：isolated
        tempX = 0; tempY = 0; tempR = 0;
    }

    // ===== 主渲染入口 =====

    @Override
    public void drawMain(Tile tile) {
        getSurround(tile);
        getTempPoint();

        // tempY 是图像顶部=0，但 splitRow 是图像底部=0
        int splitRow = 3 - tempY;
        int splitCol = tempX;

        // 逆时针旋转 → Arc 负值（Arc 正值为顺时针）
        float rotation = -tempR * 90f;

        Draw.rect(regions[splitRow][splitCol], tile.worldx(), tile.worldy(),
            Vars.tilesize, Vars.tilesize, rotation);
    }

    /** 判断 (tile.x+dx, tile.y+dy) 位置的 tile 和自己是否"同类型"。
     *  只检查 Floor 身份（相同类）。 */
    private boolean checkSame(Tile tile, int dx, int dy) {
        Tile other = tile.nearby(dx, dy);
        return other != null && other.floor() == this;
    }
}
