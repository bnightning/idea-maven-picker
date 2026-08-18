plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.bnightning"
// 支持通过 CI/命令行传入 -PpluginVersion=xxx 来覆盖版本（例如从 GitHub tag 解析得到）
version = (findProperty("pluginVersion") as String?) ?: "0.0.2-alpha"

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
        changeNotes.set(
            """
            <ul>
              <li>新增 test/verify/install/deploy 与自定义 Goal</li>
              <li>Git 变更模块识别、多 Reactor 分批执行、最近使用记录</li>
              <li>预设 JSON 导入导出、Maven 自动刷新、依赖提示与快捷键</li>
            </ul>
            """.trimIndent()
        )
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
