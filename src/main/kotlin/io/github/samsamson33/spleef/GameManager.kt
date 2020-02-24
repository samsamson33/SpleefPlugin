package io.github.samsamson33.spleef

import io.github.samsamson33.spleef.util.Countdown
import io.github.samsamson33.spleef.util.fill
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

import kotlin.properties.Delegates

class GameManager(val plugin: SpleefPlugin) {
	private val config = plugin.config

	private val dataFile: File = plugin.dataFolder.resolve(".data")
	private val dataYAML = YamlConfiguration()

	private var gameActive: Boolean = false
	private val activePlayers: MutableSet<Player> = HashSet()
	private val spectatingPlayers: MutableSet<Player> = HashSet()

	private var deathHandlerID: Int by Delegates.notNull()

	init {
		if (dataFile.isFile) {
			dataYAML.load(dataFile)
		}
	}

	fun reload() {
		dataYAML.load(dataFile)
	}

	fun save() {
		Bukkit.getLogger().info("Saving $dataFile...")
		dataYAML.save(this.dataFile)
		Bukkit.getLogger().info("Saved $dataFile")
	}

	fun startGame() {
		if (gameActive) {
			throw IllegalStateException("The spleef game was started while the game was active")
		}

		val arenaLocation = arena.getLocation()
		val spectateLocation = arena.getSpectateLocation()
		val size = arena.getSize()

		if (arenaLocation == null || spectateLocation == null || size == null) {
			throw java.lang.IllegalStateException(
					"The spleef game was started with an incomplete configuration"
			)
		}

		val world = arenaLocation.world ?: return
		val x = arenaLocation.blockX
		val y = arenaLocation.blockY
		val z = arenaLocation.blockZ

		// Reset the arena
		fill(
				Material.SAND, world,
				x - size, y, z - size,
				x + size, y, z + size
		)

		fill(
				Material.TNT, world,
				x - size, y - 1, z - size,
				x + size, y - 1, z + size
		)

		fill(
				Material.AIR, world,
				x - size, y + 1, z - size,
				x + size, y + 1, z + size
		)


		// Teleport the players
		val playersList = players.get()
		// TODO: this is temporary
		if (playersList.isEmpty()) {
			throw IllegalStateException("The spleef game was started with less than two players")
		}

		playersList.forEach { offlinePlayer ->
			if (offlinePlayer.isOnline) {
				val player = offlinePlayer.player ?: return@forEach
				locations.save(player)
			}
		}
		save()

		val teleportTarget = arenaLocation.clone().add(0.0, 1.0, 0.0)
		playersList.forEach { offlinePlayer ->
			if (offlinePlayer.isOnline) {
				val player = offlinePlayer.player ?: return@forEach
				activePlayers.add(player)
				player.teleport(teleportTarget)
				player.gameMode = GameMode.ADVENTURE
			}
		}

		gameActive = true

		Countdown(plugin, 10).then {
			fill(
					Material.STONE_PRESSURE_PLATE, world,
					x - size, y + 1, z - size,
					x + size, y + 1, z + size
			)
		}

		deathHandlerID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, { ->
			activePlayers.forEach { player ->
				if (player.location.y < arenaLocation.y - 2) {
					val wasInvulerable = player.isInvulnerable
					player.isInvulnerable = true
					player.teleport(spectateLocation)
					player.isInvulnerable = wasInvulerable
					activePlayers.remove(player)
					spectatingPlayers.add(player)

					if (activePlayers.size == 0) {
						Bukkit.broadcastMessage(
								"${ChatColor.GREEN}${ChatColor.BOLD}${player.name} " +
										"${ChatColor.BLUE}has won the game!"
						)
						endGame()
					} else {
						Bukkit.broadcastMessage(
								"${ChatColor.GRAY}${player.name} died. " +
										"${ChatColor.AQUA}${activePlayers.size}${ChatColor.GRAY} players remaining."
						)
					}
				}
			}
		}, 0L, 2L)
	}

	fun endGame() {
		if (!gameActive) {
			throw IllegalStateException("Attempted to start spleef game was started while the game was active")
		}

		Bukkit.getScheduler().cancelTask(deathHandlerID)

		spectatingPlayers.forEach { player ->
			val location = locations.get(player)
			if (location == null) {
				Bukkit.broadcastMessage("[DEBUG] Could not get the location of ${player.name}")
			} else {
				player.teleport(location)
			}
			player.gameMode = GameMode.SURVIVAL
		}

		gameActive = false
	}

	val players = PlayersManager()
	val arena = ArenaManager()
	val locations = LocationsManager()

	fun getGamemode(): Gamemode {
		// TODO: support multiple gamemodes
		return Gamemode.TNT_TAG
	}

	enum class Gamemode(val displayName: String) {
		TNT_TAG("tnt-tag")
	}

 	inner class PlayersManager {
		private val playersPath = "players.joined"

		fun add(player: OfflinePlayer): Boolean {
			if (!dataYAML.contains(playersPath)) {
				dataYAML.createSection(playersPath)
			}

			val joinedPlayers: MutableList<String?> = dataYAML.getStringList(playersPath)
			val uuid: String = player.uniqueId.toString()

			if (!joinedPlayers.contains(uuid)) {
				joinedPlayers.add(uuid)
				dataYAML.set(playersPath, joinedPlayers)
				return true
			}

			return false
		}

		fun remove(player: OfflinePlayer): Boolean {
			if (!dataYAML.contains(playersPath)) {
				dataYAML.createSection(playersPath)
			}

			val joinedPlayers: MutableList<String?> = dataYAML.getStringList(playersPath)
			val uuid: String = player.uniqueId.toString()

			if (joinedPlayers.contains(uuid)) {
				joinedPlayers.remove(uuid)
				dataYAML.set(playersPath, joinedPlayers)
				return true
			}

			return false
		}

		fun get(): List<OfflinePlayer> {
			if (!dataYAML.contains(playersPath)) {
				dataYAML.createSection(playersPath)
			}

			return dataYAML.getStringList(playersPath).map { player ->
				Bukkit.getOfflinePlayer(UUID.fromString(player))
			}
		}
	}

	inner class ArenaManager {
		fun getWorld(): World? {
			if (config.getString("arena.world") == null) {
				return null
			}

			val name = config.getString("arena.world")
			return name?.let { plugin.server.getWorld(it) }
		}

		fun getLocation(): Location? {
			val world = getWorld()
			val coords = config.getDoubleList("arena.location")

			if (world == null || coords.size != 3) {
				return null
			}

			return Location(world, coords[0], coords[1], coords[2])
		}

		fun getSpectateLocation(): Location? {
			val world = getWorld()
			val coords = config.getDoubleList("arena.spectatelocation")

			if (world == null || coords.size != 3) {
				return null
			}

			return Location(world, coords[0], coords[1], coords[2])
		}

		fun getSize(): Int? {
			return 11
		}
	}

	inner class LocationsManager {
		private val path = "original-locations"

		fun save(player: Player) {
			if (player.isOnline) {
				dataYAML.set("$path.${player.uniqueId}.world", player.world.uid.toString())
				dataYAML.set("$path.${player.uniqueId}.coords", arrayOf(
						player.location.x,
						player.location.y,
						player.location.z,
						player.location.yaw.toDouble(),
						player.location.pitch.toDouble()
				).toCollection(ArrayList()))
			}
		}

		fun get(player: Player): Location? {
			val world = dataYAML.getString("$path.${player.uniqueId}.world")
			val coords = dataYAML.getDoubleList("$path.${player.uniqueId}.coords")
			if (world == null || coords.size != 5) return null
			return Location(
					Bukkit.getWorld(UUID.fromString(world)),
					coords[0],
					coords[1],
					coords[2],
					coords[3].toFloat(),
					coords[4].toFloat()
			)
		}
	}
}
