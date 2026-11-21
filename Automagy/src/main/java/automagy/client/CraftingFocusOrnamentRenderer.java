package automagy.client;

import automagy.content.CraftingFocusItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** The base wand passes the attached stack to its ornament model renderer. */
public final class CraftingFocusOrnamentRenderer extends BlockEntityWithoutLevelRenderer {
 public static final ResourceLocation ENTRY=ResourceLocation.parse("automagy:item/crafting_focus_ornament_renderer");
 public static final ResourceLocation SPRITE=ResourceLocation.parse("automagy:item/crafting_focus_ornament");
 public CraftingFocusOrnamentRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
 @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(!CraftingFocusItem.has(stack,"transposing"))return;
  var client=Minecraft.getInstance();var model=client.getModelManager().getModel(SPRITE);
  pose.pushPose();
  try{
   // ItemRenderer already centered the custom model; cancel before the nested ordinary model draw.
   pose.translate(.5,.5,.5);
   client.getItemRenderer().render(stack,ItemDisplayContext.NONE,false,pose,buffers,light,overlay,model);
  }finally{pose.popPose();}
 }
}
