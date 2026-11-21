package heartworkaudit;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/** Isolated fixture and observation commands; excluded from both product archives. */
@Mod("heartwork_audit")
public class Audit {
    public Audit() {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent event) -> {
            event.getDispatcher().register(Commands.literal("hwp").requires(s -> s.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("id",StringArgumentType.greedyString()).executes(c -> {
                    var p=c.getSource().getServer().getPlayerList().getPlayerByName("HeartworkAudit");
                    var id=new ResourceLocation(StringArgumentType.getString(c,"id"));
                    var registry=p.level().registryAccess().registryOrThrow(mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition.REGISTRY_KEY);
                    ItemStack stack=registry.containsKey(id)?registry.get(id).getBlade():new ItemStack(heartwork.Heartwork.item(id.toString()));
                    if(stack.isEmpty())throw new IllegalStateException("Fixture item is unavailable: "+id);
                    if(stack.getItem() instanceof heartwork.compat.blade.FluxBladeItem)stack.getOrCreateTag().putInt("Energy",200000);
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);p.inventoryMenu.broadcastChanges();
                    c.getSource().sendSuccess(() -> Component.literal("Prepared "+id),false);return 1;
                })))
                .then(Commands.literal("state").executes(c -> {
                    var p=c.getSource().getServer().getPlayerList().getPlayerByName("HeartworkAudit");
                    String state=p.getMainHandItem().save(new net.minecraft.nbt.CompoundTag()).toString();
                    c.getSource().sendSuccess(() -> Component.literal(state),false);return 1;
                }))
                .then(Commands.literal("fluxcheck").executes(c -> {
                    var p=c.getSource().getServer().getPlayerList().getPlayerByName("HeartworkAudit");var stack=p.getMainHandItem();
                    if(!heartwork.compat.blade.FluxBladeItem.empowered(stack)||heartwork.compat.blade.FluxBladeItem.energy(stack)>=200000)throw new IllegalStateException("No real C2S flux toggle observed");
                    c.getSource().sendSuccess(() -> Component.literal("HEARTWORK_NETWORK_PASS energy="+heartwork.compat.blade.FluxBladeItem.energy(stack)),false);return 1;
                })));
        });
    }
}
