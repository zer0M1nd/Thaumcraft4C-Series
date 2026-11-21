package arcanearteries;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import wayoftime.bloodmagic.common.tile.TileAltar;
import wayoftime.bloodmagic.event.SacrificeKnifeUsedEvent;
import wayoftime.bloodmagic.util.helper.PlayerSacrificeHelper;

@GameTestHolder(ArcaneArteries.ID) @PrefixGameTestTemplate(false)
public class ArteriesGameTests {
    private static final BlockPos ALTAR=new BlockPos(4,2,4);
    private static TileAltar altar(GameTestHelper t){
        t.setBlock(ALTAR,Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("bloodmagic:altar"))).defaultBlockState());
        return (TileAltar)t.getBlockEntity(ALTAR);
    }
    private static ServerPlayer player(GameTestHelper t){
        var p=new ServerPlayer(t.getLevel().getServer(),t.getLevel(),new GameProfile(UUID.randomUUID(),"ArteriesTest"));
        new ServerGamePacketListenerImpl(p.server,new Connection(PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}},p);
        p.setGameMode(GameType.SURVIVAL);p.moveTo(t.absolutePos(ALTAR.above()).getCenter());t.getLevel().addNewPlayer(p);return p;
    }
    private static void use(ServerPlayer p,Item item){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));item.use(p.level(),p,InteractionHand.MAIN_HAND);}
    @GameTest(template="empty") public static void realSelfPaymentFrayAndHardMode(GameTestHelper t){
        var a=altar(t);var p=player(t);
        try {
            use(p,ArcaneArteries.THAUMIC_KNIFE.get());
            t.assertTrue(p.getHealth()==18&&a.getCurrentBlood()==250,"Thaumic self: exactly 2 HP -> 250 LP");
            ArteriesConfig.HARD_DAGGERS.set(true);
            use(p,ArcaneArteries.THAUMIC_KNIFE.get());
            t.assertTrue(p.getHealth()==16&&a.getCurrentBlood()==375,"Hard mode adds 125 LP once");
            ArteriesConfig.HARD_DAGGERS.set(false);
            var fray=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("bloodmagic:soulfray"));
            t.assertTrue(fray!=null,"Host Soul Fray effect resolved");
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(fray,200));
            use(p,ArcaneArteries.THAUMIC_KNIFE.get());
            t.assertTrue(a.getCurrentBlood()==400,"Soul Fray: exactly 25 LP");
            var fake=FakePlayerFactory.getMinecraft(t.getLevel());fake.moveTo(p.position());fake.setHealth(20);use(fake,ArcaneArteries.THAUMIC_KNIFE.get());
            t.assertTrue(fake.getHealth()==20&&a.getCurrentBlood()==400,"Fake player changes neither health nor LP");
            ArteriesConfig.SELF.set(false);float hp=p.getHealth();use(p,ArcaneArteries.THAUMIC_KNIFE.get());
            t.assertTrue(p.getHealth()==hp&&a.getCurrentBlood()==400,"Disabled self sacrifice is inert");
        }finally{ArteriesConfig.SELF.set(true);ArteriesConfig.HARD_DAGGERS.set(false);p.discard();}
        t.succeed();
    }
    public static class CancelSelf {
        final UUID id;int mode=0;CancelSelf(ServerPlayer p){id=p.getUUID();}
        @SubscribeEvent public void cancel(SacrificeKnifeUsedEvent e){if(!e.player.getUUID().equals(id))return;if(mode==0)e.setCanceled(true);else if(mode==1)e.shouldDrainHealth=false;else e.shouldFillAltar=false;}
    }
    @GameTest(template="empty") public static void canceledEventsAndIncenseFlags(GameTestHelper t){
        var a=altar(t);var p=player(t);var listener=new CancelSelf(p);MinecraftForge.EVENT_BUS.register(listener);
        try{
            use(p,ArcaneArteries.THAUMIC_KNIFE.get());t.assertTrue(p.getHealth()==20&&a.getCurrentBlood()==0,"Cancellation is atomic");
            listener.mode=1;use(p,ArcaneArteries.THAUMIC_KNIFE.get());t.assertTrue(p.getHealth()==20&&a.getCurrentBlood()==250,"No-health event respected");
            listener.mode=2;use(p,ArcaneArteries.THAUMIC_KNIFE.get());t.assertTrue(p.getHealth()==18&&a.getCurrentBlood()==250,"No-fill event respected");
            listener.mode=0;PlayerSacrificeHelper.setPlayerIncense(p,1);
            var blade=(SelfSacrificeKnife)ArcaneArteries.THAUMIC_KNIFE.get();var stack=new ItemStack(blade);blade.setUseForSacrifice(stack,true);
            blade.releaseUsing(stack,p.level(),p,71000);t.assertTrue(p.getHealth()==18&&PlayerSacrificeHelper.getPlayerIncense(p)==1&&a.getCurrentBlood()==250,"Canceled incense remains available");
            MinecraftForge.EVENT_BUS.unregister(listener);
            blade.releaseUsing(stack,p.level(),p,71000);t.assertTrue(p.getHealth()==2&&PlayerSacrificeHelper.getPlayerIncense(p)==0&&a.getCurrentBlood()>250,"Incense performs actual health and altar transfer");
        }finally{MinecraftForge.EVENT_BUS.unregister(listener);p.discard();}
        t.succeed();
    }
    @GameTest(template="empty") public static void configurableBladeAndManaRequirement(GameTestHelper t){
        var a=altar(t);var p=player(t);
        try{
            use(p,ArcaneArteries.CUSTOM_KNIFE.get());t.assertTrue(p.getHealth()==20&&a.getCurrentBlood()==0,"Custom blade disabled by default");
            ArteriesConfig.CUSTOM_SELF.set(true);ArteriesConfig.HEALTH.set(3);ArteriesConfig.SELF_LP.set(321);
            use(p,ArcaneArteries.CUSTOM_KNIFE.get());t.assertTrue(p.getHealth()==17&&a.getCurrentBlood()==321,"Custom HP and LP are applied");
            if(ModList.get().isLoaded("botania")){
                use(p,ArcaneArteries.BOTANICAL_KNIFE.get());t.assertTrue(p.getHealth()==17&&a.getCurrentBlood()==321,"Absent mana cannot pay or fill");
                var tablet=new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation("botania:mana_tablet"))));
                tablet.getOrCreateTag().putInt("mana",1000);p.getInventory().setItem(2,tablet);
                use(p,ArcaneArteries.BOTANICAL_KNIFE.get());
                t.assertTrue(p.getHealth()==15&&a.getCurrentBlood()==821&&tablet.getTag().getInt("mana")==900,"Actual Botania mana pays exactly 100 for 500 LP");
                var listener=new CancelSelf(p);MinecraftForge.EVENT_BUS.register(listener);
                try{use(p,ArcaneArteries.BOTANICAL_KNIFE.get());t.assertTrue(tablet.getTag().getInt("mana")==900,"Canceled event does not consume mana");}
                finally{MinecraftForge.EVENT_BUS.unregister(listener);}
                ArteriesConfig.REQUIRE_MANA.set(false);p.getInventory().setItem(2,ItemStack.EMPTY);use(p,ArcaneArteries.BOTANICAL_KNIFE.get());
                t.assertTrue(a.getCurrentBlood()==1321,"Mana switch permits a real use without tablet");
            }else{use(p,ArcaneArteries.BOTANICAL_KNIFE.get());t.assertTrue(a.getCurrentBlood()==321,"Missing optional host safely disables blade");}
        }finally{ArteriesConfig.CUSTOM_SELF.set(false);ArteriesConfig.HEALTH.set(2);ArteriesConfig.SELF_LP.set(100);ArteriesConfig.REQUIRE_MANA.set(true);p.discard();}
        t.succeed();
    }
    public static class CancelDeath {
        final LivingEntity target;CancelDeath(LivingEntity t){target=t;}
        @SubscribeEvent public void cancel(LivingAttackEvent e){if(e.getEntity()==target)e.setCanceled(true);}
    }
    @GameTest(template="empty") public static void mobDeathMustCommitBeforeLp(GameTestHelper t){
        var a=altar(t);var p=player(t);var blade=(MobSacrificeKnife)ArcaneArteries.THAUMIC_SACRIFICE.get();
        try{
            for(var type:List.of(EntityType.ZOMBIE,EntityType.VILLAGER,EntityType.SLIME,EntityType.ENDERMAN,EntityType.COW)){
                LivingEntity mob=(LivingEntity)type.create(t.getLevel());mob.moveTo(t.absolutePos(ALTAR.above()).getCenter());t.getLevel().addFreshEntity(mob);
                int before=a.getCurrentBlood();int expected=type==EntityType.VILLAGER?2250:type==EntityType.SLIME?225:type==EntityType.ENDERMAN?300:type==EntityType.COW?375:625;
                t.assertTrue(blade.sacrifice(mob,p),"Actual sacrifice "+type);t.assertTrue(!mob.isAlive()&&a.getCurrentBlood()==before+expected,"Death and exact fixed yield "+type);
                blade.sacrifice(mob,p);t.assertTrue(a.getCurrentBlood()==before+expected,"Dead target cannot pay twice");mob.discard();
            }
            Cow baby=EntityType.COW.create(t.getLevel());baby.setBaby(true);baby.moveTo(p.position());t.getLevel().addFreshEntity(baby);
            int before=a.getCurrentBlood();t.assertTrue(!blade.sacrifice(baby,p)&&baby.isAlive()&&a.getCurrentBlood()==before,"Babies excluded");baby.discard();
            var protectedMob=EntityType.COW.create(t.getLevel());protectedMob.moveTo(p.position());t.getLevel().addFreshEntity(protectedMob);
            var listener=new CancelDeath(protectedMob);MinecraftForge.EVENT_BUS.register(listener);
            try{blade.sacrifice(protectedMob,p);t.assertTrue(protectedMob.isAlive()&&a.getCurrentBlood()==before,"Canceled damage cannot mint LP");}
            finally{MinecraftForge.EVENT_BUS.unregister(listener);protectedMob.discard();}
            t.assertTrue(!blade.sacrifice(p,p),"Player excluded");
        }finally{p.discard();}
        t.succeed();
    }
    @GameTest(template="empty",timeoutTicks=160) public static void runeTierAndActualSlateProcessing(GameTestHelper t){
        var a=altar(t);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)t.setBlock(ALTAR.offset(x,-1,z),(x==0?ArcaneArteries.MANA_RUNE:ArcaneArteries.THAUMIC_RUNE).get());
        a.checkTier();t.assertTrue(a.getTier()==2,"Mixed original runes form a real tier-two altar");
        t.assertTrue(a.getCapacity()==10000&&a.getConsumptionMultiplier()==0,"Both rune variants are blank structural runes");
        // BM moves 20 LP into its output buffer each second. Supply a margin and count both tanks.
        a.fillMainTank(2500);a.setItem(0,new ItemStack(ArcaneArteries.THAUMIC_SLATE.get()));a.startCycle();
        t.succeedWhen(()->{t.assertTrue(a.getItem(0).is(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation("bloodmagic:reinforcedslate")))),"Altar actually processes custom slate into reinforced slate");
            var nbt=a.serialize(new net.minecraft.nbt.CompoundTag()).getCompound("bloodAltar");
            t.assertTrue(a.getCurrentBlood()+nbt.getInt("outputAmount")==1000,"Altar recipe spends exactly 1500 LP including output buffer");});
    }
    @GameTest(template="empty") public static void recipesAndResearchMatchAvailability(GameTestHelper t){
        var manager=t.getLevel().getRecipeManager();
        boolean hard=ArteriesConfig.HARD_CRAFTING.get();String suffix=hard?"_hard":"";
        for(String path:List.of("thaumic_knife"+suffix,"thaumic_sacrifice"+suffix,"thaumic_slate","thaumic_rune","thaumic_slate_reinforced"))t.assertTrue(manager.byKey(ArcaneArteries.id(path)).isPresent(),"Selected recipe loaded: "+path);
        t.assertTrue(manager.byKey(ArcaneArteries.id("thaumic_knife"+(hard?"":"_hard"))).isEmpty(),"Opposite difficulty recipe is absent");
        t.assertTrue(manager.byKey(ArcaneArteries.id("botanical_knife"+suffix)).isPresent()==ModList.get().isLoaded("botania"),"Botania recipe conditioned by actual host");
        t.assertTrue(manager.byKey(ArcaneArteries.id("evil_knife")).isPresent()==ModList.get().isLoaded("evilcraft"),"EvilCraft recipe conditioned by actual host");
        t.assertTrue(ArcaneArteries.researchEnabled("DAGGER")!=hard&&ArcaneArteries.researchEnabled("DAGGERH")==hard,"Research follows active difficulty");
        ArteriesConfig.HARD_CRAFTING.set(!hard);
        try{t.assertTrue(ArcaneArteries.researchEnabled("DAGGER")==hard&&ArcaneArteries.researchEnabled("DAGGERH")!=hard,"Research switches difficulty");}
        finally{ArteriesConfig.HARD_CRAFTING.set(hard);}
        t.succeed();
    }
    @GameTest(template="empty") public static void actualCraftingHostRecipes(GameTestHelper t){
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,-1){
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        if(ModList.get().isLoaded("evilcraft")){
            var flesh=ForgeRegistries.ITEMS.getValue(new ResourceLocation("evilcraft:flesh_werewolf"));
            grid.setItem(0,new ItemStack(flesh));grid.setItem(3,new ItemStack(flesh));
            grid.setItem(6,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("bloodmagic:sacrificialdagger"))));
            craft(t,grid,"evil_knife",ArcaneArteries.EVIL_KNIFE.get());
            grid.clearContent();grid.setItem(0,new ItemStack(ArcaneArteries.EVIL_KNIFE.get()));
            craft(t,grid,"evil_sacrifice",ArcaneArteries.EVIL_SACRIFICE.get());
        }
        if(ModList.get().isLoaded("botania")){
            grid.clearContent();grid.setItem(1,new ItemStack(ArcaneArteries.MANA_SLATE.get()));grid.setItem(7,new ItemStack(ArcaneArteries.MANA_SLATE.get()));
            grid.setItem(3,new ItemStack(Items.STONE));grid.setItem(5,new ItemStack(Items.STONE));grid.setItem(4,new ItemStack(Items.GOLD_INGOT));
            craft(t,grid,"mana_rune",ArcaneArteries.MANA_RUNE.get().asItem());
            for(String id:List.of("mana_slate","mana_slate_reinforced","botanical_knife"+(ArteriesConfig.HARD_CRAFTING.get()?"_hard":""),"botanical_sacrifice"+(ArteriesConfig.HARD_CRAFTING.get()?"_hard":"")))
                t.assertTrue(t.getLevel().getRecipeManager().byKey(ArcaneArteries.id(id)).isPresent(),"Real host serializer accepted "+id);
        }
        t.succeed();
    }
    private static void craft(GameTestHelper t,net.minecraft.world.inventory.CraftingContainer grid,String id,Item result){
        var recipe=(net.minecraft.world.item.crafting.CraftingRecipe)t.getLevel().getRecipeManager().byKey(ArcaneArteries.id(id)).orElseThrow();
        t.assertTrue(recipe.matches(grid,t.getLevel())&&recipe.assemble(grid,t.getLevel().registryAccess()).is(result),"Real crafting match/assembly: "+id);
    }
    @GameTest(template="empty") public static void realBotaniaMachines(GameTestHelper t){
        if(ModList.get().isLoaded("botania"))BotaniaChecks.run(t);
        t.succeed();
    }
    private static final class BotaniaChecks {
        static ItemStack item(String name){return new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(name))));}
        static void run(GameTestHelper t){
            var poolPos=new BlockPos(1,2,1);t.setBlock(poolPos,ForgeRegistries.BLOCKS.getValue(new ResourceLocation("botania:mana_pool")));
            var pool=(vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity)t.getBlockEntity(poolPos);
            vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity.serverTick(t.getLevel(),t.absolutePos(poolPos),pool.getBlockState(),pool);
            pool.receiveMana(9000);
            var stone=t.spawnItem(Items.STONE,poolPos.getX()+.5f,poolPos.getY()+.5f,poolPos.getZ()+.5f);
            t.assertTrue(pool.collideEntityItem(stone)&&pool.getCurrentMana()==6000,"Actual mana pool consumes 3000 for mana slate");
            assertDrop(t,poolPos,ArcaneArteries.MANA_SLATE.get());
            boolean hard=ArteriesConfig.HARD_CRAFTING.get();
            var p=player(t);
            try{
                runic(t,p,hard?List.of("botania:gaia_ingot","botania:gaia_ingot","botania:pixie_dust","botania:terrasteel_ingot","bloodmagic:sacrificialdagger")
                        :List.of("botania:elementium_ingot","botania:elementium_ingot","bloodmagic:sacrificialdagger"),hard?50000:2000,ArcaneArteries.BOTANICAL_KNIFE.get());
                if(hard)runic(t,p,List.of("botania:gaia_ingot","botania:dreamwood_twig","botania:pixie_dust","arcanearteries:botanical_knife"),55000,ArcaneArteries.BOTANICAL_SACRIFICE.get());
                else{
                    t.setBlock(poolPos.below(),ForgeRegistries.BLOCKS.getValue(new ResourceLocation("botania:alchemy_catalyst")));
                    var knife=t.spawnItem(ArcaneArteries.BOTANICAL_KNIFE.get(),poolPos.getX()+.5f,poolPos.getY()+.5f,poolPos.getZ()+.5f);
                    t.assertTrue(pool.collideEntityItem(knife)&&pool.getCurrentMana()==0,"Actual alchemy pool consumes 6000 for sacrifice knife");
                    assertDrop(t,poolPos,ArcaneArteries.BOTANICAL_SACRIFICE.get());
                }
            }finally{p.discard();}
        }
        static void runic(GameTestHelper t,ServerPlayer p,List<String> inputs,int mana,Item output){
            t.setBlock(ALTAR,net.minecraft.world.level.block.Blocks.AIR);
            t.setBlock(ALTAR,ForgeRegistries.BLOCKS.getValue(new ResourceLocation("botania:runic_altar")));
            var altar=(vazkii.botania.common.block.block_entity.RunicAltarBlockEntity)t.getBlockEntity(ALTAR);
            for(String input:inputs)t.assertTrue(altar.addItem(p,item(input),InteractionHand.MAIN_HAND),"Actual runic altar accepts ingredient "+input);
            vazkii.botania.common.block.block_entity.RunicAltarBlockEntity.serverTick(t.getLevel(),t.absolutePos(ALTAR),altar.getBlockState(),altar);
            t.assertTrue(altar.getTargetMana()==mana,"Actual runic altar selected the expected mana cost");
            altar.receiveMana(mana);
            t.spawnItem(item("botania:livingrock").getItem(),ALTAR.getX()+.5f,ALTAR.getY()+.5f,ALTAR.getZ()+.5f);
            altar.onUsedByWand(p,item("botania:twig_wand"),net.minecraft.core.Direction.UP);
            t.assertTrue(altar.getCurrentMana()==0&&altar.isEmpty(),"Wand completes recipe and consumes exact mana and ingredients");
            assertDrop(t,ALTAR,output);
        }
        static void assertDrop(GameTestHelper t,BlockPos pos,Item output){
            t.assertTrue(!t.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(t.absolutePos(pos)).inflate(2),e->e.isAlive()&&e.getItem().is(output)).isEmpty(),"Actual machine spawned "+output);
        }
    }
}
