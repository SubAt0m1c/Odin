package me.odinmain.features.impl.skyblock

import me.odinmain.events.impl.GuiEvent
import me.odinmain.events.impl.ServerTickEvent
import me.odinmain.features.Module
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.HudSetting
import me.odinmain.utils.capitalizeFirst
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.RenderUtils
import me.odinmain.utils.render.getMCTextWidth
import me.odinmain.utils.render.mcText
import me.odinmain.utils.skyblock.LocationUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.partyMessage
import me.odinmain.utils.skyblock.skyblockID
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*

object InvincibilityTimer : Module(
    name = "Invincibility Timer",
    description = "Provides visual information about your invincibility items."
) {
    private val invincibilityAnnounce by BooleanSetting("Announce Invincibility", default = true, description = "Announces when you get invincibility.")
    private val showCooldown by BooleanSetting("Durability Cooldown", default = true, description = "Shows the durability of the mask in the inventory as a durability bar.")
    private val removeWhenBlank by BooleanSetting("Remove Blank", default = false, description = "Removes the HUD entry when there is no active invincibility.")

    private val HUD by HudSetting("Hud", 10f, 10f, 1f, false) { example ->
        (0..3).reduce { acc, index ->
            val type = InvincibilityType.entries[index - 1].takeUnless { (removeWhenBlank && !example) && it.activeTime == 0 && it.currentCooldown == 0 && (it != InvincibilityType.BONZO || DungeonUtils.inDungeons) } ?: return@reduce acc
            val text = when {
                type.activeTime > 0 -> "§6${String.format(Locale.US, "%.2f", type.activeTime / 20.0)}s"
                type.currentCooldown > 0 -> "§c${String.format(Locale.US, "%.2f", type.currentCooldown / 20.0)}s"
                else -> "§a√"
            }
            mcText("${type.name.lowercase().capitalizeFirst()} $text", 0, 10 * acc, 1, type.color, center = false)
            acc + 1
        }.let { getMCTextWidth("Bonzo: 180.00s").toFloat() to 10f * it }
    }

    init {
        onWorldLoad {
            InvincibilityType.entries.forEach { it.reset() }
        }

        onMessage(Regex(".*")) {
            InvincibilityType.entries.firstOrNull { type -> it.matches(type.regex) }?.let { type ->
                if (invincibilityAnnounce) partyMessage("${type.name.lowercase().capitalizeFirst()} Procced!")
                type.proc()
            }
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        InvincibilityType.entries.forEach { it.tick() }
    }

    @SubscribeEvent
    fun onRenderSlotOverlay(event: GuiEvent.DrawSlotOverlay) {
        if (!LocationUtils.isInSkyblock || !showCooldown) return

        val durability = when (event.stack.skyblockID) {
            "BONZO_MASK", "STARRED_BONZO_MASK" -> InvincibilityType.BONZO.currentCooldown.toDouble() / InvincibilityType.BONZO.maxCooldownTime
            "SPIRIT_MASK", "STARRED_SPIRIT_MASK" -> InvincibilityType.SPIRIT.currentCooldown.toDouble() / InvincibilityType.SPIRIT.maxCooldownTime
            else -> return
        }.takeIf { it < 1.0 } ?: return

        RenderUtils.renderDurabilityBar(event.x ?: return, event.y ?: return, durability)
    }

    enum class InvincibilityType(val regex: Regex, private val maxInvincibilityTime: Int, val maxCooldownTime: Int, val color: Color = Color.WHITE) {
        PHOENIX(Regex("^Your Phoenix Pet saved you from certain death!$"),80, 1200, Color.DARK_RED),
        BONZO(Regex("^Your (?:. )?Bonzo's Mask saved your life!$"), 60, 3600, Color.BLUE),
        SPIRIT(Regex("^Second Wind Activated! Your Spirit Mask saved your life!\$"), 30, 600, Color.PURPLE);

        var activeTime: Int = 0
            private set
        var currentCooldown: Int = 0
            private set

        fun proc() {
            activeTime = maxInvincibilityTime
            currentCooldown = maxCooldownTime
        }

        fun tick() {
            if (currentCooldown > 0) currentCooldown--
            if (activeTime > 0)      activeTime--
        }

        fun reset() {
            currentCooldown = 0
            activeTime = 0
        }
    }
}