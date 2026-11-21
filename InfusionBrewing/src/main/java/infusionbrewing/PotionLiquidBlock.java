package infusionbrewing;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.levelgen.Heightmap;

public class PotionLiquidBlock extends LiquidBlock {
    public final int potionId;
    public PotionLiquidBlock(PotionFluids.Family family){super(family.source,BlockBehaviour.Properties.copy(Blocks.WATER).randomTicks());potionId=family.id;}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);if(!level.isClientSide)level.scheduleTick(pos,this,1);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){super.neighborChanged(state,level,pos,block,from,moving);if(!level.isClientSide)level.scheduleTick(pos,this,1);}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){react(level,pos);}
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){react(level,pos);}
    public void react(ServerLevel level,BlockPos pos){
        if(!level.getBlockState(pos).is(this))return;
        boolean good=BrewingRules.support(potionId);
        // Modern LiquidBlock attenuates skylight inside itself; sample the exposed surface.
        if(good&&level.getFluidState(pos).isSource()&&(level.dimensionType().ultraWarm()||sunlit(level,pos.above()))){
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());explode(level,pos);return;
        }
        for(var d:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST,Direction.UP}){
            var near=pos.relative(d);if(!level.hasChunkAt(near))continue;
            var state=level.getBlockState(near);var fluid=state.getFluidState();int other=PotionFluids.id(fluid);
            if(other==potionId)continue;
            if(other!=0){
                if(good!=BrewingRules.support(other)){explode(level,pos);return;}
                boolean same=BrewingRules.instant(potionId)==BrewingRules.instant(other);
                level.setBlockAndUpdate(pos,(good?(same?Blocks.GRAVEL:Blocks.SAND):(same?Blocks.NETHERRACK:Blocks.SOUL_SAND)).defaultBlockState());
                ReactionSpawns.potionPair(level,pos,potionId,other);return;
            }
            if(fluid.is(FluidTags.LAVA)){
                if(good)explode(level,pos);else level.setBlockAndUpdate(pos,Blocks.OBSIDIAN.defaultBlockState());return;
            }
            if(!fluid.isEmpty()&&fluid.getFluidType().getLightLevel(fluid,level,near)>=9){
                level.setBlockAndUpdate(pos,(good?Blocks.GLASS:Blocks.GLOWSTONE).defaultBlockState());
                if(!good){ReactionSpawns.spawn(level,pos,EntityType.CREEPER,1);ReactionSpawns.spawn(level,pos,EntityType.BLAZE,1);ReactionSpawns.spawn(level,pos,EntityType.CREEPER,.8);ReactionSpawns.spawn(level,pos,EntityType.BLAZE,.8);}return;
            }
            if(!fluid.isEmpty()&&state.isFlammable(level,near,d.getOpposite())){
                if(good)explode(level,pos);else {level.setBlockAndUpdate(pos,Blocks.COAL_BLOCK.defaultBlockState());ReactionSpawns.spawn(level,pos,EntityType.ZOMBIFIED_PIGLIN,1);ReactionSpawns.spawn(level,pos,EntityType.SKELETON,1);ReactionSpawns.spawn(level,pos,EntityType.ZOMBIFIED_PIGLIN,.8);ReactionSpawns.spawn(level,pos,EntityType.SKELETON,.8);}return;
            }
            if(fluid.is(FluidTags.WATER)){level.setBlockAndUpdate(pos,(good?Blocks.MYCELIUM:Blocks.CLAY).defaultBlockState());return;}
            if(state.is(Blocks.FIRE)&&good){explode(level,pos);return;}
            if(state.is(Blocks.ICE)){level.setBlockAndUpdate(pos,InfusionBrewing.SEMI_SOLID.get().defaultBlockState());return;}
        }
    }
    private static boolean sunlit(ServerLevel level,BlockPos surface){
        if(!level.canSeeSky(surface)||level.getMaxLocalRawBrightness(surface)!=15)return false;
        // Skylight is updated asynchronously. A newly placed roof must protect the
        // source immediately, even while the light engine still reports full sun.
        // Check opacity rather than motion blocking so glass remains transparent.
        int top=level.getHeight(Heightmap.Types.WORLD_SURFACE,surface.getX(),surface.getZ());
        var sample=surface.mutable();
        for(int y=surface.getY();y<top;y++){
            sample.setY(y);
            if(level.getBlockState(sample).getLightBlock(level,sample)>0)return false;
        }
        return true;
    }
    private void explode(ServerLevel level,BlockPos pos){if(BrewingConfig.EXPLOSIONS.get())level.explode(null,pos.getX(),pos.getY(),pos.getZ(),BrewingConfig.POWER.get(),Level.ExplosionInteraction.BLOCK);}
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
        super.entityInside(state,level,pos,entity);if(level.isClientSide||!(entity instanceof LivingEntity living))return;
        int period=BrewingRules.instant(potionId)?60:20;if(level.getGameTime()%period!=0)return;
        // One dose per entity/tick even if its bounds intersect several fluid cells.
        var data=living.getPersistentData();String key="infusionbrewing:contact_"+potionId;
        if(data.contains(key)&&data.getLong(key)==level.getGameTime())return;data.putLong(key,level.getGameTime());
        var effect=PotionData.base(potionId).effect();
        if(BrewingRules.instant(potionId))effect.applyInstantenousEffect(null,null,living,0,1);
        else living.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,100));
        if(potionId==7&&living instanceof Villager&&level.random.nextDouble()<.05)popResource(level,pos,new ItemStack(Items.EMERALD));
    }
}
