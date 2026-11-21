package tc4revival.data;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import tc4revival.TC4Revival;
import tc4revival.content.ShardType;

public  class TCDataGeneration {
    private TCDataGeneration() {}
    private static net.minecraft.data.DataProvider named(String name,net.minecraft.data.DataProvider delegate) {
        return new net.minecraft.data.DataProvider() {
            @Override public String getName() { return name; }
            @Override public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput output) { return delegate.run(output); }
        };
    }

    public static void gather(GatherDataEvent event) {
        // External textures exist at runtime, but deliberately do not exist in packaged resources.
        try {
            for (String path : tc4revival.resources.OriginalResources.mappings().keySet()) {
                var parts = path.split("/", 3);
                if (parts[2].startsWith("textures/") && parts[2].endsWith(".png")) {
                    String sprite = parts[2].substring("textures/".length(), parts[2].length() - 4);
                    event.getExistingFileHelper().trackGenerated(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(parts[1], sprite),
                            net.minecraft.server.packs.PackType.CLIENT_RESOURCES, ".png", "textures");
                }
            }
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot declare external textures for data generation", failure);
        }
        for(String texture:java.util.List.of("cinnabar","amber","infused"))event.getExistingFileHelper().trackGenerated(
                net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/deepslate_"+texture+"_overlay"),net.minecraft.server.packs.PackType.CLIENT_RESOURCES,".png","textures");
        var generator = event.getGenerator();
        generator.addProvider(event.includeServer(),new TreeFeatureProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(),new TCBiomeProvider(generator.getPackOutput()));
        generator.addProvider(event.includeClient(),named("TC4 eldritch block models",new EldritchResources(generator.getPackOutput(),event.getExistingFileHelper())));
        generator.addProvider(event.includeServer(),named("TC4 eldritch block loot",EldritchResources.loot(generator.getPackOutput())));
        generator.addProvider(event.includeServer(),new OuterLandsProvider(generator.getPackOutput()));
        generator.addProvider(event.includeClient(),named("TC4 taint block models",new TaintResources(generator.getPackOutput(),event.getExistingFileHelper())));
        generator.addProvider(event.includeServer(),named("TC4 taint block loot",TaintResources.loot(generator.getPackOutput())));
        generator.addProvider(event.includeClient(), new ConnectedTextureProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new FocusDamageProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new WandTestStructureProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new ArcaneRecipeProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new CrucibleRecipeProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new InfusionRecipeProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new BaubleSlotProvider(generator.getPackOutput(), event.getExistingFileHelper(), event.getLookupProvider()));
        generator.addProvider(event.includeServer(), new TCRecipeProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), TableResources.loot(generator.getPackOutput()));
        var blockTags = new TableBlockTags(generator.getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper());
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new MechanismItemTags(generator.getPackOutput(), event.getLookupProvider(), blockTags.contentsGetter(), event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new TableResources(generator.getPackOutput(), event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new ResearchCatalogProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new ResearchPagesProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new ResearchPageRecipesProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new ResearchDisplayItemsProvider(generator.getPackOutput()));
        generator.addProvider(event.includeClient(), new ResearchTextResources(generator.getPackOutput(), event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new EntityAspectProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new ItemAspectProvider(generator.getPackOutput()));
        generator.addProvider(event.includeClient() || event.includeServer(),new CleansingResources(generator.getPackOutput()));
        generator.addProvider(event.includeClient() || event.includeServer(),new FortressResources(generator.getPackOutput()));
        generator.addProvider(event.includeClient() || event.includeServer(),new UtilityDeviceResources(generator.getPackOutput()));
        generator.addProvider(event.includeClient() || event.includeServer(),new ArcaneLampResources(generator.getPackOutput()));
        generator.addProvider(event.includeClient() || event.includeServer(),new ArcaneBoreResources(generator.getPackOutput()));
        generator.addProvider(event.includeClient() || event.includeServer(),new GolemResources(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new RecipeAspectProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new LegacyItemMappingProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new TCItemMappingProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new EquipmentAspectProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new InfernalBonusProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new PotionAspectProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new ModernLootProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new LegacyDungeonLootProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new VillageResourcesProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new VillagerTradeProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new PechTradeProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new GolemFishingProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new BlockScanProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new TableScanProvider(generator.getPackOutput()));
        generator.addProvider(event.includeServer(), new LegacyItemTagProvider(generator.getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new DebugLanguage(generator.getPackOutput(), "en_us"));
        generator.addProvider(event.includeClient(), new DebugLanguage(generator.getPackOutput(), "zh_cn"));
        generator.addProvider(event.includeClient(), new TCItemModelProvider(generator.getPackOutput(), event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new TCTextureMetadataProvider(generator.getPackOutput()));
        generator.addProvider(event.includeClient(), new TCSoundProvider(generator.getPackOutput()));
        generator.addProvider(event.includeClient(),new ModernVisualDataProvider(generator.getPackOutput()));
    }

    private static  class DebugLanguage extends LanguageProvider {
        private final boolean chinese;

        private DebugLanguage(PackOutput output, String locale) {
            super(output, TC4Revival.MOD_ID, locale);
            chinese = locale.equals("zh_cn");
        }

        @Override
        protected void addTranslations() {
            try (var input = java.util.Objects.requireNonNull(TCDataGeneration.class.getResourceAsStream(
                    "/thaumcraft/tutorial_lang/" + (chinese ? "zh_cn" : "en_us") + ".json"))) {
                com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject().entrySet().forEach(entry -> add(entry.getKey(), entry.getValue().getAsString()));
            } catch (java.io.IOException failure) { throw new IllegalStateException("Missing tutorial translations", failure); }
            if(chinese) {
                add("item.Wand.ice.rod","寒霜");
                add("item.Wand.bone.rod","白骨");
                add("tc.research_page.COREHARVEST.1","安装这种核心的傀儡并不需要与容器绑定.他们会搜寻可收获的小麦,甘蔗或者其他常见农作物并收获它们.收获后的作物会被扔在地上,因此必须使用其他方式收回这些收获后的作物.<BR>不幸的是它们还没聪明到学会自动重新播种收获过的作物,但你相信再经过一点儿调试工作,你最终能够修复这个bug.");
            }
            add("thaumcraft.infusion.recovery_failed",chinese?"注魔存档存在损坏或缺失内容，已保留原事务。请修复相关数据或模组后再次使用法杖重试。":"The saved infusion contains invalid or missing data. Its transaction is preserved. Restore the data or missing mod, then use your wand to retry.");
            // The original TC4 zh_CN.lang omits this page; retain legacy keys and supply its missing translation.
            if(chinese)add("tc.research_page.TUBES.4","用法杖点击管道的一段接口，可以连接或断开它与相邻管道的连接。<BR>用法杖点击管道的中央部分，还能旋转阀门等具有方向的管道。");
            add("thaumcraft.infusion.recovery_restored",chinese?"已恢复原注魔事务。":"The saved infusion transaction has been restored.");
            add("thaumcraft.research.recipe_unavailable",chinese?"当前数据包中无可用配方":"No recipe in this datapack");
            add("thaumcraft.auto_research.inventory_full",chinese?"背包已满，发现已留在研究台中，批量研究已停止。":"Inventory full. The discovery remains on the research table; batch research has stopped.");
            add("container.thaumcraft.hungry_chest",chinese?"贪婪箱":"Hungry Chest");
            add("item.thaumcraft.thaumonomicon.cheat",chinese?"作弊指南":"Cheat Sheet");
            add("thaumcraft.mirror.already_linked",chinese?"这面镜子已连接至有效的目的地。":"That mirror is already linked to a valid destination.");
            add("thaumcraft.mirror.linked_to",chinese?"已连接至":"Linked to");
            add("thaumcraft.mirror.destination",chinese?"%s,%s,%s（%s）":"%s,%s,%s in %s");
            add("thaumcraft.golem.not_master",chinese?"你不是我的主人！":"You are not my Master!");
            add("thaumcraft.ward.pearl_blocked",chinese?"附近守卫物体的魔力摧毁了末影珍珠。":"The magic of a nearby warded object destroys the ender pearl.");

            String[] deepZh={"深层朱砂矿石","深层风之蕴魔石","深层火之蕴魔石","深层水之蕴魔石","深层地之蕴魔石","深层秩序蕴魔石","深层混沌蕴魔石","深层琥珀矿石"};
            String[] deepEn={"Deepslate Cinnabar Ore","Deepslate Air Infused Stone","Deepslate Fire Infused Stone","Deepslate Water Infused Stone","Deepslate Earth Infused Stone","Deepslate Order Infused Stone","Deepslate Entropy Infused Stone","Deepslate Amber Ore"};
            for(int ore=0;ore<8;ore++)add("block.thaumcraft."+tc4revival.content.DeepslateOreBlock.name(ore),chinese?deepZh[ore]:deepEn[ore]);
            add("attribute.name.thaumcraft.vis_cost",chinese?"魔力消耗":"Vis consumption");
            add("attribute.name.thaumcraft.potency",chinese?"潜能":"Potency");
            add("thaumcraft.golem.axis_aligned",chinese?"坐标轴对齐":"Align to axes");
            add("thaumcraft.wand.automation_only",chinese?"已安装灵魂核心：自动化使用绑定者研究":"Soul core installed: automation uses bound research");
            add("thaumcraft.focal.stored_vis",chinese?"存储魔力":"Stored Vis");
            add("thaumcraft.research.sort.default",chinese?"默认排序":"Default order");add("thaumcraft.research.sort.order",chinese?"按阶数":"Aspect order");add("thaumcraft.research.sort.quantity",chinese?"按数量":"Quantity");
            add("thaumcraft.research.ascending",chinese?"正向排序":"Ascending");add("thaumcraft.research.descending",chinese?"反向排序":"Descending");
            add("thaumcraft.research.auto_refill",chinese?"自动装填":"Auto refill");add("thaumcraft.research.random",chinese?"随机研究":"Random research");
            add("tc.research_name.REVIVAL_LISTENING_GUIDANCE",chinese?"聆听指引":"Listening for Guidance");
            add("tc.research_text.REVIVAL_LISTENING_GUIDANCE",chinese?"冥冥之中":"Let a faint insight point the way");
            add("tc.research_page.REVIVAL_LISTENING_GUIDANCE.1",chinese?"你越来越相信，知识本身是会说话的。那些尚未解开的研究并非沉默不语，只是它们的声音太过微弱，需要你静下心来侧耳倾听。<BR>如今翻阅魔导手册时，同一页上那些你已经可以着手的直接前置研究会自行浮现，不需要任何代价；左键点击，便能翻到其中的第一项。<BR>倘若前置都已满足，某项研究却依旧对你紧闭大门，你可以左键点击它，献出少许要素来聆听指引：常规研究的每种初始要素各消耗一点，次要研究的每种花费要素各消耗一点，最多三种。就连借助配方查看器（JEI）临时找到的隐藏研究，也同样可以聆听。<LINE>指引会顺着那些你尚无法着手的前置一路追溯下去，直到找到一项你此刻就能研究的课题，或是一个需要你亲自去满足的条件——扫描某种要素、物品或生物，又或是积累扭曲。<BR>每项研究同一时间只会给出一条未完成的指引。指向研究的指引可以左键跳转，其余的条件就只能靠你自己去达成了；完成之后，你可以再次聆听。间接的前置会注明阶数：直接前置的前置为 2 阶，依此类推。<BR>配方查看器里会列出已完成的前置、同页可着手的直接前置以及当前的指引，其余尚未完成的部分只会显示为问号。":"Direct prerequisites on the same Thaumonomicon page that are currently researchable are shown for free. Left-click to locate the first one. Once these are satisfied, left-click research that is still locked to listen for guidance, including hidden research temporarily located from JEI. Pay one point per initial note aspect, or one point per secondary-research cost aspect, up to three.<BR><BR>Guidance follows unavailable research prerequisites until it finds research you can pursue, or a condition such as scanning an aspect, item or creature, or experiencing warp. Each research keeps one unfinished clue. Left-click a research clue to locate it; other conditions cannot be followed. Complete the condition before listening again. Indirect prerequisites show their depth: a prerequisite of a direct prerequisite is depth 2.<BR><BR>JEI shows completed prerequisites, researchable direct prerequisites on the same page and your current clue. Other unfinished conditions remain unknown.");
            add("thaumcraft.guidance.listen",chinese?"左键以聆听指引":"Left-click to listen for guidance");
            add("thaumcraft.guidance.prerequisite",chinese?"前置：%s":"Prerequisite: %s");
            add("thaumcraft.guidance.indirect_prerequisite",chinese?"%s 阶前置：%s":"Depth %s prerequisite: %s");
            add("thaumcraft.guidance.jump",chinese?"（点击以跳转）":" (click to locate)");
            add("thaumcraft.jei.scan_aspect",chinese?"扫描要素：%s":"Scan aspect: %s");
            add("thaumcraft.guidance.warp",chinese?"永久与普通扭曲合计达到 %s，并等待扭曲事件":"Reach %s permanent and normal warp combined, then wait for a warp event");
            add("thaumcraft.guidance.crimson",chinese?"阅读红字典籍":"Read the Crimson Rites");
            add("thaumcraft.guidance.outer",chinese?"通过邪术传送门进入外域":"Enter the Outer Lands through an Eldritch Portal");
            add("thaumcraft.guidance.discover",chinese?"发现线索：%s":"Discover a clue: %s");
            add("thaumcraft.research.synthesize",chinese?"点击以合成":"Click to synthesize");
            add("thaumcraft.tooltip.infusion_stabilizer",chinese?"为注魔祭坛提供稳定性":"Provides stability to infusion altars");
            add("thaumcraft.jei.book_creation",chinese?"制作铁杖端木杖柄法杖，并右键任意书架。":"Craft an iron-capped wooden wand and right-click any bookshelf.");
            add("thaumcraft.jei.book_creation_title",chinese?"魔导手册":"Thaumonomicon");
            add("tc.research_name.REVIVAL_RESEARCH_ASSISTANCE",chinese?"研究助手":"Research Assistance");
            add("tc.research_text.REVIVAL_RESEARCH_ASSISTANCE",chinese?"磨刀不误砍柴工":"Keeping paper and ink at hand");
            add("tc.research_page.REVIVAL_RESEARCH_ASSISTANCE.1",chinese?"研究做得越多，你的研究台就越是被凌乱的手稿、空墨水瓶和揉皱的纸团淹没。你终于下定决心，花些工夫好好整理一番。<BR>你为每一种要素编好了名录，从此只需写下名字，就能在研究台上找出那些寻常的要素。你还给研究台定下了规矩：只要你亲自勾选§5自动装填§0，它便会记住你已完成的笔记，并从你的物品栏里自行补充纸张与墨水。<BR>灵感枯竭的时候，也不妨让命运替你挑选下一个课题。§5随机研究§0照样要消耗材料，也照样要求你满足前置研究——命运可不会替你付账。<LINE>至于滚轮与数字键之类的小技巧，人人都会，并不需要这项研究。（服务器管理者可以分别调整以上各项功能。）":"You can now search ordinary aspect names at the research table. Opt into auto refill to learn completed notes and replenish paper and ink from your inventory. Random research still requires its normal materials and prerequisites.<BR><BR>Wheel and number-key controls are basic operations, independent of this research. Each assistance feature can be configured separately.");
            add("tc.research_name.REVIVAL_ASPECT_RELATIONS",chinese?"要素关联":"Aspect Relations");
            add("tc.research_text.REVIVAL_ASPECT_RELATIONS",chinese?"顺藤摸瓜":"Following the links");
            add("tc.research_page.REVIVAL_ASPECT_RELATIONS.1",chinese?"缸中之脑整天在罐子里嘟囔个不停。你本以为那不过是些无意义的呓语，直到某天你注意到，它念叨的要素名称总是成群结队地出现——每一种要素都与另外几种要素有着千丝万缕的联系，就像一本家谱里的亲族。<BR>你把这些关系整理成了一张谱系图。如今只需用中键点击某个要素，或在搜索框中输入“~”再加上要素的名称，就能查出所有与它直接相连的要素——当然，也包括它自己。<BR>中键要在松开时才算一次点击，因此拖动时并不会顺手触发搜索。<LINE>至于怎样让这些联系自己连成一条完整的路径，你觉得还需要更深入的研究。":"Middle-click an aspect, or type ~ followed by its name, to find directly related aspects. The result includes the target itself.<BR><BR>A click is confirmed when the middle button is released; dragging does not also trigger a search. Connection assistance is a separate research.");
            add("tc.research_name.REVIVAL_RESEARCH_CONNECTIONS",chinese?"融会贯通":"Research Connections");
            add("tc.research_text.REVIVAL_RESEARCH_CONNECTIONS",chinese?"牵一发而动全身":"Connecting a chain of ideas");
            add("tc.research_page.REVIVAL_RESEARCH_CONNECTIONS.1",chinese?"凝视元始珍珠的那些夜里，你看见万物的要素彼此缠绕，交织成一张无边无际的网。现实中的每一根丝线都牵连着别的丝线——只要找准线头，便能顺着它一路摸到终点。<BR>如今，在研究笔记上按住中键拖动，你的脑海里就会浮现出一条连线的预览；松开之后，你会按照寻常的规矩，一格一格地把要素填上去。<BR>这算不上什么捷径。你仍然需要认识这些要素，需要足够的研究点，也只能把它们填进合法的空格里。它不会替你完成研究，更不会凭空变出任何资源。<LINE>（若服务器只启用了这一阶段，那么被关闭的前两项研究不会成为它的阻碍。）":"The Primordial Pearl suggests a way to organize complex aspect relations into paths. Hold and drag the middle button across research notes to preview a connection; release it to place aspects one at a time under the normal rules.<BR><BR>You still need known aspects, sufficient research points and legal free cells. No research or resource costs are bypassed. Disabled earlier assistance stages do not prevent this stage from being reached.");
            add("thaumcraft.research.auto_connect",chinese?"自动连线（中键拖动）":"Auto-connect (middle-button drag)");
            add("thaumcraft.research.located",chinese?"已定位：%s（完成后可阅读）":"Located: %s (complete it to read)");
            add("thaumcraft.gui.insufficient_vis",chinese?"灵气不足":"Insufficient vis");
            add("thaumcraft.bore.width",chinese?"宽度：%s":"Width: %s");
            add("thaumcraft.bore.speed",chinese?"速度：+%s":"Speed: +%s");
            add("thaumcraft.bore.properties",chinese?"其他属性：":"Other properties:");
            add("thaumcraft.bore.clusters",chinese?"原生矿簇":"Native Clusters");
            add("thaumcraft.research.size.normal",chinese?"正常":"Normal");
            add("thaumcraft.research.size.maximum",chinese?"最大":"Maximum");
            add("thaumcraft.research.mirror_jump",chinese?"点击以跳转至研究":"Click to locate the original research");
            add("thaumcraft.research.creative_complete",chinese?"鼠标中键：完成此研究及全部前置研究":"Middle-click: complete this research and its prerequisites");
            add("thaumcraft.research.hidden_suffix",chinese?"（隐藏）":" (Hidden)");
            add("thaumcraft.research.creative_unlock",chinese?"（中键解锁）":" (Middle-click to unlock)");
            add("thaumcraft.research.locked_preview",chinese?"你尚未完成这项研究。<BR><BR>返回研究树，满足前置条件并取得研究笔记，或按提示消耗要素完成研究后，即可阅读完整内容。":"You have not completed this research.<BR><BR>Return to the research tree, meet its prerequisites, and obtain its research notes or spend the requested aspects. Complete the research to read its full contents.");
            add("thaumcraft.research.aspect_search",chinese?"搜索要素…":"Search...");
            add("thaumcraft.research.search_mode.0",chinese?"搜索策略：仅原名（点击切换）":"Search: original names (click to cycle)");
            add("thaumcraft.research.search_mode.1",chinese?"搜索策略：仅译名（点击切换）":"Search: translated names (click to cycle)");
            add("thaumcraft.research.search_mode.2",chinese?"搜索策略：原名与译名（点击切换）":"Search: original and translated names (click to cycle)");
            add("thaumcraft.research.aspect_search.help",chinese?"输入要素原名或译名；原名英文仅匹配前缀，忽略大小写。~ 前缀搜索可直接连接的要素。空格或右键清空搜索并取消要素选择。安装 JustEnoughCharacters 后支持译名拼音。":"Search original or translated aspect names. Latin original names match prefixes, ignoring case. Prefix ~ finds directly related aspects. Space or right-click clears the search and aspect selection. JustEnoughCharacters enables pinyin matching of translated names.");
            add("thaumcraft.research.shortcuts",chinese?"快捷操作：%s":"Shortcuts: %s");
            add("thaumcraft.research.shortcuts.help",chinese?"滚轮或下方箭头切换选中列，数字 1–5 选择对应行要素；再次按同一数字取消。左键放置一次后取消选中，点空不会取消；拖动要素优先。右键可擦除笔记上的已放置要素，同时清空搜索。中键点击列表上的已知要素搜索其关联要素。在笔记上按住中键拖动可预览自动连线，松开后依次填入；负片格子不可填入。失焦时输入字母或 ~ 会清空旧搜索并开始新搜索。空格或右键同时清空搜索与要素选择。清空搜索恢复原列。Esc 关闭，E 不关闭。关闭快捷操作后保留搜索、搜索清空和原有拖放翻页，隐藏列框与数字。":"Wheel or arrows select a column. Keys 1\u20135 select a row; press the same key again to cancel. Left-click places once and clears the selection; invalid clicks keep it. Dragging takes priority. Right-click erases a placed aspect on the note and clears the search. Middle-click a known aspect in the list to search related aspects. Hold and drag the middle mouse button over the note to preview an automatic connection; release to place it. Negative cells cannot be filled. Typing a letter or ~ while unfocused clears the old query and starts a new search. Space or right-click clears both search and aspect selection. Clearing search restores the previous column. Esc closes; E does not. When disabled, search, clearing, original dragging and paging remain, while column highlighting and row labels are hidden.");
            add("thaumcraft.research.search",chinese?"搜索研究…":"Search research...");add("thaumcraft.research.no_results",chinese?"没有匹配的研究":"No matching research");
            for(var color:net.minecraft.world.item.DyeColor.values())add("block.minecraft.banner.thaumcraft.crimson."+color.getName(),chinese?"血腥教徒纹样":"Crimson cult sigil");
            add("thaumcraft.tooltip.stored_vis",chinese?"存储魔力":"Stored vis");
            add("thaumcraft.label.choose",chinese?"右键以选择标记要素":"Right-click to select a label aspect");
            add("thaumcraft.label.one",chinese?"左键点击标记一个标签":"Left-click to mark one label");
            add("thaumcraft.label.all",chinese?"Shift 左键标记全部标签":"Shift-left-click to mark all held labels");
            add("thaumcraft.infusion.place.none",chinese?"5 格内没有符文矩阵":"No runic matrix within 5 blocks");
            add("thaumcraft.infusion.place.multiple",chinese?"5 格内有多个符文矩阵":"Multiple runic matrices within 5 blocks");
            add("thaumcraft.infusion.place.busy",chinese?"祭坛结构不完整或正在注魔":"Altar incomplete or infusion in progress");
            add("thaumcraft.infusion.place.materials",chinese?"物品不足":"Missing ingredients");
            add("thaumcraft.infusion.place.pedestals",chinese?"奥术基座数量不足":"Not enough arcane pedestals");
            add("thaumcraft.infusion.place.denied",chinese?"无法操作此处的祭坛":"Cannot access this altar");
            add("thaumcraft.infusion.place.recipe",chinese?"此配方当前不可用":"Recipe is unavailable");
            add("thaumcraft.infusion.place.done",chinese?"注魔材料已放置":"Infusion ingredients placed");
            add("thaumcraft.infusion.repeat",chinese?"空手右键以重复上一个配方":"Right-click with an empty hand to repeat the last recipe");
            add("thaumcraft.tooltip.filter",chinese?"过滤要素":"Filtered aspect");
            add("thaumcraft.jei.arcane",chinese?"奥术合成":"Arcane crafting");
            add("thaumcraft.jei.crucible",chinese?"坩埚炼金":"Crucible alchemy");
            add("thaumcraft.jei.infusion_enchantment",chinese?"注魔附魔":"Infusion Enchanting");
            add("thaumcraft.jei.enchantment_levels",chinese?"附魔等级：%s → %s":"Enchantment: %s \u2192 %s");
            add("thaumcraft.jei.enchantment_reference",chinese?"附魔书仅表示目标附魔；实际对兼容装备施法，不制作附魔书。其他附魔可能增加消耗或不稳定性。加强模式下，每份世界盐增加一级及一倍基础源质；超过原版上限另需虚空种子。":"Reference only: apply to compatible equipment, not books. Other enchantments may increase costs or instability. Enhanced mode: each salis mundus adds one level and one base essentia cost; exceeding the vanilla maximum also needs a void seed.");
            add("thaumcraft.jei.infusion",chinese?"注魔":"Infusion");
            add("thaumcraft.jei.aspect_combine",chinese?"要素合成":"Aspect combination");
            add("thaumcraft.jei.item_aspects",chinese?"物品要素":"Item aspects");
            add("thaumcraft.jei.amount",chinese?"数量：%s":"Amount: %s");
            add("thaumcraft.jei.aspect_name",chinese?"要素：%s":"Aspect: %s");
            add("thaumcraft.thaumometer.ignore_fluids",chinese?"按住 Shift 扫描：忽略流体，观察其后的目标":"Hold Shift while scanning to look through fluids");
            add("thaumcraft.jei.instability",chinese?"不稳定性 %s；经验等级 %s":"Instability %s; XP levels %s");
            add("thaumcraft.jei.unknown",chinese?"尚未发现这些要素":"These aspects are undiscovered");
            add("thaumcraft.jei.empty",chinese?"此物品没有要素":"This item has no aspects");
            add("thaumcraft.jei.dynamic",chinese?"消耗随中心物品的附魔和强化变化":"Costs depend on the central item upgrades");
            add("thaumcraft.jei.research",chinese?"所需研究：%s":"Research: %s");
            add("thaumcraft.jei.research_category",chinese?"研究":"Research");
            add("thaumcraft.jei.research_name",chinese?"研究：%s":"Research: %s");
            add("thaumcraft.jei.scan_item",chinese?"扫描物品：%s":"Scan item: %s");
            add("thaumcraft.jei.scan_special",chinese?"扫描方块：%s":"Scan block: %s");
            add("thaumcraft.jei.scan_entity",chinese?"扫描生物：%s":"Scan entity: %s");
            add("thaumcraft.jei.discover_aspect",chinese?"发现要素：%s":"Discover aspect: %s");
            add("thaumcraft.jei.any_parent",chinese?"（任选一项研究）":" (any one research)");
            add("thaumcraft.jei.any_clue",chinese?"（任选一种线索）":" (any one clue)");
            add("thaumcraft.jei.requirements_omitted",chinese?"（省略 %s 项已完成，%s 项未完成）":"(Omitted: %s complete, %s incomplete)");
            add("thaumcraft.jei.no_requirements",chinese?"无前置条件":"No prerequisites");
            add("thaumcraft.jei.research_keys",chinese?"按 %s/%s 在 JEI 中查看":"Press %s/%s to view in JEI");
            add("thaumcraft.jei.ctrl_research",chinese?"按 Ctrl + 左键跳转到魔导手册":"Ctrl + left-click to open the Thaumonomicon");
            add("thaumcraft.jei.stability",chinese?"不稳定性：%s":"Instability: %s");
            add("thaumcraft.jei.experience",chinese?"经验等级消耗：%s":"Experience levels: %s");
            add("thaumcraft.research.eldritch_sealed",chinese?"帷幕彼端的低语，尚非你能理解。":"The whispers beyond the veil remain beyond your understanding.");
            add("thaumcraft.research.ponder",chinese?"思索":"Ponder");
            add("thaumcraft.book.play",chinese?"播放":"Play");
            add("thaumcraft.book.pause",chinese?"暂停":"Pause");
            add("thaumcraft.book.replay",chinese?"重播":"Replay");
            add("thaumcraft.research.copper_processing",chinese?"若说哪种金属最能体会“让事情变得更糟”的滋味，那非铜莫属。<BR>你发现，把铜制方块连同 1 点§8Perditio§0一起投进坩埚，它便会像在风雨里搁了许多年一样，氧化程度一下子上升一级，表面的蜡层却完好无损。反过来，若投入的是涂了蜡的铜制方块，再加上 1 点§3Aqua§0，蜡层就会被冲刷干净，而铜锈分毫不变。<LINE>你还意外地发现，震击核心的电弧每次击中铜制方块，都能剥去一层锈迹，同时保留方块原本的形状与蜡层。":"Copper reveals the power of entropy. One point of Perditio in a Crucible advances a copper block by one oxidation stage, retaining any wax coating. One point of Aqua removes wax without changing oxidation.<BR><BR>A Shock focus hit removes one oxidation stage while preserving the block shape and any wax coating.");
            add("thaumcraft.research.item_lookup",chinese?"点击或在物品上按住 Ctrl 查看研究":"Click, or hold Ctrl over the item, to read its research");
            add("thaumcraft.jei.open_research",chinese?"点击在魔导手册中定位研究":"Click to locate research in the Thaumonomicon");
            add("thaumcraft.jei.book_required",chinese?"背包中需要一本魔导手册":"A Thaumonomicon is required in your inventory");
            add("thaumcraft.inventory_scan.hint",chinese?"可在背包中扫描物品（按住 Shift 查看说明）":"Inventory scanning available (hold Shift for details)");
            add("thaumcraft.inventory_scan.help",chinese?"用鼠标拿起魔导透镜，悬停在物品或玩家模型上进行扫描。":"Pick up the Thaumometer with the cursor and hover over an item or your player model to scan it.");
            add("thaumcraft.inventory_scan.scanning",chinese?"扫描中":"Scanning");
            for(var aspect:tc4revival.aspect.AspectDefinitions.bundled().primals())
                add("attribute.name.thaumcraft."+aspect.tag()+"_vis_cost",aspect.displayName()+(chinese?" 魔力消耗":" vis consumption"));
            add("thaumcraft.toast.research_complete",chinese?"研究完成！":"Research completed!");
            add("block.thaumcraft.aura_node",chinese?"灵气节点":"Aura Node");
            add("block.thaumcraft.energized_node",chinese?"充能节点":"Energized Node");
            add("item.thaumcraft.gold_coin",chinese?"金币":"Gold Coin");
            add("item.thaumcraft.aura_node.place",chinese?"放置一个随机生成的灵气节点":"Place a randomly generated node");
            add("item.thaumcraft.aura_node.creative",chinese?"仅创造模式":"Creative Mode Only");
            add("tc.book.structure.empty",chinese?"空白空间":"Empty Space");
            add("tc.book.structure.node",chinese?"充能灵气节点":"Energized Aura Node");
            add("entity.thaumcraft.wizard",chinese?"巫师":"Wizard");
            add("entity.thaumcraft.banker",chinese?"银行家":"Banker");
            add("entity.minecraft.villager.wizard",chinese?"巫师":"Wizard");
            add("entity.minecraft.villager.banker",chinese?"银行家":"Banker");
            add("entity.minecraft.villager.thaumcraft.wizard",chinese?"巫师":"Wizard");
            add("entity.minecraft.villager.thaumcraft.banker",chinese?"银行家":"Banker");
            add("thaumcraft.infusion.risk", chinese ? "风险 %s" : "Risk %s");
            add("thaumcraft.infusion.stability", chinese ? "稳定性 %s" : "Stability %s");
            add("key.thaumcraft.change_focus", chinese ? "更换法杖核心" : "Change Wand Focus");
            add("key.thaumcraft.change_area", chinese ? "法杖其他功能切换" : "Misc Wand Toggle");
            add("key.thaumcraft.hover", chinese ? "切换悬浮背带" : "Toggle Hover Harness");
            for (var shard : ShardType.values()) add("item.thaumcraft." + shard.path(), shard.name(chinese));
            add("block.thaumcraft.magic_box",chinese ? "魔法箱" : "Magic Box");
            add("item.thaumcraft.thaumometer", chinese ? "魔导透镜" : "Thaumometer");
            // Original translations are supplied at runtime by the external resource pack.
            add("commands.thaumcraft.debug.environment", chinese
                    ? "TC4Revival：%s；维度 %s；在线玩家 %s；Curios %s"
                    : "TC4Revival: %s; dimension %s; players %s; Curios %s");
            add("commands.thaumcraft.debug.iron.pass", chinese
                    ? "铁粒检查通过：原版铁粒及 Forge 材料标签正确，无重复 TC4 铁粒。"
                    : "Iron mapping passed: vanilla item and Forge material tag, with no duplicate TC4 nugget.");
            add("block.thaumcraft.eldritch_carved_stone",chinese ? "雕纹石" : "Carved Stone");
            add("commands.thaumcraft.debug.iron.fail", chinese
                    ? "铁粒检查失败：%s"
                    : "Iron mapping failed: %s");
        }
    }
}
