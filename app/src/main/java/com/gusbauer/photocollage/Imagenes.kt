package com.gusbauer.photocollage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

/** Importa originales a almacenamiento privado, sin alterar las fotos de galería. */
class AlmacenFotos(private val context: Context) {
    val carpeta = File(context.filesDir, "photos").apply { mkdirs() }

    fun importar(uri: Uri, idsExistentes: MutableSet<String>): Foto? {
        val temporal = File.createTempFile("import_", ".tmp", carpeta)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val entrada = context.contentResolver.openInputStream(uri)
                ?: error("No se pudo abrir la foto")
            entrada.use { input ->
                FileOutputStream(temporal).use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= 30L * 1024 * 1024) { "Foto demasiado grande (max. 30 MB)" }
                        digest.update(buffer, 0, n)
                        output.write(buffer, 0, n)
                    }
                    require(total > 0) { "La imagen esta vacia" }
                }
            }
            val id = digest.digest().joinToString("") { "%02x".format(it) }
            if (!idsExistentes.add(id)) return null
            val final = File(carpeta, "$id.image")
            if (!final.exists()) {
                if (!temporal.renameTo(final)) temporal.copyTo(final, overwrite = true)
            }
            return Foto(id, final.name)
        } finally {
            temporal.delete()
        }
    }

    fun archivo(foto: Foto): File = File(carpeta, foto.archivo)

    /** Reduce las dimensiones antes de decodificar para limitar la memoria. API 29+. */
    fun bitmap(foto: Foto, ladoMax: Int = 1000): Bitmap {
        val fuente = ImageDecoder.createSource(archivo(foto))
        return ImageDecoder.decodeBitmap(fuente) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val factor = min(1f, ladoMax.toFloat() / max(width, height))
            decoder.setTargetSize(max(1, (width * factor).toInt()), max(1, (height * factor).toInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    fun borrar(foto: Foto) { archivo(foto).delete() }
}

object EfectosFoto {
    fun colorFilter(foto: Foto): ColorMatrixColorFilter {
        val base = when (foto.filtro) {
            Filtro.ORIGINAL -> ColorMatrix()
            Filtro.GRIS -> ColorMatrix().apply { setSaturation(0f) }
            Filtro.SEPIA -> ColorMatrix(floatArrayOf(
                .393f, .769f, .189f, 0f, 0f,
                .349f, .686f, .168f, 0f, 0f,
                .272f, .534f, .131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        val saturacion = ColorMatrix().apply { setSaturation(foto.saturacion / 100f) }
        val conSaturacion = ColorMatrix().apply { setConcat(base, saturacion) }
        val b = foto.brillo.toFloat()
        val brillo = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        ))
        return ColorMatrixColorFilter(ColorMatrix().apply { setConcat(brillo, conSaturacion) })
    }
}

/** Genera la imagen final con las MISMAS ediciones vistas en pantalla. */
object RenderCollage {
    fun generar(fotos: List<Foto>, columnas: Int, almacen: AlmacenFotos): Bitmap {
        require(fotos.isNotEmpty()) { "El collage esta vacio" }
        val lado = if (fotos.size > 15) 320 else 450
        val filas = (fotos.size + columnas - 1) / columnas
        val resultado = Bitmap.createBitmap(columnas * lado, filas * lado, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultado)
        canvas.drawColor(Color.WHITE)

        for ((index, foto) in fotos.withIndex()) {
            val imagen = almacen.bitmap(foto, 950)
            val col = index % columnas
            val fila = index / columnas
            val centroX = col * lado + lado / 2f
            val centroY = fila * lado + lado / 2f

            canvas.save()
            canvas.translate(centroX, centroY)
            canvas.clipRect(-lado / 2f, -lado / 2f, lado / 2f, lado / 2f)
            canvas.rotate(foto.rotacion.toFloat())
            val factor = max(lado.toFloat() / imagen.width, lado.toFloat() / imagen.height)
            val mitadAncho = imagen.width * factor / 2f
            val mitadAlto = imagen.height * factor / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = EfectosFoto.colorFilter(foto)
            }
            canvas.drawBitmap(imagen, null, RectF(-mitadAncho, -mitadAlto, mitadAncho, mitadAlto), paint)
            canvas.restore()
            imagen.recycle()

            if (foto.texto.isNotBlank()) {
                val x = centroX
                val y = when (foto.posicionTexto) {
                    0 -> fila * lado + lado * 0.14f
                    1 -> centroY
                    else -> (fila + 1) * lado - lado * 0.09f
                }
                val tamano = lado * (foto.tamanoTexto / 400f)
                val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    textSize = tamano
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    style = Paint.Style.STROKE
                    strokeWidth = max(2f, lado * 0.008f)
                    color = Color.BLACK
                }
                val blanco = Paint(borde).apply {
                    style = Paint.Style.FILL
                    color = Color.WHITE
                }
                val texto = foto.texto.take(32)
                canvas.drawText(texto, x, y, borde)
                canvas.drawText(texto, x, y, blanco)
            }
        }
        return resultado
    }
}
