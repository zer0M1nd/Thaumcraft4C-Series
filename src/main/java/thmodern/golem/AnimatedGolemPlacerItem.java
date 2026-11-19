package thmodern.golem;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.entity.player.Player;
import tc4revival.content.*;
import thaumcraft.api.golems.GolemApi;

/** Portable addon constructs retain the base core, markers and inventory filter schema. */
public final class AnimatedGolemPlacerItem extends Item {
    public AnimatedGolemPlacerItem(){super(new Properties().stacksTo(1));}
    @Override public boolean doesSneakBypassUse(ItemStack stack,LevelReader level,BlockPos pos,Player player){return true;}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var player=context.getPlayer();var level=context.getLevel();
        if(player==null||player.isShiftKeyDown())return InteractionResult.PASS;
        var pos=context.getClickedPos().relative(context.getClickedFace());
        if(!player.mayUseItemAt(pos,context.getClickedFace(),stack)||!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var golem=TCEntities.GOLEM.get().create(level);if(golem==null)return InteractionResult.FAIL;
        golem.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,player.getYRot(),0);
        GolemPlacerItem.restore(golem,stack,GolemMaterial.WOOD,pos,context.getClickedFace(),player.getGameProfile().getName());
        if(!AnimatedGolems.isAnimated(golem)||!level.noCollision(golem))return InteractionResult.FAIL;
        var data=golem.configuration().addonData(AnimatedGolems.DATA);
        data.putUUID("Owner",player.getUUID());data.putInt("Remaining",600);data.putInt("VoidCount",0);
        data.putBoolean("Permanent",golem.configuration().core()!=null);
        GolemApi.data(golem,AnimatedGolems.DATA,data);
        GolemApi.configure(golem,golem.configuration().customMaterial(),golem.configuration().core()==null?AnimatedGolems.TASK:null,golem.configuration().blockAppearance());
        if(!level.addFreshEntity(golem))return InteractionResult.FAIL;
        if(!player.getAbilities().instabuild)stack.shrink(1);
        golem.playAmbientSound();return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){
        if(stack.hasTag()&&stack.getTag().contains("BlockAppearance")){
            var state=net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),stack.getTag().getCompound("BlockAppearance"));
            lines.add(state.getBlock().getName());
        }
        TCItems.golem(GolemMaterial.WOOD).appendHoverText(stack,level,lines,flag);
    }
}
