package temodern.mixin;

import appeng.api.config.Actionable;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.common.MEStorageMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tc4revival.content.TCItems;
import temodern.essentia.EssentiaKey;

@Mixin(value=MEStorageMenu.class,remap=false)
public abstract class TerminalPhialMixin extends AEBaseMenu {
    @Shadow @Final protected MEStorage storage;
    @Unique private boolean teBorrowedPhial;
    protected TerminalPhialMixin(MenuType<?> type,int id,Inventory inventory,Object host){super(type,id,inventory,host);}
    @Inject(method="tryFillContainerItem",at=@At("HEAD"))
    private void teBorrow(AEKey key,boolean move,CallbackInfo ci){
        teBorrowedPhial=false;
        if(storage!=null&&getCarried().isEmpty()&&key instanceof EssentiaKey&&storage.extract(key,8,Actionable.SIMULATE,getActionSource())==8
            &&storage.extract(AEItemKey.of(TCItems.EMPTY_PHIAL.get()),1,Actionable.MODULATE,getActionSource())==1){setCarried(new ItemStack(TCItems.EMPTY_PHIAL.get()));teBorrowedPhial=true;}
    }
    @Inject(method="tryFillContainerItem",at=@At("RETURN"))
    private void teReturn(AEKey key,boolean move,CallbackInfo ci){
        if(teBorrowedPhial&&getCarried().is(TCItems.EMPTY_PHIAL.get())){
            if(storage.insert(AEItemKey.of(TCItems.EMPTY_PHIAL.get()),1,Actionable.MODULATE,getActionSource())==1)setCarried(ItemStack.EMPTY);
        }
        teBorrowedPhial=false;
    }
}
