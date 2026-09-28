package com.bitbytestudio.physicstodo.Utils

import kotlin.random.Random

fun Float.range(
    min: Float,
    max: Float
): Float {

    return min +
            Random.nextFloat() *
            (max - min)
}

