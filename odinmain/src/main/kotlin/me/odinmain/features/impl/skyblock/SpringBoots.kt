package me.odinmain.features.impl.skyblock

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.ColorSetting
import me.odinmain.features.settings.impl.HudSetting
import me.odinmain.ui.hud.HudElement
import me.odinmain.utils.addVec
import me.odinmain.utils.equalsOneOf
import me.odinmain.utils.getSafe
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.render.getTextWidth
import me.odinmain.utils.render.mcText
import me.odinmain.utils.skyblock.LocationUtils
import me.odinmain.utils.skyblock.itemID
import me.odinmain.utils.toAABB
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object SpringBoots : Module(
    name = "Spring Boots",
    description = "Shows how many blocks you can jump.",
    category = Category.SKYBLOCK
) {
    private val hud: HudElement by HudSetting("Display", 10f, 10f, 1f, true) {
        if (it) {
            mcText("Jump: 6.5", 2f, 5f, 1, Color.WHITE)
            getTextWidth("Jump: 6.5", 12f) to 12f
        } else {
            mcText("Jump: ${blocksList.getSafe(pitchCounts.sum()) ?: "61 (MAX)"}", 2f, 5f, 1, Color.WHITE)
            getTextWidth("Jump: ${blocksList.getSafe(pitchCounts.sum()) ?: "61 (MAX)"}", 12f) to 12f
        }
    }
    private val renderGoal: Boolean by BooleanSetting("Render Goal", true, description = "Render the goal block.")
    private val goalColor: Color by ColorSetting("Goal Color", Color.GREEN, description = "Color of the goal block.")

    private val blocksList: List<Double> = listOf(
        0.0, 3.0, 6.5, 9.0, 11.5, 13.5, 16.0, 18.0, 19.0,
        20.5, 22.5, 25.0, 26.5, 28.0, 29.0, 30.0, 31.0, 33.0,
        34.0, 35.5, 37.0, 38.0, 39.5, 40.0, 41.0, 42.5, 43.5,
        44.0, 45.0, 46.0, 47.0, 48.0, 49.0, 50.0, 51.0, 52.0,
        53.0, 54.0, 55.0, 56.0, 57.0, 58.0, 59.0, 60.0, 61.0
    )

    private val pitchCounts = IntArray(2) { 0 }
    private var blockPos: Vec3? = null

    init {
        onPacket(S29PacketSoundEffect::class.java) {
            if (!LocationUtils.inSkyblock) return@onPacket
            when (it.soundName) {
                "random.eat", "fireworks.launch" -> if (it.pitch.equalsOneOf(0.0952381f, 1.6984127f)) pitchCounts.fill(0)
                "note.pling" -> if (mc.thePlayer?.isSneaking == true && mc.thePlayer?.getCurrentArmor(0)?.itemID == "SPRING_BOOTS") {
                    when (it.pitch) {
                        0.6984127f -> pitchCounts[0] = (pitchCounts[0] + 1).takeIf { it <= 2 } ?: 0
                        0.82539684f, 0.8888889f, 0.93650794f, 1.0476191f, 1.1746032f, 1.3174603f, 1.7777778f -> pitchCounts[1]++
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || !LocationUtils.inSkyblock) return
        if (mc.thePlayer?.getCurrentArmor(0)?.itemID != "SPRING_BOOTS" || mc.thePlayer?.isSneaking == false) pitchCounts.fill(0)
        blocksList.getSafe(pitchCounts.sum())?.let { blockPos = if (it != 0.0) mc.thePlayer?.positionVector?.addVec(x = -0.5, y = it, z = -0.5) else null }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!renderGoal || !LocationUtils.inSkyblock) return
        blockPos?.let { Renderer.drawBox(it.toAABB(), goalColor, fillAlpha = 0f) }
    }
}