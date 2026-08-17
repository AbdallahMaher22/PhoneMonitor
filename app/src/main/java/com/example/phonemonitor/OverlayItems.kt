package com.example.phonemonitor

import android.content.Context

object OverlayItems {
    data class Item(val key: String, val labelRes: Int, val icon: String) {
        fun label(context: Context): String = context.getString(labelRes)
    }

    val ALL = listOf(
        Item("battery", R.string.item_battery, "🔋"),
        Item("temp", R.string.item_temp, "🌡️"),
        Item("ram", R.string.item_ram, "🧠"),
        Item("cpu", R.string.item_cpu, "⚙️"),
        Item("time", R.string.item_time, "🕐"),
        Item("uptime", R.string.item_uptime, "⏱️"),
        Item("storage", R.string.item_storage, "💾"),
        Item("network", R.string.item_network, "📶"),
        Item("load", R.string.item_load, "📊"),
        Item("charging", R.string.item_charging, "🔌"),
        Item("fps", R.string.item_fps, "🎮"),
        Item("ping", R.string.item_ping, "📡"),
        Item("netspeed", R.string.item_netspeed, "🚀")
    )
}
