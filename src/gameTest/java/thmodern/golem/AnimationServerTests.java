package thmodern.golem;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class AnimationServerTests {
    @GameTest(template="empty")
    public static void failedPaymentPreservesBlockAndRemovesSpawnedEntity(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"AnimationRollback"));player.setGameMode(GameType.SURVIVAL);player.setPos(pos.getCenter());
        test.assertTrue(!AnimatedGolems.animate(player,pos,30,false,false,()->false),"Failed payment rejects animation");
        test.assertTrue(level.getBlockState(pos).is(Blocks.STONE),"Original block remains");
        test.assertTrue(level.getEntitiesOfClass(GolemEntity.class,new AABB(pos).inflate(2),g->!g.isRemoved()).isEmpty(),"No unpaid construct remains");test.succeed();
    }
    @GameTest(template="empty")
    public static void temporaryConstructKeepsStateAndRestoresOneBlock(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var source=Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,net.minecraft.core.Direction.Axis.X);
        level.setBlock(pos,source,3);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"AnimationRestore"));player.setGameMode(GameType.SURVIVAL);player.setPos(pos.getCenter());
        test.assertTrue(AnimatedGolems.animate(player,pos,20,false,false,()->true),"Eligible source animates");
        var golems=level.getEntitiesOfClass(GolemEntity.class,new AABB(pos).inflate(2));test.assertTrue(golems.size()==1,"Exactly one construct");
        var golem=golems.get(0);
        test.assertTrue(level.isEmptyBlock(pos)&&source.equals(golem.configuration().blockAppearance()),"Full block state captured without source duplicate");
        var saved=new net.minecraft.nbt.CompoundTag();golem.addAdditionalSaveData(saved);golem.readAdditionalSaveData(saved);
        test.assertTrue(golem.configuration().addonData(AnimatedGolems.DATA).getInt("Remaining")==20,"Lifetime survives save");
        AnimatedGolems.restore(golem,false);
        AnimatedGolems.restore(golem,false);
        test.assertTrue(golem.isRemoved()&&level.getBlockState(pos).equals(source),"Restored one exact source state");
        long copies=BlockPos.betweenClosedStream(pos.offset(-1,-1,-1),pos.offset(1,1,1)).filter(p->level.getBlockState(p).equals(source)).count();
        test.assertTrue(copies==1,"Repeated callback cannot restore another copy");test.succeed();
    }
    @GameTest(template="empty")
    public static void coreMakesConstructPermanentWithoutLosingItsAppearance(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlock(pos,Blocks.IRON_BLOCK.defaultBlockState(),3);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"AnimationCore"));player.setGameMode(GameType.SURVIVAL);player.setPos(pos.getCenter());
        test.assertTrue(AnimatedGolems.animate(player,pos,1,false,false,()->true),"Metal source animates");
        var golem=level.getEntitiesOfClass(GolemEntity.class,new AABB(pos).inflate(2)).get(0);
        test.assertTrue(golem.configuration().installCore(GolemCoreType.GATHER),"Installs ordinary base core");AnimatedGolems.tick(golem);
        test.assertTrue(golem.configuration().customTask()==null&&golem.configuration().addonData(AnimatedGolems.DATA).getBoolean("Permanent"),"Hands AI to ordinary core");
        test.assertTrue(golem.configuration().blockAppearance().is(Blocks.IRON_BLOCK)&&golem.getMaxHealth()==35,"Keeps custom appearance and metal statistics");
        for(int i=0;i<4;i++)AnimatedGolems.tick(golem);
        test.assertTrue(!golem.isRemoved(),"Timer no longer expires cored construct");
        golem.configuration().removeCore();AnimatedGolems.tick(golem);
        test.assertTrue(golem.isRemoved()&&level.getBlockState(pos).is(Blocks.IRON_BLOCK),"Removing core restores inanimate matter");test.succeed();
    }
    @GameTest(template="empty")
    public static void portableConstructPreservesCoreAndCapturedState(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlock(pos,Blocks.OAK_LOG.defaultBlockState(),3);
        var first=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FirstOwner"));first.setGameMode(GameType.SURVIVAL);first.setPos(pos.getCenter());
        test.assertTrue(AnimatedGolems.animate(first,pos,200,true,true,()->true),"Animates source");
        var golem=level.getEntitiesOfClass(GolemEntity.class,new AABB(pos).inflate(2)).get(0);
        golem.configuration().installCore(GolemCoreType.GATHER);AnimatedGolems.tick(golem);
        golem.setCustomName(net.minecraft.network.chat.Component.literal("Oak helper"));
        var portable=AnimatedGolemRecovery.portable(golem);
        test.assertTrue(portable.is(Horizons.GOLEM_PLACER.get()),"Uses addon portable item");
        var restored=TCEntities.GOLEM.get().create(level);
        GolemPlacerItem.restore(restored,portable,GolemMaterial.WOOD,pos,net.minecraft.core.Direction.UP,"SecondOwner");
        test.assertTrue(restored.configuration().core()==GolemCoreType.GATHER,"Core preserved");
        test.assertTrue(restored.configuration().blockAppearance().is(Blocks.OAK_LOG),"Material preserved");
        test.assertTrue(restored.configuration().addonData(AnimatedGolems.DATA).getBoolean("Explosive")&&restored.configuration().addonData(AnimatedGolems.DATA).getBoolean("Berserk"),"Focus flags preserved");
        test.assertTrue(restored.getName().getString().equals("Oak helper"),"Name preserved");golem.discard();test.succeed();
    }
    @GameTest(template="empty")
    public static void voidInstabilityPausesWithCoreAndOnlyTransformsOnce(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"VoidOwner"));player.setPos(pos.getCenter());
        var golem=AnimatedGolems.summonVoid(player,pos.getCenter());test.assertTrue(golem!=null,"Voidling spawns");
        for(int i=0;i<3;i++)AnimatedGolems.advanceVoid(golem);
        test.assertTrue(!golem.isRemoved()&&golem.configuration().addonData(AnimatedGolems.DATA).getInt("VoidCount")==3,"Three warnings precede transformation");
        golem.configuration().installCore(GolemCoreType.GATHER);AnimatedGolems.advanceVoid(golem);
        test.assertTrue(!golem.isRemoved(),"Installed core suppresses instability");golem.configuration().removeCore();
        AnimatedGolems.advanceVoid(golem);AnimatedGolems.advanceVoid(golem);
        test.assertTrue(golem.isRemoved(),"Fourth event removes uncontrolled voidling");
        test.assertTrue(level.getEntitiesOfClass(EldritchGuardianEntity.class,new AABB(pos).inflate(3)).size()==1,"Exactly one guardian appears");test.succeed();
    }
    @GameTest(template="empty")
    public static void advancedInfusionPreservesPortableConstructAndUnlocksExtraSlot(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlockAndUpdate(pos,Blocks.OAK_LOG.defaultBlockState());
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"AdvancedConstruct"));player.setGameMode(GameType.SURVIVAL);player.setPos(pos.getCenter());
        test.assertTrue(AnimatedGolems.animate(player,pos,200,true,false,()->true),"Source animates");
        var golem=level.getEntitiesOfClass(GolemEntity.class,new AABB(pos).inflate(2)).get(0);golem.configuration().installCore(GolemCoreType.GATHER);AnimatedGolems.tick(golem);golem.setCustomName(net.minecraft.network.chat.Component.literal("Preserved helper"));
        var portable=AnimatedGolemRecovery.portable(golem);golem.discard();
        var before=portable.getTag().copy();int slots=GolemProfiles.require("wood").material().upgrades();
        var recipe=(InfusionRecipe)level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumichorizons:infusion/advanced_golem")).orElseThrow();
        var components=List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GUNPOWDER),new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:essentia_jar"))),new net.minecraft.world.item.ItemStack(TCItems.ZOMBIE_BRAIN.get()));
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(portable,components,key->false),level),"Research is required");
        var input=new InfusionRecipe.Input(portable,components,key->key.equals("focusAnimation"));test.assertTrue(recipe.matches(input,level),"Real advanced infusion matches the captured construct");
        var result=recipe.assemble(input,level.registryAccess());test.assertTrue(result.getTag().getBoolean("advanced")&&!portable.getTag().contains("advanced"),"Output is upgraded without mutating input");
        var preserved=result.getTag().copy();preserved.remove("advanced");test.assertTrue(preserved.equals(before),"All original portable NBT preserved");
        var restored=TCEntities.GOLEM.get().create(level);GolemPlacerItem.restore(restored,result,GolemMaterial.WOOD,pos,net.minecraft.core.Direction.UP,"AdvancedConstruct");
        test.assertTrue(restored.configuration().advanced()&&restored.configuration().upgrades().length==slots+1,"Advanced construct receives extra upgrade slot");
        test.assertTrue(restored.configuration().core()==GolemCoreType.GATHER&&restored.configuration().blockAppearance().is(Blocks.OAK_LOG)&&restored.getName().getString().equals("Preserved helper"),"Restoration retains core appearance and name");test.succeed();
    }
    @GameTest(template="empty")
    public static void animationFocusInfusionLoadsAndRequiresAllTenComponents(GameTestHelper test){
        var level=test.getLevel();var recipe=(InfusionRecipe)level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumichorizons:infusion/focus_animation")).orElseThrow();
        var components=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();components.add(new net.minecraft.world.item.ItemStack(TCItems.GOLEM_CORE_BLANK.get()));components.add(new net.minecraft.world.item.ItemStack(Horizons.HUMAN_BLOOD.get()));
        for(int i:new int[]{0,2,4})components.add(new net.minecraft.world.item.ItemStack(TCItems.golemUpgrade(i)));
        components.add(new net.minecraft.world.item.ItemStack(TCItems.GOLEM_CORE_BLANK.get()));components.add(new net.minecraft.world.item.ItemStack(Horizons.HUMAN_BLOOD.get()));for(int i:new int[]{1,3,5})components.add(new net.minecraft.world.item.ItemStack(TCItems.golemUpgrade(i)));
        var center=new net.minecraft.world.item.ItemStack(TCItems.PRIMORDIAL_PEARL.get());var input=new InfusionRecipe.Input(center,components,key->true);
        test.assertTrue(recipe.matches(input,level)&&recipe.assemble(input,level.registryAccess()).is(Horizons.ANIMATION_FOCUS.get()),"Full original ingredients produce the focus");components.remove(0);test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,components,key->true),level),"Missing duplicated core is rejected");test.succeed();
    }
    @GameTest(template="empty")
    public static void finiteLightningProtectsOnlyAnimatedConstructs(GameTestHelper test){
        var level=test.getLevel();var golem=TCEntities.GOLEM.get().create(level);var finite=thmodern.plane.CloudBlocks.LIGHTNING.get().create(level);
        test.assertTrue(!net.minecraftforge.event.ForgeEventFactory.onEntityStruckByLightning(golem,finite),"Ordinary golems retain normal lightning behavior");
        var data=new net.minecraft.nbt.CompoundTag();data.putInt("Remaining",1200);thaumcraft.api.golems.GolemApi.data(golem,AnimatedGolems.DATA,data);
        test.assertTrue(net.minecraftforge.event.ForgeEventFactory.onEntityStruckByLightning(golem,finite),"Animated cloud construct ignores its finite lightning");
        test.assertTrue(!net.minecraftforge.event.ForgeEventFactory.onEntityStruckByLightning(golem,net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level)),"Natural lightning is not suppressed");test.succeed();
    }
}
