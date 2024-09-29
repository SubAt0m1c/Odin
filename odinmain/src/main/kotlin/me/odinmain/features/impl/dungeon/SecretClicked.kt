package me.odinmain.features.impl.dungeon

import me.odinmain.events.impl.SecretPickupEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.Setting.Companion.withDependency
import me.odinmain.features.settings.impl.*
import me.odinmain.ui.clickgui.util.ColorUtil.withAlpha
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.runIn
import me.odinmain.utils.skyblock.PlayerUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.toAABB
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.CopyOnWriteArrayList

object SecretClicked : Module(
    name = "Secret Clicked",
    category = Category.DUNGEON,
    description = "Provides both audio and visual feedback when a secret is clicked."
) {
    private val boxesDropdown: Boolean by DropdownSetting("Secret Boxes Dropdown")
    private val boxes: Boolean by BooleanSetting("Secret Boxes", true, description = "Whether or not to render boxes around clicked secrets.").withDependency { boxesDropdown }
    private val style: Int by SelectorSetting("Style", Renderer.DEFAULT_STYLE, Renderer.styles, description = Renderer.STYLE_DESCRIPTION).withDependency { boxesDropdown && boxes }
    private val color: Color by ColorSetting("Color", Color.ORANGE.withAlpha(.4f), allowAlpha = true, description = "The color of the box.").withDependency { boxesDropdown && boxes }
    private val lineWidth: Float by NumberSetting("Line Width", 2f, 0.1f, 10f, 0.1f, description = "The width of the box's lines.").withDependency { boxesDropdown && boxes }
    private val depthCheck: Boolean by BooleanSetting("Depth check", false, description = "Boxes show through walls.").withDependency { boxesDropdown && boxes }
    private val lockedColor: Color by ColorSetting("Locked Color", Color.RED.withAlpha(.4f), allowAlpha = true, description = "The color of the box when the chest is locked.").withDependency { boxesDropdown && boxes }
    private val timeToStay: Int by NumberSetting("Time To Stay (seconds)", 7, 1, 60, 1, description = "The time the chests should remain highlighted.").withDependency { boxesDropdown && boxes }
    private val useRealSize: Boolean by BooleanSetting("Use Real Size", true, description = "Whether or not to use the real size of the block.").withDependency { boxesDropdown && boxes }
    private val boxInBoss: Boolean by BooleanSetting("Box In Boss", false, description = "Highlight clicks in boss.").withDependency { boxesDropdown && boxes }

    private val chimeDropdownSetting: Boolean by DropdownSetting("Secret Chime Dropdown")
    private val chime: Boolean by BooleanSetting("Secret Chime", true, description = "Whether or not to play a sound when a secret is clicked.").withDependency { chimeDropdownSetting }
    private val defaultSounds = arrayListOf("mob.blaze.hit", "fire.ignite", "random.orb", "random.break", "mob.guardian.land.hit", "note.pling", "Custom")
    private val sound: Int by SelectorSetting("Sound", "mob.blaze.hit", defaultSounds, description = "Which sound to play when you get a secret.").withDependency { chimeDropdownSetting && chime }
    private val customSound: String by StringSetting("Custom Sound", "mob.blaze.hit",
        description = "Name of a custom sound to play. This is used when Custom is selected in the Sound setting. Do not use the bat death sound or your game will freeze!", length = 32
    ).withDependency { sound == defaultSounds.size - 1 && chimeDropdownSetting && chime}
    private val volume: Float by NumberSetting("Volume", 1f, 0, 1, .01f, description = "Volume of the sound.").withDependency { chimeDropdownSetting && chime }
    private val pitch: Float by NumberSetting("Pitch", 2f, 0, 2, .01f, description = "Pitch of the sound.").withDependency { chimeDropdownSetting && chime }
    val reset: () -> Unit by ActionSetting("Play Sound", description = "Plays the sound with the current settings.") {
        PlayerUtils.playLoudSound(if (sound == defaultSounds.size - 1) customSound else defaultSounds[sound], volume, pitch) }.withDependency { chimeDropdownSetting && chime }
    private val chimeInBoss: Boolean by BooleanSetting("Chime In Boss", false, description = "Prevent playing the sound if in boss room.").withDependency { chimeDropdownSetting && chime }

    private data class Secret(val pos: BlockPos, var locked: Boolean = false)
    private val clickedSecretsList = CopyOnWriteArrayList<Secret>()
    private var lastPlayed = System.currentTimeMillis()

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!boxes || !DungeonUtils.inDungeons || (DungeonUtils.inBoss && boxInBoss) || clickedSecretsList.isEmpty()) return

        clickedSecretsList.forEach {
            val currentColor = if (it.locked) lockedColor else color
            if (useRealSize) Renderer.drawStyledBlock(it.pos, currentColor, style, lineWidth, depthCheck)
            else Renderer.drawStyledBox(it.pos.toAABB(), currentColor, style, lineWidth, depthCheck)
        }
    }

    @SubscribeEvent
    fun onSecret(event: SecretPickupEvent) {
        if (event is SecretPickupEvent.Interact) secretBox(event.blockPos)
        secretChime()
    }

    private fun secretChime() {
        if (!chime || (chimeInBoss && DungeonUtils.inBoss) || System.currentTimeMillis() - lastPlayed <= 10) return
        PlayerUtils.playLoudSound(if (sound == defaultSounds.size - 1) customSound else defaultSounds[sound], volume, pitch)
        lastPlayed = System.currentTimeMillis()
    }

    private fun secretBox(pos: BlockPos) {
        if (!boxes || (DungeonUtils.inBoss && boxInBoss) || clickedSecretsList.any { it.pos == pos }) return
        clickedSecretsList.add(Secret(pos))
        runIn(timeToStay * 20) { clickedSecretsList.removeFirstOrNull() }
    }

    init {
        onWorldLoad { clickedSecretsList.clear() }

        onMessage("That chest is locked!", true) {
            clickedSecretsList.lastOrNull()?.apply { locked = true }
        }
    }
}
