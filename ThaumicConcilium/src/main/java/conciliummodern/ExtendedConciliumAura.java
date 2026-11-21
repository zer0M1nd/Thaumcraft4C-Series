package conciliummodern;

import gadomancymodern.aura.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.ModList;
import tc4revival.aspect.TCAspects;

/** Linked only from the Gadomancy-present path. Optional meteor and paint classes stay in nested helpers. */
final class ExtendedConciliumAura {
    static void register(){
        if(ModList.get().isLoaded("thaumicomniverse")&&(ModList.get().isLoaded("avaritia")||ModList.get().isLoaded("endless")))
            AuraEffectRegistry.register("thaumicomniverse:terminus",new AuraEffect(){
                @Override public int interval(){return 600;}
                @Override public double range(){return 32;}
                @Override public int blockCount(net.minecraft.util.RandomSource random){return 1;}
                @Override public void block(ServerLevel level,BlockPos origin,BlockPos chosen){meteor(level,chosen);}
            });
        if(ModList.get().isLoaded("alfheim"))for(var aspect:TCAspects.catalog().all())if(aspect.tag().equals("tinctura")||aspect.tag().endsWith(":tinctura"))
            AuraEffectRegistry.register(aspect.tag(),new AuraEffect(){
                @Override public int interval(){return 20;}
                @Override public boolean accepts(Entity entity){return entity instanceof LivingEntity;}
                @Override public void entity(ServerLevel level,BlockPos origin,Entity entity){
                    WitcheryBridge.host().map(WitcheryBridge.Host::colorfulEffect).ifPresent(effect->((LivingEntity)entity).addEffect(new MobEffectInstance(effect,1200,level.random.nextInt(16),true,true)));
                }
                @Override public int blockCount(net.minecraft.util.RandomSource random){return 10;}
                @Override public void block(ServerLevel level,BlockPos origin,BlockPos chosen){if(ModList.get().isLoaded("botania"))Paint.apply(level,chosen);}
            });
    }
    private static void meteor(ServerLevel level,BlockPos chosen){
        if(!level.hasChunkAt(chosen)||!level.getWorldBorder().isWithinBounds(chosen))return;
        var modes=new java.util.ArrayList<java.util.function.BiConsumer<ServerLevel,BlockPos>>();modes.add(ExtendedConciliumAura::stone);
        if(ModList.get().isLoaded("bloodmagic"))modes.add(Blood::meteor);
        if(RemainingMobs.available("overanimated"))modes.add(ExtendedConciliumAura::moon);
        modes.addAll(ConciliumSpellIntegration.meteors());
        modes.get(level.random.nextInt(modes.size())).accept(level,chosen);
    }
    private static void stone(ServerLevel level,BlockPos pos){for(int i=0;i<10;i++){
        var orb=new tc4revival.content.ExplosiveOrbEntity(tc4revival.content.TCEntities.EXPLOSIVE_ORB.get(),level);
        var data=new net.minecraft.nbt.CompoundTag();data.putFloat("Strength",20);data.putBoolean("Ignition",true);orb.readAdditionalSaveData(data);
        orb.setPos(pos.getX(),level.getMaxBuildHeight(),pos.getZ());orb.setDeltaMovement((-.5+level.random.nextGaussian())*.1,-.1,(-.5+level.random.nextGaussian())*.1);level.addFreshEntity(orb);
    }}
    private static void moon(ServerLevel level,BlockPos pos){for(int i=0;i<5;i++){
        var mob=RemainingMobs.OVERANIMATED.get().create(level);if(mob==null)continue;mob.setPersistenceRequired();mob.setPos(pos.getX()-2+level.random.nextGaussian()*4,level.getMaxBuildHeight(),pos.getZ()-2+level.random.nextGaussian()*4);
        if(level.hasChunkAt(mob.blockPosition())&&level.getWorldBorder().isWithinBounds(mob.blockPosition()))level.addFreshEntity(mob);
    }}
    private static final class Blood {
        static void meteor(ServerLevel level,BlockPos pos){
            var meteor=new wayoftime.bloodmagic.entity.projectile.EntityMeteor(level,pos.getX()+.5,level.getMaxBuildHeight()+1,pos.getZ()+.5);
            if(!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,meteor))return;
            var stone=new ItemStack(Items.STONE);
            if(wayoftime.bloodmagic.impl.BloodMagicAPI.INSTANCE.getRecipeRegistrar().getMeteor(level,stone)==null)return;
            meteor.setContainedStack(stone);meteor.setDeltaMovement(0,-1,0);level.addFreshEntity(meteor);
        }
    }
    private static final class Paint {
        static void apply(ServerLevel level,BlockPos pos){
            if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))return;
            var old=level.getBlockState(pos);if(old.hasBlockEntity())return;
            var transform=vazkii.botania.api.BotaniaAPI.instance().getPaintableBlocks().get(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(old.getBlock()));if(transform==null)return;
            var color=DyeColor.byId(level.random.nextInt(16));var block=transform.apply(color);if(block==null||block==old.getBlock())return;
            var replacement=block.withPropertiesOf(old);var actor=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            if(!level.mayInteract(actor,pos)||net.minecraftforge.event.ForgeEventFactory.onBlockPlace(actor,net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos),Direction.UP))return;
            if(level.setBlockAndUpdate(pos,replacement)){var rgb=color.getTextureDiffuseColors();level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(rgb[0],rgb[1],rgb[2]),.8f),pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,4,.5,.5,.5,0);}
        }
    }
    private ExtendedConciliumAura(){}
}
