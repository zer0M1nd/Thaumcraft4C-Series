package conciliummodern;

import com.emoniph.witchery.brewing.WitcheryBrewRegistry;
import com.emoniph.witchery.brewing.action.BrewActionList;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.infusion.Infusion;
import com.emoniph.witchery.infusion.symbols.EffectRegistry;
import com.emoniph.witchery.init.*;
import com.emoniph.witchery.item.*;
import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tc4revival.research.KnowledgeSavedData;

/** Instantiated by the optional generator only when the real supported host is installed. */
public final class DoomCatWitcheryServerTests {
    static List<TestFunction> tests(){return List.of(
        test("witchery_native_brew_recipes",DoomCatWitcheryServerTests::brews),
        test("witchery_native_player_state",DoomCatWitcheryServerTests::states),
        test("witchery_native_symbols_cast_knowledge_cost",DoomCatWitcheryServerTests::symbols),
        test("witchery_real_imp_success_and_failure",DoomCatWitcheryServerTests::imp));}
    private static TestFunction test(String name,Consumer<GameTestHelper> action){return new TestFunction("concilium_optional/"+name,"thaumicconcilium."+name,"thaumicconcilium:empty",100,0,true,action);}
    private static ServerPlayer player(GameTestHelper t,String name){var p=FakePlayerFactory.get(t.getLevel(),new GameProfile(UUID.randomUUID(),name));p.setGameMode(GameType.SURVIVAL);p.moveTo(t.absolutePos(new BlockPos(16,1,8)).getCenter());return p;}
    private static <T extends LivingEntity>T target(GameTestHelper t,EntityType<T> type){var entity=Objects.requireNonNull(type.create(t.getLevel()));entity.moveTo(t.absolutePos(new BlockPos(16,1,16)).getCenter());if(entity instanceof Mob mob)mob.setNoAi(true);t.assertTrue(t.getLevel().addFreshEntity(entity),"Real test target inserted");return entity;}
    private static AABB area(GameTestHelper t){return new AABB(t.absolutePos(new BlockPos(1,0,1)),t.absolutePos(new BlockPos(31,15,31)));}
    private static void floor(GameTestHelper t){for(int x=1;x<31;x++)for(int z=1;z<31;z++)t.setBlock(new BlockPos(x,0,z),Blocks.STONE);}

