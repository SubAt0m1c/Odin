package me.odinmain.utils.skyblock

import me.odinmain.OdinMain.mc
import me.odinmain.events.impl.ChatPacketEvent
import me.odinmain.events.impl.PacketReceivedEvent
import me.odinmain.features.impl.nether.NoPre
import me.odinmain.utils.*
import me.odinmain.utils.clock.Executor
import me.odinmain.utils.clock.Executor.Companion.register
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityGiantZombie
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object KuudraUtils {
    var kuudraTeammates: List<KuudraPlayer> = emptyList()
    var kuudraTeammatesNoSelf: List<KuudraPlayer> = emptyList()
    var giantZombies: List<EntityGiantZombie> = emptyList()
    var supplies = BooleanArray(6) { true }
    var kuudraEntity: EntityMagmaCube = EntityMagmaCube(mc.theWorld)
    var builders = 0
    var build = 0
    var phase = 0
    var buildingPiles = listOf<EntityArmorStand>()

    inline val inKuudra get() = LocationUtils.currentArea.isArea(Island.Kuudra)

    data class KuudraPlayer(val playerName: String, var eatFresh: Boolean = false, var eatFreshTime: Long = 0, var entity: EntityPlayer? = null)

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        kuudraTeammates = emptyList()
        kuudraTeammatesNoSelf = emptyList()
        giantZombies = emptyList()
        supplies = BooleanArray(6) { true }
        kuudraEntity = EntityMagmaCube(mc.theWorld)
        builders = 0
        build = 0
        phase = 0
        buildingPiles = listOf()
        NoPre.missing = ""
    }

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {
        val message = event.message

        if (message.matches(Regex("^Party > ?(?:\\[\\S+])? (\\S{1,16}): FRESH"))) {
            val playerName = Regex("^Party > ?(?:\\[\\S+])? (\\S{1,16}): FRESH").find(message)?.groupValues?.get(1)?.takeIf { it == mc.thePlayer?.name } ?: return

            kuudraTeammates.find { it.playerName == playerName }?.let { kuudraPlayer ->
                kuudraPlayer.eatFresh = true
                runIn(200) {
                    kuudraPlayer.eatFresh = false
                }
            }
        }

        when (message) {
            "[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!" -> phase = 1

            "[NPC] Elle: OMG! Great work collecting my supplies!" -> phase = 2

            "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!" -> phase = 3

            "[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!" -> phase = 4
        }
    }


    init {
        Executor(500) {
            if (!inKuudra) return@Executor
            val entities = mc.theWorld?.loadedEntityList ?: return@Executor
            giantZombies = entities.filterIsInstance<EntityGiantZombie>().filter { it.heldItem.unformattedName == "Head" }.toMutableList()

            kuudraEntity = entities.filterIsInstance<EntityMagmaCube>().filter { it.slimeSize == 30 && it.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue.toFloat() == 100000f }[0]

            entities.filterIsInstance<EntityArmorStand>().forEach {
                if (phase == 2) {
                    Regex("Building Progress (\\d+)% \\((\\d+) Players Helping\\)").find(it.name.noControlCodes)?.let {
                        build = it.groupValues[1].toIntOrNull() ?: 0
                        builders = it.groupValues[2].toIntOrNull() ?: 0
                    }
                }

                if (phase != 1 || !it.name.contains("SUPPLIES RECEIVED")) return@forEach
                val x = it.posX.toInt()
                val z = it.posZ.toInt()
                if (x == -98 && z == -112) supplies[0] = false
                if (x == -98 && z == -99) supplies[1] = false
                if (x == -110 && z == -106) supplies[2] = false
                if (x == -106 && z == -112) supplies[3] = false
                if (x == -94 && z == -106) supplies[4] = false
                if (x == -106 && z == -99) supplies[5] = false
            }

            buildingPiles = entities.filterIsInstance<EntityArmorStand>().filter { it.name.noControlCodes.matches(Regex("PROGRESS: (\\d+)%")) }.map { it }
        }.register()
    }

    @SubscribeEvent
    fun handleTabListPacket(event: PacketReceivedEvent) {
        if (!inKuudra || event.packet !is S38PacketPlayerListItem || !event.packet.action.equalsOneOf(S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME, S38PacketPlayerListItem.Action.ADD_PLAYER)) return
        kuudraTeammates = updateKuudraTeammates(kuudraTeammates.toMutableList(), event.packet.entries)
        kuudraTeammatesNoSelf = kuudraTeammates.filter { it.playerName != mc.thePlayer?.name }
    }

    private val tablistRegex = Regex("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+)")

    private fun updateKuudraTeammates(previousTeammates: MutableList<KuudraPlayer>, tabList: List<S38PacketPlayerListItem.AddPlayerData>): List<KuudraPlayer> {

        for (line in tabList) {
            val text = line.displayName?.unformattedText?.noControlCodes ?: continue
            val (_, name) = tablistRegex.find(text)?.destructured ?: continue

            previousTeammates.find { it.playerName == name }?.let { kuudraPlayer ->
                kuudraPlayer.entity = mc.theWorld?.getPlayerEntityByName(name)
            } ?: previousTeammates.add(KuudraPlayer(name, entity = mc.theWorld?.getPlayerEntityByName(name)))
        }
        return previousTeammates
    }
}