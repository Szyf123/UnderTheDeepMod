package udmodpack;

import static udmodpack.UdBasicFloor.*;

/** 所有内容注册集中在此。
 *  UdBasicFloor 参数: (name, variants, depthDefault, drawEdgeOut, drawEdgeIn, isInfected, canBeInfected) */
public class UDContent {

    // 配对用引用
    public static UdBasicFloor shallowBasic;
    public static UdBasicFloor shallowBasicInfectious;

    public static void registerAll() {
        // 金属地板
        new UdBasicFloor(
            "ud-test-basic-floor",
            4, SHALLOW,
            false, false,
            false, false
        );
        
        // 深渊
        new UdBasicFloor(
            "ud-abyssal-basic-floor",
            4, ABYSSAL,
            true, true,
            false, true
        );
        new UdBasicFloor(
            "ud-abyssal-hole-floor",
            3, ABYSSAL,
            false, true,
            false, true
        );

        // 深海
        new UdBasicFloor(
            "ud-deep-basic-floor",
            6, DEEP,
            true, true,
            false, true
        );
        
        // 浅海
        shallowBasic = new UdBasicFloor(
            "ud-shallow-basic-floor",
            4, SHALLOW,
            true, true,
            false, true
        );
        new UdBasicFloor(
            "ud-shallow-sand-floor",
            4, SHALLOW,
            true, true,
            false, true
        );
        shallowBasicInfectious = new UdBasicFloor(
            "ud-shallow-basic-infecitious-floor",
            2, SHALLOW,
            true, false,
            true, false
        );
    }
}
