package com.gusbauer.photocollage

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class FotoAdapter(
    private val fotos: List<Foto>,
    private val almacen: AlmacenFotos,
    private val seleccionActual: () -> String?,
    private val alSeleccionar: (String) -> Unit
) : RecyclerView.Adapter<FotoAdapter.FotoHolder>() {

    class FotoHolder(view: View) : RecyclerView.ViewHolder(view) {
        val borde: FrameLayout = view.findViewById(R.id.photoFrame)
        val imagen: ImageView = view.findViewById(R.id.imagePhoto)
        val numero: TextView = view.findViewById(R.id.textNumber)
        val texto: TextView = view.findViewById(R.id.textOverlay)
    }

    private val ejecutor = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private val cache = object : LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return FotoHolder(view)
    }

    override fun getItemCount(): Int = fotos.size

    override fun onBindViewHolder(holder: FotoHolder, position: Int) {
        val foto = fotos[position]
        holder.imagen.tag = foto.id
        holder.imagen.rotation = foto.rotacion.toFloat()
        holder.imagen.colorFilter = EfectosFoto.colorFilter(foto)
        holder.borde.setBackgroundResource(
            if (seleccionActual() == foto.id) R.drawable.photo_selected else R.drawable.photo_normal
        )
        holder.numero.text = if (seleccionActual() == foto.id) "✓ ${position + 1}" else "${position + 1}"
        holder.texto.text = foto.texto
        holder.texto.textSize = foto.tamanoTexto.toFloat()
        val lp = holder.texto.layoutParams as FrameLayout.LayoutParams
        lp.gravity = when (foto.posicionTexto) {
            0 -> android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
            1 -> android.view.Gravity.CENTER
            else -> android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
        }
        holder.texto.layoutParams = lp
        holder.itemView.setOnClickListener { alSeleccionar(foto.id) }

        val bitmap = cache.get(foto.id)
        if (bitmap != null) {
            holder.imagen.setImageBitmap(bitmap)
        } else {
            holder.imagen.setImageDrawable(null)
            ejecutor.execute {
                try {
                    val creado = almacen.bitmap(foto, 700)
                    main.post {
                        cache.put(foto.id, creado)
                        if (holder.imagen.tag == foto.id) holder.imagen.setImageBitmap(creado)
                    }
                } catch (_: Exception) {
                    main.post {
                        if (holder.imagen.tag == foto.id) holder.imagen.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                }
            }
        }
    }

    fun terminar() { ejecutor.shutdown() }
}
