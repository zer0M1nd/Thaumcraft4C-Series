package technomancy;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

public class TechItem extends Item {
    public final String id;
    public TechItem(String id){super(id.equals("existence_gem")?new Properties().durability(100):new Properties().stacksTo(id.equals("potency_gem")?64:1));this.id=id;}
    @Override public ItemStack getDefaultInstance(){var stack=super.getDefaultInstance();if(id.equals("existence_gem"))stack.setDamageValue(100);return stack;}
    @Override public boolean isFoil(ItemStack stack){return id.equals("existence_gem")&&stack.getDamageValue()==0||super.isFoil(stack);}
    @Override public boolean isEnchantable(ItemStack stack){return false;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        if(!level.isClientSide&&entity instanceof Player player&&PlayerEvents.TREASURES.contains(id))PlayerEvents.treasureEffect(player,id);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(id.equals("ritual_tome")) {
            if(level.isClientSide)DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->technomancy.client.TechClient.openTome());
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        }
        return super.use(level,player,hand);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        var level=context.getLevel();var pos=context.getClickedPos();var target=level.getBlockEntity(pos);var stack=context.getItemInHand();
        if(id.equals("potency_gem")&&target instanceof MachineEntity m&&upgradeable(m.kind)) {
            if(m.boost)return InteractionResult.PASS;
            if(!level.isClientSide){MachineLogic.setBoost(m,true,player);if(!player.isCreative())stack.shrink(1);}
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!id.equals("coil_coupler"))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var tag=stack.getOrCreateTag();
        if(target instanceof MachineEntity coil&&(coil.kind==MachineKind.ITEM_COIL||coil.kind==MachineKind.ESSENTIA_COIL)) {
            if(player.isShiftKeyDown()){coil.links.clear();coil.changed();message(player,"links_cleared");}
            else {tag.putLong("coil",pos.asLong());tag.putString("dimension",level.dimension().location().toString());message(player,"link_selected");}
            return InteractionResult.CONSUME;
        }
        if(!tag.contains("coil")){message(player,"link_select_first");return InteractionResult.FAIL;}
        if(!tag.getString("dimension").equals(level.dimension().location().toString())){message(player,"link_dimension");return InteractionResult.FAIL;}
        var coilPos=BlockPos.of(tag.getLong("coil"));
        if(!level.hasChunkAt(coilPos)||!(level.getBlockEntity(coilPos) instanceof MachineEntity coil)){message(player,"link_missing");return InteractionResult.FAIL;}
        if(target==null||coilPos.equals(pos)||coil.kind!=MachineKind.ITEM_COIL&&coil.kind!=MachineKind.ESSENTIA_COIL)return InteractionResult.FAIL;
        boolean valid=coil.kind==MachineKind.ITEM_COIL?target.getCapability(ForgeCapabilities.ITEM_HANDLER,context.getClickedFace()).isPresent():target instanceof thaumcraft.api.aspects.IEssentiaStorage||target instanceof tc4revival.content.EssentiaTransport||target instanceof tc4revival.content.EssentiaVessel;
        if(!valid){message(player,"link_incompatible");return InteractionResult.FAIL;}
        coil.links.add(pos.immutable());var faces=coil.state.getCompound("linkFaces");faces.putInt(Long.toString(pos.asLong()),context.getClickedFace().get3DDataValue());coil.state.put("linkFaces",faces);coil.changed();message(player,"linked");return InteractionResult.CONSUME;
    }
    public static boolean upgradeable(MachineKind k){return k.dynamo()||k==MachineKind.NODE_FABRICATOR||k==MachineKind.ADVANCED_DECONSTRUCTION_TABLE||k==MachineKind.ESSENTIA_COIL||k==MachineKind.ITEM_COIL;}
    private static void message(Player p,String key){p.displayClientMessage(Component.translatable("message.technomancy."+key),true);}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.technomancy."+id));
        if(id.equals("existence_gem"))tooltip.add(Component.translatable("tooltip.technomancy.gem_charge",100-stack.getDamageValue()));
    }
}
