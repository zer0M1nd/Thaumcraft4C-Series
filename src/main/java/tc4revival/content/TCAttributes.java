package tc4revival.content;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/** Real synchronized attributes consumed by casting, warp and runic shielding. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class TCAttributes {
    private static final DeferredRegister<Attribute> TYPES=DeferredRegister.create(ForgeRegistries.ATTRIBUTES,"thaumcraft");
    public static final RegistryObject<Attribute> VIS_COST=attribute("vis_cost",1,-100,100);
    public static final RegistryObject<Attribute> POTENCY=attribute("potency",0,0,1024);
    public static final RegistryObject<Attribute> WARP=attribute("warp",0,0,100000);
    public static final RegistryObject<Attribute> RUNIC_SHIELD=attribute("runic_shield",0,0,100000);
    public static final Map<String,RegistryObject<Attribute>> ASPECT_COSTS=new LinkedHashMap<>();
    public static final UUID EXHAUSTION=UUID.fromString("09bed956-f36d-4186-a09c-a8d958dcd8ee");
    static { for(var aspect:tc4revival.aspect.AspectDefinitions.bundled().primals())ASPECT_COSTS.put(aspect.tag(),attribute(aspect.tag().replace(':','.')+"_vis_cost",0,-100,100)); }
    private TCAttributes() {}
    private static RegistryObject<Attribute> attribute(String name,double base,double min,double max) {
        return TYPES.register(name,()->new RangedAttribute("attribute.name.thaumcraft."+name,base,min,max).setSyncable(true));
    }
    public static void register(IEventBus bus) {
        TYPES.register(bus);bus.addListener(TCAttributes::attach);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            for(var effect:List.of(TCEffects.VIS_EXHAUST.get(),TCEffects.INFECTIOUS_VIS_EXHAUST.get()))
                effect.addAttributeModifier(VIS_COST.get(),EXHAUSTION.toString(),.1,AttributeModifier.Operation.ADDITION);
        }));
    }
    private static void attach(EntityAttributeModificationEvent event) {
        for(var type:event.getTypes()) { event.add(type,VIS_COST.get());event.add(type,POTENCY.get());event.add(type,WARP.get());event.add(type,RUNIC_SHIELD.get());for(var attribute:ASPECT_COSTS.values())event.add(type,attribute.get()); }
    }
    private static void modifier(BiConsumer<Attribute,AttributeModifier> add,String slot,Attribute attribute,double amount) {
        if(amount==0)return;
        UUID id=UUID.nameUUIDFromBytes(("thaumcraft:"+slot+":"+attribute.getDescriptionId()).getBytes(StandardCharsets.UTF_8));
        add.accept(attribute,new AttributeModifier(id,"Thaumcraft equipment",amount,AttributeModifier.Operation.ADDITION));
    }
    private static void equipment(ItemStack stack,Player player,String slot,boolean wearable,boolean discount,BiConsumer<Attribute,AttributeModifier> add) {
        if(stack.getItem() instanceof WarpingGear gear)modifier(add,slot,WARP.get(),gear.warp(stack,player));
        if(!wearable)return;
        modifier(add,slot,RUNIC_SHIELD.get(),RunicGear.charge(stack));
        if(discount && stack.getItem() instanceof VisDiscountGear gear) {
            int general=gear.visDiscount(stack,player,null);
            modifier(add,slot,VIS_COST.get(),-general/100d);
            for(var entry:ASPECT_COSTS.entrySet()) {
                int specific=gear.visDiscount(stack,player,tc4revival.aspect.TCAspects.catalog().require(entry.getKey()));
                modifier(add,slot,entry.getValue().get(),-(specific-general)/100d);
            }
        }
    }
    @SubscribeEvent public static void item(ItemAttributeModifierEvent event) {
        var stack=event.getItemStack();var slot=event.getSlotType();
        boolean armor=stack.getItem() instanceof ArmorItem item && item.getEquipmentSlot()==slot;
        if(!armor && (stack.getItem() instanceof ArmorItem || slot.getType()!=EquipmentSlot.Type.HAND))return;
        // Explicit stack attributes override item defaults, as with vanilla equipment.
        if(stack.hasTag() && stack.getTag().contains("AttributeModifiers",9))return;
        if(slot.getType()==EquipmentSlot.Type.HAND && stack.getItem() instanceof CastingWandItem wand && wand.rod(stack).runes())
            modifier(event::addModifier,slot.getName(),POTENCY.get(),1);
        if(armor && tc4revival.config.TCConfig.SERVER.revival.armorToughness.get()) {
            var item=stack.getItem();double toughness=item instanceof FortressArmorItem?3:item instanceof VoidArmorItem||item instanceof VoidRobeArmorItem?2:
                    item instanceof CultistArmorItem cultist&&cultist.definition().style==CultistEquipment.Style.LEADER?1:0;
            modifier(event::addModifier,slot.getName(),Attributes.ARMOR_TOUGHNESS,toughness);
        }
        equipment(stack,null,slot.getName(),armor,armor,(attribute,modifier)->{
            if(attribute!=WARP.get()||!(stack.getItem() instanceof WarpingGear gear)||gear.warpAppliesInSlot(stack,slot))event.addModifier(attribute,modifier);
        });
    }
    @SubscribeEvent public static void curio(CurioAttributeModifierEvent event) {
        var context=event.getSlotContext();
        if(context.cosmetic())return;
        var stack=event.getItemStack();
        if(context.identifier().equals("head") && stack.getItem() instanceof GogglesItem goggles) {
            if(goggles.canEquip(context,stack))
                modifier(event::addModifier,"curio:head:"+context.index(),RUNIC_SHIELD.get(),RunicGear.charge(stack));
            return;
        }
        if(!List.of("necklace","ring","belt").contains(context.identifier()))return;
        int limit=context.identifier().equals("ring")?2:1;if(context.index()>=limit)return;
        equipment(stack,context.entity() instanceof Player player?player:null,"curio:"+context.identifier()+":"+context.index(),true,true,(attribute,modifier)->{
            if(attribute!=WARP.get()||!(stack.getItem() instanceof WarpingGear gear)||gear.warpAppliesInSlot(stack,EquipmentSlot.CHEST))event.addModifier(attribute,modifier);
        });
    }
    /** Both exhaustion diseases penalize by their maximum amplifier, never their sum. */
    @SubscribeEvent public static void disease(LivingEvent.LivingTickEvent event) {
        var entity=event.getEntity();if(entity.level().isClientSide)return;
        var attribute=entity.getAttribute(VIS_COST.get());if(attribute==null)return;
        var flu=entity.getEffect(TCEffects.VIS_EXHAUST.get());var phage=entity.getEffect(TCEffects.INFECTIOUS_VIS_EXHAUST.get());
        double penalty=flu==null&&phage==null?0:(Math.max(flu==null?-1:flu.getAmplifier(),phage==null?-1:phage.getAmplifier())+1)/10d;
        var old=attribute.getModifier(EXHAUSTION);
        if(old!=null && old.getAmount()==penalty)return;
        if(old!=null)attribute.removeModifier(EXHAUSTION);
        if(penalty!=0)attribute.addTransientModifier(new AttributeModifier(EXHAUSTION,"Thaumcraft vis exhaustion",penalty,AttributeModifier.Operation.ADDITION));
    }
}
