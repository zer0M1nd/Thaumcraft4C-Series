package dev.tc4.resourceadapter;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("thaumcraftresourceadapter")
public final class ThaumcraftResourceAdapter {
    public ThaumcraftResourceAdapter() {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientStartup.register();
    }
}
