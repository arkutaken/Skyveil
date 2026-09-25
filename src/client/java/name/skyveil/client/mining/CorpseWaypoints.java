package name.skyveil.client.mining;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.bazaar.BazaarTooltip;
import name.skyveil.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.scores.DisplaySlot;
import java.util.ArrayList;
import java.util.List;

/** Waypoints use server-supplied entities only; no guessed spawn positions or disk cache. */
public final class CorpseWaypoints {
    private static ClientLevel world;
    private static List<Marker> markers = List.of();
    private static int scanTicks;
    private static boolean registered;
    private CorpseWaypoints() {}
    public static void register() {
        if (registered) return;
        registered = true;
        LevelRenderEvents.BEFORE_GIZMOS.register(CorpseWaypoints::render);
    }
    public static void reset() { world = null; markers = List.of(); scanTicks = 0; }
    public static void tick(Minecraft client) {
        if (!SkyblockSession.isActive() || client.level == null || client.player == null
                || !ConfigManager.get().corpseWaypoints.enabled) { reset(); return; }
        if (world != client.level) { reset(); world = client.level; }
        if (++scanTicks < 5) return;
        scanTicks = 0;
        if (!inMiningArea(client)) { markers = List.of(); return; }
        // Rebuild from currently loaded entities. This automatically drops markers
        // for corpses that despawn or leave the client's loaded world.
        var next = new ArrayList<Marker>();
        for (var entity : world.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand) || stand.isRemoved()) continue;
            var data = stand.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.CUSTOM_DATA);
            if (data == null) continue;
            var type = CorpseType.fromHelmet(BazaarTooltip.attributes(data.copyTag()).getStringOr("id", ""));
            if (type != null && enabled(type)) next.add(new Marker(stand.getId(), type));
        }
        markers = List.copyOf(next);
    }
    private static boolean inMiningArea(Minecraft client) {
        var board = client.level.getScoreboard();
        var objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective != null) for (var entry : board.listPlayerScores(objective)) {
            if (entry.display() != null && CorpseType.isMiningArea(entry.display().getString())) return true;
            var team = board.getPlayersTeam(entry.owner());
            String line = team == null ? entry.ownerName().getString() : team.getFormattedName(entry.ownerName()).getString();
            if (CorpseType.isMiningArea(line)) return true;
        }
        return client.getConnection() != null && CommissionsHud.widgetLines(client).stream().anyMatch(CorpseType::isMiningArea);
    }
    private static boolean enabled(CorpseType type) {
        var config = ConfigManager.get().corpseWaypoints;
        return switch (type) { case LAPIS -> config.lapis; case TUNGSTEN -> config.tungsten; case UMBER -> config.umber; };
    }
    private static void render(LevelRenderContext context) {
        var client = Minecraft.getInstance();
        if (!SkyblockSession.isActive() || client.level == null || client.level != world || client.player == null
                || !ConfigManager.get().corpseWaypoints.enabled || markers.isEmpty()) return;
        var collection = context.levelRenderer().collectPerFrameGizmos();
        try {
            for (var marker : markers) {
                var entity = world.getEntity(marker.entityId());
                if (entity == null || entity.isRemoved() || !enabled(marker.type())) continue;
                var type = marker.type();
                int distance = (int) Math.round(client.player.position().distanceTo(entity.position()));
                Gizmos.cuboid(entity.getBoundingBox().inflate(.15),
                        GizmoStyle.strokeAndFill(type.color, 2f, (type.color & 0xFFFFFF) | 0x30000000)).setAlwaysOnTop();
                Gizmos.billboardText(type.label + " Corpse [" + distance + "m]", entity.position().add(0, 2.2, 0),
                        TextGizmo.Style.forColorAndCentered(type.color).withScale(1f)).setAlwaysOnTop();
            }
        } finally { collection.close(); }
    }
    private record Marker(int entityId, CorpseType type) {}
}
