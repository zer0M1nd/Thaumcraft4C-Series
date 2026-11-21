package tbmodern;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tbmodern.api.RevolverUpgrades;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;

public final class Revolver extends Item implements thaumcraft.api.IRepairable,thaumcraft.api.IWarpingGear {
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {return tbmodern.client.RevolverRenderer.instance();}
        });
    }
    public Revolver() {super(new Properties().durability(1561).rarity(Rarity.RARE));}
    public static ItemStack jar(ItemStack gun) {return gun.hasTag()?ItemStack.of(gun.getTag().getCompound("Jar")):ItemStack.EMPTY;}
    public static void jar(ItemStack gun,ItemStack jar) {gun.getOrCreateTag().put("Jar",jar.save(new CompoundTag()));}
    public static boolean validJar(ItemStack jar) {return jar.getItem() instanceof EssentiaJarItem item && item.storedAspects(jar).amount(TCAspects.catalog().require("telum"))>0;}
    @Override public int getWarp(ItemStack stack,Player player) {return RevolverUpgrades.level(stack,"void");}
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) {stack.hurtAndBreak(2,attacker,e->e.broadcastBreakEvent(EquipmentSlot.MAINHAND));return true;}
    @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(EquipmentSlot slot,ItemStack stack) {
        if(slot!=EquipmentSlot.MAINHAND)return super.getAttributeModifiers(slot,stack);
        return com.google.common.collect.ImmutableMultimap.of(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID,"Weapon modifier",5+2*RevolverUpgrades.level(stack,"heavy"),net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack gun=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(gun);
        if(player.containerMenu!=player.inventoryMenu)return InteractionResultHolder.fail(gun);
        int shots=Math.max(0,Math.min(1024,gun.getOrCreateTag().getInt("Shots")));
        if(shots>0) {
            if(level.addFreshEntity(new RevolverBullet(level,player,gun))) {
                gun.getOrCreateTag().putInt("Shots",shots-1);
                gun.getOrCreateTag().putDouble("BarrelRotation",(gun.getTag().getDouble("BarrelRotation")+45)%3600000);
                gun.hurtAndBreak(1,player,e->e.broadcastBreakEvent(hand));
                level.playSound(null,player.blockPosition(),Content.REVOLVER_SHOT.get(),SoundSource.PLAYERS,3,1);
            }
        } else {
            ItemStack vessel=jar(gun);boolean loaded=false;
            if(vessel.getItem() instanceof EssentiaJarItem container) {
                var essence=container.storedAspects(vessel);var telum=TCAspects.catalog().require("telum");
                int additional=RevolverUpgrades.shotsPerEssentia(gun),needed=additional<0?2:1;
                if(essence.amount(telum)>=needed) {
                    essence.remove(telum,needed);container.setStoredAspects(vessel,essence);
                    if(essence.total()==0)vessel=new ItemStack(TCArcane.ESSENTIA_JAR.get());
                    jar(gun,vessel);gun.getOrCreateTag().putInt("Shots",additional<0?1:additional);loaded=true;
                }
            }
            level.playSound(null,player.blockPosition(),loaded?Content.REVOLVER_RELOAD.get():Content.REVOLVER_CLICK.get(),SoundSource.PLAYERS,3,2);
        }
        return InteractionResultHolder.success(gun);
    }
    @Override public boolean onEntitySwing(ItemStack gun,LivingEntity entity) {
        if(entity instanceof ServerPlayer player && player.isShiftKeyDown() && player.containerMenu==player.inventoryMenu && player.getMainHandItem()==gun) {
            int slot=player.getInventory().selected;
            var provider=new SimpleMenuProvider((id,inventory,user)->new Menu(id,inventory,slot),Component.translatable("container.revolver"));
            net.minecraftforge.network.NetworkHooks.openScreen(player,provider,buffer->buffer.writeVarInt(slot));
        }
        return false;
    }
    @Override public void inventoryTick(ItemStack gun,Level level,Entity entity,int slot,boolean selected) {
        int repair=RevolverUpgrades.level(gun,"void");
        if(!level.isClientSide && entity instanceof LivingEntity && repair>0 && entity.tickCount%100/repair==0 && gun.isDamaged())gun.setDamageValue(gun.getDamageValue()-1);
    }
    @Override public void appendHoverText(ItemStack gun,Level level,List<Component> lines,TooltipFlag flags) {
        ItemStack vessel=jar(gun);
        if(vessel.getItem() instanceof EssentiaJarItem container)for(var aspect:container.storedAspects(vessel).aspects())
            lines.add((tc4revival.client.ClientKnowledge.snapshot().map(knowledge->knowledge.knows(aspect)).orElse(false)
                ? Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName()) : Component.translatable("tc.aspect.unknown"))
                .append(" x "+container.storedAspects(vessel).amount(aspect)));
        RevolverUpgrades.levels(gun).forEach((key,value)->lines.add(Component.translatable(RevolverUpgrades.require(key).name()).append(" ").append(Component.translatable("enchantment.level."+value))));
    }
    public static final class Menu extends AbstractContainerMenu {
        public final int gunSlot;
        private final Inventory inventory;
        private final ItemStack gun;
        private final SimpleContainer vessel=new SimpleContainer(1) {@Override public int getMaxStackSize(){return 1;}};
        public Menu(int id,Inventory inventory,int gunSlot) {
            super(Content.REVOLVER_MENU.get(),id);this.inventory=inventory;this.gunSlot=Math.max(0,Math.min(8,gunSlot));gun=inventory.getItem(this.gunSlot);
            vessel.setItem(0,jar(gun));
            vessel.addListener(container->{if(!inventory.player.level().isClientSide && stillValid(inventory.player)){jar(gun,vessel.getItem(0));inventory.setChanged();}});
            addSlot(new Slot(vessel,0,80,32){@Override public boolean mayPlace(ItemStack stack){return validJar(stack);}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
            for(int col=0;col<9;col++) {
                final boolean locked=col==this.gunSlot;
                addSlot(new Slot(inventory,col,8+col*18,142){@Override public boolean mayPickup(Player player){return !locked;}@Override public boolean mayPlace(ItemStack stack){return !locked;}});
            }
        }
        @Override public boolean stillValid(Player player){return inventory.player.level().isClientSide ? inventory.getItem(gunSlot).getItem() instanceof Revolver : gun.getItem() instanceof Revolver && inventory.getItem(gunSlot)==gun;}
        @Override public void clicked(int slot,int button,ClickType type,Player player) {
            if(slot==28+gunSlot || type==ClickType.SWAP && button==gunSlot || !stillValid(player))return;
            super.clicked(slot,button,type,player);
        }
        @Override public ItemStack quickMoveStack(Player player,int index) {
            if(index<0 || index>=slots.size() || index==28+gunSlot || !stillValid(player))return ItemStack.EMPTY;
            Slot slot=slots.get(index);ItemStack current=slot.getItem(),copy=current.copy();
            if(current.isEmpty() || !(index==0?moveItemStackTo(current,1,slots.size(),true):validJar(current)&&moveItemStackTo(current,0,1,false)))return ItemStack.EMPTY;
            if(current.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,current);return copy;
        }
    }
}
