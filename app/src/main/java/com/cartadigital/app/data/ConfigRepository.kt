package com.cartadigital.app.data

data class Config(
    val restaurantName: String,
    val welcomeMessage: String
)

class ConfigRepository {
    fun getConfig(): Config {
        // Simulates fetching from Supabase
        return Config(
            restaurantName = "Camborio",
            welcomeMessage = "Bienvenido a Camborio"
        )
    }
}
