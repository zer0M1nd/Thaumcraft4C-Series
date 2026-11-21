package conciliummodern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.*;

public final class PontifexRobe extends ArmorItem implements DyeableLeatherItem,thaumcraft.api.IRepairable,thaumcraft.api.IRunicArmor,thaumcraft.api.IVisDiscountGear,thaumcraft.api.IWarpingGear,thaumcraft.api.IGoggles {
    private static final ArmorMaterial MATERIAL=new ArmorMaterial(){
        public int getDurabilityForType(Type type){return 30*switch(type){case HELMET->11;case CHESTPLATE->16;case LEGGINGS->15;case BOOTS->13;};}
        public int getDefenseForType(Type type){return switch(type){case HELMET,BOOTS->4;case CHESTPLATE->8;case LEGGINGS->7;};}
        public int getEnchantmentValue(){return 25;}
        public net.minecraft.sounds.SoundEvent getEquipSound(){return net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER;}
        public Ingredient getRepairIngredient(){return Ingredient.EMPTY;}
        public String getName(){return ThaumicConcilium.ID+":pontifex";}public float getToughness(){return 0;}public float getKnockbackResistance(){return 0;}
    };
    private static final net.minecraftforge.network.simple.SimpleChannel NETWORK=NetworkRegistry.newSimpleChannel(ResourceLocation.parse("thaumicconcilium:pontifex"),()->"1","1"::equals,"1"::equals);
    private record Toggle(){}
    public static void init(){NETWORK.messageBuilder(Toggle.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((v,b)->{}).decoder(b->new Toggle()).consumerMainThread((v,c)->{var p=c.get().getSender();if(p!=null)EtherealState.toggleRobe(p);c.get().setPacketHandled(true);}).add();}
    public static void sendToggle(){NETWORK.sendToServer(new Toggle());}
    public PontifexRobe(Type type){super(MATERIAL,type,new Properties().rarity(Rarity.EPIC));}
    public static boolean fullSet(Player player){for(var slot:java.util.List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))if(!(player.getItemBySlot(slot).getItem() instanceof PontifexRobe robe)||robe.getEquipmentSlot()!=slot)return false;return true;}
    @Override public boolean hasCustomColor(ItemStack stack){return true;}
    @Override public int getColor(ItemStack stack){var tag=stack.getTagElement("display");return tag!=null&&tag.contains("color")?tag.getInt("color"):0xa01818;}
    @Override public int getRunicCharge(ItemStack stack){return 5;}
    @Override public int getVisDiscount(ItemStack stack,Player player,thaumcraft.api.aspects.Aspect aspect){return 5;}
    @Override public int getWarp(ItemStack stack,Player player){return 3;}
    @Override public boolean showNodes(ItemStack stack,LivingEntity player){return true;}
    @Override public boolean showIngamePopups(ItemStack stack,LivingEntity player){return true;}
    @Override public void onArmorTick(ItemStack stack,Level level,Player player){if(!level.isClientSide&&getType()==Type.CHESTPLATE&&fullSet(player)&&EtherealState.robe(player))CrimsonControl.chain(player,100);}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,level,lines,flag);
        lines.add(net.minecraft.network.chat.Component.translatable("tc.visdiscount").append(": 5%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer){return "thaumicconcilium:textures/models/armor/pontifex_robe"+(layer==null?"":"_overlay")+".png";}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new PontifexRobeModel.Extensions());}
    @Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack,net.minecraft.world.item.context.UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();var state=level.getBlockState(pos);
        if(!state.is(net.minecraft.world.level.block.Blocks.WATER_CAULDRON))return net.minecraft.world.InteractionResult.PASS;
        if(!level.isClientSide){clearColor(stack);net.minecraft.world.level.block.LayeredCauldronBlock.lowerFillLevel(state,level,pos);}
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
}
