package gadomancymodern.test;
import gadomancymodern.Gadomancy;
import gadomancymodern.knowledge.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.research.*;
@GameTestHolder("gadomancy") @PrefixGameTestTemplate(false)
public class KnowledgeBookServerTests {
    private static KnowledgeBookEntity place(GameTestHelper test,BlockPos pos){test.getLevel().setBlockAndUpdate(pos.below(),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState());test.getLevel().setBlockAndUpdate(pos,Gadomancy.KNOWLEDGE_BOOK.get().defaultBlockState());return (KnowledgeBookEntity)test.getLevel().getBlockEntity(pos);}
    private static ItemStack note(){var research=ResearchCatalogs.catalog().require("GOGGLES");var note=new ResearchNote(research.key(),0xabcdef,false,2,java.util.Map.of(new ResearchHex(0,0),new ResearchNote.Cell(1,"aer")));var stack=ResearchNotesItem.create(note);stack.getOrCreateTag().putString("AddonData","retained");return stack;}
    @GameTest(templateNamespace="gadomancy",template="test_empty",timeoutTicks=90)
    public static void consumesRealCognitionAndCompletesOwnedEntity(GameTestHelper test){
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var book=place(test,pos);world.setBlockAndUpdate(pos.east(2),TCArcane.ESSENTIA_JAR.get().defaultBlockState());var jar=(EssentiaJarBlockEntity)world.getBlockEntity(pos.east(2));jar.addToContainer(tc4revival.aspect.TCAspects.catalog().require("cognitio"),4);world.setBlockAndUpdate(pos.west(2),Blocks.BOOKSHELF.defaultBlockState());
        var dropped=new ItemEntity(world,pos.getX()+.5,pos.getY()+.7,pos.getZ()+.5,note());world.addFreshEntity(dropped);book.attract();var held=world.getEntitiesOfClass(HeldResearchEntity.class,new AABB(pos).inflate(1)).get(0);
        test.assertTrue(!dropped.isAlive()&&held.getItem().getCount()==1&&book.stored.getCount()==1,"Exactly one physical entity owns the captured note");
        test.runAtTickTime(34,()->{test.assertTrue(book.researching&&book.cognition>0&&jar.amount()==3,"Real remote Cognitio spent at original 32-tick cadence");book.checkSurroundings();test.assertTrue(book.surroundingKnowledge==3,"Knowledge book contributes two and bookshelf one");test.assertTrue(Gadomancy.KNOWLEDGE_BOOK.get().getAnalogOutputSignal(book.getBlockState(),world,pos)==15,"Active funded research comparator 15");var saved=book.saveWithoutMetadata();var copy=new KnowledgeBookEntity(pos,book.getBlockState());copy.load(saved);test.assertTrue(copy.work.equals(book.work)&&copy.cognition==book.cognition,"Work and prepaid cognition survive save");book.work.clear();book.work.put("cognitio",1);book.progress();held.tick();held.tick();test.assertTrue(held.getItem().is(TCItems.DISCOVERY.get()),"Finished note changes to actual modern discovery item");var completed=ResearchNoteNbt.read(held.getItem().getTag(),tc4revival.aspect.TCAspects.catalog()).note().orElseThrow();test.assertTrue(completed.complete()&&completed.copies()==2&&held.getItem().getTag().getString("AddonData").equals("retained"),"Completion retains board/copies/addon NBT");test.succeed();});
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty",timeoutTicks=90)
    public static void missingMasterReleasesNoteAndPickupCancelsResearch(GameTestHelper test){
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var book=place(test,pos);var held=new HeldResearchEntity(world,pos,note());world.addFreshEntity(held);book.heartbeat(held);book.tick();test.assertTrue(book.researching,"Captured note starts original work list");held.discard();for(int i=0;i<9;i++)book.tick();test.assertTrue(!book.researching&&book.work.isEmpty(),"Removed or picked-up entity cancels unfinished work");
        var savedHeld=new HeldResearchEntity(world,pos,note());var nbt=savedHeld.saveWithoutId(new CompoundTag());var restored=new HeldResearchEntity(Gadomancy.HELD_RESEARCH.get(),world);restored.load(nbt);world.addFreshEntity(restored);world.removeBlock(pos,false);
        test.runAfterDelay(10,()->{test.assertTrue(!restored.isAlive(),"No orphan permanent entity after master disappears");var items=world.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),e->e.isAlive()&&e.getItem().is(TCItems.RESEARCH_NOTES.get()));test.assertTrue(items.size()==1&&items.get(0).getItem().getCount()==1,"Master removal releases exactly one original note");test.assertTrue(world.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("gadomancy","knowledge_book")).isPresent(),"Survival arcane recipe is loaded");test.succeed();});
    }
}
