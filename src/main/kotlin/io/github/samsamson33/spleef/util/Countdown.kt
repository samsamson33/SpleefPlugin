package io.github.samsamson33.spleef.util

import io.github.samsamson33.spleef.SpleefPlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates

class Countdown(val plugin: SpleefPlugin, private val seconds: Int) {
	private var taskID: Int by Delegates.notNull<Int>()

	fun start(): CompletableFuture<Unit> {
		val future = CompletableFuture<Unit>()

		var current = seconds
		taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, { ->
			if (current == 0) {
				Bukkit.broadcastMessage("Start!")
				Bukkit.getScheduler().cancelTask(taskID)
				future.complete(null)
			} else if (current <= 5 || current % 10 == 0) {
				Bukkit.broadcastMessage("${ChatColor.BLUE}$current")
			}
			current--
		}, 0L, 20L)

		return future
	}
}
