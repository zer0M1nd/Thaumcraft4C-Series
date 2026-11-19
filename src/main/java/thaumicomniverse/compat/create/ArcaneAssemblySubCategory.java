package thaumicomniverse.compat.create;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.*;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.UIRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import tc4revival.compat.jei.ThaumcraftJei;
import tc4revival.content.*;

public class ArcaneAssemblySubCategory extends SequencedAssemblySubCategory {
    private final boolean infusion,spouting;
    private final AnimatedEssentiaSpout spout=new AnimatedEssentiaSpout();
    private CrucibleBlockEntity crucible;
    private InfusionMatrixBlockEntity matrix;
    private final tc4revival.client.InfusionMatrixRenderer matrixRenderer=new tc4revival.client.InfusionMatrixRenderer(null);
    private final tc4revival.client.CrucibleRenderer crucibleRenderer=new tc4revival.client.CrucibleRenderer(null);
    public ArcaneAssemblySubCategory(boolean infusion){this(infusion,false);}
    public ArcaneAssemblySubCategory(boolean infusion,boolean spouting){super(25);this.infusion=infusion;this.spouting=spouting;}
    @Override public void setRecipe(IRecipeLayoutBuilder builder,SequencedRecipe<?> row,IFocusGroup focuses,int x){
        if(!(row.getRecipe() instanceof ArcaneProcessingRecipe step)||!spouting)return;
        var cost=step.aspects().view().entrySet().iterator().next();
        builder.addSlot(RecipeIngredientRole.INPUT,x+4,15).setBackground(CreateRecipeCategory.getRenderedSlot(),-1,-1)
                .addIngredient(ThaumcraftJei.ASPECTS,new ThaumcraftJei.AspectValue(cost.getKey().tag(),cost.getValue()));
    }
    @Override public void draw(SequencedRecipe<?> row,GuiGraphics graphics,double mouseX,double mouseY,int index){
        if(!(row.getRecipe() instanceof ArcaneProcessingRecipe recipe))return;
        var pose=graphics.pose();
        if(spouting){
            pose.pushPose();pose.translate(-7,50,0);pose.scale(.75f,.75f,.75f);
            spout.offset=index;spout.withAspect(recipe.aspects().aspects().get(0)).draw(graphics,getWidth()/2,0);pose.popPose();return;
        }
        drawRequirement(recipe,graphics);
        pose.pushPose();pose.translate(-7+getWidth()/2*.75,50,100);pose.scale(.75f,.75f,.75f);
        pose.mulPose(Axis.XP.rotationDegrees(-15.5f));pose.mulPose(Axis.YP.rotationDegrees(22.5f));
        AnimatedKinetics.defaultBlockElement((infusion?TCArcane.ARCANE_PEDESTAL:TCArcane.CRUCIBLE).get().defaultBlockState()).atLocal(0,2,0).scale(20).render(graphics);
        graphics.flush();AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
        pose.pushPose();UIRenderHelper.flipForGuiRender(pose);pose.scale(20,20,20);
        var mc=Minecraft.getInstance();
        if(infusion){
            if(matrix==null){matrix=new InfusionMatrixBlockEntity(BlockPos.ZERO,TCArcane.INFUSION_MATRIX.get().defaultBlockState());var tag=new CompoundTag();tag.putBoolean("Active",true);matrix.load(tag);matrix.startUp=1;}
            matrix.setLevel(mc.level);
            matrixRenderer.render(matrix,AnimationTickHolder.getPartialTicks(),pose,graphics.bufferSource(),0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        }else{
            if(crucible==null){crucible=new CrucibleBlockEntity(BlockPos.ZERO,TCArcane.CRUCIBLE.get().defaultBlockState());crucible.contents().fill(1000,true);}
            crucible.setLevel(mc.level);pose.translate(0,-2,0);
            // A full preview sits near the rim so water remains visible at Create's shallow GUI angle.
            pose.pushPose();pose.translate(0,.12,0);
            crucibleRenderer.render(crucible,0,pose,graphics.bufferSource(),0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            pose.popPose();
            // Native particle sheet, animated locally so JEI never spawns world particles.
            var out=graphics.bufferSource().getBuffer(tc4revival.client.TCRenderTypes.orb(ResourceLocation.parse("thaumcraft:textures/misc/particles.png")));
            float time=AnimationTickHolder.getRenderTime();
            for(int i=0;i<4;i++){
                float age=(time+i*3)%12/12f;int frame=age>.85f?18:age>.7f?17:16;
                pose.pushPose();pose.translate(.2+(i*37%60)/100d,.98+age*.16,.2+(i*23%60)/100d);
                pose.mulPose(Axis.YP.rotationDegrees(-22.5f));pose.mulPose(Axis.XP.rotationDegrees(15.5f));
                float u=frame%16/16f,v=frame/16/16f;
                for(int corner=0;corner<4;corner++){
                    float dx=corner<2?-.10f:.10f,dy=corner==0||corner==3?-.10f:.10f;
                    out.vertex(pose.last().pose(),dx,dy,0).color(.75f,.85f,1f,.85f*(1-age*.4f))
                            .uv(u+(corner<2?.0624375f:0),v+(dy<0?.0624375f:0)).uv2(240).endVertex();
                }
                pose.popPose();
            }
        }
        graphics.flush();pose.popPose();Lighting.setupFor3DItems();pose.popPose();
    }
    private void drawRequirement(ArcaneProcessingRecipe recipe,GuiGraphics graphics){
        var aspects=new java.util.ArrayList<>(recipe.aspects().view().entrySet());
        int count=recipe.components().size()+aspects.size();if(count==0)return;
        int tick=(int)(net.minecraft.Util.getMillis()/1000),selected=tick%count;
        CreateRecipeCategory.getRenderedSlot().draw(graphics,3,14);
        if(selected<recipe.components().size()){
            var items=recipe.components().get(selected).getItems();if(items.length==0)return;
            var item=items[tick/count%items.length];graphics.renderItem(item,4,15);graphics.renderItemDecorations(Minecraft.getInstance().font,item,4,15);
        }else{
            var entry=aspects.get(selected-recipe.components().size());graphics.pose().pushPose();graphics.pose().translate(4,15,0);
            new ThaumcraftJei.AspectRenderer().render(graphics,new ThaumcraftJei.AspectValue(entry.getKey().tag(),entry.getValue()));graphics.pose().popPose();
        }
    }
}
