package ttmodern;

import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class SummoningSync {
    public record Snapshot(List<MobEssenceCatalog.Recipe> recipes){public Snapshot{recipes=List.copyOf(recipes);if(recipes.size()>4096)throw new IllegalArgumentException("Too many summoning recipes");}}
    public static void encode(Snapshot packet,FriendlyByteBuf buffer){buffer.writeVarInt(packet.recipes().size());for(var recipe:packet.recipes()){buffer.writeResourceLocation(recipe.entity());for(var aspect:recipe.aspects())buffer.writeUtf(aspect,64);}}
    public static Snapshot decode(FriendlyByteBuf buffer){int count=buffer.readVarInt();if(count<0||count>4096)throw new IllegalArgumentException("Invalid summoning recipe count");var recipes=new ArrayList<MobEssenceCatalog.Recipe>();for(int i=0;i<count;i++){var entity=buffer.readResourceLocation();var aspects=new ArrayList<String>();for(int j=0;j<3;j++){var aspect=buffer.readUtf(64);if(!MobEssenceItem.ASPECTS.contains(aspect))throw new IllegalArgumentException("Unknown soul aspect");aspects.add(aspect);}recipes.add(new MobEssenceCatalog.Recipe(entity,aspects));}return new Snapshot(recipes);}
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event){var packet=new Snapshot(MobEssenceCatalog.recipes());var crops=new CropDropPreview.Snapshot(CropDropPreview.collect(event.getPlayerList().getServer()));if(event.getPlayer()!=null){SoulHearts.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer),packet);SoulHearts.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer),crops);}else for(var player:event.getPlayerList().getPlayers()){SoulHearts.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),packet);SoulHearts.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),crops);}}
}
