package com.pplm.projectinventarisuas.ui.adminsection.borrowing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.databinding.ActivityScanCodeBinding
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

class ScanReturnActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanCodeBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val borrowingRepository = BorrowingRepository()
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionAndStartCamera()
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
                finish()
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
                processImageProxy(imageProxy, scanner)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy, scanner: BarcodeScanner) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isProcessing) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val scannedItemId = barcode.rawValue
                        if (!scannedItemId.isNullOrEmpty()) {
                            isProcessing = true
                            checkBorrowingStatus(scannedItemId)
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal scan: ${it.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun checkBorrowingStatus(itemId: String) {
        borrowingRepository.getBorrowingByItemId(itemId) { borrowings ->
            if (borrowings.isNotEmpty()) {
                val latestBorrowing = borrowings.maxByOrNull { it.date_borrowed }
                latestBorrowing?.let {
                    when (it.status) {
                        "Returned" -> {
                            CustomDialog.alert(
                                context = this,
                                message = "Barang sudah dikembalikan",
                                onDismiss = { isProcessing = false }
                            )
                        }

                        "On Borrow" -> {
                            updateBorrowingStatusToReturned(it.borrowing_id, getCurrentTime())
                        }
                    }
                }
            } else {
                checkForBorrowingsOnBorrow(itemId)
            }
        }
    }

    private fun checkForBorrowingsOnBorrow(itemId: String) {
        borrowingRepository.getBorrowingsWithStatus(itemId, "On Borrow") { borrowings ->
            if (borrowings.isEmpty()) {
                CustomDialog.alert(
                    context = this,
                    message = "Semua barang dengan ID '$itemId' sudah dikembalikan",
                    onDismiss = { isProcessing = false }
                )
            } else {
                isProcessing = false
            }
        }
    }

    private fun updateBorrowingStatusToReturned(borrowingId: String, returnTime: String) {
        borrowingRepository.updateBorrowingStatus(borrowingId, "Returned", returnTime) { success ->
            if (success) {
                CustomDialog.alert(
                    context = this,
                    message = "Status berhasil diubah ke Returned",
                    onDismiss = { finish() }
                )
            } else {
                CustomDialog.alert(
                    context = this,
                    message = "Gagal update status",
                    onDismiss = { isProcessing = false }
                )
            }
        }
    }

    private fun getCurrentTime(): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(System.currentTimeMillis())
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}