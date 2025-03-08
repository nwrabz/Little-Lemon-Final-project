package com.example.littlelemon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable

data class MenuNetwork(
    @SerialName("menu")
    val menu: List<MenuItemNetwork>,
): List<MenuItemNetwork> by menu {
    companion object {
        fun MenuNetwork.toDbModel() =
            map { it.toMenuItemRoom() }
    }
}

@Serializable
data class MenuItemNetwork(

    @SerialName("id")
    val id: Int,

    @SerialName("title")
    val title: String,

    @SerialName("price")
    val price: Double,

    ) {

    fun toMenuItemRoom() = MenuItemRoom(
        id,
        title,
        price
    )
}
