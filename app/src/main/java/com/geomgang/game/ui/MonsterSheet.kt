package com.geomgang.game.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.IntOffset
import com.geomgang.core.PetKind
import com.geomgang.core.Zone
import com.geomgang.game.R

/**
 * Monster artwork routing.
 *
 * Combat monsters use one high-resolution 3 x 2 atlas per zone: the five regular
 * monsters followed by the zone boss. Pets remain in the compact legacy sheet so
 * replacing combat art cannot disturb collection data or pet cell ordering.
 */
object MonsterSheet {

    const val CELL = 32
    const val COLUMNS = 8

    const val COMBAT_CELL = 384
    const val COMBAT_COLUMNS = 3

    data class CombatSource(
        @DrawableRes val drawable: Int,
        val offset: IntOffset,
    )

    private val bossBase = Zone.entries.sumOf { it.monsters.size }
    private val petBase = bossBase + Zone.entries.size

    private val legacyCells: Map<String, Int> = buildMap {
        var monster = 0
        Zone.entries.forEach { zone -> zone.monsters.forEach { put(it.name, monster++) } }
        var boss = bossBase
        Zone.entries.forEach { zone -> put(zone.bossName, boss++) }
    }

    private data class ZoneCell(val zoneIndex: Int, val localCell: Int)

    private val combatCells: Map<String, ZoneCell> = buildMap {
        Zone.entries.forEachIndexed { zoneIndex, zone ->
            zone.monsters.forEachIndexed { monsterIndex, monster ->
                put(monster.name, ZoneCell(zoneIndex, monsterIndex))
            }
            put(zone.bossName, ZoneCell(zoneIndex, 5))
        }
    }

    private val zoneDrawables = intArrayOf(
        R.drawable.monster_zone_00,
        R.drawable.monster_zone_01,
        R.drawable.monster_zone_02,
        R.drawable.monster_zone_03,
        R.drawable.monster_zone_04,
        R.drawable.monster_zone_05,
        R.drawable.monster_zone_06,
        R.drawable.monster_zone_07,
        R.drawable.monster_zone_08,
        R.drawable.monster_zone_09,
        R.drawable.monster_zone_10,
        R.drawable.monster_zone_11,
        R.drawable.monster_zone_12,
        R.drawable.monster_zone_13,
        R.drawable.monster_zone_14,
        R.drawable.monster_zone_15,
        R.drawable.monster_zone_16,
        R.drawable.monster_zone_17,
        R.drawable.monster_zone_18,
        R.drawable.monster_zone_19,
        R.drawable.monster_zone_20,
        R.drawable.monster_zone_21,
        R.drawable.monster_zone_22,
        R.drawable.monster_zone_23,
    )

    fun combatSourceOf(name: String): CombatSource {
        val source = requireNotNull(combatCells[name]) { "unknown monster: $name" }
        return CombatSource(
            drawable = zoneDrawables[source.zoneIndex],
            offset = IntOffset(
                x = (source.localCell % COMBAT_COLUMNS) * COMBAT_CELL,
                y = (source.localCell / COMBAT_COLUMNS) * COMBAT_CELL,
            ),
        )
    }

    fun petCellOf(petId: String): Int {
        val index = PetKind.entries.indexOfFirst { it.id == petId }
        require(index >= 0) { "unknown pet: $petId" }
        return petBase + index
    }

    fun hasCell(name: String): Boolean = name in legacyCells

    /** Retained for ordering tests and save-compatible legacy atlas indices. */
    fun cellOf(name: String): Int =
        requireNotNull(legacyCells[name]) { "unknown monster: $name" }

    fun offsetOf(cell: Int): IntOffset =
        IntOffset((cell % COLUMNS) * CELL, (cell / COLUMNS) * CELL)
}
