package fmmodern.client;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
public final class HereticRenderer extends VillagerRenderer {
    public HereticRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(Villager villager){return villager.getVillagerData().getProfession()==fmmodern.Content.HERETIC_PROFESSION.get()?new ResourceLocation("forbidden_magic","textures/entity/heretic.png"):super.getTextureLocation(villager);}
    @Override public void render(Villager villager,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light) {
        if(villager.getVillagerData().getProfession()!=fmmodern.Content.HERETIC_PROFESSION.get()) {super.render(villager,yaw,partial,poses,buffers,light);return;}
        var saved=new java.util.ArrayList<>(layers);layers.clear();
        try {super.render(villager,yaw,partial,poses,buffers,light);}finally{layers.addAll(saved);}
    }
}
