package thaumicomniverse.compat.create;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumicomniverse",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class InfusionInputModel extends BakedModelWrapper<BakedModel> {
    private final ItemOverrides overrides=new ItemOverrides(){
        @Override public BakedModel resolve(BakedModel model,ItemStack stack,ClientLevel level,LivingEntity entity,int seed){
            if(stack.hasTag()){
                var original=ItemStack.of(stack.getTag().getCompound(SequenceAspects.ORIGINAL));
                if(!original.isEmpty()&&!original.is(ArcaneCreateContent.INCOMPLETE_INFUSION.get()))
                    return Minecraft.getInstance().getItemRenderer().getModel(original,level,entity,seed);
            }
            return originalModel.getOverrides().resolve(originalModel,stack,level,entity,seed);
        }
    };
    private InfusionInputModel(BakedModel original){super(original);}
    @Override public ItemOverrides getOverrides(){return overrides;}
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event){
        if(!net.minecraftforge.fml.ModList.get().isLoaded("create"))return;
        var key=new ModelResourceLocation(new net.minecraft.resources.ResourceLocation("thaumicomniverse","incomplete_essentia_infusion"),"inventory");
        event.getModels().computeIfPresent(key,(id,model)->new InfusionInputModel(model));
    }
}
