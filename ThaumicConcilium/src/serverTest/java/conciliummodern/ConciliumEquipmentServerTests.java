package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumEquipmentServerTests {
    @GameTest(template="empty") public static void actualRobeDyeRecipeKeepsLegacyPaletteAndOriginalNbt(GameTestHelper t){
        var loaded=t.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("minecraft:armor_dye")).orElseThrow();
        t.assertTrue(loaded instanceof tc4revival.content.RobeDyeRecipe,"Real loaded recipe uses core serializer with Concilium mixin");var recipe=(tc4revival.content.RobeDyeRecipe)loaded;
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
            public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return net.minecraft.world.item.ItemStack.EMPTY;}
            public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
        },3,3);
        for(var item:java.util.List.of(CrimsonContent.HELMET.get(),CrimsonContent.CHEST.get(),CrimsonContent.LEGS.get(),CrimsonContent.BOOTS.get())){
            grid.clearContent();var input=new ItemStack(item);input.setDamageValue(9);input.getOrCreateTag().putString("retained","robe-test");var before=input.copy();
            grid.setItem(0,input);grid.setItem(1,new ItemStack(net.minecraft.world.item.Items.WHITE_DYE));
            t.assertTrue(recipe.matches(grid,t.getLevel()),"Every robe piece accepts a real white dye through RecipeManager's recipe");var output=recipe.assemble(grid,t.getLevel().registryAccess());
            t.assertTrue(output.is(item)&&output.getCount()==1&&item.getColor(output)==0xcf8b8b,"Original red base and integer-brightness palette are used");
            t.assertTrue(output.getDamageValue()==9&&output.getOrCreateTag().getString("retained").equals("robe-test")&&ItemStack.isSameItemSameTags(input,before),"Output retains NBT and input stays unchanged");
            grid.setItem(2,before.copy());t.assertTrue(!recipe.matches(grid,t.getLevel())&&recipe.assemble(grid,t.getLevel().registryAccess()).isEmpty(),"Two robes are rejected without losing an input");grid.setItem(2,ItemStack.EMPTY);grid.setItem(1,ItemStack.EMPTY);t.assertTrue(!recipe.matches(grid,t.getLevel()),"A robe without dye is not a recipe");
        }
        t.succeed();
    }
    private static ServerPlayer player(GameTestHelper t){
        var p=new ServerPlayer(t.getLevel().getServer(),t.getLevel(),new GameProfile(UUID.randomUUID(),"ConciliumEquip"));
        new ServerGamePacketListenerImpl(p.server,new Connection(PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}},p);
        p.setGameMode(GameType.SURVIVAL);p.setPos(t.absoluteVec(new Vec3(10.5,3,10.5)));t.getLevel().addNewPlayer(p);return p;
    }
    private static void equip(ServerPlayer p){p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(CrimsonContent.HELMET.get()));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(CrimsonContent.CHEST.get()));p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(CrimsonContent.LEGS.get()));p.setItemSlot(EquipmentSlot.FEET,new ItemStack(CrimsonContent.BOOTS.get()));}
    @GameTest(template="empty") public static void robeRequiresCompleteSetAndRemovalRestoresCollision(GameTestHelper t){
        var p=player(t);EtherealState.toggleRobe(p);t.assertTrue(!EtherealState.active(p),"Toggle without full set is rejected");equip(p);EtherealState.toggleRobe(p);
        t.assertTrue(EtherealState.active(p)&&EtherealState.robe(p)&&CrimsonControl.chained(p),"Complete set enters phase and applies original control lock");
        EtherealState.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));t.assertTrue(p.noPhysics,"Actual player tick enables phasing collision");
        p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);EtherealState.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        t.assertTrue(!EtherealState.active(p)&&!EtherealState.robe(p)&&!p.noPhysics,"Removing a piece immediately disables robe phase and collision exemption");p.discard();t.succeed();
    }
    @GameTest(template="empty",timeoutTicks=120) public static void etherealDamageAndToolsConsumeFleshWhileOrdinaryDamageDoesNot(GameTestHelper t){
        var p=player(t);
        // ServerPlayer rejects attacks before LivingAttackEvent during its first sixty server ticks.
        t.runAfterDelay(65,()->{
            var pig=EntityType.PIG.create(t.getLevel());
            try{
                float initial=p.getHealth();t.assertTrue(p.hurt(t.getLevel().damageSources().generic(),2)&&p.getHealth()<initial,"Spawn grace has elapsed and an unphased player really takes damage");
                p.setHealth(initial);p.invulnerableTime=0;EtherealState.toggle(p);float health=p.getHealth();
                p.hurt(t.getLevel().damageSources().generic(),8);t.assertTrue(p.getHealth()==health&&EtherealState.flesh(p)==0,"Nonmagic damage is blocked without flesh loss");
                p.hurt(t.getLevel().damageSources().magic(),8);t.assertTrue(p.getHealth()==health&&EtherealState.flesh(p)==1,"Magic is blocked but consumes one flesh");
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));pig.setPos(p.position().add(2,0,0));t.getLevel().addFreshEntity(pig);EtherealState.attack(new AttackEntityEvent(p,pig));
                t.assertTrue(EtherealState.flesh(p)==2,"Mundane tool use costs another flesh");EtherealState.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));t.assertTrue(p.getHealth()==18,"Flesh loss caps actual maximum usable health");
                t.assertTrue(EtherealState.restoreFlesh(p)&&EtherealState.flesh(p)==1,"Crucible restoration returns exactly one flesh");EtherealState.restoreAllFlesh(p);t.assertTrue(EtherealState.flesh(p)==0&&!EtherealState.restoreFlesh(p),"Fully restored state refuses redundant consumption");t.succeed();
            }finally{pig.discard();p.discard();}
        });
    }
    @GameTest(template="empty") public static void hammerDrainRespectsProtectionThenHealsAndWears(GameTestHelper t){
        var p=player(t);var hammer=new ItemStack(CrimsonContent.HAMMER.get());p.setItemInHand(InteractionHand.MAIN_HAND,hammer);p.setHealth(10);CrimsonContent.HAMMER.get().use(t.getLevel(),p,InteractionHand.MAIN_HAND);
        var pig=EntityType.PIG.create(t.getLevel());pig.setNoAi(true);pig.setPos(p.position().add(2,0,0));t.getLevel().addFreshEntity(pig);
        java.util.function.Consumer<AttackEntityEvent> guard=e->{if(e.getTarget()==pig)e.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,AttackEntityEvent.class,guard);
        try{CrimsonContent.HAMMER.get().onUseTick(t.getLevel(),p,hammer,20);t.assertTrue(pig.getHealth()==pig.getMaxHealth()&&p.getHealth()==10&&hammer.getDamageValue()==0,"Protected target cannot feed hammer healing or spend durability");}finally{MinecraftForge.EVENT_BUS.unregister(guard);}
        CrimsonContent.HAMMER.get().onUseTick(t.getLevel(),p,hammer,20);t.assertTrue(pig.getHealth()<pig.getMaxHealth()&&p.getHealth()==12&&hammer.getDamageValue()==2,"Accepted actual hit heals two and costs two durability");
        p.tickCount=20;CrimsonContent.HAMMER.get().inventoryTick(hammer,t.getLevel(),p,0,true);t.assertTrue(hammer.getDamageValue()==1,"Original passive repair restores one durability each second");pig.discard();p.discard();t.succeed();
    }
}
