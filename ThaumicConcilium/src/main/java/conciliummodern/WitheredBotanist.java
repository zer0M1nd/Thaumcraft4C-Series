package conciliummodern;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;
public final class WitheredBotanist extends PathfinderMob {
 public WitheredBotanist(EntityType<? extends WitheredBotanist> type,Level level){super(type,level);setNoAi(true);}
 public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,.5).add(Attributes.MOVEMENT_SPEED,0);}
 @Override public boolean canBreatheUnderwater(){return true;}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override public boolean removeWhenFarAway(double distance){return false;}
 public static boolean spawnRules(EntityType<WitheredBotanist> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,net.minecraft.util.RandomSource random){String dimension=level.getLevel().dimension().location().toString();return (level.getLevel().dimension()==Level.OVERWORLD||dimension.equals("alfheim:alfheim"))&&level.getLevel().getEntitiesOfClass(WitheredBotanist.class,new AABB(pos).inflate(32)).isEmpty()&&Mob.checkMobSpawnRules(type,level,reason,pos,random);}
    @Override public void travel(Vec3 input){
        if(level().isClientSide)return;
        double submerged=0;var bounds=getBoundingBox();
        for(int slice=0;slice<5;slice++){
            double low=bounds.minY+bounds.getYsize()*slice/5-.125,high=bounds.minY+bounds.getYsize()*(slice+1)/5-.125;
            boolean wet=false;
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,low,bounds.minZ),BlockPos.containing(bounds.maxX,high,bounds.maxZ))){
                if(!level().hasChunkAt(pos))continue;var fluid=level().getFluidState(pos);
                if((fluid.is(FluidTags.WATER)||fluid.is(FluidTags.LAVA)) && pos.getY()+fluid.getHeight(level(),pos)>low){wet=true;break;}
            }
            if(wet)submerged+=.2;
        }
        var motion=getDeltaMovement();double vertical=motion.y;
        if(submerged<.999)vertical+=.04*(submerged*2-1);else {if(vertical<0)vertical/=2;vertical+=.007;}
        motion=new Vec3(motion.x,vertical,motion.z);double horizontal=motion.horizontalDistance();
        if(horizontal>.45)motion=new Vec3(motion.x*.45/horizontal,motion.y,motion.z*.45/horizontal);
        if(onGround())motion=motion.scale(.5);
        move(MoverType.SELF,motion);setDeltaMovement(motion.multiply(.99,.95,.99));
        if(motion.horizontalDistanceSqr()>.001){float wanted=(float)(Math.atan2(-motion.z,-motion.x)*180/Math.PI);setYRot(getYRot()+net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(wanted-getYRot()),-20,20));}
        setXRot(0);yBodyRot=getYRot();
    }

 private void fertilize(Entity actor){if(!(level() instanceof ServerLevel level)||isRemoved())return;if(!(actor instanceof Player)&&!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,this))return;
 var flowers=new java.util.ArrayList<net.minecraft.world.level.block.Block>();for(DyeColor color:DyeColor.values()){var id=ResourceLocation.parse("botania:"+color.getName()+"_mystical_flower");if(ForgeRegistries.BLOCKS.containsKey(id))flowers.add(ForgeRegistries.BLOCKS.getValue(id));}if(flowers.isEmpty())return;
 var positions=new java.util.ArrayList<BlockPos>();for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=3;y>=-1;y--){var pos=blockPosition().offset(x,y,z);if(level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)&&level.isEmptyBlock(pos)&&flowers.get(0).defaultBlockState().canSurvive(level,pos))positions.add(pos);}
 int count=Math.min(positions.size(),random.nextBoolean()?3:4);for(int i=0;i<count;i++){var pos=positions.remove(random.nextInt(positions.size()));if(actor instanceof Player player&&(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,ItemStack.EMPTY)))continue;var state=flowers.get(random.nextInt(flowers.size())).defaultBlockState();var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(actor==null?this:actor,snapshot,Direction.UP))continue;if(state.canSurvive(level,pos))level.setBlock(pos,state,3);}
 playSound(tc4revival.content.TCSounds.WIND.get(),.5f,.9f+random.nextFloat()*.2f);for(int i=0;i<15;i++)thaumcraft.api.fx.LegacyEffects.wisp(level,position().add(random.nextDouble()*6-3,1,random.nextDouble()*6-3),random.nextInt(0xffffff),.15f+random.nextFloat()*.25f,1,0,-.05f-random.nextFloat()*.1f);for(int i=0;i<6;i++){double x=getX()-2+random.nextDouble()*4,z=getZ()-2+random.nextDouble()*4;level.sendParticles(RemainingMobs.LEAF.get(),x,getY(),z,0,getX()+2+random.nextDouble()*4-x,8,getZ()-2+random.nextDouble()*4-z,1);}discard();}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){fertilize(player);return InteractionResult.sidedSuccess(level().isClientSide);}
 @Override public boolean hurt(DamageSource source,float amount){if(isRemoved()||amount<=0)return false;fertilize(source.getEntity());return true;}
}
