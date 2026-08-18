plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.bn"
version = "0.1.8"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

intellij {
    // 用已缓存的 2024.2 SDK 编译；since-build 降到 233，以安装到 IDEA 2023.3（IU-233.15619.7）
    version.set("2024.2")
    type.set("IC")
    plugins.set(listOf("java", "maven"))
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks {
    test {
        useJUnit()
    }

    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("251.*")
        version.set(project.version.toString())
        pluginDescription.set(
            """
            在多模块 Maven 工程中勾选叶子服务模块，通过 IntelliJ 内置 Maven Runner
            执行 <code>mvn clean package -pl &lt;相对路径&gt; -am</code>。
            """.trimIndent()
        )
        changeNotes.set("新增独立 Clean 执行；顶部按钮改为等宽布局，预设收入下拉框。")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
