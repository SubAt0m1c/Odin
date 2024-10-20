package me.odin

import me.odin.features.impl.floor7.p3.*
import me.odin.features.impl.render.Camera
import me.odin.features.impl.render.EtherWarpHelper
import me.odin.features.impl.skyblock.HidePlayers
import me.odin.mixin.accessors.EntityRendererAccessor
import me.odin.mixin.accessors.S19PacketEntityStatusAccessor
import me.odinmain.OdinMain
import me.odinmain.OdinMain.mc
import me.odinmain.events.impl.PacketReceivedEvent
import me.odinmain.events.impl.PostEntityStatus
import me.odinmain.features.ModuleManager
import me.odinmain.ui.util.shader.FramebufferShader
import me.odinmain.utils.postAndCatch
import me.odinmain.utils.render.RenderUtils
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.*
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

@Suppress("UNUSED_PARAMETER")
@Mod(
    modid = ModCore.MOD_ID,
    name = ModCore.NAME,
    version = ModCore.VERSION,
    clientSideOnly = true
)
class ModCore {

    @EventHandler
    fun init(event: FMLInitializationEvent) {
        OdinMain.init()
        MinecraftForge.EVENT_BUS.register(this)
        FramebufferShader.setupCameraTransform =
            { (mc.entityRenderer as? EntityRendererAccessor)?.invokeSetupCameraTransform(RenderUtils.partialTicks, 0) }}

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        OdinMain.onTick()
    }

    @EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        OdinMain.postInit()
    }

    @EventHandler
    fun loadComplete(event: FMLLoadCompleteEvent) {
        ModuleManager.addModules(SimonSays, ArrowsDevice, EtherWarpHelper, Camera, HidePlayers)

        OdinMain.loadComplete()
    }

    @SubscribeEvent
    fun onPacket(event: PacketReceivedEvent) {
        if (event.packet !is S19PacketEntityStatus) return
        val packet = event.packet as? S19PacketEntityStatusAccessor ?: return
        PostEntityStatus(packet.entityId, packet.logicOpcode).postAndCatch()
    }

    companion object {
        const val MOD_ID = "od"
        const val NAME = "Odin"
        const val VERSION = OdinMain.VERSION
    }
}
