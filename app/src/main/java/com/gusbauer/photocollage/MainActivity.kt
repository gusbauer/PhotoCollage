package com.gusbauer.photocollage

import android.app.AlertDialog
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val fotos = mutableListOf<Foto>()
    private lateinit var almacen: AlmacenFotos
    private lateinit var adaptador: FotoAdapter
    private lateinit var grid: RecyclerView
    private lateinit var textoVacio: TextView
    private lateinit var textoCantidad: TextView
    private lateinit var textoBrillo: TextView
    private lateinit var textoSaturacion: TextView
    private lateinit var barraBrillo: SeekBar
    private lateinit var barraSaturacion: SeekBar
    private lateinit var botonColumnas: Button
    private lateinit var botonAnadir: Button
    private lateinit var botonCompartir: Button
    private lateinit var botonGuardar: Button

    private var idSeleccionada: String? = null
    private var columnas = 2
    private var actualizandoBarras = false
    private val trabajador = Executors.newSingleThreadExecutor()

    private val preferencias by lazy { getSharedPreferences("photocollage", MODE_PRIVATE) }

    // El Photo Picker gestiona el acceso sin permisos de galeria globales.
    private val selector = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        val yaExistentes = fotos.map { it.id }.toMutableSet()
        botonAnadir.isEnabled = false
        trabajador.execute {
            val nuevas = mutableListOf<Foto>()
            var duplicadas = 0
            var fallidas = 0
            for (uri in uris) {
                if (yaExistentes.size >= 30) break
                try {
                    val foto = almacen.importar(uri, yaExistentes)
                    if (foto != null) nuevas.add(foto) else duplicadas++
                } catch (_: Exception) {
                    fallidas++
                }
            }
            runOnUiThread {
                fotos.addAll(nuevas)
                if (idSeleccionada == null) idSeleccionada = fotos.firstOrNull()?.id
                guardarEstado()
                refrescar()
                botonAnadir.isEnabled = true
                mensaje("Añadidas: ${nuevas.size} · Repetidas: $duplicadas · Fallidas: $fallidas")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        almacen = AlmacenFotos(this)
        cargarEstado()

        grid = findViewById(R.id.recyclerPhotos)
        textoVacio = findViewById(R.id.textEmpty)
        textoCantidad = findViewById(R.id.textCount)
        textoBrillo = findViewById(R.id.textBrightness)
        textoSaturacion = findViewById(R.id.textSaturation)
        barraBrillo = findViewById(R.id.seekBrightness)
        barraSaturacion = findViewById(R.id.seekSaturation)
        botonColumnas = findViewById(R.id.btnColumns)
        botonAnadir = findViewById(R.id.btnAdd)
        botonCompartir = findViewById(R.id.btnShare)
        botonGuardar = findViewById(R.id.btnSave)

        adaptador = FotoAdapter(
            fotos,
            almacen,
            { idSeleccionada },
            alSeleccionar = { id ->
                idSeleccionada = id
                guardarEstado()
                refrescar()
            },
            alAmpliar = { id ->
                idSeleccionada = id
                guardarEstado()
                refrescar()
                mostrarFotoGrande()
            }
        )
        grid.layoutManager = GridLayoutManager(this, columnas)
        grid.adapter = adaptador
        grid.isNestedScrollingEnabled = false

        botonAnadir.setOnClickListener {
            selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            val foto = seleccionada() ?: return@setOnClickListener
            fotos.remove(foto)
            almacen.borrar(foto)
            idSeleccionada = null
            guardarEstado()
            refrescar()
            mensaje("Fotografía eliminada")
        }
        // Eliminar varias fotos mediante un dialogo, sin perder la seleccion individual para editar.
        findViewById<Button>(R.id.btnDeleteMultiple).setOnClickListener {
            eliminarVariasFotos()
        }
        findViewById<Button>(R.id.btnPreview).setOnClickListener {
            mostrarFotoGrande()
        }
        findViewById<Button>(R.id.btnShuffle).setOnClickListener {
            if (fotos.size < 2) {
                mensaje("Necesitas al menos dos fotografías para mezclar")
            } else {
                fotos.shuffle()
                guardarEstado()
                refrescar()
                mensaje("Orden del collage mezclado")
            }
        }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            if (fotos.isEmpty()) { mensaje("El collage ya está vacío"); return@setOnClickListener }
            AlertDialog.Builder(this)
                .setTitle("Vaciar collage")
                .setMessage("¿Eliminar todas las fotografías?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ ->
                    fotos.forEach { almacen.borrar(it) }
                    fotos.clear()
                    idSeleccionada = null
                    guardarEstado()
                    refrescar()
                }.show()
        }

        findViewById<Button>(R.id.btnGray).setOnClickListener { editarSeleccionada { it.filtro = Filtro.GRIS } }
        findViewById<Button>(R.id.btnSepia).setOnClickListener { editarSeleccionada { it.filtro = Filtro.SEPIA } }
        findViewById<Button>(R.id.btnRotate).setOnClickListener {
            editarSeleccionada { it.rotacion = (it.rotacion + 90) % 360 }
        }
        findViewById<Button>(R.id.btnRestore).setOnClickListener {
            editarSeleccionada {
                it.filtro = Filtro.ORIGINAL
                it.brillo = 0
                it.rotacion = 0
                it.saturacion = 100
                it.texto = ""
                it.tamanoTexto = 22
                it.posicionTexto = 2
            }
        }
        findViewById<Button>(R.id.btnText).setOnClickListener { dialogoTexto() }

        barraBrillo.setOnSeekBarChangeListener(escuchaBarra { valor ->
            editarSeleccionada(false) { it.brillo = valor - 100 }
        })
        barraSaturacion.setOnSeekBarChangeListener(escuchaBarra { valor ->
            editarSeleccionada(false) { it.saturacion = valor }
        })

        botonColumnas.setOnClickListener {
            columnas = if (columnas == 4) 1 else columnas + 1
            (grid.layoutManager as GridLayoutManager).spanCount = columnas
            guardarEstado()
            refrescar()
        }
        botonCompartir.setOnClickListener { exportar(compartir = true) }
        botonGuardar.setOnClickListener { exportar(compartir = false) }
        refrescar()
    }

    // Mejora extra: eliminar varias fotos elegidas, con confirmacion.
    private fun eliminarVariasFotos() {
        if (fotos.isEmpty()) {
            mensaje("El collage está vacío")
            return
        }
        val instantanea = fotos.toList()
        val etiquetas = instantanea.mapIndexed { i, foto ->
            "Foto ${i + 1} · ${foto.filtro.name.lowercase()}"
        }.toTypedArray()
        val marcadas = BooleanArray(instantanea.size)

        AlertDialog.Builder(this)
            .setTitle("Selecciona las fotos que quieres eliminar")
            .setMultiChoiceItems(etiquetas, marcadas) { _, indice, elegida ->
                marcadas[indice] = elegida
            }
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Continuar") { _, _ ->
                val seleccion = instantanea.filterIndexed { i, _ -> marcadas[i] }
                if (seleccion.isEmpty()) {
                    mensaje("No has marcado ninguna fotografía")
                    return@setPositiveButton
                }
                AlertDialog.Builder(this)
                    .setTitle("Eliminar ${seleccion.size} fotografías")
                    .setMessage("Esta operación quitará las fotos marcadas del collage.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Eliminar") { _, _ ->
                        val ids = seleccion.map { it.id }.toSet()
                        // Solo borrar archivos de fotos que siguen en el collage.
                        val aBorrar = fotos.filter { it.id in ids }
                        fotos.removeAll { it.id in ids }
                        aBorrar.forEach { almacen.borrar(it) }
                        if (idSeleccionada in ids) idSeleccionada = null
                        guardarEstado()
                        refrescar()
                        mensaje("Eliminadas ${aBorrar.size} fotografías")
                    }
                    .show()
            }
            .show()
    }

    // Mejora extra: vista previa ampliada con las ediciones reales de la foto.
    private fun mostrarFotoGrande() {
        val foto = seleccionada()?.copy() ?: return
        val densidad = resources.displayMetrics.density
        val alto = (310 * densidad).toInt()
        val visor = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.WHITE)
            contentDescription = "Vista previa ampliada de la fotografía"
        }
        val contenedor = LinearLayout(this).apply {
            setPadding(20, 12, 20, 12)
            addView(
                visor,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, alto)
            )
        }
        val dialogo = AlertDialog.Builder(this)
            .setTitle("Vista previa de la fotografía")
            .setView(contenedor)
            .setPositiveButton("Cerrar", null)
            .create()

        var bitmapVisible: Bitmap? = null
        dialogo.setOnDismissListener {
            visor.setImageDrawable(null)
            bitmapVisible?.recycle()
            bitmapVisible = null
        }
        dialogo.show()

        trabajador.execute {
            try {
                // Usa el mismo renderizador que compartir/guardar: incluye filtro, texto y giro.
                val imagen = RenderCollage.generar(listOf(foto), 1, almacen)
                runOnUiThread {
                    if (dialogo.isShowing && !isFinishing && !isDestroyed) {
                        bitmapVisible = imagen
                        visor.setImageBitmap(imagen)
                    } else {
                        imagen.recycle()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (dialogo.isShowing) mensaje("No se pudo ampliar: ${e.message}")
                }
            }
        }
    }

    private fun escuchaBarra(cambio: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser && !actualizandoBarras) cambio(progress)
        }
        override fun onStartTrackingTouch(bar: SeekBar?) = Unit
        override fun onStopTrackingTouch(bar: SeekBar?) = Unit
    }

    private fun seleccionada(): Foto? {
        val foto = fotos.firstOrNull { it.id == idSeleccionada }
        if (foto == null) mensaje("Primero selecciona una fotografía")
        return foto
    }

    private fun editarSeleccionada(refrescarBarras: Boolean = true, bloque: (Foto) -> Unit) {
        val foto = seleccionada() ?: return
        bloque(foto)
        guardarEstado()
        val indice = fotos.indexOf(foto)
        if (indice >= 0) adaptador.notifyItemChanged(indice)
        if (refrescarBarras) sincronizarBarras()
        else {
            textoBrillo.text = "Brillo: ${foto.brillo}"
            textoSaturacion.text = "Saturación: ${foto.saturacion} %"
        }
    }

    private fun sincronizarBarras() {
        actualizandoBarras = true
        val foto = fotos.firstOrNull { it.id == idSeleccionada }
        barraBrillo.isEnabled = foto != null
        barraSaturacion.isEnabled = foto != null
        barraBrillo.progress = (foto?.brillo ?: 0) + 100
        barraSaturacion.progress = foto?.saturacion ?: 100
        textoBrillo.text = "Brillo: ${foto?.brillo ?: 0}"
        textoSaturacion.text = "Saturación: ${foto?.saturacion ?: 100} %"
        actualizandoBarras = false
    }

    private fun refrescar() {
        textoVacio.visibility = if (fotos.isEmpty()) View.VISIBLE else View.GONE
        grid.visibility = if (fotos.isEmpty()) View.GONE else View.VISIBLE
        textoCantidad.text = "${fotos.size} fotografías · Máximo 30"
        botonColumnas.text = "Columnas: $columnas"
        adaptador.notifyDataSetChanged()
        sincronizarBarras()
    }

    // Ampliación: texto, tamaño y posición configurables para cada fotografía.
    private fun dialogoTexto() {
        val foto = seleccionada() ?: return
        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 16, 40, 16)
        }
        val entrada = EditText(this).apply { hint = "Texto de la foto"; setText(foto.texto) }
        contenedor.addView(entrada)
        val etiqueta = TextView(this).apply { text = "Tamaño: ${foto.tamanoTexto} sp" }
        contenedor.addView(etiqueta)
        val tamanio = SeekBar(this).apply { max = 32; progress = foto.tamanoTexto - 16 }
        tamanio.setOnSeekBarChangeListener(escuchaBarra { etiqueta.text = "Tamaño: ${it + 16} sp" })
        contenedor.addView(tamanio)
        val posicion = Spinner(this)
        posicion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Arriba", "Centro", "Abajo"))
        posicion.setSelection(foto.posicionTexto)
        contenedor.addView(posicion)
        AlertDialog.Builder(this)
            .setTitle("Texto de la fotografía")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar") { _, _ ->
                editarSeleccionada {
                    it.texto = entrada.text.toString().take(32)
                    it.tamanoTexto = tamanio.progress + 16
                    it.posicionTexto = posicion.selectedItemPosition
                }
            }.show()
    }

    // Guarda el collage como JPG, o lo envía a otras aplicaciones usando FileProvider.
    private fun exportar(compartir: Boolean) {
        if (fotos.isEmpty()) { mensaje("Añade fotografías antes de exportar"); return }
        val copia = fotos.map { it.copy() }
        val columnasActuales = columnas
        botonCompartir.isEnabled = false
        botonGuardar.isEnabled = false
        trabajador.execute {
            try {
                val bitmap = RenderCollage.generar(copia, columnasActuales, almacen)
                if (compartir) {
                    val carpeta = File(cacheDir, "exports").apply { mkdirs() }
                    val archivo = File(carpeta, "collage_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(archivo).use {
                        check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it))
                    }
                    bitmap.recycle()
                    runOnUiThread {
                        try {
                            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivo)
                            val intento = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newRawUri("PhotoCollage", uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(Intent.createChooser(intento, "Compartir PhotoCollage"))
                        } catch (e: Exception) {
                            mensaje("No se pudo compartir: ${e.message}")
                        } finally { habilitarExportacion() }
                    }
                } else {
                    guardarEnGaleria(bitmap)
                    bitmap.recycle()
                    runOnUiThread {
                        habilitarExportacion()
                        mensaje("Collage guardado en Fotos / PhotoCollage")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    habilitarExportacion()
                    mensaje("Error exportando collage: ${e.message}")
                }
            }
        }
    }

    private fun guardarEnGaleria(bitmap: android.graphics.Bitmap) {
        val valores = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoCollage_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PhotoCollage")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores)
            ?: error("No se pudo crear el archivo")
        try {
            resolver.openOutputStream(uri)?.use {
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it))
            } ?: error("No se pudo escribir la imagen")
            val terminado = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, terminado, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun habilitarExportacion() {
        botonCompartir.isEnabled = true
        botonGuardar.isEnabled = true
    }

    // Mantiene selección, efectos y rutas de las imágenes entre reinicios y rotación del móvil.
    private fun guardarEstado() {
        val array = JSONArray()
        fotos.forEach { foto ->
            array.put(JSONObject().apply {
                put("id", foto.id)
                put("archivo", foto.archivo)
                put("filtro", foto.filtro.name)
                put("brillo", foto.brillo)
                put("rotacion", foto.rotacion)
                put("saturacion", foto.saturacion)
                put("texto", foto.texto)
                put("tamanoTexto", foto.tamanoTexto)
                put("posicionTexto", foto.posicionTexto)
            })
        }
        preferencias.edit()
            .putString("fotos", array.toString())
            .putString("seleccion", idSeleccionada)
            .putInt("columnas", columnas)
            .apply()
    }

    private fun cargarEstado() {
        columnas = preferencias.getInt("columnas", 2).coerceIn(1, 4)
        idSeleccionada = preferencias.getString("seleccion", null)
        try {
            val array = JSONArray(preferencias.getString("fotos", "[]") ?: "[]")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val foto = Foto(
                    id = obj.getString("id"),
                    archivo = obj.getString("archivo"),
                    filtro = runCatching { Filtro.valueOf(obj.optString("filtro", "ORIGINAL")) }.getOrDefault(Filtro.ORIGINAL),
                    brillo = obj.optInt("brillo", 0),
                    rotacion = obj.optInt("rotacion", 0),
                    saturacion = obj.optInt("saturacion", 100),
                    texto = obj.optString("texto", ""),
                    tamanoTexto = obj.optInt("tamanoTexto", 22),
                    posicionTexto = obj.optInt("posicionTexto", 2)
                )
                if (File(filesDir, "photos/${foto.archivo}").exists()) fotos.add(foto)
            }
        } catch (_: Exception) {
            fotos.clear()
        }
        if (fotos.none { it.id == idSeleccionada }) idSeleccionada = null
    }

    private fun mensaje(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        if (::adaptador.isInitialized) adaptador.terminar()
        trabajador.shutdown()
        super.onDestroy()
    }
}
