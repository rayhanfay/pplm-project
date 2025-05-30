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
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.databinding.ActivityScanCodeBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

class ScanReturnActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanCodeBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val borrowingRepository = BorrowingRepository()
    private val itemRepository = ItemRepository()
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

                        "In Use" -> {
                            updateBorrowingStatusToReturned(it, getCurrentTime())
                        }
                    }
                } ?: run {
                    checkItemExists(itemId)
                }
            } else {
                checkItemExists(itemId)
            }
        }
    }

    private fun checkItemExists(itemId: String) {
        itemRepository.getItemById(itemId) { item ->
            if (item != null) {
                CustomDialog.alert(
                    context = this,
                    message = "Barang dengan ID '$itemId' terdaftar, tetapi tidak sedang dipinjam.",
                    onDismiss = { isProcessing = false }
                )
            } else {
                CustomDialog.alert(
                    context = this,
                    message = "Barang dengan ID '$itemId' tidak terdaftar.",
                    onDismiss = { isProcessing = false }
                )
            }
        }
    }

    private fun updateBorrowingStatusToReturned(borrowing: Borrowing, returnTime: String) {
        borrowingRepository.updateBorrowingStatus(
            borrowing.borrowing_id,
            "Returned",
            returnTime
        ) { success ->
            if (success) {
                val itemId = borrowing.item_id
                itemRepository.updateItemStatus(itemId, "Available") { itemUpdated ->
                    if (itemUpdated) {
                        CustomDialog.alert(
                            context = this,
                            message = "Barang berhasil dikembalikan dan status item diubah ke 'Available'",
                            onDismiss = { finish() }
                        )
                    } else {
                        CustomDialog.alert(
                            context = this,
                            message = "Status pengembalian berhasil, tapi gagal update status item",
                            onDismiss = { finish() }
                        )
                    }
                }
            } else {
                CustomDialog.alert(
                    context = this,
                    message = "Gagal update status peminjaman",
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