package emtmodern.item;

import ic2.api.item.ElectricItem;
import ic2.core.item.armor.ItemArmorElectric;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IVisDiscountGear;

/** Powered protection stays in IC2's damage pipeline, with the author's energy balance. */
public class ElectricGoggles extends ItemArmorElectric implements IGoggles,IVisDiscountGear {
    public static final ArmorMaterial MATERIAL=new ArmorMaterial(){
        public int getDurabilityForType(ArmorItem.Type type){return 0;}public int getDefenseForType(ArmorItem.Type type){return 0;}
        public int getEnchantmentValue(){return 4;}public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_IRON;}
        public Ingredient getRepairIngredient(){return Ingredient.EMPTY;}public String getName(){return "emt:powered";}
        public float getToughness(){return 0;}public float getKnockbackResistance(){return 0;}
    };
    protected final String kind;protected final int discount,energyPerDamage;
    protected final java.util.Map<ItemStack,SunState> sunStates=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    public ElectricGoggles(String kind){
        // The original getTier() returned 2 for all four types despite unused tier=3 fields.
        super(MATERIAL,EquipmentSlot.HEAD,new Properties().stacksTo(1),kind.equals("electric")?10000:kind.equals("nano")?100000:10000000,kind.equals("electric")?10:kind.equals("nano")?100:10000,2);
        this.kind=kind;discount=switch(kind){case "electric"->5;case "nano"->6;case "quantum"->8;default->7;};energyPerDamage=kind.equals("electric")?100:kind.equals("solar")?10000:1000;
    }
    public String kind(){return kind;}
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer){return "emt:textures/models/"+switch(kind){case "electric"->"electricgoggles";case "nano"->"thaumicnanohelmet";case "quantum"->"thaumicquantumhelmet";default->"solarrevealinghelmet";}+".png";}
    @Override public int getEnergyPerDamage(){return energyPerDamage;}
    @Override public double getDamageAbsorptionRatio(){return 1.1;}
    @Override public boolean isEnchantable(ItemStack stack){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get();}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return isEnchantable(stack);}
    @Override public int getEnchantmentValue(){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get()?4:0;}
    @Override public int getVisDiscount(ItemStack stack,Player player,thaumcraft.api.aspects.Aspect aspect){return discount;}
    @Override public boolean showNodes(ItemStack stack,LivingEntity wearer){return true;}
    @Override public boolean showIngamePopups(ItemStack stack,LivingEntity wearer){return true;}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flags){super.appendHoverText(stack,level,lines,flags);lines.add(Component.translatable("tooltip.EMT.visDiscount").append(": "+discount+"%"));}
    @Override public void onArmorTick(ItemStack stack,Level world,Player player){
        if(world.isClientSide)return;
        if(kind.equals("quantum")||kind.equals("solar")){
            if(player.getAirSupply()<100&&ElectricItem.manager.use(stack,1000,null))player.setAirSupply(player.getAirSupply()+200);
            // Legacy milk cure removes every milk-curable effect after any paid poison/wither/confusion trigger.
            for(var effect:new java.util.ArrayList<>(player.getActiveEffects())){
                int base=effect.getEffect()==MobEffects.POISON?10000:effect.getEffect()==MobEffects.WITHER?15000:effect.getEffect()==MobEffects.CONFUSION?5000:0;
                if(base>0&&ElectricItem.manager.use(stack,(double)base*(effect.getAmplifier()+1),null))player.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
            }
        }
        if(!kind.equals("electric")&&!emtmodern.energy.EmtEnergyConfig.NIGHT_VISION_OFF.get()){
            // Keep the author's zero-EU, negative-amplifier night vision rule; no silent balance correction.
            if(ElectricItem.manager.canUse(stack,0)){player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,300,-3));ElectricItem.manager.use(stack,0,player);}
            else player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,300,0,true,false));
        }
        if(kind.equals("solar"))generate(stack,world,player);
    }
    public void generate(ItemStack helmet,Level world,Player player){
        var state=sunStates.computeIfAbsent(helmet,key->new SunState());
        if(state.world!=world||state.counter++%128==0){
            state.world=world;var pos=player.blockPosition();boolean wet=world.getBiome(pos).value().hasPrecipitation()&&(world.isRaining()||world.isThundering());
            state.output=world.canSeeSky(pos.above())&&!world.dimensionType().hasCeiling()?(world.isDay()&&!wet?256:128):0;
        }
        double left=state.output;
        for(var stack:player.getInventory().armor){if(left<=0)return;if(stack.getItem() instanceof ic2.api.item.IElectricItem)left-=ElectricItem.manager.charge(stack,left,4,false,false);}
        for(var stack:player.getInventory().items){if(left<=0)return;if(stack.getItem() instanceof ic2.api.item.IElectricItem)left-=ElectricItem.manager.charge(stack,left,4,false,false);}
    }
    public static class SunState {protected Level world;protected int counter,output;}
}
