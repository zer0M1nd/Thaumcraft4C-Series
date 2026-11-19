package automagy.content;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
public final class EnderDisjunctionRingItem extends Item implements ICurioItem,thaumcraft.api.IRunicArmor {
    private final boolean lesser;
    public EnderDisjunctionRingItem(boolean lesser){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));this.lesser=lesser;}
    @Override public int getRunicCharge(ItemStack stack){return 0;}
    public int damageAndPay(net.minecraft.server.level.ServerPlayer player){
        return lesser?8:thaumcraft.api.ThaumcraftApiHelper.consumeVisFromInventory(player,new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ENTROPY,100))?15:0;
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){if(lesser)lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.disjunction_lesser"));}
}
