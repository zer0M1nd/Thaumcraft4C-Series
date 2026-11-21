package thmodern.vat;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class SelfInfusionServerTests {
    @GameTest(template="empty")
    public static void togglesRequireActualAbilityAndClimbCanBeStopped(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"BodyToggleTest"));player.setPos(test.absolutePos(new BlockPos(3,5,3)).getCenter());
        test.assertTrue(!SelfInfusions.toggle(player,0)&&!SelfInfusions.toggle(player,1)&&!SelfInfusions.toggle(player,255),"Absent powers and unknown packet actions are rejected");
        SelfInfusions.replace(player,Set.of("spider_climb","chameleon_skin"));player.horizontalCollision=true;player.fallDistance=8;SelfInfusions.climb(player);
        test.assertTrue(Math.abs(player.getDeltaMovement().y-.2)<.00001&&player.fallDistance==0,"Wall contact climbs and clears fall distance");
        player.setShiftKeyDown(true);SelfInfusions.climb(player);test.assertTrue(player.getDeltaMovement().y==0,"Sneaking holds on the wall");
        test.assertTrue(SelfInfusions.toggle(player,0),"Owned climb can be disabled");player.setDeltaMovement(0,-.4,0);SelfInfusions.climb(player);test.assertTrue(player.getDeltaMovement().y==-.4,"Disabled climb leaves movement alone");
        test.assertTrue(player.hasEffect(MobEffects.INVISIBILITY)&&SelfInfusions.toggle(player,1)&&!player.hasEffect(MobEffects.INVISIBILITY),"Chameleon skin starts active and toggles off");
        SelfInfusions.refresh(player);test.assertTrue(!player.hasEffect(MobEffects.INVISIBILITY),"Periodic refresh respects disabled state");SelfInfusions.toggle(player,1);test.assertTrue(player.hasEffect(MobEffects.INVISIBILITY),"Second toggle restores invisibility");test.succeed();
    }
    @GameTest(template="empty")
    public static void photosynthesisNeedsDaylightAndOpenSky(GameTestHelper test){
        var level=test.getLevel();long before=level.getDayTime();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SunSkinTest"));
        // GameTest's flat template may be below the natural terrain; use genuinely open sky.
        var pos=test.absolutePos(new BlockPos(3,7,3)).atY(level.getMaxBuildHeight()-8);
        player.setPos(pos.getCenter());player.tickCount=200;player.getFoodData().setFoodLevel(10);SelfInfusions.replace(player,Set.of("photosynthetic_skin"));
        test.runAfterDelay(3,()->{
            try{
                level.setDayTime(6000);level.updateSkyBrightness();SelfInfusions.tick(player);
                test.assertTrue(player.getFoodData().getFoodLevel()==11,"Open daylight feeds one food point per 200 ticks");
                level.setBlock(pos.above(2),Blocks.STONE.defaultBlockState(),3);
            }catch(Throwable failure){level.setDayTime(before);level.updateSkyBrightness();throw failure;}
            test.runAfterDelay(3,()->{
                try{
                    SelfInfusions.tick(player);test.assertTrue(player.getFoodData().getFoodLevel()==11,"A roof blocks photosynthesis");
                    level.setBlock(pos.above(2),Blocks.AIR.defaultBlockState(),3);level.setDayTime(18000);level.updateSkyBrightness();SelfInfusions.tick(player);
                    test.assertTrue(player.getFoodData().getFoodLevel()==11,"Open night sky does not feed");test.succeed();
                }finally{level.setBlock(pos.above(2),Blocks.AIR.defaultBlockState(),3);level.setDayTime(before);level.updateSkyBrightness();}
            });
        });
    }

    @GameTest(template="empty")
    public static void bodyPowersSurviveEndReturnButNotOrdinaryRespawn(GameTestHelper test){
        var original=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"BodyOriginal"));SelfInfusions.replace(original,Set.of("awake_blood","amphibious"));
        var returned=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"BodyReturned"));SelfInfusions.clonePlayer(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(returned,original,false));
        var respawned=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"BodyRespawned"));SelfInfusions.clonePlayer(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(respawned,original,true));
        test.assertTrue(SelfInfusions.has(returned,"awake_blood")&&SelfInfusions.has(returned,"amphibious")&&!SelfInfusions.has(respawned,"awake_blood"),"End return copies body data; ordinary death does not");test.succeed();
    }
    @GameTest(template="empty")
    public static void completeSelfCycleKeepsBodyAndRejectsDuplicate(GameTestHelper test){
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(5,5,5));
        for(int part=0;part<36;part++)level.setBlock(center.offset(VatStructure.offset(part)),Horizons.VAT.get().defaultBlockState().setValue(VatBlock.PART,part),2);
        level.setBlock(center.above(),Horizons.VAT_MATRIX.get().defaultBlockState(),2);var vat=(VatEntity)level.getBlockEntity(center);
        vat.setItem(0,new ItemStack(Horizons.CORPSE_EFFIGY.get()));vat.tick();for(int i=0;i<80;i++)vat.tick();vat.accept("victus",8);vat.accept("sano",8);vat.tick();
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SelfInfusionTest"));player.setPos(center.above(2).getCenter());thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"selfInfusion");
        var recipe=VatInfusionRecipes.all().stream().filter(r->r.id.getPath().equals("self_quicksilver_limbs")).findFirst().orElseThrow();
        var positions=List.of(center.offset(-2,-1,0),center.offset(2,-1,0),center.offset(0,-1,-2),center.offset(0,-1,2),center.offset(-2,-1,-2),center.offset(2,-1,2));
        for(int i=0;i<positions.size();i++){level.setBlock(positions.get(i),tc4revival.content.TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),2);((tc4revival.content.ArcanePedestalBlockEntity)level.getBlockEntity(positions.get(i))).setItem(0,recipe.ingredients.get(i).getItems()[0].copy());}
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)if(x!=0||z!=0)level.setBlock(center.offset(x,2,z),Blocks.SKELETON_SKULL.defaultBlockState(),2);
        test.assertTrue(!recipe.target(player,false)&&recipe.target(null,true),"Human recipe requires a prepared body rather than a live player");
        test.assertTrue(vat.beginInfusion(player)&&vat.instability()<=0,"The body can start a stabilized self infusion");
        for(int i=0;i<30;i++)vat.tick();test.assertTrue(vat.selfInfusions().isEmpty(),"Missing essentia never yields a body upgrade");
        recipe.aspects.forEach(vat::accept);for(int i=0;i<450;i++)vat.tick();
        test.assertTrue(vat.mode()==VatEntity.EFFIGY&&vat.selfInfusions().equals(Set.of("quicksilver_limbs"))&&vat.subject()==null,"Finished infusion upgrades the detached body without creating an entity");
        var restored=new VatEntity(center,vat.getBlockState());restored.setLevel(level);restored.load(vat.saveWithoutMetadata());
        test.assertTrue(restored.selfInfusions().equals(vat.selfInfusions())&&!restored.canInfuseSelf(recipe),"Body abilities survive reload and reject duplicate upgrade");test.succeed();
    }
    @GameTest(template="empty")
    public static void passiveEffectsReplaceWithoutErasingUnrelatedEffects(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"SelfEffectsTest"));
        SelfInfusions.replace(player,Set.of("quicksilver_limbs","awake_blood","diamond_skin","unknown"));
        test.assertTrue(player.hasEffect(MobEffects.JUMP)&&player.hasEffect(MobEffects.MOVEMENT_SPEED)&&player.hasEffect(MobEffects.REGENERATION)&&player.hasEffect(MobEffects.DAMAGE_RESISTANCE)&&!SelfInfusions.has(player,"unknown"),"Known body effects apply and unknown data is ignored");
        test.assertTrue(player.getEffect(MobEffects.REGENERATION).isInfiniteDuration()&&player.getEffect(MobEffects.REGENERATION).getCurativeItems().isEmpty(),"Body effect is permanent and cannot be cured by milk");
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,600));SelfInfusions.replace(player,Set.of("amphibious"));
        test.assertTrue(!player.hasEffect(MobEffects.JUMP)&&!player.hasEffect(MobEffects.REGENERATION)&&player.hasEffect(MobEffects.NIGHT_VISION)&&SelfInfusions.has(player,"amphibious"),"Replacing body removes its own abilities without erasing unrelated potions");test.succeed();
    }
    @GameTest(template="empty")
    public static void silverHeartWeakensOnlySpecifiedDebuffs(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"SilverHeartTest"));SelfInfusions.replace(player,Set.of("silverwood_heart"));
        player.addEffect(new MobEffectInstance(MobEffects.POISON,80,2));player.addEffect(new MobEffectInstance(MobEffects.HUNGER,80,1));SelfInfusions.tick(player);
        test.assertTrue(player.getEffect(MobEffects.POISON).getDuration()==79&&player.getEffect(MobEffects.POISON).getAmplifier()==0,"Poison loses one additional tick and becomes level one");
        test.assertTrue(player.getEffect(MobEffects.HUNGER).getDuration()==80&&player.getEffect(MobEffects.HUNGER).getAmplifier()==1,"Hunger is not part of the silver-heart cure list");
        var type=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.parse("thaumcraft:taint"));
        var source=new net.minecraft.world.damagesource.DamageSource(test.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(type));
        var event=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,source,4);SelfInfusions.taintDamage(event);test.assertTrue(event.isCanceled()&&event.getAmount()==0,"Silverwood heart blocks taint damage");test.succeed();
    }
}
