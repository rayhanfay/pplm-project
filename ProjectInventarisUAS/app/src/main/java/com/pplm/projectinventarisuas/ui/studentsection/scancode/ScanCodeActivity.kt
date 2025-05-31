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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.databinding.ActivityScanCodeBinding
import com.pplm.projectinventarisuas.ui.studentsection.borrowing.BorrowingItemActivity
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import java.util.concurrent.Executors
import com.pplm.projectinventarisuas.R

class ScanCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanCodeBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var isActivityStarted = false
    private val itemRepository = ItemRepository()
    private val borrowingRepository = BorrowingRepository()
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
                    message = getString(R.string.camera_permission_denied),
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
                                                    message = getString(R.string.item_not_found, rawValue),
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
                                                        message = getString(R.string.item_in_use),
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
                                    message = getString(R.string.image_processing_failed, it.message),
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
        itemRepository.itemExists(itemId) { exists ->
            onResult(exists)
        }
    }

    private fun checkExistingBorrowing(itemId: String, onComplete: (Boolean) -> Unit) {
        borrowingRepository.getBorrowingByItemId(itemId) { borrowings ->
            val hasActiveBorrowing = borrowings.any { it.status == "Sedang Digunakan" }
            onComplete(hasActiveBorrowing)
        }
    }

    private fun startAutoCloseTimer() {
        shutdownHandler = Handler(mainLooper)
        shutdownRunnable = Runnable {
            if (!isDialogShowing) {
                isDialogShowing = true
                CustomDialog.alert(
                    context = this,
                    message = getString(R.string.scan_timeout),
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