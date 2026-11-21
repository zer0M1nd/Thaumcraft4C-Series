package fmmodern;

import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

public final class SinEvents {
    @SubscribeEvent public void crafted(net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent event){
        if(event.getEntity().level().isClientSide)return;
        if(event.getCrafting().is(Content.item("eldritch_orb"))){event.getEntity().drop(new ItemStack(tc4revival.content.TCItems.PRIMORDIAL_PEARL.get()),false);return;}
        if(!event.getCrafting().is(Content.PRIMEWELL.get()))return;
        // The arcane workbench fires this event before debiting its grid, as did the legacy bench.
        var grid=event.getInventory();for(int slot=0;slot<grid.getContainerSize();slot++){
            var ingredient=grid.getItem(slot);if(ingredient.is(tc4revival.content.TCItems.PRIMORDIAL_PEARL.get())){ingredient.grow(1);grid.setItem(slot,ingredient);}
        }
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void pain(LivingHurtEvent event){
        if(fmmodern.ForbiddenConfig.value(ForbiddenConfig.NO_LUST)||!(event.getEntity() instanceof Player player)||player.level().isClientSide||event.getAmount()<=0)return;
        var collar=CuriosApi.getCuriosInventory(player).resolve().flatMap(i->i.findFirstCurio(Content.COLLAR.get()));if(collar.isEmpty())return;
        int doses=3*(int)Math.min(event.getAmount(),player.getHealth());if(event.getSource().getEntity() instanceof Player attacker&&attacker.getMainHandItem().is(Content.CROP.get()))doses+=3;
        var primals=tc4revival.aspect.TCAspects.catalog().primals();for(int i=0;i<doses;i++)PainCollar.add(collar.get().stack(),primals.get(player.getRandom().nextInt(primals.size())),1);
    }
    @SubscribeEvent public void imprint(LivingDeathEvent event){
        if(!(event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player)||!player.getMainHandItem().is(Content.FORK.get()))return;
        var id=ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());if(!CageMobs.allowed(id))return;
        for(var stack:player.getInventory().items)if(stack.is(Content.MOB_CRYSTAL.get())&&SpecialItems.MobCrystal.mob(stack)==null){stack.getOrCreateTag().putString("mob",id.toString());return;}
    }
    @SubscribeEvent public void digging(net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event){
        if(!event.getEntity().getMainHandItem().is(Content.TAINT_PICK.get())||event.getPosition().isEmpty())return;
        float hardness=event.getState().getDestroySpeed(event.getEntity().level(),event.getPosition().get());
        if(hardness>=0)event.setNewSpeed(hardness==0?0:hardness<5?.1f:hardness<20?hardness/2:5+hardness);
    }
    @SubscribeEvent public void eat(LivingEntityUseItemEvent.Finish event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||!event.getItem().isEdible())return;
        if(player.level().dimension()==Level.NETHER&&!event.getItem().is(Content.GLUTTONY.get())&&player.getRandom().nextInt(10)<2)player.drop(new ItemStack(Content.GLUTTONY.get()),false);
        if(CuriosApi.getCuriosInventory(player).resolve().flatMap(i->i.findFirstCurio(Content.RING.get())).isPresent())player.getFoodData().eat(2,2);
    }
    @SubscribeEvent public void birth(BabyEntitySpawnEvent event) {
        var baby=event.getChild();
        if(!fmmodern.ForbiddenConfig.value(ForbiddenConfig.NO_LUST)&&baby!=null&&!baby.level().isClientSide&&baby.level().dimension()==Level.NETHER&&baby.getRandom().nextInt(3)==1)baby.spawnAtLocation(Content.item("lust_shard"));
    }
    @SubscribeEvent public void spawn(MobSpawnEvent.FinalizeSpawn event) {
        var mob=event.getEntity();
        if(envyMob(mob)&&mob.level().dimension()==Level.NETHER
                &&SinRules.envyCarrier(mob.getRandom().nextInt(SinRules.ENVY_ROLL_BOUND)))
            mob.getPersistentData().putBoolean("fmPendingEnvy",true);
    }
    @SubscribeEvent public void equipEnvy(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide||event.loadedFromDisk()||!(event.getEntity() instanceof Mob mob)
                ||!envyMob(mob)||!mob.getPersistentData().getBoolean("fmPendingEnvy"))return;
        // FinalizeSpawn is posted BEFORE vanilla initializes its weapon. Apply after that initialization.
        mob.getPersistentData().remove("fmPendingEnvy");
        mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Content.item("envy_shard")));
        // The player-only sin drop below owns this reward; ordinary equipment loot must not duplicate it.
        mob.setDropChance(EquipmentSlot.MAINHAND,-1);
    }
    private static boolean envyMob(LivingEntity mob){return mob instanceof ZombifiedPiglin||mob instanceof Piglin;}
    @SubscribeEvent public void xp(LivingExperienceDropEvent event) {
        var player=event.getAttackingPlayer(); if(player==null)return; var weapon=player.getMainHandItem();
        if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING,weapon)==0)event.setDroppedExperience(SinRules.educatedExperience(event.getDroppedExperience(),DarkEnchantments.level("educational",weapon)));
    }
    @SubscribeEvent public void drops(LivingDropsEvent event) {
        var victim=event.getEntity();if(victim.level().isClientSide)return;
        var random=victim.getRandom();var killer=event.getSource().getEntity(); var player=killer instanceof Player p?p:null;
        var weapon=player==null?ItemStack.EMPTY:player.getMainHandItem(); int looting=event.getLootingLevel();
        if(player!=null&&event.isRecentlyHit()) {
            if(fmmodern.ForbiddenConfig.value(ForbiddenConfig.SILVERFISH_EMERALDS)&&victim instanceof Silverfish) { boolean mountain=victim.level().getBiome(victim.blockPosition()).is(BiomeTags.IS_MOUNTAIN);if((mountain||!(player instanceof FakePlayer))&&SinRules.silverfish(random.nextInt(mountain?30:70),looting,mountain))drop(event,new ItemStack(Content.EMERALD_NUGGET.get())); }
            if(looting==0&&DarkEnchantments.level("greedy",weapon)>0) { if(victim instanceof Villager)drop(event,new ItemStack(Items.EMERALD));else if(victim instanceof Enemy&&random.nextInt(35)<3)drop(event,new ItemStack(Content.EMERALD_NUGGET.get())); }
            if(weapon.is(Content.SKULL_AXE.get())) {
                var type=ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()).getPath();
                Item head=SkullAxeHeads.resolve(victim.getType());
                if(head!=null&&SinRules.skull(random.nextInt(type.equals("player")?11:26),looting,type)){var stack=new ItemStack(head);if(victim instanceof Player target)stack.getOrCreateTag().put("SkullOwner",NbtUtils.writeGameProfile(new net.minecraft.nbt.CompoundTag(),target.getGameProfile()));drop(event,stack);}
            }
        }
        if(victim.level().dimension()!=Level.NETHER)return;
        if(player==null||player instanceof FakePlayer||!event.isRecentlyHit()){if(random.nextInt(30)<=3)drop(event,new ItemStack(Content.item("sloth_shard"),1+random.nextInt(3)));return;}
        if(victim instanceof Enemy){
            int wrath=2+(int)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            int greed=looting;
            if(weapon.getItem() instanceof tc4revival.content.CastingWandItem wand){wrath+=tc4revival.content.FocusPotency.discrete(wand.focusPotency(weapon,player),random);greed+=wand.focusUpgrade(weapon,tc4revival.content.FocusUpgrade.TREASURE);}
            wrath+=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS,weapon)+EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS,weapon);
            if(!victim.fireImmune())wrath+=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT,weapon)+EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS,weapon);
            if(victim.getMobType()==MobType.UNDEAD)wrath+=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE,weapon);
            if(victim.getMobType()==MobType.ARTHROPOD)wrath+=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS,weapon);
            if(random.nextInt(61)<=wrath)drop(event,new ItemStack(Content.item("wrath_shard")));
            if(victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss || victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)drop(event,new ItemStack(Content.item("pride_shard"),2+random.nextInt(1+looting)));
            if(greed>0&&random.nextInt(20)<=greed)drop(event,new ItemStack(Content.item("greed_shard")));
        }
        if(envyMob(victim)&&victim.getMainHandItem().is(Content.item("envy_shard")))drop(event,new ItemStack(Content.item("envy_shard")));
    }
    private static void drop(LivingDropsEvent event,ItemStack stack){var victim=event.getEntity();var drop=new ItemEntity(victim.level(),victim.getX(),victim.getY(),victim.getZ(),stack);drop.setDefaultPickUpDelay();event.getDrops().add(drop);}
}
