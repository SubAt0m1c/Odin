package me.odinmain.features.impl.render

import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import me.odinmain.OdinMain
import me.odinmain.clickgui.settings.AlwaysActive
import me.odinmain.clickgui.settings.Setting.Companion.withDependency
import me.odinmain.clickgui.settings.impl.*
import me.odinmain.features.Module
import me.odinmain.utils.fetchData
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.Colors
import me.odinmain.utils.sendDataToServer
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.model.ModelBase
import net.minecraft.client.model.ModelRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.cos
import kotlin.math.sin

@AlwaysActive
object PlayerSize : Module(
    name = "Player Size",
    description = "Changes the size of the player model."
) {
    private val devSize by BooleanSetting("Dev Size", true, desc = "Toggles client side dev size for your own player.").withDependency { isRandom }
    private val devSizeX by NumberSetting("Size X", 1f, -1, 3f, 0.1, desc = "X scale of the dev size.")
    private val devSizeY by NumberSetting("Size Y", 1f, -1, 3f, 0.1, desc = "Y scale of the dev size.")
    private val devSizeZ by NumberSetting("Size Z", 1f, -1, 3f, 0.1, desc = "Z scale of the dev size.")
    private val devWings by BooleanSetting("Wings", false, desc = "Toggles dragon wings.").withDependency { isRandom }
    private val devWingsColor by ColorSetting("Wings Color", Colors.WHITE, desc = "Color of the dev wings.").withDependency { devWings && isRandom }
    private var showHidden by DropdownSetting("Show Hidden", false).withDependency { isRandom }
    private val passcode by StringSetting("Passcode", "odin", desc = "Passcode for dev features.").withDependency { showHidden && isRandom }

    private val sendDevData by ActionSetting("Send Dev Data", desc = "Sends dev data to the server.") {
        showHidden = false
        OdinMain.scope.launch {
            modMessage(sendDataToServer(body = "${mc.thePlayer.name}, [${devWingsColor.red},${devWingsColor.green},${devWingsColor.blue}], [$devSizeX,$devSizeY,$devSizeZ], $devWings, , $passcode", "https://tj4yzotqjuanubvfcrfo7h5qlq0opcyk.lambda-url.eu-north-1.on.aws/"))
            updateCustomProperties()
        }
    }.withDependency { isRandom }

    private var randoms: HashMap<String, RandomPlayer> = HashMap()
    val isRandom get() = randoms.containsKey(mc.session?.username)

    data class RandomPlayer(val scale: Triple<Float, Float, Float>, val wings: Boolean = false, val wingsColor: Color = Colors.WHITE, val customName: String, val isDev: Boolean)

    @JvmStatic
    fun preRenderCallbackScaleHook(entityLivingBaseIn: AbstractClientPlayer) {
        if (enabled && entityLivingBaseIn == mc.thePlayer && !randoms.containsKey(entityLivingBaseIn.name)) {
            if (devSizeY < 0) GlStateManager.translate(0f, devSizeY * 2, 0f)
            GlStateManager.scale(devSizeX, devSizeY, devSizeZ)
        }
        if (!randoms.containsKey(entityLivingBaseIn.name)) return
        if (!devSize && entityLivingBaseIn.name == mc.thePlayer.name) return
        val random = randoms[entityLivingBaseIn.name] ?: return
        if (random.scale.second < 0) GlStateManager.translate(0f, random.scale.second * 2, 0f)
        GlStateManager.scale(random.scale.first, random.scale.second, random.scale.third)
    }

    private val pattern = Regex("Decimal\\('(-?\\d+(?:\\.\\d+)?)'\\)")

    fun updateCustomProperties() {
        val data = fetchData("https://tj4yzotqjuanubvfcrfo7h5qlq0opcyk.lambda-url.eu-north-1.on.aws/").replace(pattern) { match -> match.groupValues[1] }.ifEmpty { null } ?: return
        JsonParser().parse(data)?.asJsonArray?.forEach {
            val jsonElement = it.asJsonObject
            val randomsName = jsonElement.get("DevName")?.asString ?: return@forEach
            val size = jsonElement.get("Size")?.asJsonArray?.let { sizeArray -> Triple(sizeArray[0].asFloat, sizeArray[1].asFloat, sizeArray[2].asFloat) } ?: return@forEach
            val wings = jsonElement.get("Wings")?.asBoolean == true
            val wingsColor = jsonElement.get("WingsColor")?.asJsonArray?.let { colorArray -> Color(colorArray[0].asInt, colorArray[1].asInt, colorArray[2].asInt) } ?: Colors.WHITE
            val customName = jsonElement.get("CustomName")?.asString?.replace("COLOR", "§") ?: ""
            val isDev = jsonElement.get("IsDev")?.asBoolean ?: false
            randoms[randomsName] = RandomPlayer(size, wings, Color(wingsColor.red, wingsColor.green, wingsColor.blue), customName, isDev)
        }
    }

    init {
        OdinMain.scope.launch {
            updateCustomProperties()
        }
    }

    @SubscribeEvent
    fun onRenderPlayer(event: RenderPlayerEvent.Post) {
        if (!randoms.containsKey(event.entity.name)) return
        if (!devSize && event.entity.name == mc.thePlayer.name) return
        val random = randoms[event.entity.name] ?: return
        if (!random.wings) return
        DragonWings.renderWings(event.entityPlayer, event.partialRenderTick, random)
    }

    fun replaceText(text: String?): String? {
        var replacedText = text
        for (random in randoms) {
            if (random.value.customName.isBlank()) continue
            replacedText = randoms[random.key]?.let { replacedText?.replace(random.key, it.customName) }
        }

        return replacedText
    }

    object DragonWings : ModelBase() {

        private val dragonWingTextureLocation = ResourceLocation("textures/entity/enderdragon/dragon.png")
        private val wing: ModelRenderer
        private val wingTip: ModelRenderer

        init {
            textureWidth = 256
            textureHeight = 256
            setTextureOffset("wing.skin", -56, 88)
            setTextureOffset("wingtip.skin", -56, 144)
            setTextureOffset("wing.bone", 112, 88)
            setTextureOffset("wingtip.bone", 112, 136)

            wing = ModelRenderer(this, "wing")
            wing.setRotationPoint(-12.0f, 5.0f, 2.0f)
            wing.addBox("bone", -56.0f, -4.0f, -4.0f, 56, 8, 8)
            wing.addBox("skin", -56.0f, 0.0f, 2.0f, 56, 0, 56)
            wingTip = ModelRenderer(this, "wingtip")
            wingTip.setRotationPoint(-56.0f, 0.0f, 0.0f)
            wingTip.addBox("bone", -56.0f, -2.0f, -2.0f, 56, 4, 4)
            wingTip.addBox("skin", -56.0f, 0.0f, 2.0f, 56, 0, 56)
            wing.addChild(wingTip)
        }

        fun renderWings(player: EntityPlayer, partialTicks: Float, random: RandomPlayer) {
            val rotation = this.interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTicks)

            GlStateManager.pushMatrix()
            val x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
            val y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
            val z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
            if (random.scale.second < 0) GlStateManager.translate(0f, random.scale.second * -2, 0f)

            GlStateManager.translate(-mc.renderManager.viewerPosX + x, -mc.renderManager.viewerPosY + y, -mc.renderManager.viewerPosZ + z)
            GlStateManager.scale(-0.2, -0.2, 0.2)
            GlStateManager.scale(random.scale.first, random.scale.second, random.scale.third)
            GlStateManager.rotate(180 + rotation, 0f, 1f, 0f)
            GlStateManager.translate(0.0, -(1.25 / 0.2f), 0.0)
            GlStateManager.translate(0.0, 0.0, 0.25)

            if (player.isSneaking) {
                GlStateManager.rotate(45f, 1f, 0f, 0f)
                GlStateManager.translate(0.0, 1.0, -0.5)
            }

            GlStateManager.color(random.wingsColor.red.toFloat()/255, random.wingsColor.green.toFloat()/255, random.wingsColor.blue.toFloat()/255, 1f)
            mc.textureManager.bindTexture(dragonWingTextureLocation)

            for (j in 0..1) {
                GlStateManager.enableCull()
                GlStateManager.rotate(20f, 0f, 1f, 0f)
                val f11 = System.currentTimeMillis() % 1000 / 1000f * Math.PI.toFloat() * 2.0f
                wing.rotateAngleX = Math.toRadians(-80.0).toFloat() - cos(f11) * 0.2f
                wing.rotateAngleY = Math.toRadians(20.0).toFloat() + sin(f11) * 0.4f
                wing.rotateAngleZ = Math.toRadians(20.0).toFloat()
                wingTip.rotateAngleZ = -(sin((f11 + 2.0f)) + 0.5).toFloat() * 0.75f
                wing.render(0.0625f)
                GlStateManager.scale(-1.0f, 1.0f, 1.0f)
                if (j == 0) {
                    GlStateManager.rotate(20f, 0f, 1f, 0f)
                    GlStateManager.cullFace(1028)
                }
            }

            GlStateManager.cullFace(1029)
            GlStateManager.disableCull()
            GlStateManager.color(1f, 1f, 1f, 1f)
            GlStateManager.popMatrix()
        }

        private fun interpolate(yaw1: Float, yaw2: Float, percent: Float): Float {
            var f = (yaw1 + (yaw2 - yaw1) * percent) % 360
            if (f < 0) {
                f += 360f
            }
            return f
        }
    }
}