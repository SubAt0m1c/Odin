package me.odinclient.features.impl.floor7

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.utils.profile
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.dungeon.M7Phases
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

object DioriteFucker : Module(
    name = "Fuck Diorite",
    description = "Replaces the pillars in the storm fight with glass.",
    category = Category.FLOOR7,
) {
    private val stainedGlass by BooleanSetting("Stained glass", default = false, description = "Swaps the diorite with stained glass.")
    private val color by NumberSetting("Color", 0, 0.0, 15.0, 1.0, description = "Color for the stained glass.").withDependency { stainedGlass }
    private val pillars = listOf(listOf(46, 169, 41), listOf(46, 169, 65), listOf(100, 169, 65), listOf(100, 179, 41))
    private val coordinates: MutableList<BlockPos> = mutableListOf<BlockPos>().apply {
        pillars.forEach { (x, y, z) ->
            (-3..3).forEach { dx ->
                (0..37).forEach { dy ->
                    (-3..3).forEach { dz ->
                        add(BlockPos(x + dx, y + dy, z + dz))
                    }
                }
            }
        }
    }

    init {
        onMessage("[BOSS] Storm: Pathetic Maxor, just like expected.", false) {
            modMessage("Fucking Diorite")
            replaceDiorite()
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        profile("Diorite Fucker") {
            if (event.phase == TickEvent.Phase.END && mc.theWorld != null && DungeonUtils.getF7Phase() == M7Phases.P2) replaceDiorite()
        }
    }

    private fun replaceDiorite() {
        coordinates.forEach {
            if (isDiorite(it)) setGlass(it)
        }
    }

    private fun setGlass(pos: BlockPos) =
        mc.theWorld?.setBlockState(pos, if (stainedGlass) Blocks.stained_glass.getStateFromMeta(color) else Blocks.glass.defaultState, 3)

    private fun isDiorite(pos: BlockPos): Boolean =
        mc.theWorld?.chunkProvider?.provideChunk(pos.x shr 4, pos.z shr 4)?.getBlock(pos) == Blocks.stone
}