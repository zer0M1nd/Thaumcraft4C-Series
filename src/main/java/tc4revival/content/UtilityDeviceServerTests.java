package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class UtilityDeviceServerTests {
    @GameTest(template="wand_test_empty")
    public static void levitatorStackOcclusionAndPowerFollowLegacyRules(GameTestHelper test) {
        var level=test.getLevel(); var lower=test.absolutePos(new BlockPos(2,2,2)); var upper=lower.above();
        level.setBlock(lower,TCArcane.LEVITATOR.get().defaultBlockState(),3); level.setBlock(upper,TCArcane.LEVITATOR.get().defaultBlockState(),3);
        var entity=(LevitatorBlockEntity)level.getBlockEntity(upper); var bottom=(LevitatorBlockEntity)level.getBlockEntity(lower);
        LevitatorBlockEntity.tick(level,upper,entity.getBlockState(),entity); LevitatorBlockEntity.tick(level,lower,bottom.getBlockState(),bottom);
        test.assertTrue(entity.rangeAbove==20 && bottom.rangeAbove==0,"Only the top lifter opens a twenty-block column above two stacked machines");
        level.setBlock(upper.above(4),Blocks.GLASS.defaultBlockState(),3); entity.requiresUpdate=true; LevitatorBlockEntity.tick(level,upper,entity.getBlockState(),entity);
        test.assertTrue(entity.rangeAbove==20,"Transparent blocks do not shorten the old opaque-block range");
        level.setBlock(upper.above(7),Blocks.STONE.defaultBlockState(),3); entity.requiresUpdate=true; LevitatorBlockEntity.tick(level,upper,entity.getBlockState(),entity);
        test.assertTrue(entity.rangeAbove==6,"Opaque obstacle ends the column immediately below itself");
        level.setBlock(upper.above(7),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(lower.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3); entity.requiresUpdate=true; LevitatorBlockEntity.tick(level,upper,entity.getBlockState(),entity);
        test.assertTrue(entity.rangeAbove==10,"A powered lower lifter contributes no extra height");
        level.setBlock(lower.west(),Blocks.AIR.defaultBlockState(),3); level.setBlock(upper.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        test.assertTrue(entity.powered(),"Side redstone disables the device");
        var state=entity.getBlockState(); test.assertTrue(state.isFaceSturdy(level,upper,Direction.EAST) && !state.isFaceSturdy(level,upper,Direction.UP) && !state.isFaceSturdy(level,upper,Direction.DOWN),"Legacy horizontal support excludes top and bottom");
        level.setBlock(upper,Blocks.AIR.defaultBlockState(),3); test.assertTrue(bottom.requiresUpdate,"Removing a stacked machine invalidates the lower range cache");
        pass(test,"stack-range-and-power");
    }
    @GameTest(template="wand_test_empty",timeoutTicks=60)
    public static void levitatorActuallyMovesItemsAndStopsWhenPowered(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,2,2)); level.setBlock(pos,TCArcane.LEVITATOR.get().defaultBlockState(),3);
        var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+1.2,pos.getZ()+.5,new ItemStack(Items.IRON_INGOT)); item.setDeltaMovement(Vec3.ZERO); item.fallDistance=8; level.addFreshEntity(item);
        double[] poweredHeight={0};
        test.runAfterDelay(5,()->{
            test.assertTrue(item.getY()>pos.getY()+1.2 && item.getDeltaMovement().y>0,"Real item ticks rise in an active levitation column");
            poweredHeight[0]=item.getY();
            level.setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3); item.setDeltaMovement(Vec3.ZERO);
        });
        test.runAfterDelay(8,()->{
            var device=(LevitatorBlockEntity)level.getBlockEntity(pos);
            test.assertTrue(device.powered() && item.getY()<poweredHeight[0]
                    && (item.getDeltaMovement().y<=0 || item.onGround()),
                    "Powered levitator stops lifting; powered="+device.powered()+" height="+item.getY()
                    +" atPower="+poweredHeight[0]+" motion="+item.getDeltaMovement()+" grounded="+item.onGround());
            item.discard(); pass(test,"real-item-motion");
        });
    }
    @GameTest(template="wand_test_empty",timeoutTicks=70)
    public static void arcaneEarRecognizesNotesAndEmitsTenTickPulse(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,2,2)); var source=pos.east(3);
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3); level.setBlock(pos,TCArcane.ARCANE_EAR.get().defaultBlockState(),3);
        level.setBlock(source.below(),Blocks.STONE.defaultBlockState(),3); level.setBlock(source,Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.INSTRUMENT,NoteBlockInstrument.BASEDRUM),3);
        test.runAfterDelay(2,()->{
            var ear=(ArcaneEarBlockEntity)level.getBlockEntity(pos);
            MinecraftForge.EVENT_BUS.post(new NoteBlockEvent.Play(level,source,level.getBlockState(source),1,NoteBlockInstrument.BASEDRUM));
            MinecraftForge.EVENT_BUS.post(new NoteBlockEvent.Play(level,source,level.getBlockState(source),0,NoteBlockInstrument.HARP));
            test.assertTrue(ear.signalTicks()==0,"Both pitch and support instrument must match");
            level.blockEvent(source,Blocks.NOTE_BLOCK,0,0);
        });
        test.runAfterDelay(4,()->{
            var ear=(ArcaneEarBlockEntity)level.getBlockEntity(pos); test.assertTrue(ear.signalTicks()>0,"Actual vanilla note playback reaches registered listeners");
            for (var direction:Direction.values()) test.assertTrue(level.getBlockState(pos).getSignal(level,pos,direction)==15 && level.getBlockState(pos).getDirectSignal(level,pos,direction)==15,"Ear emits fifteen in every direction");
        });
        test.runAfterDelay(14,()->{
            test.assertTrue(!level.getBlockState(pos).getValue(ArcaneEarBlock.POWERED),"Pulse ends after ten game ticks without retriggering");
            var ear=(ArcaneEarBlockEntity)level.getBlockEntity(pos);
            MinecraftForge.EVENT_BUS.post(new NoteBlockEvent.Play(level,pos.east(65),Blocks.NOTE_BLOCK.defaultBlockState(),0,NoteBlockInstrument.BASEDRUM));
            test.assertTrue(ear.signalTicks()==0,"Sixty-five blocks is out of range");
            MinecraftForge.EVENT_BUS.post(new NoteBlockEvent.Play(level,pos.east(64),Blocks.NOTE_BLOCK.defaultBlockState(),0,NoteBlockInstrument.BASEDRUM));
            test.assertTrue(ear.signalTicks()==10,"Sixty-four blocks is included"); pass(test,"note-playback-and-pulse");
        });
    }
    @GameTest(template="wand_test_empty")
    public static void arcaneEarTuningPersistenceAndDeviceAcquisitionAreConnected(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,2,2)); level.setBlock(pos,TCArcane.ARCANE_EAR.get().defaultBlockState(),3);
        var ear=(ArcaneEarBlockEntity)level.getBlockEntity(pos); for (int i=0;i<25;i++) ear.changePitch(); test.assertTrue(ear.note()==0,"Tuning wraps through twenty-five semitones");
        ear.changePitch(); var restored=new ArcaneEarBlockEntity(pos,ear.getBlockState()); restored.load(ear.saveWithoutMetadata()); test.assertTrue(restored.note()==1 && restored.signalTicks()==0,"Only tuning persists, not the active redstone pulse");
        var malformed=new CompoundTag(); malformed.putByte("note",(byte)100); restored.load(malformed); test.assertTrue(restored.note()==24,"Legacy out-of-range pitch clamps during load");
        test.assertTrue(ArcaneEarListening.tone(Blocks.EMERALD_BLOCK.defaultBlockState())==ArcaneEarListening.instrument(NoteBlockInstrument.BIT) && ArcaneEarListening.tone(Blocks.SOUL_SAND.defaultBlockState())==ArcaneEarListening.instrument(NoteBlockInstrument.COW_BELL) && ArcaneEarListening.tone(Blocks.GLOWSTONE.defaultBlockState())==ArcaneEarListening.instrument(NoteBlockInstrument.PLING),"Native special instruments are supported");
        for (String name:new String[]{"levitator","arcane_ear"}) {
            var recipe=level.getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:arcane/"+name));
            test.assertTrue(recipe.isPresent(),"Device has an actual loaded arcane recipe");
        }
        var drops=Block.getDrops(ear.getBlockState(),level,pos,ear);
        test.assertTrue(drops.size()==1 && drops.get(0).is(TCItems.ARCANE_EAR.get()),"Breaking an ear returns its ordinary item");
        pass(test,"tuning-persistence-and-acquisition");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Utility device system test passed: {}",name); test.succeed(); }
}
