package name.skyveil.client;

import com.mojang.blaze3d.platform.InputConstants;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.cache.SkyveilCacheManager;
import name.skyveil.client.equipment.EquipmentShortcutRow;
import name.skyveil.client.config.SettingsRegistry;
import name.skyveil.client.gui.SkyveilConfigScreen;
import name.skyveil.client.gui.SearchManager;
import name.skyveil.client.itemprotection.ItemProtectionInputHandler;
import name.skyveil.client.customkeybind.CustomKeybindInputHandler;
import name.skyveil.client.customkeybind.CustomKeybindManager;
import name.skyveil.client.inventorybuttons.InventoryButtonManager;
import name.skyveil.client.bestiary.BestiaryChatFilter;
import name.skyveil.client.combat.CompactDamageManager;
import name.skyveil.client.combat.CompactDamageRenderer;
import name.skyveil.client.hunting.AttributeMenuPanel;
import name.skyveil.client.hunting.AttributeProgressStore;
import name.skyveil.client.hunting.HuntingBoxValuePanel;
import name.skyveil.client.hunting.ShardPriceService;
import name.skyveil.client.pet.PetDisplayHud;
import name.skyveil.client.pet.PetTracker;
import name.skyveil.client.wardrobe.WardrobeKeybindHandler;
import name.skyveil.client.update.ReleaseNoticeManager;
import name.skyveil.client.update.GitHubUpdateManager;
import name.skyveil.client.zoom.ZoomManager;
import name.skyveil.client.storage.StoragePreviewManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SkyveilClientEntrypoint implements ClientModInitializer {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-client");
    private static KeyMapping openMenu;
    private static volatile boolean menuOpenRequested;
    private static boolean initialized;
    @Override public void onInitializeClient() {
        if(initialized){LOGGER.warn("Ignoring duplicate Skyveil client initialization request");return;}
        initialized=true;
        long started=System.nanoTime();
        LOGGER.info("Skyveil client initialization started");
        ConfigManager.load();SkyveilCacheManager.initialize();AttributeProgressStore.initialize();ShardPriceService.initialize();ReleaseNoticeManager.initialize();GitHubUpdateManager.initialize();LOGGER.info("Skyveil configuration and runtime cache loading started");
        SettingsRegistry.registerDefaults();SearchManager.initialize();
        InventoryButtonManager.initialize();CustomKeybindManager.initialize();
        LOGGER.info("Skyveil reusable infrastructure initialized before gameplay");
        CompactDamageRenderer.register();PetDisplayHud.register();
        LOGGER.info("Skyveil HUD features registered");
        ClientLifecycleEvents.CLIENT_STOPPING.register(client->{StoragePreviewManager.shutdown(client);EquipmentShortcutRow.shutdown(client);PetTracker.shutdown(client);AttributeProgressStore.flush();ShardPriceService.flush();SkyveilCacheManager.shutdown();ConfigManager.shutdown();});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{SkyblockSession.disconnect();StoragePreviewManager.disconnect(client);EquipmentShortcutRow.disconnect(client);PetTracker.disconnect(client);CompactDamageManager.clear();});
        KeyMapping.Category category=KeyMapping.Category.register(name.skyveil.Skyveil.INSTANCE.id("keybindings"));
        // In 26.1.2 KeyMapping registers itself; Fabric's former helper is no longer present.
        openMenu=new KeyMapping("key.skyveil.open_menu",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,category);
        ZoomManager.initialize(category);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            SkyblockSession.tick(client);ZoomManager.tick();PetTracker.tick(client);
            if(SkyblockSession.isActive()){
                CompactDamageManager.tick(client);StoragePreviewManager.tick(client);EquipmentShortcutRow.tick(client);AttributeMenuPanel.tick(client);HuntingBoxValuePanel.tick();ItemProtectionInputHandler.tick(client);CustomKeybindInputHandler.tick(client);WardrobeKeybindHandler.tick(client);BestiaryChatFilter.tick(client);ReleaseNoticeManager.tick(client);GitHubUpdateManager.tick(client);
            }else CompactDamageManager.clear();
            if(menuOpenRequested){menuOpenRequested=false;SkyveilConfigScreen.open();}
            while(openMenu.consumeClick())SkyveilConfigScreen.open();
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->{
            dispatcher.register(command("skyveil").executes(ctx->open()).then(command("menu").executes(ctx->open())).then(command("version").executes(ctx->showVersion())).then(command("changelog").executes(ctx->showChangelog())).then(command("update").executes(ctx->update())).then(debugPetCommand()).then(debugHuntingCommand()).then(debugWardrobeCommand()));
            dispatcher.register(command("sv").executes(ctx->open()).then(command("menu").executes(ctx->open())).then(command("version").executes(ctx->showVersion())).then(command("changelog").executes(ctx->showChangelog())).then(command("update").executes(ctx->update())).then(debugPetCommand()).then(debugHuntingCommand()).then(debugWardrobeCommand()));
        });
        LOGGER.info("Skyveil client initialization completed in {} ms",(System.nanoTime()-started)/1_000_000L);
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> command(String name) {
        return com.mojang.brigadier.builder.LiteralArgumentBuilder.literal(name);
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> debugPetCommand(){return command("debugpet").executes(ctx->debugPet()).then(command("cache").executes(ctx->debugPetCache())).then(command("trace").executes(ctx->togglePetTrace()));}
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> debugHuntingCommand(){return command("debughunting").executes(ctx->debugHunting());}
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> debugWardrobeCommand(){return command("debugwardrobe").executes(ctx->debugWardrobe());}
    /**
     * Chat closes its own screen after a submitted command returns. Opening the
     * config screen directly here therefore gets immediately overwritten by
     * that close. Defer the screen change until the next client tick instead.
     */
    private static int open(){menuOpenRequested=true;return 1;}
    private static int showVersion(){var client=net.minecraft.client.Minecraft.getInstance();String version=net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("skyveil").map(container->container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");if(client.player!=null)client.player.sendSystemMessage(Component.literal("[Skyveil] Version "+version));return 1;}
    private static int showChangelog(){return ReleaseNoticeManager.showNow(net.minecraft.client.Minecraft.getInstance())?1:0;}
    private static int update(){return GitHubUpdateManager.requestUpdate(net.minecraft.client.Minecraft.getInstance());}
    private static int debugPet(){var client=net.minecraft.client.Minecraft.getInstance();String text=PetTracker.debugSummary();LOGGER.info("Pet sync diagnostic: {}",text);if(client.player!=null)client.player.sendSystemMessage(Component.literal("[Skyveil] "+text));return 1;}
    private static int debugPetCache(){var client=net.minecraft.client.Minecraft.getInstance();var lines=PetTracker.debugCacheLines();if(client.player!=null){client.player.sendSystemMessage(Component.literal("[Skyveil] Cached pets: "+lines.size()));for(String line:lines)client.player.sendSystemMessage(Component.literal(" - "+line));}return 1;}
    private static int togglePetTrace(){var client=net.minecraft.client.Minecraft.getInstance();boolean enabled=PetTracker.toggleDebugTracing();LOGGER.info("Pet debug tracing {}",enabled?"enabled":"disabled");if(client.player!=null)client.player.sendSystemMessage(Component.literal("[Skyveil] Pet debug tracing "+(enabled?"enabled":"disabled")));return 1;}
    private static int debugHunting(){var client=net.minecraft.client.Minecraft.getInstance();var attributes=AttributeMenuPanel.debugSnapshot();LOGGER.info("Attribute Menu diagnostic: {}",attributes);if(client.player!=null){client.player.sendSystemMessage(Component.literal("[Skyveil] Attribute Menu: title='"+attributes.title()+"' pages="+attributes.visitedPages()+"/"+attributes.totalPages()+" discovered="+attributes.discovered()+" rows="+attributes.rows()+" stabilizing="+attributes.awaitingStableContent()));for(String line:attributes.entries())client.player.sendSystemMessage(Component.literal(" - "+line));}return 1;}
    private static int debugWardrobe(){var client=net.minecraft.client.Minecraft.getInstance();var lines=WardrobeKeybindHandler.debugLines();LOGGER.info("Wardrobe diagnostic: {}",lines);if(client.player!=null){client.player.sendSystemMessage(Component.literal("[Skyveil] Wardrobe diagnostic"));for(String line:lines)client.player.sendSystemMessage(Component.literal(" - "+line));}return 1;}
}
