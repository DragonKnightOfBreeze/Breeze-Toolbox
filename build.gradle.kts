import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date

plugins {
	id("org.jetbrains.kotlin.jvm") version "1.7.0"
	id("org.jetbrains.intellij") version "1.9.0"
	id("org.jetbrains.changelog") version "2.0.0"
}

group = "icu.windea"
version = "0.2.1"

intellij {
	pluginName.set("Breeze Toolbox")
	version.set("2022.2")
	plugins.set(listOf("java", "java-i18n", "properties", "org.jetbrains.kotlin"))
}

repositories {
	maven("https://maven.aliyun.com/nexus/content/groups/public")
	maven("https://www.jetbrains.com/intellij-repository/releases")
	mavenCentral()
}

dependencies {
	testImplementation("junit:junit:4.13.2")
}

sourceSets.main {
	java.srcDirs("src/main/java", "src/main/kotlin", "src/main/gen")
}

kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of(11))
	}
}

changelog {
	version.set(version.get())
	header.set(provider { "${version.get()} ${date("yyyy/MM/dd")}" })
	headerParserRegex.set("""^(.*)\s*.*""".toRegex())
	groups.set(emptyList())
}

tasks {
	runIde {
		systemProperties["idea.is.internal"] = true
	}
	patchPluginXml {
		sinceBuild.set("222")
		untilBuild.set("")
		pluginDescription.set(projectDir.resolve("DESCRIPTION.md").readText())
		// Get the latest available change notes from the changelog file
		changeNotes.set(provider {
			changelog.renderItem(changelog.run {
				getOrNull(version.get()) ?: getUnreleased()
			}, Changelog.OutputType.MARKDOWN)
		})
	}
	jar {
		from("README.md", "LICENSE")
	}
	publishPlugin {
		dependsOn("patchChangelog")
		token.set(System.getenv("IDEA_TOKEN"))
	}
}