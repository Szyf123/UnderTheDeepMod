package udmodpack;

import mindustry.mod.*;

public class UDModMain extends Mod{

    @Override
    public void loadContent(){
        new UdBasicFloor("ud-test-basic-floor", 4){{
            inEditor = true;
            alwaysUnlocked = true;
        }};
    }
}
