package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thmodern.Horizons;
import thmodern.containment.CreatureJar;

public final class CreatureJarItemRenderer extends BlockEntityWithoutLevelRenderer {
    private CompoundTag last;
    private Level lastLevel;
    private LivingEntity preview;
    public CreatureJarItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var client=Minecraft.getInstance();if(client.level!=lastLevel||!java.util.Objects.equals(last,stack.getTag())){lastLevel=client.level;last=stack.hasTag()?stack.getTag().copy():null;preview=CreatureJar.read(stack,client.level);}
        client.getBlockRenderer().renderSingleBlock(Horizons.CREATURE_JAR.get().defaultBlockState(),pose,buffers,light,overlay);CreatureJarRenderer.creature(preview,pose,buffers,light);
    }
}