    static void brews(GameTestHelper t){
        var counts=Map.of("attack",17,"attack_werewolf",16,"attack_vampire",22,"ill_fitting",22,"moonshine",2);
        for(var entry:counts.entrySet()){
            var stack=WitcheryBridge.brew(entry.getKey());t.assertTrue(stack.is(WitcheryItems.BREW.get()),"Native Witchery brew item: "+entry.getKey());
            var list=new BrewActionList(stack.getTag(),WitcheryBrewRegistry.INSTANCE.getIngredients());
            t.assertTrue(list.items.size()==entry.getValue()&&list.actions.size()==entry.getValue(),"Every original ingredient resolves to an actual host brew action: "+entry.getKey()+"="+list.actions.size());
            if(entry.getKey().equals("ill_fitting")){t.assertTrue(list.items.get(20).is(Items.GUNPOWDER)&&list.items.get(21).is(Items.GUNPOWDER),"Both final gunpowder actions retained");t.assertTrue(WitcheryBrewRegistry.INSTANCE.isSplash(stack.getTag()),"Real splash dispersal recognized");}
            if(entry.getKey().equals("moonshine"))t.assertTrue(stack.getTag().getInt("Color")==0xfec20c&&!stack.getTag().getString("BrewName").isBlank()&&stack.getTag().contains("BrewInfo")&&stack.getTag().getInt("EffectCount")>0,"Real moonshine name, information, color and effects derived");
        }
        var host=WitcheryBridge.host().orElseThrow();t.assertTrue(host.stockade().is(WitcheryBlocks.STOCKADE_OAK.get())&&host.witchHunter()==WitcheryEntities.WITCH_HUNTER.get(),"Burning-site supplies actual host block/entity");t.succeed();
    }
    static void states(GameTestHelper t){
        var player=player(t,"WitcheryState");var host=WitcheryBridge.host().orElseThrow();
        t.assertTrue(!host.vampire(player)&&host.werewolfLevel(player)==0,"Fresh host species state");
        ExtendedPlayer.setVampireLevel(player,3);t.assertTrue(host.vampire(player),"Real vampire level recognized");
        ExtendedPlayer.setWerewolfLevel(player,4);t.assertTrue(host.werewolfLevel(player)==4,"Real werewolf level recognized");
        t.assertTrue(host.gasMask()==WitcheryEffects.GAS_MASK.get()&&host.colorfulEffect()==WitcheryEffects.COLORFUL.get(),"Native effect identities");
        var restored=player(t,"WitcheryCopy");restored.getPersistentData().merge(player.getPersistentData().copy());t.assertTrue(host.vampire(restored)&&host.werewolfLevel(restored)==4,"Host persisted species data survives a copy");t.succeed();
    }
    private static void cast(ServerPlayer player,int id){player.getPersistentData().putInt("WITCSpellEffectID",id);MysticBranchItem.cast(player.level(),player);}
    static void symbols(GameTestHelper t){
        floor(t);var player=player(t,"WitcherySpell");
        for(int id=90;id<=94;id++){var spell=EffectRegistry.getEffect(id);t.assertTrue(spell!=null&&EffectRegistry.getEffect(spell.getDefaultStrokes())==spell,"Native ID and stroke lookup for spell "+id);}
        Infusion.setEnergy(player,2,20,20);cast(player,91);t.assertTrue(Infusion.getCurrentEnergy(player)==20&&t.getLevel().getEntitiesOfClass(Arrow.class,area(t)).isEmpty(),"Host rejects unknown knowledge without charging or creating arrows");
        KnowledgeSavedData.get(t.getLevel().getServer()).update(player.getUUID(),player.getRandom()::nextInt,k->{k.rememberResearch("thaumicconcilium:CRIMSONSPELLS");return null;});
        t.assertTrue(ConciliumSpellIntegration.repairWitchery(player),"Research completion grants real host knowledge");
        for(int id=90;id<=94;id++)t.assertTrue(EffectRegistry.getEffect(id).hasValidKnowledge(player,Infusion.getNBT(player)),"Host recognizes granted knowledge for "+id);
        cast(player,91);var arrows=t.getLevel().getEntitiesOfClass(Arrow.class,area(t));t.assertTrue(arrows.size()==1&&arrows.get(0).getOwner()==player&&arrows.get(0).isCritArrow()&&arrows.get(0).getBaseDamage()==1&&Infusion.getCurrentEnergy(player)==18,"Actual host casts researched arrow, assigns owner and charges two");arrows.forEach(Entity::discard);
        Infusion.setEnergy(player,4,20,20);cast(player,90);t.assertTrue(t.getLevel().getEntitiesOfClass(EtherealShackles.class,area(t)).stream().anyMatch(e->e.getOwner()==player)&&Infusion.getCurrentEnergy(player)==18,"Infernal incarceration creates real owned shackles through native casting");
        Infusion.setEnergy(player,1,20,20);var spider=target(t,EntityType.SPIDER);player.lookAt(EntityAnchorArgument.Anchor.EYES,spider.getEyePosition());float before=spider.getHealth();cast(player,92);t.assertTrue(spider.getHealth()==before-1&&Infusion.getCurrentEnergy(player)==18,"Light infusion spider spell applies actual one damage at zero lux pool");spider.discard();
        var undead=target(t,EntityType.ZOMBIE);player.lookAt(EntityAnchorArgument.Anchor.EYES,undead.getEyePosition());before=undead.getHealth();cast(player,93);t.assertTrue(undead.getHealth()==before-1&&Infusion.getCurrentEnergy(player)==16,"Native undead spell selects and damages real undead");undead.discard();
        var cow=target(t,EntityType.COW);player.lookAt(EntityAnchorArgument.Anchor.EYES,cow.getEyePosition());Infusion.setEnergy(player,4,20,20);cast(player,94);t.assertTrue(cow.hasEffect(MobEffects.BLINDNESS)&&cow.hasEffect(tc4revival.content.TCEffects.BLURRED_VISION.get())&&cow.getEffect(MobEffects.BLINDNESS).getDuration()==200&&Infusion.getCurrentEnergy(player)==18,"Native infernal conjunctivitis applies both actual effects and host cost");t.succeed();
    }
    public static final class DealObserver {
        final ServerPlayer actor;int calls;LivingEntity target;ItemStack contract=ItemStack.EMPTY;
        DealObserver(ServerPlayer actor){this.actor=actor;}
        @SubscribeEvent public void deal(com.ilya3point999k.thaumicconcilium.api.event.WitcheryImpDealEvent event){if(event.impOwner==actor){calls++;target=event.target;contract=event.stack;}}
    }
    static void imp(GameTestHelper t){
        floor(t);var player=player(t,"ImpContract");var cow=target(t,EntityType.COW);var imp=target(t,WitcheryEntities.IMP.get());imp.tame(player);imp.setAffection(20);var data=new CompoundTag();imp.saveWithoutId(data);data.putLong("LastGiftTime",-72000);imp.load(data);var observer=new DealObserver(player);MinecraftForge.EVENT_BUS.register(observer);
        try{
            var failed=new ItemStack(WitcheryItems.CONTRACT_EVAPORATE.get(),2);Taglocks.setTaglockForEntity(failed,cow,1);player.setItemInHand(InteractionHand.MAIN_HAND,failed);imp.interact(player,InteractionHand.MAIN_HAND);t.assertTrue(observer.calls==0&&ImpPacts.remaining(player)==0&&failed.getCount()==2,"Actual unsupported victim contract fails with no event, protection or consumption");
            var good=new ItemStack(WitcheryItems.CONTRACT_RESIST_FIRE.get(),2);Taglocks.setTaglockForEntity(good,cow,1);player.setItemInHand(InteractionHand.MAIN_HAND,good);imp.interact(player,InteractionHand.MAIN_HAND);t.assertTrue(cow.hasEffect(MobEffects.FIRE_RESISTANCE)&&good.getCount()==1,"Host commits actual contract effect and consumes exactly one");t.assertTrue(observer.calls==1&&observer.target==cow&&observer.contract.is(WitcheryItems.CONTRACT_RESIST_FIRE.get())&&observer.contract.getCount()==2&&ImpPacts.remaining(player)==72000,"Optional mixin posts exactly once after success with original contract snapshot");
            imp.interact(player,InteractionHand.MAIN_HAND);t.assertTrue(observer.calls==1&&good.getCount()==1,"Host cooldown rejection never refreshes contract protection");
        }finally{MinecraftForge.EVENT_BUS.unregister(observer);}t.succeed();
    }
}
