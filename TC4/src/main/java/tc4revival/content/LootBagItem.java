package tc4revival.content;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public  class LootBagItem extends Item {
    public enum Kind {
        COMMON(0,"common_loot_bag","lootbag",Rarity.COMMON),
        UNCOMMON(1,"uncommon_loot_bag","lootbagunc",Rarity.UNCOMMON),
        RARE(2,"rare_loot_bag","lootbagrare",Rarity.RARE);
        public final int metadata; public final String path,texture; public final Rarity rarity;
        Kind(int metadata,String path,String texture,Rarity rarity) { this.metadata=metadata; this.path=path; this.texture=texture; this.rarity=rarity; }
    }
    private final Kind kind;
    public LootBagItem(Kind kind) { super(new Properties().stacksTo(16).rarity(kind.rarity)); this.kind=kind; }
    @Override public Rarity getRarity(ItemStack stack) { return kind.rarity; }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) { lines.add(Component.translatable("tc.lootbag")); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if (!level.isClientSide) {
            var server=(net.minecraft.server.level.ServerLevel)level;
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,player.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,player)
                    .withLuck(player.getLuck()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            var table=server.getServer().getLootData().getLootTable(net.minecraft.resources.ResourceLocation.parse("thaumcraft:bags/"+kind.path));
            for (var loot:table.getRandomItems(params)) level.addFreshEntity(new ItemEntity(level,player.getX(),player.getY(),player.getZ(),loot));
            level.playSound(null,player.getX(),player.getY(),player.getZ(),TCSounds.COINS.get(),SoundSource.MASTER,.75f,1);
        }
        stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
