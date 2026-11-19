package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.*;

public  class TaintedAnimalRenderer extends MobRenderer<TaintedAnimalEntity,EntityModel<TaintedAnimalEntity>> {
    private final ResourceLocation texture;
    public TaintedAnimalRenderer(EntityRendererProvider.Context context,TaintedAnimalEntity.Kind kind) {
        super(context,model(context,kind),switch(kind) {case CHICKEN->.3f;case CREEPER,VILLAGER->.5f;default->.7f;});
        texture=ResourceLocation.parse("thaumcraft:textures/models/"+kind.name().toLowerCase(java.util.Locale.ROOT)+".png");
        if(kind==TaintedAnimalEntity.Kind.SHEEP)addLayer(new Fleece(this));
    }
    private static EntityModel<TaintedAnimalEntity> model(EntityRendererProvider.Context context,TaintedAnimalEntity.Kind kind) {
        return switch(kind) {
            case CHICKEN->new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN));
            case COW->new CowModel<>(context.bakeLayer(ModelLayers.COW));
            case PIG->new PigModel<>(context.bakeLayer(ModelLayers.PIG));
            case CREEPER->new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER));
            case SHEEP->new TaintedSheepModel(false);
            case VILLAGER->{var root=context.bakeLayer(ModelLayers.VILLAGER);root.getChild("head").getChild("hat").visible=false;yield new VillagerModel<>(root);}
        };
    }
    @Override public ResourceLocation getTextureLocation(TaintedAnimalEntity entity) { return texture; }
    @Override protected boolean shouldShowName(TaintedAnimalEntity entity) { return entity.kind()!=TaintedAnimalEntity.Kind.VILLAGER && super.shouldShowName(entity); }
    @Override protected float getBob(TaintedAnimalEntity entity,float partial) {
        if(entity.kind()==TaintedAnimalEntity.Kind.CHICKEN)return (Mth.sin(Mth.lerp(partial,entity.oldFlap,entity.flap))+1)*Mth.lerp(partial,entity.oldWingSpread,entity.wingSpread);
        return super.getBob(entity,partial);
    }
    @Override protected void scale(TaintedAnimalEntity entity,PoseStack pose,float partial) {
        if(entity instanceof TaintedCreeperEntity creeper) {
            float flash=creeper.flash(partial),oscillation=1+Mth.sin(flash*100)*flash*.01f;
            float amount=Mth.clamp(flash,0,1);amount*=amount;amount*=amount;
            float horizontal=(1+.4f*amount)*oscillation;
            pose.scale(horizontal,(1+.1f*amount)/oscillation,horizontal);
        }
    }
    @Override protected float getWhiteOverlayProgress(TaintedAnimalEntity entity,float partial) {
        if(!(entity instanceof TaintedCreeperEntity creeper))return 0;
        float flash=creeper.flash(partial);return (int)(flash*10)%2==0?0:Mth.clamp(flash*.2f,0,1);
    }
    private static  class Fleece extends RenderLayer<TaintedAnimalEntity,EntityModel<TaintedAnimalEntity>> {
        private final TaintedSheepModel fleece=new TaintedSheepModel(true);
        Fleece(TaintedAnimalRenderer parent) { super(parent); }
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,TaintedAnimalEntity entity,float swing,float amount,float partial,float age,float yaw,float pitch) {
            if(!entity.sheared())coloredCutoutModelCopyLayerRender(getParentModel(),fleece,ResourceLocation.parse("thaumcraft:textures/models/sheep_fur.png"),pose,buffers,light,entity,swing,amount,age,yaw,pitch,partial,1,1,1);
        }
    }
}
