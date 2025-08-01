import org.apache.commons.lang3.SystemUtils

group = "me.odinclient"

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    shadowImpl(project(":"))
}

loom {
    launchConfigs {
        getByName("client") {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        getByName("client") {
            property("fml.coreMods.load", "me.odinmain.lwjgl.plugin.LWJGLLoadingPlugin")
            if (SystemUtils.IS_OS_MAC_OSX) vmArgs.remove("-XstartOnFirstThread")
        }
        remove(getByName("server"))
    }
    forge {
        mixinConfig("mixins.odinclient.json")
    }
    @Suppress("UnstableApiUsage")
    mixin.defaultRefmapName.set("mixins.odinclient.refmap.json")
}

tasks {
    processResources {
        inputs.property("version", version)

        filesMatching("mcmod.info") {
            expand(inputs.properties)
        }
        dependsOn(compileJava)
    }

    jar {
        manifest.attributes(
            "FMLCorePlugin" to "me.odinmain.lwjgl.plugin.LWJGLLoadingPlugin",
            "FMLCorePluginContainsFMLMod" to true,
            "ForceLoadAsMod" to true,
            "MixinConfigs" to "mixins.odinclient.json",
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
        )
        dependsOn(shadowJar)
        enabled = false
    }

    remapJar {
        archiveBaseName.set("OdinClient")
        input.set(shadowJar.get().archiveFile)
    }

    shadowJar {
        destinationDirectory.set(layout.buildDirectory.dir("archiveJars"))
        archiveBaseName.set("OdinClient")
        archiveClassifier.set("dev")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        configurations = listOf(
            project.configurations.getByName("shadowImpl")
        )
        exclude("META-INF/versions/**")
        mergeServiceFiles()
    }
}