package automagy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The static pedestal is supplied by the block model. */
public final class GolemDeviceRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final InventariumRenderer brain=new InventariumRenderer(null);
    private final ResourceLocation texture;
    public GolemDeviceRenderer(boolean entropy){texture=ResourceLocation.parse("automagy:textures/models/brain_"+(entropy?"entropy":"order")+".png");}
    @Override public void render(T device,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float time=device.hasLevel()?(device.getLevel().getGameTime()%10800)+partial:0;
        boolean active=device instanceof automagy.content.GolemTaskmasterBlockEntity master?master.active():device instanceof automagy.content.GolemInhibitorBlockEntity inhibitor&&inhibitor.active();
        brain.draw(time,device.hasLevel()&&active,texture,.2f,pose,buffers,light,overlay);
    }
}
