package com.dangets.officialdocs.clustering

data class StatsJob(val text: String)
data class StatsResult(val meanWordLength: Double)

data class JobFailed(val reason: String)