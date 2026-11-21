package thmodern.blood;

import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import thmodern.Horizons;

public final class InjectorItem extends BowItem implements thaumcraft.api.IRepairable {
    public static final int SLOTS=7;
    public InjectorItem(){super(new Properties().durability(1000));}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
        private thmodern.client.InjectorRenderer renderer;
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new thmodern.client.InjectorRenderer();return renderer;}
    });}
    @Override public int getEnchantmentValue(){return 3;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    public static boolean ammunition(ItemStack stack){return stack.is(Horizons.INJECTION.get())||stack.is(Horizons.INJECTION_PHIAL.get());}
    public static ItemStack ammo(ItemStack injector,int slot){
        if(slot<0||slot>=SLOTS||!injector.hasTag())return ItemStack.EMPTY;
        var stack=ItemStack.of(injector.getTag().getList("Ammo",Tag.TAG_COMPOUND).getCompound(slot));return ammunition(stack)?stack.copyWithCount(1):ItemStack.EMPTY;
    }
    public static void saveAmmo(ItemStack injector,java.util.List<ItemStack> slots){
        var list=new ListTag();for(int i=0;i<SLOTS;i++){var stack=i<slots.size()?slots.get(i):ItemStack.EMPTY;list.add(ammunition(stack)?stack.copyWithCount(1).save(new CompoundTag()):new CompoundTag());}
        injector.getOrCreateTag().put("Ammo",list);
    }
    public static void advance(ItemStack injector){var slots=new java.util.ArrayList<ItemStack>();for(int i=1;i<SLOTS;i++)slots.add(ammo(injector,i));slots.add(ItemStack.EMPTY);saveAmmo(injector,slots);var tag=injector.getOrCreateTag();tag.putInt("Rotation",(tag.getInt("Rotation")+90)%360);}
    public static float power(int ticks){float f=Math.max(0,ticks)/30f;return Math.min(1,(f*f+2*f)/3);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()){
            if(player instanceof ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,p)->new InjectorMenu(id,inventory,hand),stack.getHoverName()),buffer->buffer.writeEnum(hand));
        }else if(ammo(stack,0).isEmpty()){
            if(!level.isClientSide){advance(stack);level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);}
        }else player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){
        if(level.isClientSide||!(entity instanceof Player player))return;
        var payload=ammo(stack,0);float power=power(getUseDuration(stack)-remaining);if(payload.isEmpty()||power<.1f)return;
        net.minecraft.world.entity.projectile.Projectile projectile;
        if(payload.is(Horizons.INJECTION.get())){
            var syringe=new SyringeProjectile(level,player,payload);syringe.setCritArrow(power==1);
            int damage=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS,stack);if(damage>0)syringe.setBaseDamage(1.5+damage*.5+.5);
            syringe.setKnockback(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS,stack));
            if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS,stack)>0)syringe.setSecondsOnFire(100);
            projectile=syringe;
        }else{var phial=new net.minecraft.world.entity.projectile.ThrownPotion(level,player);phial.setItem(payload);projectile=phial;}
        projectile.shootFromRotation(player,player.getXRot(),player.getYRot(),0,power*3,1);
        if(!level.addFreshEntity(projectile))return;
        advance(stack);stack.hurtAndBreak(1,player,p->p.broadcastBreakEvent(player.getUsedItemHand()));
        level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ARROW_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,1,1/(level.random.nextFloat()*.4f+1.2f)+power*.5f);
    }
}
