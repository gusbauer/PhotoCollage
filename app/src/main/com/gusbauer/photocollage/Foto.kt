package com.gusbauer.photocollage

enum class Filtro { ORIGINAL, GRIS, SEPIA }

data class Foto(
    val id: String,
    val archivo: String,
    var filtro: Filtro = Filtro.ORIGINAL,
    var brillo: Int = 0,             // -100 .. +100
    var rotacion: Int = 0,          // 0, 90, 180, 270
    var saturacion: Int = 100,      // 0 .. 200 (100 = original)
    var texto: String = "",
    var tamanoTexto: Int = 22,
    var posicionTexto: Int = 2      // 0 arriba, 1 centro, 2 abajo
)
