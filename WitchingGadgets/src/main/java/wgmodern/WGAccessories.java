package wgmodern;

import java.util.*;
import com.google.common.collect.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class WGAccessories {
    public static void registerCrafting(){thaumcraft.api.crafting.ArcaneResultTransforms.register(new net.minecraft.resources.ResourceLocation("witchinggadgets","cloak_to_kama"),(inputs,w,h,registries,result,config)->{for(var source:inputs)if(source.getItem() instanceof CloakItem cloak&&!cloak.kama){if(source.hasTag())result.setTag(source.getTag().copy());return result;}return ItemStack.EMPTY;});}
    public static final Map<String,String> TYPES=new LinkedHashMap<>();
    public static final UUID BOW_SPEED=UUID.fromString("b2b43de2-7880-44ac-88d3-7d381dab0606");
    static{TYPES.put("shoulders_doublejump","shouldersDoublejump");TYPES.put("shoulders_knockback","shouldersKnockback");TYPES.put("vambrace_strength","vambraceStrength");TYPES.put("vambrace_haste","vambraceHaste");TYPES.put("title_crimson_cult","titleCrimsonCult");TYPES.put("ring_luck","ringLuck");TYPES.put("ring_sniper","ringSniper");TYPES.put("shoulders_base","shouldersBase");TYPES.put("vambrace_base","vambraceBase");}
    public static class Gear extends Item implements ICurioItem {
        public final String kind;public Gear(String kind){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.kind=kind;}
        @Override public String getDescriptionId(){return "item.WG_Baubles."+TYPES.get(kind);}
        @Override public boolean canEquip(SlotContext c,ItemStack s){return c.identifier().equals(slot(kind));}
        @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(SlotContext c,UUID uuid,ItemStack stack){var m=HashMultimap.<Attribute,AttributeModifier>create();if(kind.equals("shoulders_knockback"))m.put(Attributes.KNOCKBACK_RESISTANCE,new AttributeModifier(uuid,"WGKnockbackResistance",.6,AttributeModifier.Operation.ADDITION));if(kind.equals("vambrace_strength"))m.put(Attributes.ATTACK_DAMAGE,new AttributeModifier(uuid,"WGStrengthBonus",2,AttributeModifier.Operation.ADDITION));return m;}
        @Override public int getFortuneLevel(SlotContext c,LootContext loot,ItemStack s){return luck(c,s);}
        @Override public int getLootingLevel(SlotContext c,DamageSource source,LivingEntity target,int base,ItemStack s){return luck(c,s);}
        private int luck(SlotContext c,ItemStack s){if(!WGConfig.LUCK_MODIFIERS.get()||!kind.equals("ring_luck"))return 0;return CuriosApi.getCuriosInventory(c.entity()).map(inv->inv.findCurios(x->x.getItem() instanceof Gear gear&&gear.kind.equals("ring_luck")).stream().findFirst().map(first->first.slotContext().identifier().equals(c.identifier())&&first.slotContext().index()==c.index()?2:0).orElse(0)).orElse(0);}
        @Override public void curioTick(SlotContext c,ItemStack s){var living=c.entity();if(kind.equals("vambrace_haste")&&living.onClimbable()){if(living.horizontalCollision)living.move(MoverType.SELF,new net.minecraft.world.phys.Vec3(0,.25,0));else if(!living.isShiftKeyDown())living.move(MoverType.SELF,new net.minecraft.world.phys.Vec3(0,-.1875,0));}}
        @Override public void appendHoverText(ItemStack s,net.minecraft.world.level.Level l,List<net.minecraft.network.chat.Component> lines,TooltipFlag f){if(s.hasTag()&&s.getTag().contains("title"))lines.add(net.minecraft.network.chat.Component.translatable(s.getTag().getString("title")));}
    }
    public static String slot(String kind){return kind.startsWith("shoulders")?"shoulders":kind.startsWith("vambrace")?"vambraces":kind.startsWith("title")?"title":"ring";}
    public static boolean wearing(LivingEntity p,String kind){return CuriosApi.getCuriosInventory(p).map(inv->inv.findFirstCurio(s->s.getItem() instanceof Gear gear&&gear.kind.equals(kind)).isPresent()).orElse(false);}
    public static boolean ignoresSpectral(Mob mob){return mob instanceof tc4revival.content.EldritchMob||mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss||mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon||tc4revival.content.RevealingGear.visible(mob,true);}
    @SubscribeEvent public static void hurt(LivingHurtEvent e){for(var slot:CloakItem.equipped(e.getEntity()))if(((CloakItem)slot.stack().getItem()).kind.equals("wolf"))slot.stack().getOrCreateTag().putInt("wolfPotion",e.getAmount()>=12?3:e.getAmount()>=8?2:1);}
    @SubscribeEvent public static void target(LivingChangeTargetEvent e){if(e.getNewTarget() instanceof Player p&&CloakItem.spectral(p)&&e.getEntity() instanceof Mob mob&&!ignoresSpectral(mob))e.setNewTarget(null);}
    @SubscribeEvent public static void interact(PlayerInteractEvent e){if(e.isCancelable()&&CloakItem.spectral(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(CloakItem.spectral(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed e){var p=e.getEntity();if(wearing(p,"vambrace_haste")){float speed=e.getNewSpeed();if(!p.onGround())speed*=5;if(p.isEyeInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get())&&!EnchantmentHelper.hasAquaAffinity(p))speed*=5;float hardness=e.getState().getDestroySpeed(p.level(),e.getPosition().orElse(p.blockPosition()));e.setNewSpeed(hardness>20?5+hardness:speed);}}
    @SubscribeEvent public static void drops(LivingDropsEvent e){var living=e.getEntity();if(living instanceof Wolf)for(int i=0;i<2+Math.min(4,e.getLootingLevel());i++)if(living.getRandom().nextInt(Math.max(1,3-e.getLootingLevel()))==0)e.getDrops().add(new ItemEntity(living.level(),living.getX(),living.getY(),living.getZ(),new ItemStack(Content.MATERIALS.get("wolf_pelt").get())));var id=ForgeRegistries.ENTITY_TYPES.getKey(living.getType());if(id==null||!id.getNamespace().equals("thaumcraft"))return;String title=switch(id.getPath()){case "cultist_cleric"->"crimsonCultist";case "cultist_knight"->"crimsonKnight";case "cultist_leader"->"crimsonPraetor";default->"";};if(!title.isEmpty()&&living.getRandom().nextInt(title.equals("crimsonPraetor")?2:10)<(title.equals("crimsonPraetor")?1:1+e.getLootingLevel())){var s=new ItemStack(Content.GEAR.get("title_crimson_cult").get());s.getOrCreateTag().putString("title","wg.title."+title);e.getDrops().add(new ItemEntity(living.level(),living.getX(),living.getY(),living.getZ(),s));}}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){var p=e.player;var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);if(speed==null)return;if(e.phase==TickEvent.Phase.START&&p.isUsingItem()&&p.getUseItem().getUseAnimation()==UseAnim.BOW&&wearing(p,"ring_sniper")){if(speed.getModifier(BOW_SPEED)==null)speed.addTransientModifier(new AttributeModifier(BOW_SPEED,"WGBowBonus",2.75,AttributeModifier.Operation.MULTIPLY_BASE));}else if(e.phase==TickEvent.Phase.END){speed.removeModifier(BOW_SPEED);if(p.onGround()||p.isInWater()||p.onClimbable()){p.getPersistentData().remove("WGDoubleJump");p.getPersistentData().putInt("WGAirborneTicks",0);}else p.getPersistentData().putInt("WGAirborneTicks",Math.min(10,p.getPersistentData().getInt("WGAirborneTicks")+1));}}
    public static void doubleJump(net.minecraft.server.level.ServerPlayer p){if(!p.isAlive()||p.isSpectator()||p.getPersistentData().getInt("WGAirborneTicks")<2||p.onGround()||p.isInWater()||p.onClimbable()||p.getAbilities().flying||p.isPassenger()||p.getPersistentData().getBoolean("WGDoubleJump")||!wearing(p,"shoulders_doublejump"))return;p.getPersistentData().putBoolean("WGDoubleJump",true);double power=.42;var boost=p.getEffect(net.minecraft.world.effect.MobEffects.JUMP);if(boost!=null)power+=(boost.getAmplifier()+1)*.1;var velocity=p.getDeltaMovement();p.setDeltaMovement(velocity.x,power,velocity.z);p.fallDistance=0;p.hurtMarked=true;net.minecraftforge.common.ForgeHooks.onLivingJump(p);}
}

