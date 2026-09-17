package udmodpack;

import mindustry.mod.*;
import static udmodpack.UdBasicFloor.*;  // 引入 SHALLOW/DEEP/ABYSSAL

public class UDModMain extends Mod{

    @Override
    // UdBasicFloor 参数: (name, variants, depthDefault, drawEdgeOut, drawEdgeIn)
    public void loadContent(){
        //金属地板
        new UdBasicFloor("ud-test-basic-floor", 4, SHALLOW, false, false);

        //深渊
        new UdBasicFloor("ud-abyssal-basic-floor", 4, ABYSSAL, true, true);
        new UdBasicFloor("ud-abyssal-hole-floor", 3, ABYSSAL, false, true);

        //深海
        new UdBasicFloor("ud-deep-basic-floor", 6, DEEP, true, true);

        //浅海
        new UdBasicFloor("ud-shallow-basic-floor", 3, SHALLOW, true, true);
        new UdBasicFloor("ud-shallow-sand-floor", 4, SHALLOW, true, true);
    }
}
