package com.bnightning.mavenpicker.model

data class MavenModuleInfo(
    /** 相对 Reactor 根目录的路径选择器，例如 `dq-services/dq-auth-service-v2` */
    val selector: String,
    val groupId: String,
    val artifactId: String,
    val packaging: String,
    val relativePath: String,
    val reactorRootDir: String,
    val groupName: String,
    val isAggregator: Boolean
)

data class MavenScanResult(
    val reactorRootDir: String,
    val modules: List<MavenModuleInfo>,
    val errorMessage: String? = null
) {
    val leafModules: List<MavenModuleInfo> get() = modules.filter { !it.isAggregator }
    val hasError: Boolean get() = errorMessage != null
}
