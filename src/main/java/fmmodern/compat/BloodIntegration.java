package fmmodern.compat;

import fmmodern.*;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thaumcraft.api.*;
import thaumcraft.api.wands.WandRod;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import wayoftime.bloodmagic.common.item.*;
import wayoftime.bloodmagic.core.data.*;
import wayoftime.bloodmagic.util.helper.*;

/** Blood Magic calls are isolated behind the installed-mod bridge. */
public final class BloodIntegration {
    public static final wayoftime.bloodmagic.common.registration.impl.BloodOrbDeferredRegister ORBS=new wayoftime.bloodmagic.common.registration.impl.BloodOrbDeferredRegister(ForbiddenMagic.ID);
    public static final java.util.function.Supplier<BloodOrb> ORB=ORBS.register("eldritch_orb",ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID,"eldritch_orb"),5,80000000,140);
    public static void register(){
        Content.item("bloodwell",()->new Inkwell(false));Content.item("boundwell",()->new Inkwell(true));Content.item("blood_rapier",Rapier::new);Content.item("eldritch_orb",Orb::new);
        ORBS.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
    }
    public static void setup(){
        wayoftime.bloodmagic.core.registry.OrbRegistry.tierMap.put(ORB.get().getTier(),new ItemStack(Content.item("eldritch_orb")));
        WandRod.registerUpdate("forbidden_magic:blood",BloodIntegration::recharge);WandRod.registerUpdate("forbidden_magic:blood_staff",BloodIntegration::recharge);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new BloodIntegration());
        ThaumcraftApi.addWarpToItem(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID,"blood_rapier"),2);ThaumcraftApi.addWarpToItem(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID,"eldritch_orb"),5);
    }
    public static Binding bind(ItemStack stack,Player player){
        Binding binding=Binding.fromStack(stack);if(binding==null){binding=new Binding(player.getUUID(),player.getGameProfile().getName());BindableHelper.applyBinding(stack,binding);}return binding;
    }
    private static boolean pay(ItemStack stack,int amount){
        if(amount==0)return true;var binding=Binding.fromStack(stack);if(binding==null)return false;
        var network=NetworkHelper.getSoulNetwork(binding);return network!=null&&network.syphon(new SoulTicket(amount))>=amount;
    }
    private static void recharge(ItemStack stack,Player player){
        if(player.level().isClientSide||!IntegrationContent.bloodEnabled()||!fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_WAND)||!(stack.getItem() instanceof CastingWandItem wand))return;
        boolean staff=WandParts.rod(stack.getTag()).staff();if(player.tickCount%(staff?25:100)!=0)return;
        if(staff){boolean hotbar=false;for(int slot=0;slot<9;slot++)if(player.getInventory().getItem(slot)==stack){hotbar=true;break;}if(!hotbar)return;}
        bind(stack,player);String cap=WandParts.cap(stack.getTag()).tag;int cost=Math.max(0,fmmodern.ForbiddenConfig.value(ForbiddenConfig.BLOOD_VIS)-(cap.equals("forbidden_magic:alchemical")||cap.endsWith(":blood_iron")?1:0));
        for(var aspect:TCAspects.catalog().primals()){
            int amount=Math.min(100,Math.max(0,wand.capacity(stack)-wand.vis(stack,aspect)));if(amount==0)continue;
            if(player.getAbilities().instabuild||pay(stack,amount*cost))wand.addVis(stack,aspect,amount,false,true);
            else {
                if(player.getHealth()>(staff?3:6)){player.setHealth(player.getHealth()-3);wand.addVis(stack,aspect,amount,false,true);}
                else if(staff&&player.isAlive()){player.hurt(player.damageSources().fellOutOfWorld(),Float.MAX_VALUE);wand.addVis(stack,aspect,amount,false,true);}
                return;
            }
        }
    }
    @SubscribeEvent public void alchemicalStrike(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        if(event.getEntity().level().isClientSide||!IntegrationContent.bloodEnabled()||!(event.getSource().getEntity() instanceof Player player))return;
        var stack=player.getMainHandItem();if(!(stack.getItem() instanceof CastingWandItem))return;
        String rod=WandParts.rod(stack.getTag()).tag,cap=WandParts.cap(stack.getTag()).tag;
        if((rod.equals("forbidden_magic:blood")||rod.equals("forbidden_magic:blood_staff"))&&(cap.equals("forbidden_magic:alchemical")||cap.endsWith(":blood_iron")))event.getEntity().addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,60,2));
    }
    @SubscribeEvent public void seal(wayoftime.bloodmagic.event.SoulNetworkEvent.Fill event){
        var player=event.getNetwork().getPlayer();if(IntegrationContent.bloodEnabled()&&player!=null&&player.hasEffect(Content.BLOOD_SEAL.get()))event.setCanceled(true);
    }
    private static final String SEALED_LP="forbidden_magic:sealed_lp";
    @SubscribeEvent public void sealApplied(net.minecraftforge.event.entity.living.MobEffectEvent.Added event){
        if(event.getEffectInstance().getEffect()==Content.BLOOD_SEAL.get()&&event.getEntity() instanceof Player player&&!player.level().isClientSide&&IntegrationContent.bloodEnabled()){
            int current=NetworkHelper.getSoulNetwork(player).getCurrentEssence();var data=player.getPersistentData();
            data.putInt(SEALED_LP,player.hasEffect(Content.BLOOD_SEAL.get())&&data.contains(SEALED_LP)?Math.min(current,data.getInt(SEALED_LP)):current);
        }
    }
    @SubscribeEvent public void sealTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event){
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.END)enforceSeal(event.player);
    }
    /** Retains the legacy lower-water-mark rule even for integrations that directly set LP. */
    static void enforceSeal(Player player){
        if(player.level().isClientSide)return;
        var data=player.getPersistentData();
        if(!IntegrationContent.bloodEnabled()||!player.hasEffect(Content.BLOOD_SEAL.get())){data.remove(SEALED_LP);return;}
        var network=NetworkHelper.getSoulNetwork(player);int current=network.getCurrentEssence();
        int ceiling=data.contains(SEALED_LP)?Math.min(data.getInt(SEALED_LP),current):current;
        if(current>ceiling)network.setCurrentEssence(ceiling);
        data.putInt(SEALED_LP,ceiling);
    }
    @SubscribeEvent public void drops(net.minecraftforge.event.entity.living.LivingDropsEvent event){
        var victim=event.getEntity();if(victim.level().isClientSide||!IntegrationContent.bloodEnabled()||!(event.getSource().getDirectEntity() instanceof Player))return;
        var weakness=victim.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
        if(weakness==null||weakness.getAmplifier()<2||!victim.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID,"tainted_mobs"))))return;
        if(victim.getRandom().nextBoolean()){var item=new net.minecraft.world.entity.item.ItemEntity(victim.level(),victim.getX(),victim.getY(),victim.getZ(),new ItemStack(Content.item("tainted_blood_shard")));item.setDefaultPickUpDelay();event.getDrops().add(item);}
    }
    @SubscribeEvent public void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event){
        if(!(event.getItemStack().getItem() instanceof CastingWandItem))return;var binding=Binding.fromStack(event.getItemStack());if(binding!=null)event.getToolTip().add(Component.translatable("tooltip.bloodmagic.currentOwner",binding.getOwnerName()));
    }
    public static class Inkwell extends Item implements IScribeTools,IBindable {
        private final boolean bound;
        public Inkwell(boolean bound){super(new Properties().durability(bound?100:5).setNoRepair());this.bound=bound;}
        @Override public void setInk(ItemStack stack,int ink){
            int next=Math.max(0,Math.min(getMaxDamage(stack),getMaxDamage(stack)-ink));int spent=Math.max(0,next-stack.getDamageValue());
            if(!bound&&spent>0&&IntegrationContent.bloodEnabled()&&net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer()&&pay(stack,spent*25))return;
            stack.setDamageValue(next);
        }
        @Override public boolean isFoil(ItemStack stack){return bound&&getInk(stack)==0;}
        @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
            if(bound||level.isClientSide||!IntegrationContent.bloodEnabled()||!stack.isDamaged()||!(entity instanceof Player player)||getBinding(stack)==null)return;
            if(player.getAbilities().instabuild)stack.setDamageValue(0);
            else if(pay(stack,25))stack.setDamageValue(stack.getDamageValue()-1);
            else if(player.getHealth()>6){player.setHealth(player.getHealth()-2);stack.setDamageValue(stack.getDamageValue()-1);}
        }
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            var stack=player.getItemInHand(hand);if(level.isClientSide)return InteractionResultHolder.success(stack);if(!IntegrationContent.bloodEnabled())return InteractionResultHolder.pass(stack);
            var binding=bind(stack,player);
            if(bound&&getInk(stack)==0&&player instanceof ServerPlayer server){
                if(!player.getAbilities().instabuild){NetworkHelper.getSoulNetwork(binding).syphonAndDamage(player,new SoulTicket(10000));if(!player.isAlive())return InteractionResultHolder.fail(stack);}
                stack.setDamageValue(0);for(var aspect:TCAspects.catalog().primals())ThaumcraftApiHelper.addResearchPoints(server,thaumcraft.api.aspects.Aspect.wrap(aspect),(short)(4+player.getRandom().nextInt(4)));
            }
            return InteractionResultHolder.success(stack);
        }
        @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flags){var binding=getBinding(stack);if(binding!=null)tooltip.add(Component.translatable("tooltip.bloodmagic.currentOwner",binding.getOwnerName()));}
    }
    private static final Tier RAPIER_TIER=new Tier(){
        public int getUses(){return 700;}public float getSpeed(){return 10;}public float getAttackDamageBonus(){return -3;}public int getLevel(){return 3;}public int getEnchantmentValue(){return 18;}public Ingredient getRepairIngredient(){return Ingredient.of(TCItems.PRIMAL_CHARM.get());}
    };
    public static final class Rapier extends SwordItem implements IRepairable,IWarpingGear {
        public Rapier(){super(RAPIER_TIER,3,-2.4f,new Properties().rarity(Rarity.RARE));}
        @Override public int getWarp(ItemStack stack,Player player){return 2;}
        @Override public boolean hurtEnemy(ItemStack stack,LivingEntity victim,LivingEntity attacker){
            super.hurtEnemy(stack,victim,attacker);if(victim.level().isClientSide)return true;
            victim.setDeltaMovement(victim.getDeltaMovement().multiply(1,.8,1));if(victim.invulnerableTime>18)victim.invulnerableTime-=5;
            if(IntegrationContent.bloodEnabled()&&victim instanceof Player target){var network=NetworkHelper.getSoulNetwork(target);int lp=network.getCurrentEssence();network.setCurrentEssence(Math.max(0,lp-Math.max(4000,lp/4)));target.addEffect(new MobEffectInstance(Content.BLOOD_SEAL.get(),1200));}return true;
        }
        @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){if(!level.isClientSide&&holder instanceof LivingEntity&&holder.tickCount%20==0&&stack.isDamaged())stack.setDamageValue(stack.getDamageValue()-1);}
    }
    public static final class Orb extends ItemBloodOrb implements IWarpingGear {
        public Orb(){super(ORB);}
        @Override public int getWarp(ItemStack stack,Player player){return 5;}
        @Override public Rarity getRarity(ItemStack stack){return Rarity.EPIC;}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            var stack=player.getItemInHand(hand);if(!IntegrationContent.bloodEnabled())return InteractionResultHolder.pass(stack);if(!level.isClientSide)bind(stack,player);return super.use(level,player,hand);
        }
    }
}
