package ttmodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import tc4revival.aspect.*;

public final class SmeltingFocus extends TinkererFocus {
    private record Progress(BlockPos pos,BlockState state,net.minecraft.resources.ResourceKey<Level> dimension,long lastTick,int remaining){}
    private final Map<Player,Progress> progress=Collections.synchronizedMap(new WeakHashMap<>());
    public SmeltingFocus(){super("focus_smelting","TTEF",0xff0000);}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return cost("ignis",45,"perditio",12);}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){player.startUsingItem(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
    @Override public void release(ItemStack stack,Level level,Player player,int remaining){progress.remove(player);if(level.isClientSide)tc4revival.client.ContinuousFocusBeam.release(player);}
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        var level=player.level();
        var hit=level.clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(10)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(hit.getType()!=HitResult.Type.BLOCK){progress.remove(player);return;}
        if(level.isClientSide){
            var input=new SimpleContainer(new ItemStack(level.getBlockState(hit.getBlockPos()).getBlock()));
            var result=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level).map(r->r.assemble(input,level.registryAccess())).orElse(ItemStack.EMPTY);
            if(result.getItem() instanceof BlockItem && ((CastingWandItem)stack.getItem()).consumeForPlayer(stack,player,costs(attached(stack)),false,false,false))
                tc4revival.client.ContinuousFocusBeam.update(player,hit.getBlockPos().getCenter(),2,0xff0000,0,true);
            return;
        }
        heat(stack,player,hit.getBlockPos());
    }
    /** Server-authoritative conversion; called after targeting and independently exercised by integration tests. */
    void heat(ItemStack stack,Player player,BlockPos pos){
        var level=player.level();if(level.isClientSide||!level.hasChunkAt(pos)||!level.mayInteract(player,pos)||!player.mayBuild()){progress.remove(player);return;}
        var state=level.getBlockState(pos);var wand=(CastingWandItem)stack.getItem();
        if(state.hasBlockEntity()||state.getDestroySpeed(level,pos)<0||!wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,false)){progress.remove(player);return;}
        var input=new SimpleContainer(new ItemStack(state.getBlock()));
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level);
        var output=recipe.map(r->r.assemble(input,level.registryAccess())).orElse(ItemStack.EMPTY);
        if(!(output.getItem() instanceof BlockItem block)||output.getCount()!=1||block.getBlock().defaultBlockState().hasBlockEntity()){progress.remove(player);return;}
        var before=progress.get(player);int ticks=thaumcraft.api.wands.SpellParameters.duration("smelting.speed",Math.max(1,(int)Math.ceil(20-wand.focusPotency(stack,player)*5)));
        if(before==null||!before.pos.equals(pos)||!before.state.equals(state)||!before.dimension.equals(level.dimension())||level.getGameTime()-before.lastTick>1){progress.put(player,new Progress(pos.immutable(),state,level.dimension(),level.getGameTime(),ticks));return;}
        if(before.remaining>1){progress.put(player,new Progress(pos.immutable(),state,level.dimension(),level.getGameTime(),before.remaining-1));if(level instanceof net.minecraft.server.level.ServerLevel server)thaumcraft.api.fx.LegacyEffects.wisp(server,pos.getCenter(),0xff3333,.25f,2,.4f,-.05f);return;}
        progress.remove(player);
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,server.gameMode.getGameModeForPlayer(),server,pos)==-1)return;
        // Preflight payment above and mutation below run on the same server thread.
        if(!level.getBlockState(pos).equals(state)||!level.setBlock(pos,block.getBlock().defaultBlockState(),3))return;
        if(!wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true)){level.setBlock(pos,state,3);return;}
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,net.minecraft.sounds.SoundSource.BLOCKS,.6f,1);
        if(level instanceof net.minecraft.server.level.ServerLevel server)thaumcraft.api.fx.LegacyEffects.wisp(server,pos.getCenter(),0xff3333,.25f,25,.5f,-.05f);
    }
}
