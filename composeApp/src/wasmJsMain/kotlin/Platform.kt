package com.waliahimanshu.wealthmate

private fun jsDateNow(): Double = js("Date.now()")

actual fun currentTimeMillis(): Long = jsDateNow().toLong()
