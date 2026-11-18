# 📸 PhotoCollage

**Editor de collages fotográficos para Android, desarrollado con Kotlin.**

Proyecto práctico de **2.º DAM**: selecciona fotografías del dispositivo, organízalas en una cuadrícula, edítalas individualmente y exporta una composición en formato JPG.

![Android](https://img.shields.io/badge/Android-Nativo-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF?logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle&logoColor=white)
![API](https://img.shields.io/badge/Min%20SDK-29-blue)

## ✨ Funcionalidades

**Collage y selección**
- Selección múltiple de imágenes con Android Photo Picker.
- Hasta **30 fotografías** y detección de archivos duplicados mediante SHA-256.
- Cuadrícula adaptable con `RecyclerView` + `GridLayoutManager`, de **1 a 4 columnas**.
- Selección visual de una fotografía para editarla.
- Eliminar una foto, eliminar varias o vaciar el collage (con confirmación).
- Mezclar el orden de las imágenes.

**Edición fotográfica**
- Filtros **blanco y negro** y **sepia**.
- Ajustes de **brillo** y **saturación**.
- Rotación acumulativa de **90°**.
- Texto personalizado con tamaño y posición.
- Vista previa ampliada de una fotografía.
- Restauración de los ajustes originales de cada imagen.

**Exportación y almacenamiento**
- Generación de un **JPG** del collage.
- Guardado en la galería mediante `MediaStore`.
- Compartición segura mediante `Intent` + `FileProvider`.
- Conservación local de fotografías y ajustes al volver a abrir la aplicación.

> Los filtros se aplican sobre la foto seleccionada. La aplicación conserva una copia privada del original importado para poder restaurar los ajustes sin modificar la foto de la galería.

## 📱 Capturas de pantalla

![Captura 1](screenshots/Screenshot%202026-11-18%20135909.png)

![Captura 2](screenshots/Screenshot%202026-11-18%20140203.png)

<!-- Tras añadir las imágenes, puedes activar esta galería:
![Collage](screenshots/01-collage.png)
![Filtros](screenshots/03-filtros.png)
-->

## 🛠️ Tecnologías

| Tecnología | Uso |
|---|---|
| **Kotlin** | Lógica de la aplicación |
| **Android SDK** | Componentes Android nativos |
| **XML** | Interfaz gráfica |
| **RecyclerView** | Cuadrícula de fotografías |
| **Photo Picker** | Selección de imágenes sin acceso global a la galería |
| **ColorMatrix** | Filtros, brillo y saturación |
| **Canvas / Bitmap** | Renderizado del collage |
| **MediaStore** | Guardado del JPG |
| **FileProvider** | Compartición segura |
| **SharedPreferences** | Conservación del estado de edición |

## 🚀 Instalación y ejecución

**Requisitos:** Android Studio, JDK 21, Android SDK 36 y un dispositivo o emulador Android **API 29 o superior**.

1. Clona el repositorio:
   ```bash
   git clone https://github.com/gusbauer/PhotoCollage.git
   ```
2. Abre la carpeta `PhotoCollage` desde **Android Studio → File → Open**.
3. Selecciona **Gradle Wrapper** y **JDK 21**. Sincroniza el proyecto.
4. Inicia un dispositivo o emulador Android.
5. Selecciona la configuración `app` y pulsa **Run ▶**.

**Compilar desde Windows (PowerShell):**
```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

El APK de desarrollo se genera en `app/build/outputs/apk/debug/app-debug.apk`. Es un archivo compilado localmente, no es necesario incluirlo en Git.

## 🧭 Cómo utilizar PhotoCollage

1. Pulsa **Añadir fotos** y selecciona varias imágenes.
2. Toca una fotografía para resaltarla.
3. Aplica filtros, ajusta brillo/saturación o gira la foto.
4. Utiliza **Restaurar** para recuperar sus ajustes iniciales.
5. Reorganiza el collage o cambia las columnas.
6. Pulsa **Guardar** o **Compartir** para exportar la composición.

## 📂 Organización del código

```text
PhotoCollage/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gusbauer/photocollage/
│       │   ├── MainActivity.kt     # Interacción, estado y exportación
│       │   ├── Foto.kt             # Modelo de fotografía
│       │   ├── FotoAdapter.kt      # Celdas de la cuadrícula
│       │   └── Imagenes.kt         # Importación, filtros y renderizado
│       └── res/
│           ├── layout/            # Interfaces XML
│           ├── drawable/          # Resaltado de selección
│           └── xml/               # Configuración de FileProvider
├── gradle/wrapper/
├── gradlew.bat
├── build.gradle.kts
├── settings.gradle.kts
├── screenshots/                   # Capturas reales de la aplicación
└── README.md
```
