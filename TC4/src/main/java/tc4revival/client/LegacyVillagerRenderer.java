package tc4revival.client;

import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import tc4revival.content.*;

/** Retain vanilla rendering for ordinary villagers, use complete old skins for TC professions. */
public  class LegacyVillagerRenderer extends VillagerRenderer {
    public LegacyVillagerRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(Villager villager) {
        var role=TCVillagers.role(villager);
        return role==null?super.getTextureLocation(villager):ResourceLocation.parse("thaumcraft:textures/models/"+(role==LegacyVillagerTrades.Role.WIZARD?"wizard":"moneychanger")+".png");
    }
    @Override public void render(Villager villager,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light) {
        if(TCVillagers.role(villager)==null){super.render(villager,yaw,partial,pose,buffers,light);return;}
        var original=new java.util.ArrayList<>(layers);var hat=model.getHead().getChild("hat");boolean visible=hat.visible;
        try {
            layers.removeIf(layer->layer instanceof net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer);
            hat.visible=false;super.render(villager,yaw,partial,pose,buffers,light);
        }finally{layers.clear();layers.addAll(original);hat.visible=visible;}
    }
}
