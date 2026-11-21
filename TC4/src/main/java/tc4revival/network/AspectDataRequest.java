package tc4revival.network;

import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import tc4revival.aspect.sync.*;

public record AspectDataRequest(int revision,List<String> files) {
    public AspectDataRequest{files=List.copyOf(files);if(files.size()>AspectDataSet.MAX_FILES)throw new IllegalArgumentException("Too many aspect files");files.forEach(AspectDataSet::validateKey);}
    public void encode(FriendlyByteBuf buffer){buffer.writeVarInt(revision);buffer.writeVarInt(files.size());files.forEach(key->buffer.writeUtf(key,512));}
    public static AspectDataRequest decode(FriendlyByteBuf buffer){
        int revision=buffer.readVarInt(),count=buffer.readVarInt();if(count<0||count>AspectDataSet.MAX_FILES)throw new IllegalArgumentException("Too many aspect files");
        var files=new ArrayList<String>();for(int i=0;i<count;i++)files.add(buffer.readUtf(512));return new AspectDataRequest(revision,files);
    }
    public void handle(ServerPlayer player){if(player!=null)ServerAspectData.reply(player,this);}
}
