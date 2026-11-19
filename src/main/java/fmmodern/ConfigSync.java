package fmmodern;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.*;

/** Global legacy settings load before recipes; clients use a non-persistent server snapshot. */
public final class ConfigSync {
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("forbidden_magic:settings"),()->"2","2"::equals,"2"::equals);
    private static volatile CompoundTag clientValues=new CompoundTag();
    public record Packet(CompoundTag values){}
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event){
        event.enqueueWork(()->CHANNEL.messageBuilder(Packet.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((packet,buffer)->buffer.writeNbt(packet.values())).decoder(buffer->new Packet(buffer.readNbt()))
            .consumerMainThread((packet,context)->{clientValues=packet.values()==null?new CompoundTag():packet.values().copy();CageMobs.receive(clientValues.getCompound("cage_mobs"));}).add());
    }
    static Object override(net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?> setting){
        if(!net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient())return null;
        String key=String.join(".",setting.getPath());var tag=clientValues;
        if(!tag.contains(key))return null;
        return setting.getDefault() instanceof Boolean?tag.getBoolean(key):tag.getInt(key);
    }
    public static void clear(){clientValues=new CompoundTag();CageMobs.receive(new CompoundTag());}
    @SubscribeEvent public void sync(net.minecraftforge.event.OnDatapackSyncEvent event){
        var tag=new CompoundTag();
        for(var setting:ForbiddenConfig.settings()){
            String key=String.join(".",setting.getPath());Object value=setting.get();
            if(value instanceof Boolean bool)tag.putBoolean(key,bool);else tag.putInt(key,(Integer)value);
        }
        tag.put("cage_mobs",CageMobs.snapshot());
        if(event.getPlayer()!=null)CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer),new Packet(tag));
        else for(var player:event.getPlayerList().getPlayers())CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Packet(tag));
    }
    @SubscribeEvent public void worldReady(net.minecraftforge.event.server.ServerAboutToStartEvent event){
        // Forge loads world SERVER settings only after the initial recipe reload.
        // Re-select our single world-dependent recipe; do not reload or alter base data.
        var server=event.getServer();var id=ResourceLocation.parse("forbidden_magic:crystalwell");
        var resource=server.getResourceManager().getResource(ResourceLocation.parse("forbidden_magic:recipes/crystalwell.json"));
        if(resource.isEmpty())return;
        try(var reader=resource.get().openAsReader()){
            var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            var recipe=net.minecraft.world.item.crafting.RecipeManager.fromJson(id,json);
            var recipes=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(server.getRecipeManager().getRecipes());
            recipes.removeIf(r->r.getId().equals(id));if(recipe!=null)recipes.add(recipe);
            server.getRecipeManager().replaceRecipes(recipes);
        }catch(java.io.IOException exception){throw new IllegalStateException("Cannot select crystalwell research mode",exception);}
    }
}
