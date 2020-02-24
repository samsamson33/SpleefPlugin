package io.github.samsamson33.spleef

import co.aikar.commands.PaperCommandManager
import io.github.samsamson33.spleef.commands.SpleefCommand
import org.bukkit.plugin.java.JavaPlugin

class SpleefPlugin : JavaPlugin() {
	private lateinit var commandManager: PaperCommandManager
	lateinit var gameManager: GameManager

	override fun onEnable() {
		commandManager = PaperCommandManager(this)
		commandManager.commandReplacements.addReplacements(
				"spleef", "spleef"
		)

		commandManager.registerCommand(SpleefCommand(this))

		gameManager = GameManager(this)
	}

	override fun onDisable() {
		gameManager.save()
	}
}

