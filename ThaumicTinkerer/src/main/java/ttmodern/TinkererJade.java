package ttmodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Discovered only by Jade; optional API classes are never loaded by the base mod path. */
@WailaPlugin
public final class TinkererJade implements IWailaPlugin {
    private static final Provider PROVIDER=new Provider();
    @Override public void register(IWailaCommonRegistration registration){
        registration.registerBlockDataProvider(PROVIDER,TabletEntity.class);
        registration.registerBlockDataProvider(PROVIDER,InterfaceEntity.class);
        registration.registerBlockDataProvider(PROVIDER,RestorerEntity.class);
        registration.registerBlockDataProvider(PROVIDER,WarpGateEntity.class);
        registration.registerBlockDataProvider(PROVIDER,MagnetEntity.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration){
        registration.registerBlockComponent(PROVIDER,TabletBlock.class);
        registration.registerBlockComponent(PROVIDER,InterfaceBlock.class);
        registration.registerBlockComponent(PROVIDER,RestorerBlock.class);
        registration.registerBlockComponent(PROVIDER,WarpGateBlock.class);
        registration.registerBlockComponent(PROVIDER,MagnetBlock.class);
    }
    public static final class Provider implements IBlockComponentProvider,IServerDataProvider<BlockAccessor> {
        @Override public ResourceLocation getUid(){return ResourceLocation.parse(Tinkerer.ID+":machine_status");}
        @Override public void appendServerData(CompoundTag data,BlockAccessor accessor){data.put("TinkererStatus",MachineTooltip.snapshot(accessor.getBlockEntity()));}
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){for(var line:MachineTooltip.read(accessor.getServerData().getCompound("TinkererStatus")))tooltip.add(line);}
    }
}
