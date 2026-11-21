package temodern.mixin;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import temodern.crafting.*;

@Mixin(value=PatternEncodingTermMenu.class,remap=false)
public abstract class PatternTerminalMixin extends AEBaseMenu implements ArcaneEncodingMenu {
    @Shadow @Final private PatternEncodingLogic encodingLogic;
    @Shadow @Final private appeng.menu.slot.PatternTermSlot craftOutputSlot;
    @GuiSync(86) public boolean teArcane;
    protected PatternTerminalMixin(MenuType<?> type,int id,Inventory inv,Object host){super(type,id,inv,host);}
    @Inject(method="<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/helpers/IPatternTerminalMenuHost;Z)V",at=@At("RETURN"))
    private void teInit(CallbackInfo ci){registerClientAction("te_arcane_mode",Boolean.class,this::teSetArcane);teArcane=((ArcaneEncodingLogic)encodingLogic).teArcaneMode();}
    @Override public boolean teIsArcane(){return teArcane;}
    @Override public void teSetArcane(boolean value){
        if(isClientSide())sendClientAction("te_arcane_mode",value);
        teArcane=value;
        if(value){encodingLogic.setMode(EncodingMode.CRAFTING);((PatternEncodingTermMenu)(Object)this).mode=EncodingMode.CRAFTING;}
        ((ArcaneEncodingLogic)encodingLogic).teArcaneMode(value);
        if(isServerSide())broadcastChanges();
    }
    @Inject(method="setMode",at=@At("HEAD"))
    private void teNormalMode(EncodingMode mode,CallbackInfo ci){if(isClientSide()&&teArcane)teSetArcane(false);}
    @Inject(method={"broadcastChanges","m_38946_"},at=@At("HEAD"))
    private void teSync(CallbackInfo ci){if(isServerSide())teArcane=((ArcaneEncodingLogic)encodingLogic).teArcaneMode();}
    @Unique private ItemStack teEncode(){
        var inv=((PatternEncodingTermMenu)(Object)this).getCraftingMatrix();var grid=new CraftGrid();
        for(int i=0;i<9;i++){
            var stack=inv.getStackInSlot(i);if(appeng.api.stacks.GenericStack.unwrapItemStack(stack)!=null)return null;
            grid.setItem(i,stack.copyWithCount(stack.isEmpty()?0:1));
        }
        var level=getPlayer().level();
        for(var recipe:level.getRecipeManager().getAllRecipesFor(tc4revival.content.TCArcane.RECIPE_TYPE.get()))
            if(recipe.matches(grid,level))return ArcanePattern.encode(grid,recipe,level);
        return null;
    }
    @Inject(method="encodePattern",at=@At("HEAD"),cancellable=true)
    private void teEncodePattern(CallbackInfoReturnable<ItemStack> cir){if(teArcane)cir.setReturnValue(teEncode());}
    @Inject(method="getAndUpdateOutput",at=@At("HEAD"),cancellable=true)
    private void tePreview(CallbackInfoReturnable<ItemStack> cir){
        if(!teArcane)return;var pattern=teEncode();ItemStack result=ItemStack.EMPTY;
        if(pattern!=null){var decoded=new ArcanePattern.Decoder().decodePattern(pattern,getPlayer().level(),false);if(decoded!=null)result=((appeng.api.stacks.AEItemKey)decoded.getPrimaryOutput().what()).toStack();}
        craftOutputSlot.setDisplayedCraftingOutput(result);cir.setReturnValue(result);
    }
}
