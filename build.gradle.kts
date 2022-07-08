import org.jetbrains.kotlin.gradle.tasks.*

plugins {
	id("org.jetbrains.kotlin.jvm") version "1.7.10"
	id("org.jetbrains.intellij") version "1.6.0"
}

group = "icu.windea"
version = "0.1.0"

intellij {
	version.set("2021.3")
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
	jar {
		from("README.md", "README_en.md", "LICENSE")
	}
	withType<JavaCompile> {
		sourceCompatibility = "11"
		targetCompatibility = "11"
	}
	withType<KotlinCompile> {
		kotlinOptions.jvmTarget = "11"
		kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
	}
	publishPlugin {
		token.set(System.getenv("IDEA_TOKEN"))
	}
}