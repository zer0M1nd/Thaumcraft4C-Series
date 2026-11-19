package tc4revival.network;

import java.util.ArrayList;
import net.minecraft.network.FriendlyByteBuf;
import tc4revival.research.sync.KnowledgeSnapshot.Field;
import tc4revival.research.sync.KnowledgeSnapshot.Key;
import tc4revival.research.sync.KnowledgeTransfer;
import tc4revival.research.sync.KnowledgeTransfer.Edit;
import tc4revival.research.sync.KnowledgeTransfer.Frame;

public record KnowledgePacket(Frame frame) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(frame.owner());
        buffer.writeVarLong(frame.revision());
        buffer.writeVarInt(frame.index());
        buffer.writeBoolean(frame.reset());
        buffer.writeBoolean(frame.last());
        buffer.writeVarInt(frame.edits().size());
        for (var edit : frame.edits()) {
            buffer.writeEnum(edit.key().field());
            buffer.writeUtf(edit.key().name());
            buffer.writeBoolean(edit.value() != null);
            if (edit.value() != null) buffer.writeInt(edit.value());
        }
    }
    public static KnowledgePacket decode(FriendlyByteBuf buffer) {
        var owner = buffer.readUUID();
        long revision = buffer.readVarLong();
        int index = buffer.readVarInt();
        boolean reset = buffer.readBoolean(), last = buffer.readBoolean();
        int count = buffer.readVarInt();
        if (count < 0 || count > KnowledgeTransfer.MAX_EDITS) throw new IllegalArgumentException("Invalid knowledge edit count");
        var edits = new ArrayList<Edit>();
        for (int i = 0; i < count; i++) {
            var key = new Key(buffer.readEnum(Field.class), buffer.readUtf());
            edits.add(new Edit(key, buffer.readBoolean() ? buffer.readInt() : null));
        }
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing knowledge bytes");
        return new KnowledgePacket(new Frame(owner, revision, index, reset, last, edits));
    }
}
