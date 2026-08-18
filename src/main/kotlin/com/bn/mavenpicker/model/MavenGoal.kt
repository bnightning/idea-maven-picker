package com.bnightning.mavenpicker.model

object MavenGoal {
    const val CLEAN = "clean"
    const val TEST = "test"
    const val VERIFY = "verify"
    const val PACKAGE = "package"
    const val INSTALL = "install"
    const val DEPLOY = "deploy"
    const val CUSTOM = "custom"

    val PREDEFINED = listOf(CLEAN, TEST, VERIFY, PACKAGE, INSTALL, DEPLOY)

    fun requiresConfirmation(goal: String): Boolean = goal == DEPLOY

    fun goalsForExecution(primaryGoal: String, cleanBeforePackage: Boolean): List<String> {
        if (primaryGoal == CUSTOM || primaryGoal.isBlank()) return listOf(PACKAGE)
        if (primaryGoal == PACKAGE && cleanBeforePackage) return listOf(CLEAN, PACKAGE)
        return listOf(primaryGoal)
    }
}
