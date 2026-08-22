package name.skyveil

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier

object Skyveil : ModInitializer {
	const val MOD_ID: String = "skyveil"

	override fun onInitialize() {
		// Client features are registered by SkyveilClientEntrypoint.
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
