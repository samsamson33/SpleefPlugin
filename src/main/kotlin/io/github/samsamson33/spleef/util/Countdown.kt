package io.github.samsamson33.spleef.util

import io.github.samsamson33.spleef.SpleefPlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import kotlin.properties.Delegates

class Countdown(val plugin: SpleefPlugin, private val seconds: Int) {
	private var taskID: Int by Delegates.notNull<Int>()
	private val listeners = mutableListOf<(() -> Unit)>()

	init {
		start()
	}

	private fun start() {
		var current = seconds
		taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, { ->
			if (current == 0) {
				Bukkit.broadcastMessage("Start!")
				stop()
			} else if (current <= 5 || current % 10 == 0) {
				Bukkit.broadcastMessage("${ChatColor.BLUE}$current")
			}
			current--
		}, 0L, 20L)
	}

	private fun stop() {
		Bukkit.getScheduler().cancelTask(taskID)
		listeners.forEach { fn -> fn() }
	}

	fun then(fn: () -> Unit) {
		listeners.add(fn)
	}
}
