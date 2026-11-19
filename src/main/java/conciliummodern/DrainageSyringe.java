package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import tc4revival.content.*;

/** The original three reservoirs; optional-mod types are confined to SyringeIntegrations. */
public final class DrainageSyringe extends Item implements thaumcraft.api.IWarpingGear {
    public static final int[] CAPACITY={100,250000,500000},DOSE={10,5000,20000};
    public DrainageSyringe(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    public static int type(ItemStack stack){return stack.hasTag()?Math.max(0,Math.min(2,stack.getTag().getInt("Type"))):0;}
    public static int amount(ItemStack stack){return stack.hasTag()?Math.max(0,Math.min(CAPACITY[type(stack)],stack.getTag().getInt("Amount"))):0;}
    public static boolean open(ItemStack stack){return stack.hasTag()&&stack.getTag().getBoolean("IsOpen");}
    public static boolean available(){var mods=net.minecraftforge.fml.ModList.get();return mods.isLoaded("thaumichorizons")&&mods.isLoaded("botania")&&mods.isLoaded("bloodmagic");}
    @Override public int getWarp(ItemStack stack,Player player){return 3;}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public boolean shouldCauseReequipAnimation(ItemStack before,ItemStack after,boolean slotChanged){return slotChanged||before.getItem()!=after.getItem();}
    @Override public boolean canContinueUsing(ItemStack oldStack,ItemStack newStack){return oldStack.getItem()==newStack.getItem()&&type(oldStack)==type(newStack)&&open(oldStack)==open(newStack);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!available())return InteractionResultHolder.fail(stack);
        if(player.isShiftKeyDown()){
            if(!level.isClientSide)stack.getOrCreateTag().putBoolean("IsOpen",!open(stack));
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        if(open(stack)?amount(stack)>0:amount(stack)<CAPACITY[type(stack)]&&target(player)!=null){
            player.startUsingItem(hand);
            if(!level.isClientSide&&!open(stack))level.playSound(null,player.blockPosition(),ConciliumSounds.STAB.get(),SoundSource.PLAYERS,1,Math.max(.1f,player.getRandom().nextFloat()));
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }
    @Override public boolean onEntitySwing(ItemStack stack,LivingEntity entity){
        if(entity instanceof Player player&&player.isShiftKeyDown()&&amount(stack)==0){
            if(!player.level().isClientSide&&available())stack.getOrCreateTag().putInt("Type",(type(stack)+1)%3);
            return true;
        }
        return super.onEntitySwing(stack,entity);
    }
    private static LivingEntity target(Player player){
        var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(3));
        var wall=player.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        var hit=ProjectileUtil.getEntityHitResult(player,start,wall.getLocation(),player.getBoundingBox().expandTowards(end.subtract(start)).inflate(.1),
                e->e instanceof LivingEntity&&e.isAlive()&&!e.isSpectator()&&e.isPickable(),start.distanceToSqr(wall.getLocation()));
        return hit!=null&&hit.getEntity() instanceof LivingEntity living?living:null;
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
        if(!(entity instanceof ServerPlayer player)||!available())return;
        int elapsed=getUseDuration(stack)-remaining;
        if(elapsed<=0||elapsed%(open(stack)?20:40)!=0)return;
        if(open(stack))pour(player,stack);else drain(player,stack);
    }
    private static void drain(ServerPlayer player,ItemStack stack){
        int type=type(stack),stored=amount(stack);
        if(stored>=CAPACITY[type]){player.stopUsingItem();return;}
        var victim=target(player);
        if(victim==null||victim instanceof AbstractGolem||victim.getHealth()<=victim.getMaxHealth()/2
                ||victim instanceof Player other&&!player.canHarmPlayer(other)
                ||MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player,victim)))return;
        if(!victim.hurt(player.damageSources().playerAttack(player),victim.getMaxHealth()/10))return;
        stack.getOrCreateTag().putInt("Amount",Math.min(CAPACITY[type],stored+DOSE[type]));
        if(victim instanceof ServerPlayer target){
            if(type==0){var costs=new java.util.LinkedHashMap<tc4revival.aspect.Aspect,Integer>();
                tc4revival.aspect.TCAspects.catalog().primals().forEach(aspect->costs.put(aspect,5000));WandInventory.consume(target,costs);
            }else SyringeIntegrations.drain(target,stack,type);
        }
        player.getInventory().setChanged();
    }
    private static void pour(ServerPlayer player,ItemStack stack){
        int type=type(stack),stored=amount(stack),used=0;
        if(stored<=0){player.stopUsingItem();return;}
        int offered=Math.min(stored,DOSE[type]);
        if(type==0){
            var primals=new java.util.ArrayList<>(tc4revival.aspect.TCAspects.catalog().primals());var aspect=primals.get(player.getRandom().nextInt(primals.size()));
            for(int slot=0;slot<9;slot++){var wandStack=player.getInventory().getItem(slot);
                // Whole-vis doses avoid rounding small capacity gaps into resource duplication.
                if(wandStack.getItem() instanceof CastingWandItem wand){int accepted=wand.receiveVis(wandStack,aspect,offered*100,true)/100;
                    if(accepted>0){used=wand.receiveVis(wandStack,aspect,accepted*100,false)/100;break;}}
            }
        }else used=SyringeIntegrations.pour(player,stack,type,offered);
        if(used>0){stack.getOrCreateTag().putInt("Amount",stored-used);player.getInventory().setChanged();
            player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_DRINK,SoundSource.PLAYERS,1,.9f+player.getRandom().nextFloat()*.2f);}
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flags){
        lines.add(Component.translatable("tc.syringe.type."+type(stack)).append(" "+amount(stack)+" / "+CAPACITY[type(stack)]));
        lines.add(Component.translatable(open(stack)?"tc.syringe.open":"tc.syringe.closed"));
        lines.add(Component.translatable("tc.syringe.controls"));
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return DrainageSyringeRenderer.instance();}
        });
    }
}
