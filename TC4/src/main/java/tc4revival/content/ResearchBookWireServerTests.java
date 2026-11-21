package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.network.*;
import tc4revival.research.*;
import tc4revival.research.sync.KnowledgeTransfer;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ResearchBookWireServerTests {
    @GameTest(template="wand_test_empty")
    public static void bookOpeningSendsCompleteKnowledgeBeforeScreen(GameTestHelper test) {
        var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BookWire"));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.THAUMONOMICON.get()));
        var messages=new ArrayList<Object>();boolean[] rejectOpen={false};
        var receiver=new KnowledgeTransfer.Receiver(player.getUUID());
        var connection=new Connection(PacketFlow.SERVERBOUND) {
            @Override public void send(Packet<?> packet) {
                if(packet instanceof ClientboundCustomPayloadPacket payload && payload.getIdentifier().toString().equals("thaumcraft:main")) {
                    var buffer=new FriendlyByteBuf(payload.getData().copy());
                    try {
                        int discriminator=buffer.readUnsignedByte();
                        if(discriminator==0) {var message=KnowledgePacket.decode(buffer);messages.add(message);receiver.accept(message.frame());}
                        else if(discriminator==8) {
                            if(rejectOpen[0])throw new IllegalStateException("Fixture rejected opening send");
                            var message=BookOpenPacket.decode(buffer);messages.add(message);
                            test.assertTrue(receiver.snapshot().isPresent(),"Screen packet follows committed knowledge");
                            test.assertTrue(KnowledgeNbt.write(receiver.snapshot().orElseThrow().toKnowledge(TCAspects.catalog())).equals(KnowledgeNbt.write(KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElseThrow())),"All knowledge pages arrive before opening");
                        }
                    } finally {buffer.release();}
                }
            }
        };
        new ServerGamePacketListenerImpl(player.server,connection,player);
        var data=KnowledgeSavedData.get(player.server);
        data.update(player.getUUID(),bound->0,k->{for(int i=0;i<700;i++)k.rememberScan(PlayerKnowledge.ScanKind.PHENOMENON,"#book-wire-"+i);return null;});
        try {
            var opening=ResearchBookOpening.open(player,InteractionHand.MAIN_HAND,s->s.is(TCItems.THAUMONOMICON.get()),s->false);
            test.assertTrue(messages.size()>6 && messages.get(messages.size()-1) instanceof BookOpenPacket p && p.token().equals(opening.token()),"Paged actual channel messages end with correct opening token");
            messages.clear();
            var second=ResearchBookOpening.open(player,InteractionHand.MAIN_HAND,s->s.is(TCItems.THAUMONOMICON.get()),s->false);
            test.assertTrue(messages.size()==1 && messages.get(0) instanceof BookOpenPacket p && p.token().equals(second.token()),"Unchanged knowledge suppressed on reopening");
            messages.clear();data.update(player.getUUID(),bound->0,k->{k.rememberResearch("@wire-new-clue");return null;});
            ResearchBookOpening.open(player,InteractionHand.MAIN_HAND,s->s.is(TCItems.THAUMONOMICON.get()),s->false);
            test.assertTrue(messages.size()==2 && messages.get(0) instanceof KnowledgePacket k && !k.frame().reset() && messages.get(1) instanceof BookOpenPacket,"Changed knowledge delta precedes new opening");
            rejectOpen[0]=true;boolean failed=false;
            try {ResearchBookOpening.open(player,InteractionHand.MAIN_HAND,s->s.is(TCItems.THAUMONOMICON.get()),s->false);}
            catch(IllegalStateException expected){failed=expected.getMessage().equals("Fixture rejected opening send");}
            var denied=ResearchBookSessions.LIVE.request(player,UUID.randomUUID(),0,"THAUMIUM");
            test.assertTrue(failed && denied instanceof ServerResearchBook.Denied d && d.reason()==ServerResearchBookSession.Rejection.CLOSED,"Failed send removes opening rather than leaving an orphan session");
        } finally {
            ResearchBookSessions.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
            KnowledgeSync.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
        }
        tc4revival.TC4Revival.LOGGER.info("PASS bookOpeningSendsCompleteKnowledgeBeforeScreen: registered channel capture, 700 records, atomic receiver before screen, no-op and delta");test.succeed();
    }
}
