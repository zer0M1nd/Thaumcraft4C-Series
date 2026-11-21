package emtmodern.item;

import ic2.api.item.ElectricItem;
import ic2.api.util.Keys;
import ic2.core.item.armor.ItemArmorElectric;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="emt")
public class ElectricTravellerBoots extends ItemArmorElectric implements thaumcraft.api.IVisDiscountGear {
    protected final String kind;protected final int discount,energyPerDamage;
    protected static final java.util.Map<Player,Float> PREVIOUS_STEP=new java.util.WeakHashMap<>();
    public ElectricTravellerBoots(String kind){
        super(ElectricGoggles.MATERIAL,EquipmentSlot.FEET,new Properties().stacksTo(1),kind.equals("electric")?100000:kind.equals("nano")?1000000:10000000,kind.equals("electric")?100:kind.equals("nano")?1000:10000,2);
        this.kind=kind;discount=kind.equals("electric")?2:kind.equals("nano")?4:5;energyPerDamage=kind.equals("electric")?100:kind.equals("nano")?500:1000;
    }
    public float speed(){return kind.equals("electric")?.055f:(kind.equals("nano")?emtmodern.energy.EmtEnergyConfig.NANO_SPEED:emtmodern.energy.EmtEnergyConfig.QUANTUM_SPEED).get().floatValue();}
    public float jump(){return kind.equals("electric")?.3f:(kind.equals("nano")?emtmodern.energy.EmtEnergyConfig.NANO_JUMP:emtmodern.energy.EmtEnergyConfig.QUANTUM_JUMP).get().floatValue();}
    @Override public int getEnergyPerDamage(){return energyPerDamage;}
    @Override public double getDamageAbsorptionRatio(){return 1.1;}
    @Override public int getVisDiscount(ItemStack stack,Player player,thaumcraft.api.aspects.Aspect aspect){return discount;}
    @Override public boolean isEnchantable(ItemStack stack){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get();}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return isEnchantable(stack);}
    @Override public int getEnchantmentValue(){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get()?4:0;}
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer){return "emt:textures/models/"+(kind.equals("electric")?"electricboots":kind+"bootstravel")+".png";}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flags){super.appendHoverText(stack,level,lines,flags);lines.add(Component.translatable("tooltip.EMT.visDiscount").append(": "+discount+"%"));}
    public static boolean boost(ItemStack stack){return stack.hasTag()&&stack.getTag().getBoolean("EMTBoost");}
    public void updateToggle(ItemStack stack,boolean keyDown){var tag=stack.getOrCreateTag();if(keyDown&&!tag.getBoolean("EMTBoostKey"))tag.putBoolean("EMTBoost",!boost(stack));tag.putBoolean("EMTBoostKey",keyDown);}
    @Override public void onArmorTick(ItemStack stack,Level level,Player player){
        if(Keys.instance!=null)updateToggle(stack,Keys.instance.isBoostKeyDown(player)&&Keys.instance.isModeSwitchKeyDown(player));
        if(!player.getAbilities().flying&&player.zza>0){
            if(level.isClientSide&&!player.isShiftKeyDown()){PREVIOUS_STEP.putIfAbsent(player,player.maxUpStep());player.setMaxUpStep(1);}
            if(boost(stack)&&player.onGround())player.moveRelative(speed(),new Vec3(0,0,1));
        }
    }
    /** Called by a small accessor hook where 1.7.10's jumpMovementFactor used to apply. */
    public static float airSpeed(float original,Player player){
        var stack=player.getItemBySlot(EquipmentSlot.FEET);return stack.getItem() instanceof ElectricTravellerBoots boots&&boost(stack)&&player.zza>0&&!player.getAbilities().flying?boots.jump():original;
    }
    @SubscribeEvent public static void restoreStep(LivingEvent.LivingTickEvent event){
        if(event.getEntity() instanceof Player player&&player.level().isClientSide&&(player.isShiftKeyDown()||!(player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ElectricTravellerBoots))){var original=PREVIOUS_STEP.remove(player);if(original!=null)player.setMaxUpStep(original);}
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event){
        if(event.getEntity() instanceof Player player&&player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ElectricTravellerBoots boots)player.setDeltaMovement(player.getDeltaMovement().add(0,boots.jump(),0));
    }
    @SubscribeEvent public static void fall(LivingFallEvent event){
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide)return;var stack=player.getItemBySlot(EquipmentSlot.FEET);
        // Author registers the basic boots listener first; it handles subclasses before their listeners.
        if(stack.getItem() instanceof ElectricTravellerBoots&&ElectricItem.manager.use(stack,100,player))event.setCanceled(true);
    }
}
