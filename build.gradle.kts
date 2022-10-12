import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.7.20"
  kotlin("plugin.serialization") version "1.7.20"
  java
  application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(kotlin("test"))
  implementation("org.jetbrains.lets-plot:lets-plot-common:2.5.0")
  implementation("org.jetbrains.lets-plot:lets-plot-image-export:2.5.0")
  implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.1.0")
  implementation("org.slf4j:slf4j-simple:2.0.3")
  implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "17"
}

application {
  mainClass.set("MainKt")
}