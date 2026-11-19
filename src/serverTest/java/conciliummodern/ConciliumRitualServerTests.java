package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumRitualServerTests {
    private static ServerPlayer player(GameTestHelper t,int x,int y,int z){
        var p=new ServerPlayer(t.getLevel().getServer(),t.getLevel(),new GameProfile(UUID.randomUUID(),"ConciliumRitual"));
        new ServerGamePacketListenerImpl(p.server,new Connection(PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}},p);
        p.setGameMode(GameType.SURVIVAL);p.moveTo(t.absoluteVec(new Vec3(x+.5,y,z+.5)));t.getLevel().addNewPlayer(p);return p;
    }
    private static void aim(ServerPlayer p,Vec3 target){var d=target.subtract(p.getEyePosition());p.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));p.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));p.yRotO=p.getYRot();p.xRotO=p.getXRot();}
    private static DestabilizedCrystalBlockEntity crystal(GameTestHelper t,BlockPos pos,String aspect,int amount){t.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);t.getLevel().setBlock(pos,ConciliumBlocks.CRYSTAL.get().defaultBlockState(),2);var tile=(DestabilizedCrystalBlockEntity)t.getLevel().getBlockEntity(pos);var n=new CompoundTag();n.putString("aspect",aspect);n.putInt("capacity",amount);tile.load(n);return tile;}
    private static PredictabilityBlockEntity hex(GameTestHelper t){var center=t.absolutePos(new BlockPos(16,3,16));for(var at:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1))){t.getLevel().setBlock(at,ConciliumBlocks.HEX.get().defaultBlockState(),2);((PredictabilityBlockEntity)t.getLevel().getBlockEntity(at)).configure(center);}return (PredictabilityBlockEntity)t.getLevel().getBlockEntity(center);}
    private static RiftEntity rift(GameTestHelper t,PredictabilityBlockEntity tile){var r=ConciliumEntities.RIFT.get().create(t.getLevel());r.initialize(55,25);r.bind(tile.center());r.setPos(Vec3.atCenterOf(tile.center()).add(0,2,0));t.getLevel().addFreshEntity(r);return r;}
    private static void tick(GameTestHelper t,PredictabilityBlockEntity tile){PredictabilityBlockEntity.tick(t.getLevel(),tile.getBlockPos(),tile.getBlockState(),tile);}
    @GameTest(template="empty") public static void nodeJarCannotOverlapPeelerAndCreatesOnlyOneRift(GameTestHelper t){
        var tile=hex(t);var level=t.getLevel();var jar=tile.center().above();level.setBlock(jar,TCArcane.NODE_JAR.get().defaultBlockState(),2);
        var p=player(t,16,4,14);p.setItemInHand(InteractionHand.MAIN_HAND,TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true));
        var peeler=ConciliumEntities.PEELER.get().create(level);peeler.bind(tile.center());peeler.setPos(Vec3.atCenterOf(tile.center()).add(0,2,0));level.addFreshEntity(peeler);
        var area=new AABB(tile.center()).inflate(2,4,2);var hit=new BlockHitResult(Vec3.atCenterOf(jar),Direction.UP,jar,false);
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,jar,hit));
        t.assertTrue(level.getBlockEntity(jar) instanceof NodeJarBlockEntity&&level.getEntitiesOfClass(RiftEntity.class,area).isEmpty(),"Existing peeler preserves jar and prevents overlapping ritual");
        peeler.discard();MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,jar,hit));
        var rifts=level.getEntitiesOfClass(RiftEntity.class,area);t.assertTrue(level.isEmptyBlock(jar)&&rifts.size()==1&&tile.center().equals(rifts.get(0).boundCenter()),"Cleared ritual consumes jar and creates one correctly bound rift");
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,jar,hit));
        t.assertTrue(level.getEntitiesOfClass(RiftEntity.class,area).size()==1,"Repeated interaction cannot duplicate a consumed node jar");rifts.forEach(Entity::discard);p.discard();t.succeed();
    }
    @GameTest(template="empty") public static void crystalReceiverRequiresCompleteBoundStructureAndPreservesCapacity(GameTestHelper t){
        var tile=hex(t);var crystal=crystal(t,t.absolutePos(new BlockPos(10,4,16)),"sensus",128);
        t.assertTrue(!tile.receiveCrystal(crystal)&&crystal.amount()==128,"No bound rift cannot consume crystal");var rift=rift(t,tile);
        t.assertTrue(tile.receiveCrystal(crystal)&&crystal.amount()==0&&tile.heat()==1200,"Complete bound structure accepts all crystal contents atomically");
        t.assertTrue(tile.revealedAspects().amount(TCAspects.catalog().require("sensus"))==128,"Received essentia stays in actual master reservoir");
        var restored=new PredictabilityBlockEntity(tile.getBlockPos(),tile.getBlockState());restored.load(tile.saveWithoutMetadata());t.assertTrue(restored.center().equals(tile.center())&&restored.heat()==1200&&restored.revealedAspects().amount(TCAspects.catalog().require("sensus"))==128,"Master position heat and essentia survive serialized reload");
        rift.collapseTo(0);crystal=crystal(t,crystal.getBlockPos(),"sensus",4);t.assertTrue(!tile.receiveCrystal(crystal)&&crystal.amount()==4,"Closing rift rejects more charge");rift.discard();t.succeed();
    }
    @GameTest(template="empty") public static void hexBreakRestoresBedrockAndRemainingOriginalStones(GameTestHelper t){
        var tile=hex(t);var center=tile.center();var removed=center.east();t.assertTrue(PredictabilityStructure.complete(t.getLevel(),center),"Nine linked members form complete structure");t.getLevel().removeBlock(removed,false);
        t.assertTrue(t.getLevel().getBlockState(center).is(Blocks.BEDROCK),"Breaking ring restores immovable bedrock center");
        for(var pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))if(!pos.equals(center)&&!pos.equals(removed))t.assertTrue(t.getLevel().getBlockState(pos).is(TCEldritch.STONE.get()),"Other ring members restore original eldritch stone");
        t.assertTrue(t.getLevel().isEmptyBlock(removed),"Broken member remains broken");t.succeed();
    }
    @GameTest(template="empty") public static void realRiftRecipeRequiresThrowerResearchThenConsumesOneCatalyst(GameTestHelper t){
        var tile=hex(t);var rift=rift(t,tile);var p=player(t,10,3,10);var state=tile.saveWithoutMetadata();var amounts=new CompoundTag();amounts.putInt("sensus",128);amounts.putInt("vacuos",128);amounts.putInt("cognitio",64);state.put("essentia",amounts);state.putBoolean("hasRift",true);state.putInt("heat",0);tile.load(state);
        var catalyst=new ItemEntity(t.getLevel(),rift.getX(),rift.getY(),rift.getZ(),new ItemStack(TCItems.THAUMOMETER.get(),2));catalyst.setNoGravity(true);var n=new CompoundTag();catalyst.saveWithoutId(n);n.putUUID("Thrower",p.getUUID());catalyst.load(n);t.getLevel().addFreshEntity(catalyst);
        tick(t,tile);t.assertTrue(catalyst.getItem().getCount()==2&&!tile.revealedAspects().isEmpty(),"Unknown research preserves catalyst and essentia");
        thaumcraft.api.ThaumcraftApiHelper.completeResearch(p,"thaumicconcilium:ASTRALMONITOR");tick(t,tile);
        var outputs=t.getLevel().getEntitiesOfClass(ItemEntity.class,rift.getBoundingBox().inflate(4),e->e.getItem().is(ConciliumItems.ASTRAL_MONITOR.get()));
        t.assertTrue(outputs.size()==1&&catalyst.getItem().getCount()==1&&tile.revealedAspects().isEmpty(),"Actual loaded recipe yields monitor, consumes one catalyst, clears reservoir");tick(t,tile);
        t.assertTrue(t.getLevel().getEntitiesOfClass(ItemEntity.class,rift.getBoundingBox().inflate(4),e->e.getItem().is(ConciliumItems.ASTRAL_MONITOR.get())).size()==1,"Empty reservoir cannot duplicate craft");outputs.forEach(Entity::discard);catalyst.discard();rift.discard();p.discard();t.succeed();
    }
    @GameTest(template="empty") public static void overheatedHexCollapsesRiftAndClearsReservoir(GameTestHelper t){
        var tile=hex(t);var rift=rift(t,tile);var n=tile.saveWithoutMetadata();n.putBoolean("hasRift",true);n.putInt("heat",2400);var amounts=new CompoundTag();amounts.putInt("aer",20);n.put("essentia",amounts);tile.load(n);tick(t,tile);
        t.assertTrue(rift.isClosing()&&tile.heat()==0&&tile.revealedAspects().isEmpty(),"Overload performs real collapse and clears charged reservoir");rift.discard();t.succeed();
    }
    @GameTest(template="empty") public static void polishmentRequiresActualDrainAndCommitsOnlyOnce(GameTestHelper t){
        var p=player(t,5,2,5);var focus=ConciliumItems.VIS_CONDUCTOR.get();var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,new ItemStack(focus));p.setItemInHand(InteractionHand.MAIN_HAND,wand);
        var crystal=crystal(t,t.absolutePos(new BlockPos(5,3,8)),"sensus",20);var recipe=new PolishmentRecipe(ResourceLocation.parse("thaumicconcilium:test_polish"),Ingredient.of(Items.DIAMOND),"sensus",3);var matrix=t.absolutePos(new BlockPos(6,3,5));
        aim(p,Vec3.atCenterOf(crystal.getBlockPos()));
        var hit=p.pick(8,0,false);t.assertTrue(hit instanceof BlockHitResult block&&block.getBlockPos().equals(crystal.getBlockPos()),"Actual partial-tick ray reaches the ritual crystal: "+hit.getLocation());
        focus.cast(wand,t.getLevel(),p,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,p,0);
        t.assertTrue(!focus.completePolishment(p,matrix,recipe)&&crystal.amount()==20,"Lock without drain cannot bypass ritual requirement");for(int i=0;i<3;i++)focus.usingTick(wand,p,3);
        t.assertTrue(crystal.amount()==17&&p.isUsingItem()&&p.getUseItem()==wand,"Three real drain ticks consume exactly three and retain the same active wand: "+crystal.amount());
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> guard=e->{if(e.getPos().equals(crystal.getBlockPos()))e.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,net.minecraftforge.event.level.BlockEvent.BreakEvent.class,guard);
        try{t.assertTrue(!focus.completePolishment(p,matrix,recipe)&&t.getLevel().getBlockEntity(crystal.getBlockPos())==crystal,"Denied completion preserves original crystal");}finally{MinecraftForge.EVENT_BUS.unregister(guard);}
        t.assertTrue(focus.completePolishment(p,matrix,recipe)&&t.getLevel().isEmptyBlock(crystal.getBlockPos())&&!p.isUsingItem(),"Accepted completion consumes crystal and closes casting session");t.assertTrue(!focus.completePolishment(p,matrix,recipe),"Consumed casting session cannot commit twice");p.discard();t.succeed();
    }
    @GameTest(template="empty") public static void condenserBeamStopsAtWallAndCooldownPreventsRepeatedCharge(GameTestHelper t){
        var pos=t.absolutePos(new BlockPos(16,3,16));t.getLevel().setBlock(pos,ConciliumBlocks.CONDENSER.get().defaultBlockState(),2);var condenser=(VisCondenserBlockEntity)t.getLevel().getBlockEntity(pos);var c=crystal(t,pos.above(),"ignis",40);
        // crystal helper places its support; restore the condenser and reacquire its actual tile.
        t.getLevel().setBlock(pos,ConciliumBlocks.CONDENSER.get().defaultBlockState(),2);condenser=(VisCondenserBlockEntity)t.getLevel().getBlockEntity(pos);
        var pig=EntityType.PIG.create(t.getLevel());pig.setNoAi(true);pig.setPos(Vec3.atCenterOf(pos.west(3)).add(0,1,0));t.getLevel().addFreshEntity(pig);t.getLevel().setBlock(pos.west(2).above(),Blocks.STONE.defaultBlockState(),2);
        t.getLevel().setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),2);condenser.activate();
        t.assertTrue(c.amount()==24&&condenser.cooldown()==40&&pig.getHealth()==pig.getMaxHealth(),"Beam pays sixteen, sets cooldown, cannot hit behind solid wall");condenser.activate();t.assertTrue(c.amount()==24,"Cooldown rejects repeated redstone activation");
        var restored=new VisCondenserBlockEntity(pos,condenser.getBlockState());restored.load(condenser.saveWithoutMetadata());t.assertTrue(restored.cooldown()==40,"Cooldown survives save and reload");pig.discard();t.succeed();
    }
}
