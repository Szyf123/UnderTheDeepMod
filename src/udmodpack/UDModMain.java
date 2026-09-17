package udmodpack;

import mindustry.mod.*;

public class UDModMain extends Mod{

    @Override
    public void loadContent(){
        UDContent.registerAll();
        InfectionManager.addPair(UDContent.shallowBasic, UDContent.shallowBasicInfectious);
        InfectionManager.init();
    }
}
