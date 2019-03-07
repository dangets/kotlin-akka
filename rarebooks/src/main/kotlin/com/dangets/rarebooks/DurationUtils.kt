package com.dangets.rarebooks

import scala.concurrent.duration.FiniteDuration
import java.time.Duration
import java.util.concurrent.TimeUnit

fun Duration.toFiniteDuration(): FiniteDuration = FiniteDuration(this.toMillis(), TimeUnit.MILLISECONDS)
