package thaumicfutures.compat;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

/** Exposes the Futures material namespace to GT's generated assets and data packs. */
@GTAddon
public final class FutureGregTechAddon implements IGTAddon {
    public static final GTRegistrate REGISTRATE=GTRegistrate.create("thaumicfutures");
    @Override public GTRegistrate getRegistrate(){return REGISTRATE;}
    @Override public String addonModId(){return "thaumicfutures";}
    @Override public void initializeAddon(){}
}
