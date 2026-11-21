package heartwork;

import heartwork.compat.blade.*;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("heartwork") @PrefixGameTestTemplate(false)
public class HeartworkCombatTests {
    @GameTest(template="empty") public static void driveCadenceAndInterruptionRemainDistinct(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var owner=h.spawn(EntityType.ZOMBIE,2,4,2);owner.setNoAi(true);
        var victim=h.spawn(EntityType.COW,2,4,3);victim.setNoAi(true);
        var drive=new HeartworkDriveEntity(BladeCompat.DRIVE.get(),h.getLevel());drive.setShooter(owner);drive.setPos(victim.position());drive.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);drive.setDamage(2.25);drive.setLifetime(20);
        drive.tickCount=1;drive.tick();h.assertTrue(victim.getHealth()==10,"Quick-drive skips odd ticks");
        drive.tickCount=2;drive.tick();h.assertTrue(victim.getHealth()==7.75F,"Quick-drive applies fractional damage on even ticks");
        drive.tickCount=4;drive.tick();h.assertTrue(victim.getHealth()==5.5F,"The same victim can be struck again");
        var cut=new HeartworkCutEntity(BladeCompat.CUT.get(),h.getLevel());cut.setShooter(owner);cut.setPos(victim.position());cut.setDamage(100);cut.setLifetime(20);cut.tickCount=2;cut.tick();
        h.assertTrue(victim.getHealth()==5.5F,"FIX area interruption does not invent an extra magic hit");
        var enderman=h.spawn(EntityType.ENDERMAN,2,4,3);enderman.setNoAi(true);cut.tickCount=4;cut.tick();
        h.assertTrue(enderman.getPersistentData().getLong("HeartworkTeleportBlockedUntil")==h.getLevel().getGameTime()+200,"Enderman teleport suppression lasts 200 ticks");
        owner.discard();victim.discard();enderman.discard();drive.discard();cut.discard();h.succeed();
    }
    @GameTest(template="empty") public static void wrapperPreservesSourceAndRejectsAlreadyWrappedBlade(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var json=com.google.gson.JsonParser.parseString("""
            {"pattern":["  P"," S ","T  "],"key":{"P":{"item":"slashblade:proudsoul"},"S":{"item":"heartwork:wrapped_blade"},"T":{"item":"minecraft:diamond_sword"}},"result":{"item":"heartwork:wrapped_blade"},"texture":"heartwork:blade/named/exparmoryblade/exparmorysteel.png","attack":4}
            """).getAsJsonObject();
        var recipe=BladeCompat.WRAPPING.get().fromJson(new ResourceLocation("heartwork:wrapper_fixture"),json);
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}
            public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
        },3,3);
        grid.setItem(2,new ItemStack(Heartwork.item("slashblade:proudsoul")));grid.setItem(4,new ItemStack(Heartwork.item("wrapped_blade")));
        var source=new ItemStack(Items.DIAMOND_SWORD);source.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);source.getOrCreateTag().putString("CustomSourceState","retained");grid.setItem(6,source);
        h.assertTrue(recipe.matches(grid,h.getLevel()),"Empty scabbard accepts the source blade");
        var result=recipe.assemble(grid,h.getLevel().registryAccess());var saved=ItemStack.of(result.save(new CompoundTag()));
        h.assertTrue(result.getMaxDamage()==source.getMaxDamage(),"Fresh crafted blade exposes its durability before any save/load");
        h.assertTrue(WrappedBladeItem.wrapped(saved).getTag().getString("CustomSourceState").equals("retained")&&saved.getMaxDamage()==source.getMaxDamage(),"Wrapping preserves full source NBT and durability limit across save/load");
        h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,saved)==3,"Wrapping transfers original enchantments");
        grid.setItem(4,result);h.assertTrue(!recipe.matches(grid,h.getLevel()),"An occupied scabbard cannot swallow another blade");
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {BladeCompat.WRAPPING.get().toNetwork(buffer,recipe);var decoded=BladeCompat.WRAPPING.get().fromNetwork(recipe.getId(),buffer);h.assertTrue(decoded.getResultItem(h.getLevel().registryAccess()).getMaxDamage()==source.getMaxDamage(),"Wrapper recipe crosses the network");}finally{buffer.release();}h.succeed();
    }
    @GameTest(template="empty") public static void chestLootRetainsVanillaAndWeightedBooks(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        for(String path:new String[]{"village/village_weaponsmith","jungle_temple","stronghold_corridor","stronghold_crossing","simple_dungeon"}) {
            var table=h.getLevel().getServer().getLootData().getLootTable(new ResourceLocation("minecraft:chests/"+path));
            h.assertTrue(table!=LootTable.EMPTY,"Original chest table still loads: "+path);
            var json=Deserializers.createLootTableSerializer().create().toJsonTree(table).getAsJsonObject();
            int a=0,b=0,vanilla=0;
            for(var element:json.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")) {
                var entry=element.getAsJsonObject();if(!entry.has("name"))continue;
                String name=entry.get("name").getAsString();
                if(name.equals("heartwork:book1"))a=entry.get("weight").getAsInt();
                else if(name.equals("heartwork:book2"))b=entry.get("weight").getAsInt();else vanilla++;
            }
            h.assertTrue(a==(path.startsWith("stronghold")||path.equals("simple_dungeon")?16:10)&&b==(path.equals("simple_dungeon")?10:4)&&vanilla>0,"Legacy weights plus original alternatives: "+path);
            var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,h.absoluteVec(net.minecraft.world.phys.Vec3.ZERO)).create(LootContextParamSets.CHEST);
            boolean original=false,book=false;
            for(int seed=0;seed<128;seed++)for(var stack:table.getRandomItems(params,seed+1)) {
                if(stack.is(Heartwork.item("book1"))||stack.is(Heartwork.item("book2")))book=true;else original=true;
            }
            h.assertTrue(book&&original,"Real loot generation returns books and original loot: "+path);
        }h.succeed();
    }
    @GameTest(template="empty") public static void fluxStartupUsesLegacyRankUnits(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var user=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.fromString("deabfc80-de00-4a91-bab7-6f5a3b4d7851"),"HeartworkRank"));
        var rank=user.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).orElseThrow(IllegalStateException::new);rank.setRawRankPoint(0);rank.setLastUpdte(h.getLevel().getGameTime());
        var blade=new ItemStack(Heartwork.item("rf_blade"));blade.getOrCreateTag().putInt("Energy",20_000);
        FluxBladeItem.toggle(user,blade);
        h.assertTrue(FluxBladeItem.energy(blade)==18_000,"1000 startup + 500 legacy rank points times two");
        h.assertTrue(rank.getRank(h.getLevel().getGameTime()).level==5,"Startup reaches S rank in the modern scale");
        FluxBladeItem.toggle(user,blade);h.assertTrue(!FluxBladeItem.empowered(blade)&&FluxBladeItem.energy(blade)==18_000,"Switching off has no startup cost");h.succeed();
    }
    @GameTest(template="empty") public static void summonedSwordUsesFractionalDamageAndPersistentAttachment(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var owner=h.spawn(EntityType.ZOMBIE,2,4,2);owner.setNoAi(true);
        var victim=h.spawn(EntityType.COW,2,4,5);victim.setNoAi(true);
        victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);victim.setHealth(100);
        var sword=new HeartworkSummonedSword(BladeCompat.SWORDS.get(),h.getLevel());sword.setShooter(owner);sword.setDamage(5.25);sword.configure(victim,9,0);
        sword.tick();h.assertTrue(sword.hasFired(),"No legacy hold limit releases the sword immediately");
        sword.setPos(victim.position().add(0,0.5,0));sword.tick();
        h.assertTrue(sword.getHitEntity()==victim&&Math.abs(victim.getHealth()-94.75F)<0.001,"Exact fractional initial magic damage and attachment");
        for(int i=0;i<70;i++)sword.tick();
        var tag=new CompoundTag();sword.addAdditionalSaveData(tag);
        var restored=new HeartworkSummonedSword(BladeCompat.SWORDS.get(),h.getLevel());restored.readAdditionalSaveData(tag);
        h.assertTrue(restored.getOwner()==owner&&restored.getHitEntity()==victim&&restored.phaseAge()==70,"Owner/target UUID and attachment age survive reload");
        for(int i=0;i<129;i++)restored.tick();
        h.assertTrue(!restored.isRemoved()&&victim.getHealth()==94.75F,"No second damage before 200 attached ticks");
        restored.tick();h.assertTrue(restored.isRemoved()&&Math.abs(victim.getHealth()-92.125F)<0.001,"Half damage exactly at 200 attached ticks");
        owner.discard();victim.discard();sword.discard();h.succeed();
    }
    @GameTest(template="empty") public static void matureCropDropsOriginalSeedDistribution(GameTestHelper h) {
        var crop=(heartwork.content.HeartworkCrop)Heartwork.BLOCK_MAP.get("strawberrycrop").get();
        var state=crop.getStateForAge(7);var pos=h.absolutePos(new BlockPos(2,2,2));
        int seeds=0;boolean zero=false;
        for(int n=0;n<1024;n++) {
            var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,ItemStack.EMPTY);
            int count=0,fruit=0;
            for(var stack:state.getDrops(params)) {if(stack.is(Heartwork.item("strawberry_seeds")))count+=stack.getCount();if(stack.is(Heartwork.item("strawberry")))fruit+=stack.getCount();}
            h.assertTrue(fruit==1&&count<=3,"One fruit and at most three seeds without Fortune");seeds+=count;if(count==0)zero=true;
        }
        h.assertTrue(zero&&Math.abs(seeds/1024.0-1.6)<0.15,"Zero seed result allowed; legacy mean 3 times 8/15");h.succeed();
    }
}
