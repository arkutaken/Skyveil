package name.skyveil

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier

object Skyveil : ModInitializer {
	const val MOD_ID: String = "skyveil"

	override fun onInitialize() {
		// Client features are registered by SkyveilClientEntrypoint.
	}

	// Keep resource, HUD and keybinding identifiers in one namespace; callers pass
	// only the path so renaming a feature cannot accidentally select minecraft:.
	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
