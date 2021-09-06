package io.github.dzulfikar68.polandweatherapp

data class City (
    val id: Long,
    val name: String,
    val country: String,
    var lat: Double = 0.0,
    var long: Double = 0.0
) {
    override fun toString(): String {
        return name
    }
}
