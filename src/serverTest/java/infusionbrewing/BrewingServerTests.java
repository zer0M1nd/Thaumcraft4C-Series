package infusionbrewing;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("infusionbrewing") @PrefixGameTestTemplate(false)
public class BrewingServerTests {
    private static InfusionRecipe recipe(GameTestHelper t,String name){return (InfusionRecipe)t.getLevel().getRecipeManager().byKey(InfusionBrewing.id(name)).orElseThrow();}
    private static InfusionRecipe.Input input(InfusionRecipe r,ItemStack center,boolean known){return new InfusionRecipe.Input(center,r.plan(center).components().stream().map(i->i.displayStacks().get(0)).toList(),key->known);}
    @GameTest(template="empty") public static void registryAndReloadedRecipes(GameTestHelper t){
        t.assertTrue(InfusionBrewing.ITEMS.getEntries().size()==38,"38 registered item types");
        t.assertTrue(PotionFluids.ALL.size()==23,"23 fluid families");
        long count=t.getLevel().getRecipeManager().getRecipes().stream().filter(r->r.getId().getNamespace().equals("infusionbrewing")).count();
        t.assertTrue(count==41,"All 41 data recipes loaded, got "+count);
        for(var f:PotionFluids.ALL){t.assertTrue(f.source.get().getBucket()==f.bucket.get(),"Bucket fluid "+f.id);t.assertTrue(PotionData.read(PotionData.base(f.id).potion(false)).orElseThrow().legacyId()==f.id,"NBT effect "+f.id);}
        t.succeed();
    }
    @GameTest(template="empty") public static void allDurationAndAmplifierBoundaries(GameTestHelper t){
        var duration=recipe(t,"duration");var amplifier=recipe(t,"amplifier");
        for(int id=1;id<=23;id++){
            var base=PotionData.base(id).potion(false);
            if(!BrewingRules.instant(id)){
                for(int expected:new int[]{4800,9600,19200}){
                    t.assertTrue(duration.matches(input(duration,base,true),t.getLevel()),"Duration matches "+id);
                    var next=duration.assemble(input(duration,base,true),t.getLevel().registryAccess());
                    t.assertTrue(PotionData.read(next).orElseThrow().duration()==expected,"Exact doubled duration "+id);base=next;
                }
            }
            t.assertTrue(!duration.acceptsCenter(base),"Maximum/instant duration rejected "+id);
            duration.plan(base); // Browsers may request a plan even for a capped input.
            base=PotionData.base(id).potion(false);
            if(!BrewingRules.noAmplifier(id))for(int strength=1;strength<=3;strength++){
                t.assertTrue(amplifier.matches(input(amplifier,base,true),t.getLevel()),"Amplifier matches "+id);
                base=amplifier.assemble(input(amplifier,base,true),t.getLevel().registryAccess());
                t.assertTrue(PotionData.read(base).orElseThrow().amplifier()==strength,"Exact amplifier "+id);
            }
            t.assertTrue(!amplifier.acceptsCenter(base),"Cap/non-amplifiable rejected "+id);
            amplifier.plan(base);
        }t.succeed();
    }
    @GameTest(template="empty") public static void conversionsPreserveEffectsAndRequireResearch(GameTestHelper t){
        var splash=recipe(t,"splash");var focus=recipe(t,"potion_focus");
        for(int id=1;id<=23;id++){
            int amp=BrewingRules.noAmplifier(id)?0:3;
            var center=new PotionData(id,BrewingRules.instant(id)?1:19200,amp).potion(false);
            center.setHoverName(net.minecraft.network.chat.Component.literal("kept name"));
            t.assertTrue(!splash.matches(input(splash,center,false),t.getLevel()),"Research gate");
            var thrown=splash.assemble(input(splash,center,true),t.getLevel().registryAccess());
            t.assertTrue(thrown.is(Items.SPLASH_POTION)&&PotionData.read(thrown).equals(PotionData.read(center))&&thrown.hasCustomHoverName(),"Splash preserves full effect and name");
            t.assertTrue(!splash.acceptsCenter(thrown),"Already splash rejected");
            t.assertTrue(focus.matches(input(focus,thrown,true),t.getLevel()),"Focus matches all 23 effects");
            var made=focus.assemble(input(focus,thrown,true),t.getLevel().registryAccess());
            t.assertTrue(made.is(InfusionBrewing.FOCUS.get())&&PotionData.read(made).equals(PotionData.read(center)),"Focus preserves effect");
            for(var kind:List.of("amulet","ring","belt")){
                var r=recipe(t,"potion_"+kind);
                t.assertTrue(r.acceptsCenter(center)==!BrewingRules.instant(id),"Instant excluded from baubles");
                if(!BrewingRules.instant(id))t.assertTrue(PotionData.read(r.assemble(input(r,center,true),t.getLevel().registryAccess())).equals(PotionData.read(center)),"Bauble effect retained");
            }
        }
        t.assertTrue(!splash.acceptsCenter(new ItemStack(Items.POTION)),"Empty potion rejected");
        t.assertTrue(!focus.acceptsCenter(PotionData.base(1).potion(true)),"Short focus input rejected");t.succeed();
    }
    @GameTest(template="empty") public static void spawnerIngredientsCheckStrengthWithoutDuration(GameTestHelper t){
        for(int tier=1;tier<=4;tier++)for(String kind:List.of("power","speed")){
            var r=recipe(t,"spawner_"+kind+"_"+tier);var center=r.centerSamples().get(0);var valid=input(r,center,true);
            t.assertTrue(r.matches(valid,t.getLevel()),"Exact upgrade ingredients match "+kind+tier);
            var potion=PotionData.read(valid.getItem(1)).orElseThrow();new PotionData(potion.legacyId(),19200,potion.amplifier()).write(valid.getItem(1));
            t.assertTrue(r.matches(valid,t.getLevel()),"Long duration allowed");
            new PotionData(potion.legacyId(),19200,(potion.amplifier()+1)%4).write(valid.getItem(1));
            t.assertTrue(!r.matches(valid,t.getLevel()),"Wrong strength rejected");
        }t.succeed();
    }
    @GameTest(template="empty") public static void actualSpawnerUpgradeAndPersistence(GameTestHelper t){
        var pos=t.absolutePos(new BlockPos(2,2,2));t.getLevel().setBlockAndUpdate(pos,Blocks.SPAWNER.defaultBlockState());
        var spawner=(SpawnerBlockEntity)t.getLevel().getBlockEntity(pos);spawner.setEntityId(EntityType.ZOMBIE,t.getLevel().random);var initial=spawner.saveWithoutMetadata();
        var player=player(t);
        try{
            for(int code=0;code<8;code++){
                var stack=new ItemStack(InfusionBrewing.UPGRADES.get(code).get());player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
                t.assertTrue(stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Real item upgrade "+code);
                t.assertTrue(stack.isEmpty(),"One upgrade consumed");
            }
            var saved=spawner.saveWithoutMetadata();t.assertTrue(saved.getShort("SpawnCount")==12&&saved.getShort("MaxNearbyEntities")==24&&saved.getShort("MinSpawnDelay")==20&&saved.getShort("MaxSpawnDelay")==40,"All original tier IV values");
            t.assertTrue(saved.get("SpawnData").equals(initial.get("SpawnData")),"Spawner entity preserved");
            var restored=new SpawnerBlockEntity(pos,Blocks.SPAWNER.defaultBlockState());restored.load(saved);t.assertTrue(restored.saveWithoutMetadata().getShort("SpawnCount")==12,"NBT load retains upgrade");
            t.assertTrue(!SpawnerUpgradeItem.upgrade(saved,3),"Tier IV cannot be applied twice");
        }finally{player.discard();}t.succeed();
    }
    @GameTest(template="empty") public static void fluidReactionsUseOriginalProducts(GameTestHelper t){
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(4,2,4));
        level.setBlockAndUpdate(pos.above(3),Blocks.STONE.defaultBlockState());
        for(int id:new int[]{1,2})for(var neighbor:List.of(Blocks.WATER,Blocks.LAVA,Blocks.ICE)){
            if(id==1&&neighbor==Blocks.LAVA)continue;
            var block=(PotionLiquidBlock)PotionFluids.family(id).block.get();level.setBlock(pos,block.defaultBlockState(),2);level.setBlock(pos.north(),neighbor.defaultBlockState(),2);
            block.react(level,pos);
            var expected=neighbor==Blocks.ICE?InfusionBrewing.SEMI_SOLID.get():neighbor==Blocks.LAVA?Blocks.OBSIDIAN:id==1?Blocks.MYCELIUM:Blocks.CLAY;
            t.assertTrue(level.getBlockState(pos).is(expected),"Reaction "+id+" + "+neighbor+" -> "+expected);
            level.setBlock(pos.north(),Blocks.AIR.defaultBlockState(),2);
        }t.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100) public static void twoSourcesRegenerate(GameTestHelper t){
        var level=t.getLevel();var p=t.absolutePos(new BlockPos(4,2,4));var fluid=PotionFluids.family(2);
        for(var x:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,-1,2)))level.setBlockAndUpdate(x,Blocks.STONE.defaultBlockState());
        for(var x:List.of(p.west(2),p.east(2),p.north(),p.south(),p.west().north(),p.west().south(),p.east().north(),p.east().south()))level.setBlockAndUpdate(x,Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(p.west(),fluid.block.get().defaultBlockState());level.setBlockAndUpdate(p.east(),fluid.block.get().defaultBlockState());
        t.succeedWhen(()->t.assertTrue(level.getFluidState(p).isSource()&&PotionFluids.id(level.getFluidState(p))==2,"Two matching sources create center source"));
    }
    @GameTest(template="empty") public static void ultrawarmRemovesSourceEvenWithExplosionsDisabled(GameTestHelper t){
        // GameTest deliberately does not initialize normal skylight. Daylight is tested in the real client.
        var level=t.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);var p=new BlockPos(0,120,0);var explosions=BrewingConfig.EXPLOSIONS.get();
        try{
            BrewingConfig.EXPLOSIONS.set(false);
            var block=(PotionLiquidBlock)PotionFluids.family(1).block.get();level.setBlockAndUpdate(p,block.defaultBlockState());block.react(level,p);
            t.assertTrue(level.getFluidState(p).isEmpty(),"Ultrawarm dimension actually consumes positive source");
            t.succeed();
        }finally{BrewingConfig.EXPLOSIONS.set(explosions);}
    }
    @GameTest(template="empty") public static void dispenserBottleAndBucketRoundTrip(GameTestHelper t){
        var level=t.getLevel();var p=t.absolutePos(new BlockPos(2,2,2));level.setBlockAndUpdate(p,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));
        var source=new BlockSourceImpl(level,p);var target=p.east();level.setBlock(target,PotionFluids.family(2).block.get().defaultBlockState(),2);
        var bottle=new ItemStack(InfusionBrewing.MAGIC_BOTTLE.get(),4);var block=(DispenserBlock)Blocks.DISPENSER;
        var filled=block.getDispenseMethod(bottle).dispense(source,bottle);
        t.assertTrue(filled.getCount()==4&&PotionData.read(filled).orElseThrow().legacyId()==2&&level.getFluidState(target).isEmpty(),"Legacy whole-stack bottle fills and consumes source: "+filled+" / "+level.getBlockState(target));
        var bucket=new ItemStack(PotionFluids.family(2).bucket.get());var empty=block.getDispenseMethod(bucket).dispense(source,bucket);
        t.assertTrue(empty.is(Items.BUCKET)&&PotionFluids.id(level.getFluidState(target))==2,"Bucket places source");
        var again=block.getDispenseMethod(empty).dispense(source,empty);
        t.assertTrue(again.is(PotionFluids.family(2).bucket.get())&&level.getFluidState(target).isEmpty(),"Ordinary bucket retrieves source by default");
        var magicOnly=BrewingConfig.MAGIC_BUCKET.get();
        try{
            BrewingConfig.MAGIC_BUCKET.set(true);
            var magic=block.getDispenseMethod(again).dispense(source,again);
            t.assertTrue(magic.is(InfusionBrewing.MAGIC_BUCKET.get()),"Configured placement returns a magic bucket");
            var ordinary=block.getDispenseMethod(new ItemStack(Items.BUCKET)).dispense(source,new ItemStack(Items.BUCKET));
            t.assertTrue(ordinary.isEmpty()&&PotionFluids.id(level.getFluidState(target))==2,"Magic-only option ejects ordinary bucket without collecting fluid");
            var recollected=block.getDispenseMethod(magic).dispense(source,magic);
            t.assertTrue(recollected.is(PotionFluids.family(2).bucket.get())&&level.getFluidState(target).isEmpty(),"Magic bucket still collects in magic-only mode");
            var invalid=new ItemStack(InfusionBrewing.MAGIC_BUCKET.get());
            t.assertTrue(block.getDispenseMethod(invalid).dispense(source,invalid).isEmpty(),"Magic bucket falls back to ejection without a fluid source");
        }finally{BrewingConfig.MAGIC_BUCKET.set(magicOnly);}t.succeed();
    }
    @GameTest(template="empty") public static void curiosRequireVisAndRefreshAtThreshold(GameTestHelper t){
        var player=player(t);
        try{
            var data=new PotionData(1,19200,3);var stack=data.write(new ItemStack(InfusionBrewing.RING.get()));var item=(PotionCurioItem)stack.getItem();
            var context=new top.theillusivec4.curios.api.SlotContext("ring",player,0,false,true);
            item.curioTick(context,stack);t.assertTrue(!player.hasEffect(data.effect()),"No vis gives no effect");
            var wand=((CastingWandItem)TCItems.CASTING_WAND.get()).create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,false);var tag=wand.getOrCreateTag();tag.putInt("aqua",5000);tag.putInt("ordo",5000);player.getInventory().setItem(8,wand);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(data.effect(),5000,0));
            item.curioTick(context,stack);var current=player.getEffect(data.effect());t.assertTrue(current!=null&&current.getAmplifier()==3&&current.getDuration()==600,"First paid ring effect");
            t.assertTrue(!current.save(new CompoundTag()).contains("HiddenEffect")&&current.isAmbient()&&current.isVisible(),"Legacy replacement discards weaker effect and preserves ambient particles");
            var paid=wand.getTag().copy();item.curioTick(context,stack);t.assertTrue(wand.getTag().equals(paid),"No repeat payment while effect is sufficient");
            t.assertTrue(paid.getInt("aqua")<5000&&paid.getInt("ordo")<5000,"Both primal costs debited");
            player.removeEffect(data.effect());player.addEffect(new net.minecraft.world.effect.MobEffectInstance(data.effect(),299,3));
            item.curioTick(context,stack);t.assertTrue(player.getEffect(data.effect()).getDuration()==600&&wand.getTag().getInt("aqua")<paid.getInt("aqua")&&wand.getTag().getInt("ordo")<paid.getInt("ordo"),"Refresh below 300 ticks pays again");
        }finally{player.discard();}t.succeed();
    }
    @GameTest(template="empty") public static void focusConsumesVisAndSpawnsPotion(GameTestHelper t){
        var player=player(t);
        try{
            var focus=new PotionData(19,19200,3).write(new ItemStack(InfusionBrewing.FOCUS.get()));
            var wand=((CastingWandItem)TCItems.CASTING_WAND.get()).create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,false);WandFocusItem.attach(wand,focus);
            var item=(PotionFocusItem)focus.getItem();int before=t.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownPotion.class,new AABB(player.blockPosition()).inflate(4)).size();
            item.cast(wand,t.getLevel(),player,InteractionHand.MAIN_HAND,null);
            t.assertTrue(t.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownPotion.class,new AABB(player.blockPosition()).inflate(4)).size()==before,"Unfunded focus cannot cast");
            wand.getOrCreateTag().putInt("ignis",5000);wand.getOrCreateTag().putInt("perditio",5000);item.cast(wand,t.getLevel(),player,InteractionHand.MAIN_HAND,null);
            var projectiles=t.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownPotion.class,new AABB(player.blockPosition()).inflate(4),p->p.getOwner()==player);
            t.assertTrue(projectiles.size()==1,"Exactly one real projectile");var data=PotionData.read(projectiles.get(0).getItem()).orElseThrow();t.assertTrue(data.legacyId()==19&&data.amplifier()==3&&data.duration()==2400,"Projectile uses original base duration and infused strength");
            projectiles.forEach(Entity::discard);
        }finally{player.discard();}t.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper t){
        var p=new net.minecraft.server.level.ServerPlayer(t.getLevel().getServer(),t.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"IBTest"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(t.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var pos=t.absolutePos(new BlockPos(4,3,4));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);t.getLevel().addNewPlayer(p);return p;
    }
}


