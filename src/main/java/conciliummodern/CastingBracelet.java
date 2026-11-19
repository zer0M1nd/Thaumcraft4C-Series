package conciliummodern;

import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

/** Original named carriers, sharing the modern wand protocol and the originating rod callbacks. */
public final class CastingBracelet extends CastingWandItem {
    public record Definition(String name,int capacity,String rod,String cap,String material,String mod,String research){}
    public static java.util.List<Definition> definitions(){try(var in=CastingBracelet.class.getResourceAsStream("/conciliummodern/bracelets.json")){
        if(in==null)throw new IllegalStateException("Missing bracelet definitions");
        return java.util.List.of(new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8),Definition[].class));
    }catch(java.io.IOException failure){throw new IllegalStateException(failure);}}
    private final Definition definition;
    private record Parts(WandParts.Rod rod,WandParts.Cap cap,boolean present){}
    private volatile Parts parts;
    private Parts parts(){
        var value=parts;if(value!=null)return value;
        try{value=new Parts(WandParts.Rod.require(definition.rod()),WandParts.Cap.require(definition.cap()),true);}
        catch(IllegalArgumentException missing){value=new Parts(WandParts.Rod.WOOD,WandParts.Cap.IRON,false);}
        parts=value;return value;
    }
    public CastingBracelet(Definition definition){this.definition=definition;}
    public Definition definition(){return definition;}
    public boolean terrasteel(){return definition.name().equals("terrasteel");}
    public boolean available(){
        var mods=net.minecraftforge.fml.ModList.get();if(!mods.isLoaded("thaumicbases")||!definition.mod().isEmpty()&&!mods.isLoaded(definition.mod()))return false;
        if(definition.name().equals("witchwood")&&!ArsLegacyIntegration.wandsAvailable())return false;
        // A missing legacy integration remains dormant; saved items still deserialize safely.
        return parts().present();
    }
    @Override public WandParts.Rod rod(ItemStack stack){return parts().rod();}
    @Override public WandParts.Cap cap(ItemStack stack){return parts().cap();}
    public void initializeParts(ItemStack stack){var tag=stack.getOrCreateTag();tag.putString("rod",definition.rod());tag.putString("cap",definition.cap());tag.remove("sceptre");}
    @Override public boolean isStaff(ItemStack stack){return true;}
    @Override public boolean isSceptre(ItemStack stack){return false;}
    @Override public int capacity(ItemStack stack){return definition.capacity()*100;}
    @Override public int focusCooldownMillis(ItemStack stack,ItemStack focus,int normal){return normal/2;}
    @Override public Component getName(ItemStack stack){return Component.translatable(getDescriptionId());}
    @Override public float consumptionModifier(ItemStack stack,Player player,Aspect aspect,boolean crafting){
        var focus=WandFocusItem.attached(stack);int frugal=(terrasteel()||!crafting)&&focus.getItem() instanceof WandFocusItem item?item.upgradeLevel(focus,1):0;
        float discount=player==null?0:WandInventory.discount(player,aspect);
        return Math.max(.1f,(terrasteel()?3:cap(stack).cost(aspect.tag())/1.5f)-discount-frugal/11f)
                *(float)thaumcraft.api.wands.FocusUpgradeType.costMultiplier(focus,player,aspect);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(!available())return;
        if(!level.isClientSide)initializeParts(stack);
        super.inventoryTick(stack,level,entity,slot,selected);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,net.minecraft.world.item.context.UseOnContext context){return available()?super.onItemUseFirst(stack,context):InteractionResult.FAIL;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!available()||!thaumcraft.api.wands.WandAutomation.allowed(stack,player))return InteractionResultHolder.fail(stack);
        if(!terrasteel())return super.use(level,player,hand);
        try(var automation=thaumcraft.api.wands.WandAutomation.open(stack,player)){
            // Release an interrupted block-use session before choosing the next target.
            var previous=WandUseTarget.read(stack.getTag(),level.dimension().location().toString());
            if(previous.isPresent()){var t=previous.get();var pos=new net.minecraft.core.BlockPos(t.x(),t.y(),t.z());
                if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof WandInteractable target)target.onWandRelease(stack,level,player,0);}
            WandUseTarget.clear(stack.getTag());
            var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);var solid=getPlayerPOVHitResult(level,player,ClipContext.Fluid.NONE);
            if(solid.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&level.hasChunkAt(solid.getBlockPos())&&level.getBlockEntity(solid.getBlockPos()) instanceof AuraNodeBlockEntity)hit=solid;
            if(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&level.hasChunkAt(hit.getBlockPos())){
                if(level.getBlockState(hit.getBlockPos()).getBlock() instanceof WandInteractable target){var result=target.onWandUse(level,stack,player);if(result.isPresent())return InteractionResultHolder.consume(result.get());}
                if(level.getBlockEntity(hit.getBlockPos()) instanceof WandInteractable target){var result=target.onWandUse(level,stack,player);if(result.isPresent())return InteractionResultHolder.consume(result.get());}
            }
            var focus=WandFocusItem.attached(stack);
            if(focus.getItem() instanceof WandFocusItem item&&WandCooldowns.begin(player,(int)Math.min(Integer.MAX_VALUE,(long)item.cooldownMillis(focus)*3))){
                InteractionResultHolder<ItemStack> result=InteractionResultHolder.pass(stack);
                for(int i=0;i<10;i++){
                    if(!player.isAlive()||player.getItemInHand(hand)!=stack||!ItemStack.matches(focus,WandFocusItem.attached(stack)))break;
                    result=item.cast(stack,level,player,hand,hit);if(result.getObject()!=stack||result.getResult()==InteractionResult.FAIL)break;
                }
                return result;
            }
            return InteractionResultHolder.pass(stack);
        }
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
        if(!(entity instanceof Player player)||!available()||!thaumcraft.api.wands.WandAutomation.allowed(stack,player))return;
        try(var automation=thaumcraft.api.wands.WandAutomation.open(stack,player)){
            var position=WandUseTarget.read(stack.getTag(),level.dimension().location().toString());
            if(position.isPresent()){
                var p=position.get();var pos=new net.minecraft.core.BlockPos(p.x(),p.y(),p.z());
                if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof WandInteractable target){
                    target.onWandUseTick(stack,player,remaining);if(player.isUsingItem()&&player.getUseItem()==stack)target.onWandUseTick(stack,player,remaining+1);return;
                }
            }
            var focus=WandFocusItem.attached(stack);
            if(focus.getItem() instanceof WandFocusItem item&&WandCooldowns.begin(player,item.cooldownMillis(focus)/2))
                for(int i=0;i<(terrasteel()?10:2);i++){
                    if(!player.isUsingItem()||player.getUseItem()!=stack||!player.isAlive())break;
                    item.usingTick(stack,player,terrasteel()?i:remaining+i);
                }
        }
    }
    public static void setup(){
        thaumcraft.api.wands.WandRod.registerUpdate("thaumicconcilium:terrasteel",CastingBracelet::rechargeTerra);
        for(var entry:ConciliumItems.BRACELETS.values())thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:"+entry.get().definition().research(),entry.get()::available);
        thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:TCBASES",()->net.minecraftforge.fml.ModList.get().isLoaded("thaumicbases"));
    }
    private static void rechargeTerra(ItemStack stack,Player player){
        if(player.level().isClientSide||player.tickCount%40!=0||!net.minecraftforge.fml.ModList.get().isLoaded("botania")
                ||!fmmodern.IntegrationContent.botanyEnabled()||!fmmodern.ForbiddenConfig.value(fmmodern.ForbiddenConfig.CROSS_WAND))return;
        boolean hotbar=false;for(int i=0;i<9;i++)if(player.getInventory().getItem(i)==stack){hotbar=true;break;}if(hotbar)Botany.recharge(stack,player);
    }
    private static final class Botany {
        static void recharge(ItemStack stack,Player player){if(!(stack.getItem() instanceof CastingWandItem wand))return;
            for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()){
                int amount=wand.receiveVis(stack,aspect,100,true);
                if(amount>0&&vazkii.botania.api.mana.ManaItemHandler.instance().requestManaExact(stack,player,amount*20,true))wand.receiveVis(stack,aspect,amount,false);
            }
        }
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return CastingBraceletRenderer.instance();}
    });}
}
