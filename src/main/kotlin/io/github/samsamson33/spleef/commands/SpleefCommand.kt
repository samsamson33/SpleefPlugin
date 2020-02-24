package io.github.samsamson33.spleef.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Subcommand
import io.github.samsamson33.spleef.SpleefPlugin
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("spleef")
class SpleefCommand(val plugin: SpleefPlugin) : BaseCommand() {
	@Subcommand("reload")
	@CommandPermission("spleef.reload")
	@Description("Queue yourself for the next spleef game")
	fun reloadSubcommand(sender: CommandSender) {
		sender.sendMessage("Reloading...")
		plugin.reloadConfig()
		plugin.gameManager.reload()
		sender.sendMessage("Reloaded!")
	}

	@Subcommand("join")
	@CommandPermission("spleef.join")
	@Description("Remove yourself from the spleef queue")
	fun joinSubcommand(sender: Player) {
		if (!plugin.gameManager.players.add(sender)) {
			sender.sendMessage("You've already joined!")
		} else {
			sender.sendMessage("You've joined the game")
		}
	}

	@Subcommand("leave")
	@Description("Remove yourself from the spleef queue")
	fun leaveSubcommand(sender: Player) {
		if (plugin.gameManager.players.remove(sender)) {
			sender.sendMessage("You've left the game")
		} else {
			sender.sendMessage("You hadn't joined")
		}

	}

	@Subcommand("players")
	@CommandPermission("spleef.manage")
	@Description("List spleef players")
	fun playersSubcommand(sender: CommandSender) {
		val joinedPlayers: List<OfflinePlayer> = plugin.gameManager.players.get()

		if (joinedPlayers.isEmpty()) {
			sender.sendMessage("No players!")
		} else {
			sender.sendMessage("Players: ${joinedPlayers.joinToString(separator = ", ") { player -> (
					if (player.isOnline)
						ChatColor.GREEN.toString()
					else
						ChatColor.DARK_GRAY.toString() + ChatColor.ITALIC.toString()
			) + player.name }}")
		}
	}

	@Subcommand("start")
	@CommandPermission("spleef.manage")
	@Description("Starts the game")
	fun startSubcommand(sender: CommandSender) {
		plugin.gameManager.startGame()
		sender.sendMessage("Started the game!")
	}

	@Subcommand("config")
	@Description("View spleef configuration")
	fun configSubcommand(sender: CommandSender) {
		// TODO: this says "Not Set" if the config is invalid
		val placeholder = "${ChatColor.DARK_GRAY}[Not Set]${ChatColor.RESET}"
		fun stringify(location: Location?):String? {
			return if (location == null) null else "[${location.x}, ${location.y}, ${location.z}]"
		}

		val world = plugin.gameManager.arena.getWorld()?.name ?: placeholder
		val location = stringify(plugin.gameManager.arena.getLocation()) ?: placeholder
		val size = plugin.gameManager.arena.getSize() ?: placeholder
		val gamemode = plugin.gameManager.getGamemode().displayName

		sender.sendMessage(
				"${ChatColor.RED}Spleef Configuration:\n" +
						"    ${ChatColor.BLUE}Arena world: ${ChatColor.AQUA}$world\n" +
						"    ${ChatColor.BLUE}Arena location: ${ChatColor.AQUA}$location\n" +
						"    ${ChatColor.BLUE}Arena size: ${ChatColor.AQUA}$size\n" +
						"    ${ChatColor.BLUE}Game mode: ${ChatColor.AQUA}$gamemode"
		)
	}
}
