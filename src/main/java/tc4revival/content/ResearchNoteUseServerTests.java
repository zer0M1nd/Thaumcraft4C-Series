package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ResearchNoteUseServerTests {
    @GameTest(template="wand_test_empty")
    public static void unknownNotesRevealEveryEligibleHiddenResearchAndRefundFragments(GameTestHelper test) {
        var catalog=ResearchCatalogs.catalog(); var rules=ResearchCatalogs.rules();
        var candidates=catalog.all().stream().filter(e->ResearchCatalogs.enabled(e) && e.has(ResearchDefinition.Flag.HIDDEN) && !e.aspects().isEmpty() && e.triggers().present()).toList();
        test.assertTrue(!candidates.isEmpty(),"Loaded catalog has hidden research");
        var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
        for(var entry:candidates) {
            var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"UnknownNote"));
            player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            var data=KnowledgeSavedData.get(player.server);
            data.update(player.getUUID(),bound->0,k->{for(var other:catalog.all())if(!other.key().equals(entry.key()))k.rememberResearch(other.key());return null;});
            var knowledge=data.snapshot(player.getUUID()).orElseThrow();
            test.assertTrue(rules.hiddenCandidates(knowledge).equals(List.of(entry.key())),"Only undiscovered eligible research remains");
            var discovered=knowledge.copy();discovered.rememberResearch("@"+entry.key());
            test.assertTrue(rules.hiddenCandidates(discovered).isEmpty(),"An already-known clue is excluded");
            var unknown=new ItemStack(TCItems.UNKNOWN_RESEARCH_NOTES.get(),3);unknown.getOrCreateTag().putString("audit_custom","kept");
            player.setItemInHand(InteractionHand.OFF_HAND,unknown);long revision=data.revision(player.getUUID());
            var result=unknown.getItem().use(test.getLevel(),player,InteractionHand.OFF_HAND);
            var revealed=player.getOffhandItem();var note=ResearchNoteNbt.read(revealed.getTag(),TCAspects.catalog()).note().orElseThrow();
            test.assertTrue(result.getResult().consumesAction() && revealed.is(TCItems.RESEARCH_NOTES.get()) && revealed.getCount()==3 && note.key().equals(entry.key()) && !note.complete() && !note.grid().isEmpty(),"Unknown entire stack becomes actual unsolved hidden note: "+entry.key());
            test.assertTrue(revealed.getTag().getString("audit_custom").equals("kept") && data.revision(player.getUUID())==revision,"Revealing preserves NBT and does not complete research or charge points");
        }
        var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"UnknownRefund"));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var data=KnowledgeSavedData.get(player.server);data.update(player.getUUID(),bound->0,k->{for(var entry:catalog.all())k.rememberResearch(entry.key());return null;});
        long revision=data.revision(player.getUUID());
        test.assertTrue(rules.hiddenCandidates(data.snapshot(player.getUUID()).orElseThrow()).isEmpty(),"Refund fixture has exhausted hidden research");
        var box=new net.minecraft.world.phys.AABB(pos).inflate(3);
        for(int attempt=0;attempt<9;attempt++) {
            var before=new java.util.HashSet<>(test.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).stream().map(net.minecraft.world.entity.Entity::getUUID).toList());
            var unknown=new ItemStack(TCItems.UNKNOWN_RESEARCH_NOTES.get(),3);player.setItemInHand(InteractionHand.MAIN_HAND,unknown);
            unknown.getItem().use(test.getLevel(),player,InteractionHand.MAIN_HAND);
            var spawned=test.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box,e->!before.contains(e.getUUID()));
            test.assertTrue(unknown.getCount()==2 && spawned.size()==1,"Failed hidden search consumes one note and creates one world drop");
            var drop=spawned.get(0);test.assertTrue(drop.getItem().is(TCItems.KNOWLEDGE_FRAGMENT.get()) && drop.getItem().getCount()>=7 && drop.getItem().getCount()<=9,"Legacy fallback yields 7 through 9 fragments");drop.discard();
        }
        test.assertTrue(data.revision(player.getUUID())==revision,"Fallback never changes knowledge");
        tc4revival.TC4Revival.LOGGER.info("PASS unknownNotesRevealEveryEligibleHiddenResearchAndRefundFragments: {} hidden definitions, offhand stacked reveal, nine actual fragment drops",candidates.size());test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void discoveryUseGatesCompletionConsumesOneAndAwardsWarpOnce(GameTestHelper test) {
        var catalog=ResearchCatalogs.catalog();
        var forbidden=catalog.all().stream().filter(e->e.warp()>0 && ResearchCatalogs.enabled(e)).findFirst().orElseThrow();
        boolean oldDisable=TCConfig.SERVER.disableWarp.get();
        try {
            for(boolean disabled:new boolean[]{false,true}) for(var definition:List.of(catalog.require("THAUMIUM"),forbidden)) {
                TCConfig.SERVER.disableWarp.set(disabled);
                var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"NoteUse"));
                var data=KnowledgeSavedData.get(player.server);
                data.update(player.getUUID(),bound->0,k->{
                    for(var entry:catalog.all())if(!entry.key().equals(definition.key()))k.rememberResearch(entry.key());
                    for(var kind:PlayerKnowledge.WarpKind.values())k.setWarp(kind,0);
                    return null;
                });
                var incomplete=ResearchNotesItem.create(new ResearchNote(definition.key(),123,false,0,Map.of()));
                player.setItemInHand(InteractionHand.MAIN_HAND,incomplete);long revision=data.revision(player.getUUID());
                incomplete.getItem().use(test.getLevel(),player,InteractionHand.MAIN_HAND);
                test.assertTrue(incomplete.getCount()==1 && data.revision(player.getUUID())==revision,"Incomplete note cannot unlock research");
                var stack=ResearchNotesItem.create(new ResearchNote(definition.key(),123,true,2,Map.of()));stack.setCount(3);
                player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var result=stack.getItem().use(test.getLevel(),player,InteractionHand.MAIN_HAND);
                var knowledge=data.snapshot(player.getUUID()).orElseThrow();
                int warp=disabled?0:definition.warp();
                test.assertTrue(result.getResult().consumesAction() && stack.getCount()==2 && knowledge.researchMarkers().contains(definition.key()),"Completed discovery consumes only one from copied stack");
                test.assertTrue(knowledge.warp(PlayerKnowledge.WarpKind.PERMANENT)==warp-warp/2 && knowledge.warp(PlayerKnowledge.WarpKind.STICKY)==warp/2,"Actual item use applies configured permanent/sticky warp split");
                revision=data.revision(player.getUUID());stack.getItem().use(test.getLevel(),player,InteractionHand.MAIN_HAND);
                test.assertTrue(stack.getCount()==2 && data.revision(player.getUUID())==revision,"Repeated discovery cannot consume or award warp again");
                var malformed=stack.copy();malformed.getOrCreateTag().putString("hexgrid","invalid");player.setItemInHand(InteractionHand.MAIN_HAND,malformed);
                test.assertTrue(malformed.getItem().use(test.getLevel(),player,InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.FAIL && malformed.getCount()==2 && data.revision(player.getUUID())==revision,"Malformed grid rejects without mutation");
                var restored=KnowledgeSavedData.load(data.save(new net.minecraft.nbt.CompoundTag()),TCAspects.catalog()).snapshot(player.getUUID()).orElseThrow();
                test.assertTrue(restored.researchMarkers().contains(definition.key()) && restored.totalWarp()==warp,"Item completion and warp survive knowledge serialization");
            }
            var blocked=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"NoteBlocked"));
            var data=KnowledgeSavedData.get(blocked.server);data.ensure(blocked.getUUID(),bound->0);
            test.assertTrue(!ResearchCatalogs.rules().prerequisites(data.snapshot(blocked.getUUID()).orElseThrow(),"INFUSION"),"Fixture has locked prerequisites");
            var note=ResearchNotesItem.create(new ResearchNote("INFUSION",123,true,0,Map.of()));blocked.setItemInHand(InteractionHand.MAIN_HAND,note);
            long revision=data.revision(blocked.getUUID());note.getItem().use(test.getLevel(),blocked,InteractionHand.MAIN_HAND);
            test.assertTrue(note.getCount()==1 && data.revision(blocked.getUUID())==revision,"Missing parents reject discovery before consumption");
            tc4revival.TC4Revival.LOGGER.info("PASS discoveryUseGatesCompletionConsumesOneAndAwardsWarpOnce: normal/forbidden, warp toggle, copied stacks, malformed and parent gates");test.succeed();
        } finally {TCConfig.SERVER.disableWarp.set(oldDisable);}
    }
}
