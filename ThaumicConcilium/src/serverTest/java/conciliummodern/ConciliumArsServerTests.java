package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumArsServerTests {
    @GameTest(template="empty") public static void optionalArsDefinitionsAndDormantItemsRemainConsistent(GameTestHelper h){
        var parts=List.of("witchwood_rod","witchwood_staff","vinteum_cap","base_essence_core","high_essence_core","pure_essence");
        for(String name:parts)h.assertTrue(net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse("thaumicconcilium:"+name)),"Static bridge item registered independently of optional AML: "+name);
        h.assertTrue(tc4revival.content.WandParts.Rod.require("thaumicconcilium:witchwood").item.equals("thaumicconcilium:witchwood_rod"),"Witchwood rod uses the registered bridge item");
        h.assertTrue(tc4revival.content.WandParts.Rod.require("thaumicconcilium:witchwood_staff").item.equals("thaumicconcilium:witchwood_staff"),"Witchwood staff uses a distinct registered bridge item");
        h.assertTrue(tc4revival.content.WandParts.Cap.require("thaumicconcilium:vinteum").item.equals("thaumicconcilium:vinteum_cap"),"Vinteum cap uses the registered bridge item");
        if(!ArsLegacyIntegration.available()){
            h.assertTrue(!ConciliumSpellIntegration.arsAvailable(),"Absent AML installs no spell host");
            for(String key:List.of("ARS_SCHOOLS","ROD_witchwood","ROD_witchwood_staff","CAP_vinteum","CRIMSONRAID"))h.assertTrue(!thaumcraft.api.research.ResearchCategories.enabled("thaumicconcilium:"+key),"Absent AML disables research: "+key);
            for(String name:parts)h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumicconcilium:"+name)).isEmpty(),"Absent AML hides survival recipe: "+name);
        }
        h.succeed();
    }
    private static boolean missing(GameTestHelper h){if(ArsLegacyIntegration.available())return false;h.assertTrue(!ConciliumSpellIntegration.arsAvailable(),"Missing Ars never installs a fake spell host");org.slf4j.LoggerFactory.getLogger("ConciliumArsServerTests").info("CONCILIUM_TEST_NOT_RUN: Ars positive host branch absent; only dormant-provider assertion passed");h.succeed();return true;}
    @GameTest(template="empty",timeoutTicks=450) public static void realArsRaidResearchManaLeaseAndSavedLifetime(GameTestHelper h){if(!missing(h)){if(!CrimsonContent.available()){org.slf4j.LoggerFactory.getLogger("ConciliumArsServerTests").info("CONCILIUM_TEST_NOT_RUN: Crimson Raid requires TaintedMagic; AML-only registration is not positive summon acceptance");h.assertTrue(!ConciliumSpellIntegration.arsAvailable(),"Raid remains dormant without TaintedMagic");h.succeed();}else Linked.raid(h);}}
    @GameTest(template="empty") public static void realArsWitchwoodRechargeAndRecipeNbt(GameTestHelper h){if(!missing(h))Linked.wands(h);}
    @GameTest(template="empty",timeoutTicks=100) public static void realArsTerminusMoonstoneAndShootingStar(GameTestHelper h){if(!missing(h))Linked.meteors(h);}
    @GameTest(template="empty") public static void arsSchoolsProgressesWithoutBotaniaBloodMagicOrForeignMarker(GameTestHelper h){if(!missing(h))Linked.progression(h);}
    private static final class Linked {
        static com.github.minecraftschurlimods.arsmagicalegacy.api.ArsMagicaAPI api(){return com.github.minecraftschurlimods.arsmagicalegacy.api.ArsMagicaAPI.get();}
        static FakePlayer player(GameTestHelper h){var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"test-mock-player"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.getAttribute(com.github.minecraftschurlimods.arsmagicalegacy.common.init.AMAttributes.MAX_MANA.get()).setBaseValue(20000);player.getAttribute(com.github.minecraftschurlimods.arsmagicalegacy.common.init.AMAttributes.MAX_BURNOUT.get()).setBaseValue(20000);api().getManaHelper().setMana(player,20000);api().getBurnoutHelper().setBurnout(player,0);return player;}
        static void robes(FakePlayer player){player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(CrimsonContent.HELMET.get()));player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(CrimsonContent.CHEST.get()));player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(CrimsonContent.LEGS.get()));player.setItemSlot(EquipmentSlot.FEET,new ItemStack(CrimsonContent.BOOTS.get()));}
        static void raid(GameTestHelper h){
            h.assertTrue(ConciliumSpellIntegration.arsAvailable(),"Real Ars and TaintedMagic make Crimson Raid available");var player=player(h);var at=h.absolutePos(new BlockPos(4,3,4));player.setPos(Vec3.atBottomCenterOf(at));
            // A constructed FakePlayer alone is absent from ServerLevel.players(). Keep a
            // real world caster nearby rather than depending on players left by earlier tests.
            player.setNoGravity(true);player.setInvulnerable(true);h.getLevel().addNewPlayer(player);
            try{
            var raid=api().getSpellPartRegistry().getValue(ArsLegacyIntegration.RAID);h.assertTrue(raid instanceof com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpellComponent,"Registry contains a real host component");
            var data=api().getSpellDataManager().getDataForPart(raid);h.assertTrue(data!=null&&data.manaCost()==3000&&data.getBurnout()==300&&data.recipe().size()==9,"Host datapack loaded costs, three exact essences and DARK power");
            var spell=api().makeSpell(com.github.minecraftschurlimods.arsmagicalegacy.api.spell.SpellStack.of(com.github.minecraftschurlimods.arsmagicalegacy.common.init.AMSpellParts.SELF.get(),raid));
            robes(player);float before=api().getManaHelper().getMana(player);h.assertTrue(spell.cast(player,h.getLevel(),0,true,false).isFail()&&api().getManaHelper().getMana(player)==before,"Unresearched cast fails without host mana debit");
            ResearchCompletionBridge.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));
            tc4revival.research.KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,k->{k.rememberResearch("thaumicconcilium:CRIMSONRAID");return null;});
            ResearchCompletionBridge.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
            h.assertTrue(api().getSkillHelper().knows(player,ArsLegacyIntegration.RAID),"Committed research event unlocks actual AML skill capability");
            player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);h.assertTrue(spell.cast(player,h.getLevel(),0,true,false).isFail(),"Incomplete Pontifex set rejects cast");robes(player);
            float cost=spell.mana(player),burnout=spell.burnout(player);long created=player.server.overworld().getGameTime();h.assertTrue(spell.cast(player,h.getLevel(),0,true,false).isSuccess(),"Actual host spell cast summons raid");
            h.assertTrue(Math.abs(api().getManaHelper().getMana(player)-(before-cost))<.01&&Math.abs(api().getBurnoutHelper().getBurnout(player)-burnout)<.01,"AML consumes mana and burnout exactly once on success");
            var portals=h.getLevel().getEntitiesOfClass(LesserPortal.class,new AABB(at).inflate(2));h.assertTrue(portals.size()==1,"Exactly one portal inserted");var portal=portals.get(0);
            h.assertTrue(portal.getPersistentData().getUUID(ArsLegacyIntegration.OWNER).equals(player.getUUID())&&portal.getPersistentData().getLong(ArsLegacyIntegration.EXPIRES)==created+400,"Owner and original 400-tick lifetime committed");
            float after=api().getManaHelper().getMana(player);h.assertTrue(!ArsLegacyIntegration.canSummon(player)&&spell.cast(player,h.getLevel(),0,true,false).isFail()&&api().getManaHelper().getMana(player)==after,"Live summon blocks a second cast without debit");
            var saved=portal.saveWithoutId(new CompoundTag());var restored=CrimsonContent.PORTAL.get().create(h.getLevel());restored.load(saved);h.assertTrue(restored.getPersistentData().getUUID(ArsLegacyIntegration.OWNER).equals(player.getUUID())&&restored.getPersistentData().getLong(ArsLegacyIntegration.EXPIRES)==created+400,"Full entity NBT roundtrip retains owner and absolute expiry");
            var fakeLease=player.getPersistentData().getCompound(ArsLegacyIntegration.LEASE).copy();fakeLease.putUUID("Entity",UUID.randomUUID());player.getPersistentData().put(ArsLegacyIntegration.LEASE,fakeLease);h.assertTrue(!ArsLegacyIntegration.canSummon(player),"Unloaded summon lease cannot bypass the cap");fakeLease.putUUID("Entity",portal.getUUID());
            h.runAfterDelay(398,()->{
                long now=player.server.overworld().getGameTime();String state=raidState(portal,now,created);
                if(!portal.isAlive()||now>=created+400){player.discard();portal.discard();h.fail("Portal remains alive before expiry; "+state);}
            });
            h.runAfterDelay(401,()->{
                try{long now=player.server.overworld().getGameTime();h.assertTrue(now>=created+400&&portal.isRemoved()&&ArsLegacyIntegration.canSummon(player),"Real ticking expires portal and releases quota after 400 ticks; "+raidState(portal,now,created));h.succeed();}
                finally{player.discard();portal.discard();}
            });
            }catch(RuntimeException|Error failure){player.discard();throw failure;}
        }
        private static String raidState(LesserPortal portal,long now,long created){return "elapsed="+(now-created)+", expiry="+portal.getPersistentData().getLong(ArsLegacyIntegration.EXPIRES)+", now="+now+", removed="+portal.getRemovalReason()+", health="+portal.getHealth()+", stage="+portal.stage()+", age="+portal.tickCount;}
        static void wands(GameTestHelper h){
            var player=player(h);var rod=tc4revival.content.WandParts.Rod.require("thaumicconcilium:witchwood_staff");var cap=tc4revival.content.WandParts.Cap.require("thaumicconcilium:vinteum");var wand=tc4revival.content.TCItems.CASTING_WAND.get().create(rod,cap,false,false);var item=(tc4revival.content.CastingWandItem)wand.getItem();
            player.getInventory().items.set(15,wand);player.tickCount=40;float before=api().getManaHelper().getMana(player);thaumcraft.api.wands.WandRod.tick(wand,player);
            for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())h.assertTrue(item.vis(wand,aspect)==100,"One vis of every primal recharges even outside hotbar");h.assertTrue(Math.abs(api().getManaHelper().getMana(player)-(before-300))<.01,"Original Vinteum staff consumes 50 mana per full vis");
            player.tickCount=41;float charged=api().getManaHelper().getMana(player);thaumcraft.api.wands.WandRod.tick(wand,player);h.assertTrue(api().getManaHelper().getMana(player)==charged,"Non-40-tick updates do not charge");
            var air=tc4revival.aspect.TCAspects.catalog().require("aer");for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())item.receiveVis(wand,aspect,item.capacity(wand),false);
            item.consumeForPlayer(wand,player,Map.of(air,1),false,false,true);player.tickCount=80;before=api().getManaHelper().getMana(player);int deficit=item.capacity(wand)-item.vis(wand,air);thaumcraft.api.wands.WandRod.tick(wand,player);h.assertTrue(item.vis(wand,air)==item.capacity(wand)&&Math.abs(api().getManaHelper().getMana(player)-(before-deficit*.5f))<.01,"Partial capacity is paid in exact centivis without overcharge");
            var bracelet=ConciliumItems.BRACELETS.get("witchwood").get();boolean bases=net.minecraftforge.fml.ModList.get().isLoaded("thaumicbases");h.assertTrue(bracelet.available()==bases,"Real bridge rod/cap activate bracelet only with its ThaumicBases host");
            if(!bases)org.slf4j.LoggerFactory.getLogger("ConciliumArsServerTests").info("CONCILIUM_TEST_NOT_RUN: witchwood bracelet positive branch needs ThaumicBases; actual AML wand recharge still tested");
            for(String name:List.of("witchwood_rod","witchwood_staff","vinteum_cap","base_essence_core","high_essence_core","pure_essence"))h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumicconcilium:"+name)).isPresent(),"Bridge survival recipe exists: "+name);
            h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumicconcilium:witchwood_bracelet")).isPresent()==bases,"Bracelet recipe follows real host condition");
            var recipe=(tc4revival.content.InfusionRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumicconcilium:witchwood_rod")).orElseThrow();var essence=api().getAffinityHelper().getEssenceForAffinity(ResourceLocation.parse("arsmagicalegacy:arcane"));
            var plan=recipe.plan(ItemStack.EMPTY);h.assertTrue(plan.components().get(0).matches(essence)&&!plan.components().get(0).matches(api().getAffinityHelper().getEssenceForAffinity(ResourceLocation.parse("arsmagicalegacy:fire"))),"Real infusion matching requires the correct essence affinity NBT");h.succeed();
        }
        static void progression(GameTestHelper h){
            var player=player(h);var saved=tc4revival.research.KnowledgeSavedData.get(player.server);
            saved.update(player.getUUID(),player.getRandom()::nextInt,k->{tc4revival.research.ResearchCatalogs.progression().unlockAutomatic(k,false);k.forgetResearch("forbidden_magic:SCHOOLS");k.rememberResearch("ROD_silverwood");k.rememberResearch("INFUSION");return null;});
            var knowledge=saved.snapshot(player.getUUID()).orElseThrow();h.assertTrue(knowledge.researchMarkers().contains("thaumicconcilium:ARS_SCHOOLS")&&!knowledge.researchMarkers().contains("forbidden_magic:SCHOOLS"),"Own equivalent school auto-unlocks independently of FM's Botania/BloodMagic marker");
            h.assertTrue(tc4revival.research.ResearchCatalogs.rules().prerequisites(knowledge,"thaumicconcilium:ROD_witchwood"),"Actual rod progression opens with core prerequisites and own school");
            knowledge.rememberResearch("thaumicconcilium:ROD_witchwood");knowledge.rememberResearch("CAP_thaumium");knowledge.rememberResearch("ROD_silverwood_staff");
            h.assertTrue(tc4revival.research.ResearchCatalogs.rules().prerequisites(knowledge,"thaumicconcilium:CAP_vinteum")&&tc4revival.research.ResearchCatalogs.rules().prerequisites(knowledge,"thaumicconcilium:ROD_witchwood_staff"),"Cap and staff prerequisites remain reachable independently of unrelated hosts");
            org.slf4j.LoggerFactory.getLogger("ConciliumArsServerTests").info("Ars school matrix: Botania={}, BloodMagic={}",net.minecraftforge.fml.ModList.get().isLoaded("botania"),net.minecraftforge.fml.ModList.get().isLoaded("bloodmagic"));h.succeed();
        }
        static void meteors(GameTestHelper h){
            var level=h.getLevel();var at=h.absolutePos(new BlockPos(14,3,14));for(var p:BlockPos.betweenClosed(at.offset(-4,-1,-4),at.offset(4,-1,4)))level.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
            var effects=ConciliumSpellIntegration.meteors();h.assertTrue(effects.size()==2,"Terminus has both actual Ars meteor choices");effects.get(0).accept(level,at);effects.get(1).accept(level,at);
            // GameTest terrain placement may put its origin below zero. Build height is
            // an absolute boundary, not an offset from that origin: meteors start at 319.
            var area=new AABB(at.getX()-10,level.getMinBuildHeight(),at.getZ()-10,at.getX()+10,level.getMaxBuildHeight(),at.getZ()+10);var moons=level.getEntitiesOfClass(ConciliumMoonstoneMeteor.class,area,e->!e.shootingStar());var stars=level.getEntitiesOfClass(ConciliumMoonstoneMeteor.class,area,ConciliumMoonstoneMeteor::shootingStar);
            h.assertTrue(moons.size()==1&&stars.size()==1,"Terminus creates both ownerless AM2 meteor modes with actual AML ore integration; target="+at+", range="+area+", moon="+moons.size()+", star="+stars.size());
            var moon=moons.get(0);var tag=moon.saveWithoutId(new CompoundTag());var roundtrip=ConciliumMoonstoneMeteor.TYPE.get().create(level);roundtrip.load(tag);h.assertTrue(roundtrip.target().equals(at),"Meteor target survives NBT roundtrip");
            moon.setPos(Vec3.atCenterOf(at.above(4)));var star=stars.get(0);star.setPos(Vec3.atCenterOf(at.offset(6,4,0)));level.setBlockAndUpdate(at.offset(6,-1,0),Blocks.STONE.defaultBlockState());
            var victim=EntityType.ZOMBIE.create(level);victim.setNoAi(true);victim.setPos(Vec3.atBottomCenterOf(at.offset(6,0,0)));level.addFreshEntity(victim);
            h.runAfterDelay(20,()->{var ore=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("arsmagicalegacy:moonstone_ore"));int count=0;for(var p:BlockPos.betweenClosed(at.offset(-3,-1,-3),at.offset(3,4,3)))if(level.getBlockState(p).is(ore))count++;h.assertTrue(moon.isRemoved()&&count>=1&&count<=6,"Real collision deposits original 1-6 surface objects including moonstone ore");h.assertTrue(star.isRemoved()&&!victim.isAlive(),"Ownerless shooting star reaches impact and applies its lethal original 50 damage");h.succeed();});
        }
    }
}
