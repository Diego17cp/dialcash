package com.dialcadev.dialcash.core.utils.extensions

fun Double.toCurrencyFormat(): String {
    return String.format("%.2f", this)
}