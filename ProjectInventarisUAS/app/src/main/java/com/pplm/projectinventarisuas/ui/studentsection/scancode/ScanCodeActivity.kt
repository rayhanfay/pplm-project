package com.pplm.projectinventarisuas.ui.studentsection.scancode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Size
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.pplm.projectinventarisuas.databinding.ActivityScanCodeBinding
import com.pplm.projectinventarisuas.ui.studentsection.borrowing.BorrowingItemActivity
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import java.util.concurrent.Executors

class ScanCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanCodeBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var isActivityStarted = false
    private val database = FirebaseDatabase.getInstance().reference
    private var shutdownHandler: Handler? = null
    private var shutdownRunnable: Runnable? = null
    private var isDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionAndStartCamera()
        startAutoCloseTimer()
    }

    private fun requestPermissionAndStartCamera() {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                CustomDialog.alert(
                    context = this,
                    message = "Izin kamera ditolak",
                    onDismiss = { finish() }
                )
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                val rawValue = barcode.rawValue
                                if (rawValue != null && !isActivityStarted) {
                                    isActivityStarted = true
                                    checkItemExists(rawValue) { exists ->
                                        if (!exists) {
                                            if (!isDialogShowing) {
                                                isDialogShowing = true
                                                CustomDialog.alert(
                                                    context = this,
                                                    message = "Item dengan kode '$rawValue' tidak ditemukan.",
                                                    onDismiss = {
                                                        isDialogShowing = false
                                                        isActivityStarted = false
                                                    }
                                                )
                                            }
                                            return@checkItemExists
                                        }

                                        checkExistingBorrowing(rawValue) { hasActiveBorrowing ->
                                            if (hasActiveBorrowing) {
                                                if (!isDialogShowing) {
                                                    isDialogShowing = true
                                                    CustomDialog.alert(
                                                        context = this,
                                                        message = "Item sedang dipinjam dan belum dikembalikan",
                                                        onDismiss = {
                                                            isDialogShowing = false
                                                            isActivityStarted = false
                                                        }
                                                    )
                                                }
                                            } else {
                                                val intent = Intent(this, BorrowingItemActivity::class.java).apply {
                                                    putExtra("ITEM_CODE", rawValue)
                                                }
                                                startActivity(intent)
                                                finish()
                                            }
                                        }
                                    }
                                    break
                                }
                            }
                        }
                        .addOnFailureListener {
                            if (!isDialogShowing) {
                                isDialogShowing = true
                                CustomDialog.alert(
                                    context = this,
                                    message = "Gagal memproses gambar: ${it.message}",
                                    onDismiss = {
                                        isDialogShowing = false
                                        isActivityStarted = false
                                    }
                                )
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkItemExists(itemId: String, onResult: (Boolean) -> Unit) {
        database.child("item").child(itemId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onResult(snapshot.exists())
                }

                override fun onCancelled(error: DatabaseError) {
                    if (!isDialogShowing) {
                        isDialogShowing = true
                        CustomDialog.alert(
                            context = this@ScanCodeActivity,
                            message = "Gagal memeriksa item: ${error.message}",
                            onDismiss = {
                                isDialogShowing = false
                                isActivityStarted = false
                            }
                        )
                    }
                    onResult(false)
                }
            })
    }

    private fun checkExistingBorrowing(itemId: String, onComplete: (Boolean) -> Unit) {
        database.child("borrowing").orderByChild("item_id").equalTo(itemId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hasActiveBorrowing = snapshot.children.any {
                        it.child("status").getValue(String::class.java) == "On Borrow"
                    }
                    onComplete(hasActiveBorrowing)
                }

                override fun onCancelled(error: DatabaseError) {
                    if (!isDialogShowing) {
                        isDialogShowing = true
                        CustomDialog.alert(
                            context = this@ScanCodeActivity,
                            message = "Gagal mengecek peminjaman: ${error.message}",
                            onDismiss = {
                                isDialogShowing = false
                                isActivityStarted = false
                            }
                        )
                    }
                    onComplete(false)
                }
            })
    }

    private fun startAutoCloseTimer() {
        shutdownHandler = Handler(mainLooper)
        shutdownRunnable = Runnable {
            if (!isDialogShowing) {
                isDialogShowing = true
                CustomDialog.alert(
                    context = this,
                    message = "Pemindaian kedaluwarsa. Silakan coba lagi.",
                    onDismiss = {
                        isDialogShowing = false
                        finish()
                    }
                )
            }
        }
        shutdownHandler?.postDelayed(shutdownRunnable!!, 60000)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        shutdownHandler?.removeCallbacks(shutdownRunnable!!)
    }
}