package tc4revival.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraftforge.registries.ForgeRegistries;

/** TC professions retain their data for curing, with the vanilla zombie skin as placeholder. */
public  class LegacyZombieVillagerRenderer extends ZombieVillagerRenderer {
    public LegacyZombieVillagerRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public void render(ZombieVillager entity,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.client.renderer.MultiBufferSource buffers,int light) {
        var profession=ForgeRegistries.VILLAGER_PROFESSIONS.getKey(entity.getVillagerData().getProfession());
        if(profession==null || !profession.getNamespace().equals("thaumcraft")){super.render(entity,yaw,partial,pose,buffers,light);return;}
        var original=new java.util.ArrayList<>(layers);
        try{layers.removeIf(layer->layer instanceof VillagerProfessionLayer);super.render(entity,yaw,partial,pose,buffers,light);}
        finally{layers.clear();layers.addAll(original);}
    }
}
