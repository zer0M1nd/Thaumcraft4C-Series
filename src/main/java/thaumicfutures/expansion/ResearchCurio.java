package thaumicfutures.expansion;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;
import tc4revival.network.*;

public class ResearchCurio extends Item {
    public static final Map<String,List<String>> TYPES=new LinkedHashMap<>();
    static {
        TYPES.put("arcane",List.of("ignis","praecantatio","potentia"));
        TYPES.put("preserved",List.of("aqua","victus","permutatio"));
        TYPES.put("ancient",List.of("terra","fabrico","machina"));
        TYPES.put("eldritch",List.of("perditio","alienis","tenebrae"));
        TYPES.put("knowledge",List.of("ordo","cognitio","sensus"));
        TYPES.put("twisted",List.of("aer","vitium","vacuos"));
    }
    private final List<String> aspects;
    public ResearchCurio(List<String> aspects){super(new Properties().rarity(Rarity.UNCOMMON));this.aspects=List.copyOf(aspects);}
    public static void reward(ServerPlayer player,List<String> aspects){
        var rewards=KnowledgeSavedData.get(player.getServer()).update(player.getUUID(),player.getRandom()::nextInt,knowledge->{
            var result=new ArrayList<ScanRules.Reward>();
            for(var name:aspects){
                var aspect=TCAspects.catalog().require(name);int before=knowledge.poolAmount(aspect);
                knowledge.changePool(aspect,1+player.getRandom().nextInt(8));
                result.add(new ScanRules.Reward(aspect,knowledge.poolAmount(aspect)-before,knowledge.pool(aspect),false));
            }
            return List.copyOf(result);
        });
        TCNetwork.send(player,ScanFeedbackPacket.from(new ScanRules.Result(ScanRules.Status.SUCCESS,Optional.empty(),rewards)));
        player.level().playSound(null,player,tc4revival.content.TCSounds.LEARN.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return InteractionResultHolder.pass(stack);
        if(player instanceof ServerPlayer server){reward(server,aspects);if(!player.getAbilities().instabuild)stack.shrink(1);}
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
