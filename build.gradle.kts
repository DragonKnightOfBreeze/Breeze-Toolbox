import org.jetbrains.kotlin.gradle.tasks.*

plugins {
	id("org.jetbrains.kotlin.jvm") version "1.7.0"
	id("org.jetbrains.intellij") version "1.8.0"
}

group = "icu.windea"
version = "0.2.0"

intellij {
	version.set("2022.2")
	pluginName.set("Breeze Toolbox")
	plugins.set(listOf("java", "java-i18n", "properties", "org.jetbrains.kotlin"))
}

repositories {
	maven("https://maven.aliyun.com/nexus/content/groups/public")
	mavenCentral()
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
}

sourceSets.main {
	java.srcDirs("src/main/java", "src/main/kotlin", "src/main/gen")
}

tasks {
	withType<JavaCompile> {
		sourceCompatibility = "11"
		targetCompatibility = "11"
	}
	withType<KotlinCompile> {
		kotlinOptions.jvmTarget = "11"
		kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
	}
	jar {
		from("README.md", "LICENSE")
	}
	publishPlugin {
		token.set(System.getenv("IDEA_TOKEN"))
	}
}