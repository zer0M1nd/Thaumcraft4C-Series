package thaumicexploration;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class ExplorationBoots extends ArmorItem implements thaumcraft.api.IRepairable {
    private static final boolean APOTHEOSIS = net.minecraftforge.fml.ModList.get().isLoaded("apotheosis");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> STEP_BOOTS =
            net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:traveller_step_boots"));

    public final boolean meteor;
    private static final java.util.Map<Player,Float> PREVIOUS_STEP=new java.util.WeakHashMap<>();
    public ExplorationBoots(boolean meteor){super(tc4revival.content.SpecialArmorMaterial.INSTANCE,Type.BOOTS,new Properties().rarity(Rarity.RARE));this.meteor=meteor;}
    @Override public String getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,String type){return "thaumicexploration:textures/models/armor/boots"+(meteor?"meteor":"comet")+".png";}
    public static void restoreStep(Player player){
        if(player.isShiftKeyDown()||!(player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ExplorationBoots)){
            var previous=PREVIOUS_STEP.remove(player);if(previous!=null)player.setMaxUpStep(previous);
        }
    }
    @Override public void onArmorTick(ItemStack stack,Level level,Player player){
        var tag=stack.getOrCreateTag();var motion=player.getDeltaMovement();
        if(meteor)player.fallDistance=0;else if(player.fallDistance>.25F)player.fallDistance-=.25F;
        if(!player.getAbilities().flying&&player.zza>0){
            if(level.isClientSide&&!player.isShiftKeyDown()&&!(APOTHEOSIS && stack.is(STEP_BOOTS))){PREVIOUS_STEP.putIfAbsent(player,player.maxUpStep());player.setMaxUpStep(1);}
            float bonus=meteor?.055F:.11F+(tag.getInt("runTicks")/5)*.003F;
            if(player.isInWater())bonus/=4;
            if(player.onGround()||!meteor&&player.onClimbable())player.moveRelative(bonus,new Vec3(0,0,1));
        }
        if(meteor){
            int air=tag.getInt("airTicks"),ticks=tag.getInt("smashTicks");boolean smash=tag.getBoolean("smashing");
            if(player.onGround()||player.onClimbable()){
                if(!level.isClientSide&&smash&&ticks>5)level.explode(player,player.getX(),player.getY(),player.getZ(),ticks>15?3:ticks>10?2:1,Level.ExplosionInteraction.NONE);
                smash=false;ticks=0;air=0;
            }else if(!player.getAbilities().flying){air++;if(air>5&&player.isShiftKeyDown())smash=true;}
            if(player.getAbilities().flying){smash=false;ticks=0;air=0;}
            if(smash){ticks++;player.setDeltaMovement(player.getDeltaMovement().add(0,-.1,0));}
            tag.putInt("airTicks",air);tag.putInt("smashTicks",ticks);tag.putBoolean("smashing",smash);
            if(level.isClientSide&&(smash||motion.lengthSqr()>.01)&&!player.isInWater())level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,player.getX(),player.getY()+.15,player.getZ(),0,.025,0);
        }else {
            boolean moving=Math.abs(motion.x)+Math.abs(motion.y)+Math.abs(motion.z)>.1||!player.onGround()||player.onClimbable();
            tag.putInt("runTicks",moving?Math.min(100,tag.getInt("runTicks")+1):0);
            if(!level.isClientSide&&!player.isInWater())for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
                if(Math.abs(x)+Math.abs(z)>=8)continue;var pos=BlockPos.containing(player.getX()+x,player.getY()-1,player.getZ()+z);
                if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(Blocks.WATER)||!level.getFluidState(pos).isSource()||!level.isEmptyBlock(pos.above()))continue;
                var ice=Blocks.FROSTED_ICE.defaultBlockState();
                if(!net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos),net.minecraft.core.Direction.UP)){
                    level.setBlockAndUpdate(pos,ice);level.scheduleTick(pos,Blocks.FROSTED_ICE,60+player.getRandom().nextInt(61));
                }
            }
            if(level.isClientSide&&moving&&!player.isInWater())level.addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,player.getX(),player.getY()+.15,player.getZ(),0,.025,0);
        }
    }
}
