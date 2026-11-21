package thaumicomniverse.client;

public final class BackpackClient {
    public static void setup() {
        net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerTab(thaumicomniverse.compat.backpack.ArcaneCraftingContainer.TYPE, ArcaneCraftingTab::new);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(BackpackClient::nodeTooltip);
        net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerTab(
                thaumicomniverse.compat.backpack.EssentiaPumpContainer.TYPE, EssentiaPumpTab::new);
        net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerTab(
                thaumicomniverse.compat.backpack.EssentiaTankContainer.TYPE, EssentiaTankTab::new);
        try {
            Class.forName("net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryControlBase", false, BackpackClient.class.getClassLoader());
        } catch (ClassNotFoundException legacyApi) {
            Legacy.register();
            return;
        }
        try {
            Class.forName("thaumicomniverse.client.ModernBackpackClient").getMethod("register").invoke(null);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot register the Sophisticated Core inventory control", error);
        }
    }
    private static final class Legacy {
        private static void register() {
            net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager.registerInventoryPart(
                    thaumicomniverse.compat.backpack.EssentiaTankContainer.TYPE, EssentiaTankInventoryPart::new);
        }
    }
    private static void nodeTooltip(net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents event) {
        if(!tc4revival.client.AspectTooltips.enabled()
                ||!(event.getItemStack().getItem() instanceof thaumicomniverse.compat.backpack.EnergizedNodeUpgradeItem))return;
        var rates=thaumicomniverse.compat.backpack.EnergizedNodeUpgradeItem.rates(event.getItemStack());
        var cells=new java.util.ArrayList<tc4revival.client.AspectTooltips.Cell>();
        for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())if(rates.amount(aspect)>0)
            cells.add(new tc4revival.client.AspectTooltips.Cell(aspect.tag(),rates.amount(aspect)+" cv/t"));
        if(cells.isEmpty())return;
        int width=Math.max(24,event.getScreenWidth()/2);
        if(event.getMaxWidth()>0)width=Math.min(width,event.getMaxWidth());
        var data=new tc4revival.client.AspectTooltips.Data(java.util.List.of(
                new tc4revival.client.AspectTooltips.Row(net.minecraft.network.chat.Component.empty(),cells)),width);
        event.getTooltipElements().add(Math.min(1,event.getTooltipElements().size()),com.mojang.datafixers.util.Either.right(data));
    }
}
