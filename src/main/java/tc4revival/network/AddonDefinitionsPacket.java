package tc4revival.network;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import net.minecraft.network.FriendlyByteBuf;
import tc4revival.data.AddonDefinitions;

/** Bounded compressed research snapshot and a startup-registry compatibility fingerprint. */
public record AddonDefinitionsPacket(String fingerprint, JsonObject research) {
    public static String fingerprintNow() {
        var data=new JsonArray();
        data.add(new Gson().toJsonTree(tc4revival.aspect.TCAspects.catalog().all()));
        for(var kind:java.util.List.of("wand_rods","wand_caps","focus_upgrades"))data.add(AddonDefinitions.load(kind));
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(data.toString().getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(fingerprint,64);
        try {
            var bytes=new ByteArrayOutputStream();
            try(var zip=new GZIPOutputStream(bytes)) { zip.write(research.toString().getBytes(StandardCharsets.UTF_8)); }
            if(bytes.size()>1000000)throw new IllegalArgumentException("Research snapshot exceeds 1 MB compressed limit");
            buffer.writeByteArray(bytes.toByteArray());
        } catch(IOException e) { throw new IllegalStateException(e); }
    }
    public static AddonDefinitionsPacket decode(FriendlyByteBuf buffer) {
        var fingerprint=buffer.readUtf(64);
        try(var zip=new GZIPInputStream(new ByteArrayInputStream(buffer.readByteArray(1000000)))) {
            var bytes=zip.readNBytes(8*1024*1024+1);
            if(bytes.length>8*1024*1024)throw new IllegalArgumentException("Research snapshot too large");
            return new AddonDefinitionsPacket(fingerprint,JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject());
        } catch(IOException e) { throw new IllegalArgumentException("Invalid research snapshot",e); }
    }
    public static void send(net.minecraft.server.level.ServerPlayer player) {
        var snapshot=tc4revival.research.ResearchDefinitions.server();
        if(snapshot!=null)TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),
                new AddonDefinitionsPacket(fingerprintNow(),snapshot.json()));
    }
    public static void sync(net.minecraftforge.event.OnDatapackSyncEvent event) {
        if(event.getPlayer()!=null)send(event.getPlayer());
        else event.getPlayerList().getPlayers().forEach(player->{
            var data=tc4revival.research.KnowledgeSavedData.get(player.server);
            if(data.snapshot(player.getUUID()).filter(k->!tc4revival.research.ResearchCatalogs.progression().missingAutomatic(k).isEmpty()).isPresent())
                data.update(player.getUUID(),player.getRandom()::nextInt,k->tc4revival.research.ResearchCatalogs.progression().unlockAutomatic(k,tc4revival.config.TCConfig.SERVER.disableWarp.get()));
            send(player);
        });
    }
}
