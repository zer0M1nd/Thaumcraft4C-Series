package conciliummodern;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

public final class ThaumaturgeMenu extends AbstractContainerMenu {
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ThaumicConcilium.ID);
    public static final RegistryObject<MenuType<ThaumaturgeMenu>> TYPE=MENUS.register("thaumaturge",()->net.minecraftforge.common.extensions.IForgeMenuType.create(ThaumaturgeMenu::new));
    private final Thaumaturge merchant;
    private final Player owner;
    private final SimpleContainer contents=new SimpleContainer(5);
    public ThaumaturgeMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf buffer) {
        this(id,inventory,inventory.player.level().getEntity(buffer.readInt()) instanceof Thaumaturge mob?mob:null);
    }
    public ThaumaturgeMenu(int id,Inventory inventory,Thaumaturge merchant) {
        super(TYPE.get(),id);this.merchant=merchant;owner=inventory.player;
        addSlot(new Slot(contents,0,36,29));
        for(int i=0;i<4;i++)addSlot(new Slot(contents,i+1,106+i%2*18,20+i/2*18){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    private static int value(ItemStack stack) {
        if(stack.isEmpty())return 0;
        if(stack.is(TCItems.MANA_BEAN.get()))return 1;
        if(stack.getItem() instanceof net.minecraft.world.item.StandingAndWallBlockItem block && block.getBlock() instanceof net.minecraft.world.level.block.AbstractSkullBlock)return 3;
        if(stack.getItem() instanceof CastingWandItem || stack.is(Items.EXPERIENCE_BOTTLE))return 3;
        if(stack.is(Items.ENCHANTED_BOOK))return 5;
        if(stack.getItem() instanceof ResearchNotesItem)return -1;
        return 0;
    }
    public boolean ready() {
        if(value(contents.getItem(0))==0)return false;
        for(int i=1;i<5;i++)if(!contents.getItem(i).isEmpty())return false;return true;
    }
    @Override public boolean stillValid(Player player) {
        return merchant!=null && merchant.isAlive() && merchant.anger()==0 && merchant.level()==player.level() && player.distanceToSqr(merchant)<=64
                && (player.level().isClientSide || merchant.customer==player);
    }
    private record Reward(int weight,ItemStack stack) {}
    private static List<Reward> rewards() {
        var result=new ArrayList<Reward>();result.add(new Reward(9,new ItemStack(TCItems.MANA_BEAN.get())));
        for(var shard:ShardType.values())if(shard!=ShardType.BALANCED)result.add(new Reward(10,new ItemStack(TCItems.shard(shard))));
        result.add(new Reward(7,new ItemStack(TCItems.KNOWLEDGE_FRAGMENT.get())));result.add(new Reward(7,new ItemStack(TCItems.KNOWLEDGE_FRAGMENT.get())));
        for(var shard:ShardType.values())result.add(new Reward(2,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumcraft",shard.crystalClusterPath())))));
        result.add(new Reward(1,new ItemStack(TCItems.FOCUS_POUCH.get())));result.add(new Reward(1,new ItemStack(TCItems.FIRE_FOCUS.get())));
        result.add(new Reward(4,new ItemStack(TCItems.LESSER_VIS_AMULET.get())));
        var profane=ForgeRegistries.ITEMS.getValue(new ResourceLocation("forbidden_magic","wand_rod_profane"));
        if(profane!=null && profane!=Items.AIR)result.add(new Reward(1,new ItemStack(profane)));
        return result;
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(button!=0 || player!=owner || player.level().isClientSide || player.containerMenu!=this || !stillValid(player) || !ready())return false;
        var offer=contents.getItem(0);var random=merchant.getRandom();
        if(offer.getItem() instanceof ResearchNotesItem) {
            var read=ResearchNoteNbt.read(offer.getTag(),TCAspects.catalog());
            if(read.problems().isEmpty() && read.note().filter(ResearchNote::complete).isPresent()) {
                int roll=random.nextInt(4);String key=roll==0?"VISCONDUCTOR":roll==3?"IMPULSEFOCI":"POSITIVEBURSTFOCI";
                var definition=ResearchCatalogs.catalog().find(ThaumicConcilium.ID+":"+key);
                var note=definition.flatMap(research->new ResearchNotes(TCAspects.catalog()).generate(research,random::nextInt,tc4revival.config.TCConfig.SERVER.randomizedResearchNotes.get()));
                if(note.isEmpty())return false;
                var reward=ResearchNotesItem.create(note.get());reward.getOrCreateTag().putBoolean(roll==0?"VISCONDUCTOR_HINT":roll==3?"IMPULSE_HINT":"POSITIVE_BURST_HINT",true);
                contents.setItem(1,reward);contents.removeItem(0,1);
                player.sendSystemMessage(Component.translatable("tc.thaumaturge.trade."+(1+random.nextInt(3))).withStyle(ChatFormatting.DARK_PURPLE));broadcastChanges();return true;
            }
        }
        int value=value(offer);
        if(random.nextInt(5)==0)value+=random.nextInt(2);else if(random.nextBoolean())value-=random.nextInt(2);
        var rewards=rewards();int roll=random.nextInt(rewards.stream().mapToInt(Reward::weight).sum());contents.removeItem(0,1);
        // Preserve the original weighted starting point and possible subsequent rewards.
        for(var reward:rewards) {
            if(roll<reward.weight() && value>random.nextInt(10)) {
                var stack=reward.stack().copy();mergeReward(stack);
                if(!stack.isEmpty())net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player,stack);
                if(value<random.nextInt(10))break;
            }
            roll-=reward.weight();
        }
        broadcastChanges();return true;
    }
    private void mergeReward(ItemStack stack) {
        for(int i=1;i<5 && !stack.isEmpty();i++) {
            var existing=contents.getItem(i);
            if(!existing.isEmpty() && ItemStack.isSameItemSameTags(existing,stack)) {
                int count=Math.min(stack.getCount(),existing.getMaxStackSize()-existing.getCount());existing.grow(count);stack.shrink(count);
            }
        }
        for(int i=1;i<5 && !stack.isEmpty();i++)if(contents.getItem(i).isEmpty())contents.setItem(i,stack.split(stack.getMaxStackSize()));
        contents.setChanged();
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;
        var original=stack.copy();if(!(index<5?moveItemStackTo(stack,5,41,true):moveItemStackTo(stack,0,1,true)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
    @Override public void removed(Player player) {
        super.removed(player);if(merchant!=null && merchant.customer==player)merchant.customer=null;
        if(!player.level().isClientSide)clearContainer(player,contents);
    }
}
