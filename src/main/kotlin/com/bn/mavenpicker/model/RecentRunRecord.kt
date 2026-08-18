package com.bnightning.mavenpicker.model

data class RecentRunRecord(
    val selectors: List<String>,
    val goal: String,
    val cleanBeforePackage: Boolean,
    val alsoMake: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun displayName(): String {
        val goalText = if (goal == MavenGoal.PACKAGE && cleanBeforePackage) "clean+package" else goal
        val modules = selectors.take(2).joinToString(", ")
        val suffix = if (selectors.size > 2) " 等${selectors.size}个" else ""
        return "$goalText · $modules$suffix"
    }
}
