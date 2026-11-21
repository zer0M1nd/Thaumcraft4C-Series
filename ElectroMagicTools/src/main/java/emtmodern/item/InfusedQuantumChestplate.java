package emtmodern.item;

import ic2.api.item.ElectricItem;
import ic2.api.util.Keys;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** The author's 1.2.6.2 chestplate, including its separately stored attachment charge. */
public class InfusedQuantumChestplate extends ic2.core.item.armor.ItemArmorElectric {
    public record Attachment(int number, String item, String chargeKey, String texture, String icon) {}
    public static final java.util.List<Attachment> ATTACHMENTS=java.util.List.of(
        new Attachment(1,"ic2:jetpack_electric","jetpackCharge","quantum_jetpack","armor_quantum_chest_jetpack"),
        new Attachment(2,"emt:thaumium_wings","","quantum_wings_t","armor_quantum_chest_wing_t"),
        new Attachment(3,"emt:nano_wings","NWCharge","quantum_wings_n","armor_quantum_chest_wing_n"),
        new Attachment(4,"emt:quantum_wings","QWCharge","quantum_wings_q","armor_quantum_chest_wing_q"));
    public InfusedQuantumChestplate(){super(ElectricGoggles.MATERIAL,EquipmentSlot.CHEST,new Properties().stacksTo(1).rarity(Rarity.RARE),2000000,12000,4);}
    @Override public int getEnergyPerDamage(){return 20000;}
    @Override public double getDamageAbsorptionRatio(){return 1.1;}
    @Override public boolean isEnchantable(ItemStack stack){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get();}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return isEnchantable(stack);}
    @Override public int getEnchantmentValue(){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get()?4:0;}
    public static Attachment attachment(ItemStack chest){int id=chest.hasTag()?chest.getTag().getByte("wing"):0;return ATTACHMENTS.stream().filter(a->a.number()==id).findFirst().orElse(null);}
    public static void migrate(CompoundTag tag){
        if(tag.contains("useother")){
            String old=tag.getString("useother");int id=switch(old){case "Jetpack"->1;case "TW"->2;case "NW"->3;case "QW"->4;default->tag.getByte("wing");};
            tag.putByte("wing",(byte)id);tag.remove("useother");
        }
    }
    public static boolean install(Player player,ItemStack chest){
        if(!(chest.getItem() instanceof InfusedQuantumChestplate)||!player.isShiftKeyDown()||attachment(chest)!=null)return false;
        ItemStack held=player.getMainHandItem();if(held.isEmpty())return false;
        ResourceLocation id=ForgeRegistries.ITEMS.getKey(held.getItem());
        for(Attachment part:ATTACHMENTS)if(part.item().equals(String.valueOf(id))){
            CompoundTag tag=chest.getOrCreateTag();tag.putByte("wing",(byte)part.number());
            // Both the author and GTNH retain only integer EU, not the attachment's
            // custom name/enchantments. Do not change this old gameplay rule silently.
            if(!part.chargeKey().isEmpty())tag.putInt(part.chargeKey(),(int)ElectricItem.manager.getCharge(held));
            ItemStack stored=held.copy();stored.setCount(1);
            held.shrink(1);player.displayClientMessage(Component.translatable("message.emt.attachment_installed",stored.getHoverName()),true);return true;
        }
        return false;
    }
    public static boolean detach(Player player,ItemStack chest){
        if(!(chest.getItem() instanceof InfusedQuantumChestplate)||!player.isShiftKeyDown()||!player.getMainHandItem().isEmpty())return false;
        Attachment part=attachment(chest);if(part==null)return false;
        CompoundTag tag=chest.getOrCreateTag();Item item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(part.item()));if(item==null||item==Items.AIR)return false;
        ItemStack stored=new ItemStack(item);
        if(!part.chargeKey().isEmpty())ElectricItem.manager.charge(stored,Math.max(0,tag.getInt(part.chargeKey())),Integer.MAX_VALUE,true,false);
        tag.putByte("wing",(byte)0);tag.putByte("toggleTimer",(byte)30);
        for(Attachment other:ATTACHMENTS)if(!other.chargeKey().isEmpty())tag.remove(other.chargeKey());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stored);
        player.displayClientMessage(Component.translatable("message.emt.attachment_removed",stored.getHoverName()),true);return true;
    }
    @Override public void onArmorTick(ItemStack stack,Level world,Player player){
        var keys=Keys.instance;
        tick(stack,world,player,keys!=null&&keys.isJumpKeyDown(player),keys!=null&&keys.isModeSwitchKeyDown(player),keys!=null&&keys.isForwardKeyDown(player),keys!=null&&keys.isSneakKeyDown(player));
    }
    public void tick(ItemStack stack,Level world,Player player,boolean jump,boolean mode,boolean forward,boolean sneak){
        CompoundTag tag=stack.getOrCreateTag();int timer=Byte.toUnsignedInt(tag.getByte("toggleTimer"));boolean hover=tag.getBoolean("hoverMode");
        if(!world.isClientSide){
            migrate(tag);
            if(jump&&mode&&timer==0){timer=30;hover=!hover;tag.putBoolean("hoverMode",hover);player.displayClientMessage(Component.translatable(hover?"message.emt.hover_on":"message.emt.hover_off"),true);}
            if(timer==0)install(player,stack);
            if(tag.getBoolean("unequip")&&detach(player,stack))timer=30;
            tag.remove("unequip");
        }
        Attachment part=attachment(stack);int wing=part==null?0:part.number();boolean flying=false;
        // Parentheses intentionally retain the author's hover behavior independently of
        // the installed wing. This is a gameplay quirk, not an EU duplication path.
        if((jump&&wing==1)||(hover&&player.getDeltaMovement().y<-.03f))flying=useJetpack(player,hover,stack,jump,sneak,forward);
        if(wing>=2){String kind=switch(wing){case 2->"thaumium";case 3->"nano";default->"quantum";};useWings(player,stack,WingFlight.PROFILES.get(kind),jump);}
        if(!world.isClientSide&&timer>0)tag.putByte("toggleTimer",(byte)(timer-1));
        if(world.isClientSide)emtmodern.client.QuantumChestClient.jetpackSound(player,flying);
        player.clearFire();if(flying)player.inventoryMenu.broadcastChanges();
    }
    public static boolean useJetpack(Player player,boolean hover,ItemStack stack,boolean jump,boolean sneak,boolean forward){
        double charge=ElectricItem.manager.getCharge(stack);if(charge<=0)return false;
        float basePower=QuantumFlightRules.jetpackPower(charge,0,Integer.MAX_VALUE);
        if(forward)player.moveRelative(.02f,new Vec3(0,0,.4f*basePower*(hover?.5f:3.5f)*2));
        float power=QuantumFlightRules.jetpackPower(charge,player.getY(),player.level().getMaxBuildHeight());
        Vec3 movement=player.getDeltaMovement();double vertical=QuantumFlightRules.jetpackVertical(movement.y,power,hover,jump,sneak);
        player.setDeltaMovement(movement.x,vertical,movement.z);ElectricItem.manager.use(stack,hover?10:8,player);
        player.fallDistance=0;player.resetFallDistance();resetFlightTimer(player);return true;
    }
    protected static java.lang.reflect.Field aboveGroundTicks;
    public static void resetFlightTimer(Player player){
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&server.connection!=null){
            // The Refactored jetpack no longer exposes IC2's old resetPlayerInAirTime.
            // Cache the mapped vanilla field; no per-tick reflective lookup is needed.
            if(aboveGroundTicks==null)aboveGroundTicks=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.server.network.ServerGamePacketListenerImpl.class,"f_9737_");
            try{aboveGroundTicks.setInt(server.connection,0);}catch(IllegalAccessException ex){throw new IllegalStateException("Cannot reset flight timer for EMT jetpack",ex);}
        }
    }
    public static void useWings(Player player,ItemStack stack,WingFlight.Profile profile,boolean jump){
        CompoundTag tag=stack.getOrCreateTag();tag.putBoolean("isJumping",jump);int force=Math.max(0,Math.min(8,tag.getByte("f")));Vec3 motion=player.getDeltaMovement();
        if(jump){tag.putBoolean("isHolding",true);tag.putByte("f",(byte)(force>7?7:force+1));}
        else if(tag.getBoolean("isHolding")){
            tag.putBoolean("isHolding",false);double lift=profile.lift()*force;if(profile.energy()>0)ElectricItem.manager.use(stack,lift*10*profile.energy(),player);
            double x=motion.x,z=motion.z;if(x<.5&&x>-.5&&z<.5&&z>-.5){x/=profile.horizontal();z/=profile.horizontal();}motion=new Vec3(x,lift,z);tag.putByte("f",(byte)0);
            if(player.level().isClientSide){player.level().playLocalSound(player.getX(),player.getY(),player.getZ(),SoundEvents.GHAST_SHOOT,SoundSource.PLAYERS,1,1,false);for(int i=0;i<4;i++)player.level().addParticle(ParticleTypes.CLOUD,player.getX()-1+player.getRandom().nextInt(100)/50d,player.getY()-1,player.getZ()-1+player.getRandom().nextInt(100)/50d,0,-.5,0);}
        }
        if(jump&&!player.onGround()&&motion.y<0)motion=motion.multiply(1,profile.glide(),1);
        if(player.isInWater()&&!player.getAbilities().instabuild)motion=motion.add(0,-.05,0);
        var world=player.level();var pos=player.blockPosition();if(emtmodern.energy.EmtEnergyConfig.RAIN_IMPACT.get()&&!player.getAbilities().instabuild&&world.isRaining()&&world.canSeeSky(pos)&&world.getBiome(pos).value().hasPrecipitation())motion=motion.add(0,-.05,0);
        if(player.isShiftKeyDown()&&!player.onGround()&&motion.y<0)motion=motion.multiply(1,.6,1);
        player.setDeltaMovement(motion);player.fallDistance=0;
    }
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer){Attachment part=attachment(stack);return "emt:textures/models/"+(part==null?"quantum":part.texture())+".png";}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new emtmodern.client.QuantumChestArmorExtensions());}
}
