package com.enrpau.dualscreendex

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false
    private var isPaused = false

    private var knownPokemonList: List<Pokemon> = emptyList()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == "PAUSE") {
            Log.d("DualDex", "Service Paused (Sleeping)")
            isPaused = true
            return START_STICKY
        }

        if (intent?.action == "RESUME") {
            Log.d("DualDex", "Service Resumed (Waking Up)")
            isPaused = false
            return START_STICKY
        }

        Log.d("DualDex", "Service Initializing...")

        createNotificationChannel()
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } catch (e: Exception) {
                startForeground(1, notification)
            }
        } else {
            startForeground(1, notification)
        }

        if (knownPokemonList.isEmpty()) {
            Thread {
                val dbHelper = PokedexHelper(this)
                knownPokemonList = dbHelper.getAllPokemon()
                    .sortedByDescending { it.name.length }
                Log.d("DualDex", "DB Loaded: ${knownPokemonList.size} Pokemon ready.")
            }.start()
        }

        val resultCode = intent?.getIntExtra("RESULT_CODE", 0) ?: 0
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("DATA", Intent::class.java)
        } else {
            intent?.getParcelableExtra("DATA")
        }

        // if init call fails or data is missing, bail out
        if (resultCode != Activity.RESULT_OK || data == null) {
            // keep running if we already have the projection setup
            if (mediaProjection == null) {
                Log.e("DualDex", "Failed to start: Missing permission data.")
                stopSelf()
                return START_NOT_STICKY
            }
            return START_STICKY
        }

        startProjection(resultCode, data)
        return START_STICKY
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, data)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        Log.d("DualDex", "Screen Size: ${width}x${height} - Density: $density")

        // using rgba_8888 cause it's safe everywhere
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, handler
        )

        handler.post(captureRunnable)
    }

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (!isPaused && !isProcessing) {
                captureAndProcess()
            }
            handler.postDelayed(this, 2000)
        }
    }

    private fun captureAndProcess() {
        val image = imageReader?.acquireLatestImage() ?: return
        isProcessing = true

        var bitmap = try {
            imageToBitmap(image)
        } catch (e: Exception) {
            Log.e("DualDex", "Bitmap error: ${e.message}")
            image.close()
            isProcessing = false
            return
        }

        image.close() // drop buffer asap

        if (bitmap == null) {
            isProcessing = false
            return
        }

        // crop to top half only, since that's usually where the enemy battle info is
        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height / 2)

        // feed cropped bit to ml kit
        val inputImage = InputImage.fromBitmap(croppedBitmap, 0)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                processDetectedText(visionText.text)
                isProcessing = false
            }
            .addOnFailureListener {
                isProcessing = false
            }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        if (rowPadding == 0) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun processDetectedText(rawText: String) {
        val cleanText = rawText.uppercase().replace("\n", " ")

        // grab every unique pokemon we see, not just the first match
        val matches = ArrayList<Pokemon>()
        val foundNames = HashSet<String>()

        for (p in knownPokemonList) {
            if (cleanText.contains(p.name.uppercase())) {
                // prevent dupes (e.g. don't add charizard twice)
                if (!foundNames.contains(p.name)) {
                    matches.add(p)
                    foundNames.add(p.name)
                }
            }
        }

        val intent = Intent("POKEMON_DETECTED")
        if (matches.isNotEmpty()) {
            val namesList = ArrayList<String>()
            val idsList = ArrayList<Int>()
            val t1List = ArrayList<String>()
            val t2List = ArrayList<String>()

            for (m in matches) {
                namesList.add(m.name)
                idsList.add(m.id)
                t1List.add(m.type1.name)
                t2List.add(m.type2?.name ?: "UNKNOWN")
            }

            Log.d("DualDex", "FOUND: $namesList")

            intent.putExtra("FOUND", true)
            intent.putStringArrayListExtra("NAMES", namesList)
            intent.putIntegerArrayListExtra("IDS", idsList)
            intent.putStringArrayListExtra("TYPE1S", t1List)
            intent.putStringArrayListExtra("TYPE2S", t2List)
        } else {
            intent.putExtra("FOUND", false)
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "screen_capture_channel",
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "screen_capture_channel")
            .setContentTitle("DualScreenDex Running")
            .setContentText("Scanning top screen...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        virtualDisplay?.release()
        imageReader?.close()
        handler.removeCallbacks(captureRunnable)
    }
}