package thmodern.vat;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;
import thmodern.Horizons;

public final class VatMatrixEntity extends VisNetworkNodeBlockEntity implements WandInteractable {
    public VatMatrixEntity(BlockPos pos,BlockState state){super(Horizons.VAT_MATRIX_ENTITY.get(),pos,state);}
    @Override public int range(){return 8;}
    @Override public boolean source(){return false;}
    public VatEntity vat(){return level!=null&&level.hasChunkAt(worldPosition.below())&&level.getBlockEntity(worldPosition.below()) instanceof VatEntity vat?vat:null;}
    public float visualStartup;
    public int visualCraftTicks;
    public void tick(){
        if(level!=null&&level.isClientSide){
            boolean working=vat()!=null&&vat().mode()==VatEntity.INFUSING;
            if(working){
                if(visualCraftTicks==0||visualCraftTicks%65==0)level.playLocalSound(worldPosition.below(),visualCraftTicks==0?tc4revival.content.TCSounds.INFUSER_START.get():tc4revival.content.TCSounds.INFUSER.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1,false);
                visualCraftTicks++;visualStartup=Math.min(1,visualStartup+Math.max(.001f,visualStartup/10));
            }
            else{visualCraftTicks=Math.max(0,Math.min(50,visualCraftTicks-2));visualStartup*=.9f;if(visualStartup<.001f)visualStartup=0;}
        }else tickNetwork();
    }
    private boolean activate(Player player){return player instanceof ServerPlayer server&&vat()!=null&&vat().stillValid(player)&&vat().beginInfusion(server);}
    @Override public InteractionResult onWandUseFirst(ItemStack stack,UseOnContext context){return context.getLevel().isClientSide?InteractionResult.SUCCESS:activate(context.getPlayer())?InteractionResult.CONSUME:InteractionResult.PASS;}
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack stack,Player player){return activate(player)?Optional.of(stack):Optional.empty();}
    @Override public void onWandUseTick(ItemStack stack,Player player,int remaining){}
    @Override public void onWandRelease(ItemStack stack,Level level,Player player,int remaining){}
}
