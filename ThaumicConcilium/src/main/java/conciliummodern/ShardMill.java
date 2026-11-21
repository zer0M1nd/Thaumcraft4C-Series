package conciliummodern;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Server-owned loading and immediate powder accounting survive interrupted use and logout. */
public final class ShardMill extends Item implements thaumcraft.api.IWarpingGear {
    private final boolean silver;
    public ShardMill(boolean silver){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));this.silver=silver;}
    public boolean silver(){return silver;}
    @Override public int getWarp(ItemStack stack,Player player){return silver?1:0;}
    public PowderType selected(ItemStack stack){int value=stack.hasTag()?stack.getTag().getInt("Type"):(silver?7:0);var type=PowderType.byId(value);return type.silver()==silver?type:(silver?PowderType.WRATH:PowderType.AIR);}
    public static boolean open(ItemStack stack){return !stack.hasTag() || !stack.getTag().contains("IsOpen") || stack.getTag().getBoolean("IsOpen");}
    public static int amount(ItemStack stack){return stack.hasTag()?net.minecraft.util.Mth.clamp(stack.getTag().getInt("Amount"),0,20):0;}
    public PowderType loaded(ItemStack stack){int id=stack.hasTag() && stack.getTag().contains("Loaded")?stack.getTag().getInt("Loaded"):-1;return id<0 || id>14 || PowderType.byId(id).silver()!=silver?null:PowderType.byId(id);}
    private CompoundTag data(ItemStack stack){
        var tag=stack.getOrCreateTag();if(!tag.contains("IsOpen"))tag.putBoolean("IsOpen",true);
        tag.putInt("Type",selected(stack).ordinal());tag.putInt("Amount",loaded(stack)==null?0:amount(stack));if(!tag.contains("Loaded"))tag.putInt("Loaded",-1);return tag;
    }
    public List<PowderType> available(Player player){return Arrays.stream(PowderType.values()).filter(t->t.silver()==silver && find(player,t)>=0).toList();}
    private int find(Player player,PowderType type){var item=type.shard();if(item.isEmpty())return -1;for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(item.getItem()))return i;return -1;}
    public boolean select(ItemStack stack,Player player,int number){
        if(number<0 || number>=PowderType.values().length)return false;var type=PowderType.byId(number);
        if(type.silver()!=silver || find(player,type)<0 || player.isUsingItem())return false;
        data(stack).putInt("Type",number);player.getInventory().setChanged();return true;
    }
    @Override public int getUseDuration(ItemStack stack){return open(stack)?20:Integer.MAX_VALUE;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()){
            if(!level.isClientSide){var tag=data(stack);tag.putBoolean("IsOpen",!open(stack));level.playSound(null,player.blockPosition(),ConciliumSounds.CLACK.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,.4f/(level.random.nextFloat()*.4f+.8f));}
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        if(open(stack)?amount(stack)>0 || find(player,selected(stack))<0:amount(stack)<=0 || loaded(stack)==null)return InteractionResultHolder.fail(stack);
        if(!level.isClientSide)data(stack);player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(!level.isClientSide && entity instanceof Player player && open(stack) && amount(stack)==0){
            var type=selected(stack);int slot=find(player,type);
            if(slot>=0){player.getInventory().getItem(slot).shrink(1);var tag=data(stack);tag.putInt("Amount",20);tag.putInt("Loaded",type.ordinal());player.getInventory().setChanged();
                level.playSound(null,player.blockPosition(),ConciliumSounds.SHARD_DROP.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));}
        }return stack;
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
        if(level.isClientSide || !(entity instanceof Player player) || open(stack))return;
        var type=loaded(stack);int amount=amount(stack);if(type==null || amount<=0){player.stopUsingItem();return;}
        var look=player.getLookAngle();double yaw=Math.toRadians(player.getYRot());
        var position=player.getEyePosition().add(look).add(-Math.cos(yaw)*.32,-.5,-Math.sin(yaw)*.32);boolean spawned=false;
        for(int i=0,count=2+level.random.nextInt(3);i<count;i++){
            Entity orb=type==PowderType.BALANCED?new tc4revival.content.AspectOrbEntity(level,position.x,position.y,position.z,tc4revival.aspect.TCAspects.catalog().primals().get(level.random.nextInt(6)),1):new ShardPowder(player,type,position);
            orb.setDeltaMovement(look.add(level.random.nextGaussian()*.09,level.random.nextGaussian()*.09,level.random.nextGaussian()*.09));spawned|=level.addFreshEntity(orb);
        }
        if(spawned){var tag=data(stack);tag.putInt("Amount",amount-1);if(amount==1)tag.putInt("Loaded",-1);
            if(player.getTicksUsingItem()%10==0)level.playSound(null,player.blockPosition(),ConciliumSounds.MILL.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);
        }else player.stopUsingItem();
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return ShardMillRenderer.instance();}});}
}
