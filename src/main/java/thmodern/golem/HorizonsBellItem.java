package thmodern.golem;

import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import tc4revival.content.*;
import thmodern.Horizons;

/** Uses the frozen base's marker implementation without replacing its registry entry. */
public final class HorizonsBellItem extends Item {
    public HorizonsBellItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return TCItems.GOLEM_BELL.get().onItemUseFirst(stack,context);}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity entity,InteractionHand hand){return TCItems.GOLEM_BELL.get().interactLivingEntity(stack,player,entity,hand);}
    @Override public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()){GolemBellItem.resetMarkers(stack,player);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
        return InteractionResultHolder.pass(stack);
    }
    @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity entity){
        if(entity instanceof GolemEntity golem&&AnimatedGolems.isAnimated(golem))return recover(player,golem);
        return TCItems.GOLEM_BELL.get().onLeftClickEntity(stack,player,entity);
    }
    public static ItemStack portable(GolemEntity golem){
        var stack=new ItemStack(Horizons.GOLEM_PLACER.get());var tag=stack.getOrCreateTag();var config=golem.configuration();
        config.saveExtension(tag);
        if(config.advanced())tag.putByte("advanced",(byte)1);
        if(config.core()!=null)tag.putByte("core",(byte)config.core().ordinal());
        tag.putByteArray("upgrades",config.upgrades());tag.putString("deco",config.decorations());
        golem.markers().inDimension(golem.level().dimension().location());tag.put("markers",golem.markers().save());
        var filters=new CompoundTag();golem.filters().save(filters);tag.put("Inventory",filters.getList("Inventory",Tag.TAG_COMPOUND));
        if(golem.hasCustomName())stack.setHoverName(golem.getCustomName());return stack;
    }
    public static boolean recover(Player player,GolemEntity golem){
        if(!golem.isAlive()||golem.isRemoved())return true;
        if(golem.level().isClientSide){golem.spawnAnim();return true;}
        var config=golem.configuration();
        if(config.core()==null){AnimatedGolems.restore(golem,true);return true;}
        if(player.isShiftKeyDown()){
            AnimatedGolems.restore(golem,true);
            // A cancelled placement/drop leaves the construct alive; do not duplicate components on retry.
            if(!golem.isRemoved())return true;
            golem.spawnAtLocation(new ItemStack(TCItems.golemCore(config.core().ordinal())),.5f);
            for(byte upgrade:config.upgrades())if(upgrade>=0&&golem.getRandom().nextBoolean())golem.spawnAtLocation(new ItemStack(TCItems.golemUpgrade(upgrade)),.5f);
        }else{
            if(golem.spawnAtLocation(portable(golem),.5f)==null)return true;
            golem.dropCargo();golem.discard();
        }
        golem.playSound(TCSounds.ZAP.get(),.5f,1);return true;
    }
}
