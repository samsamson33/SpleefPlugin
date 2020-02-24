package io.github.samsamson33.spleef.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import java.lang.IllegalArgumentException

fun fill(material: Material, world: World, x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) {

	for (x in x1..x2) {
		for (y in y1..y2) {
			for (z in z1..z2) {
				world.getBlockAt(x, y, z).type = material
			}
		}
	}
}
