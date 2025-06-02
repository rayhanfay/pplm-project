package com.pplm.projectinventarisuas.ui.adminsection.borrowing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
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
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.databinding.ActivityScanCodeBinding
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import java.text.SimpleDateFormat
import java.util.Locale
import com.pplm.projectinventarisuas.R
import java.util.concurrent.Executors
import android.view.View
import java.util.concurrent.atomic.AtomicInteger
import android.util.Log

class ScanReturnActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanCodeBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val borrowingRepository = BorrowingRepository()
    private val itemRepository = ItemRepository()
    private var shutdownHandler: Handler? = null
    private var shutdownRunnable: Runnable? = null
    private var isDialogShowing = false
    private var isProcessing = false
    private var camera: Camera? = null
    private var isFlashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionAndStartCamera()
        startAutoCloseTimer()

        setupFlashButton()
    }

    private fun setupFlashButton() {
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }
    }

    private fun toggleFlash() {
        camera?.cameraControl?.enableTorch(!isFlashOn)?.let {
            it.addListener(Runnable {
                isFlashOn = !isFlashOn
                updateFlashButtonIcon()
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun updateFlashButtonIcon() {
        if (isFlashOn) {
            binding.btnFlash.setImageResource(R.drawable.ic_flash_on)
        } else {
            binding.btnFlash.setImageResource(R.drawable.ic_flash_off)
        }
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
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                binding.btnFlash.visibility = View.VISIBLE
            } else {
                binding.btnFlash.visibility = View.GONE
            }

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
        Log.d("ScanReturn", "Checking borrowing status for item: $itemId")

        borrowingRepository.getBorrowingByItemId(itemId) { borrowings ->
            Log.d("ScanReturn", "Found ${borrowings.size} borrowing records for item $itemId")

            if (borrowings.isNotEmpty()) {
                borrowings.forEachIndexed { index, borrowing ->
                    Log.d("ScanReturn", "Borrowing $index: ID=${borrowing.borrowing_id}, Status=${borrowing.status}, ItemID=${borrowing.item_id}")
                }

                val activeBorrowings = borrowings.filter {
                    it.status in listOf("Dipinjam", "Terlambat", "Hilang")
                }

                Log.d("ScanReturn", "Found ${activeBorrowings.size} active borrowings")
                activeBorrowings.forEach { borrowing ->
                    Log.d("ScanReturn", "Active borrowing: ID=${borrowing.borrowing_id}, Status=${borrowing.status}")
                }

                if (activeBorrowings.isEmpty()) {
                    Log.d("ScanReturn", "All borrowings already returned")
                    CustomDialog.success(
                        context = this,
                        message = getString(R.string.item_already_returned),
                        onDismiss = { isProcessing = false }
                    )
                } else {
                    Log.d("ScanReturn", "Updating ${activeBorrowings.size} active borrowings to returned")
                    updateAllActiveBorrowingsToReturned(activeBorrowings, itemId)
                }
            } else {
                Log.d("ScanReturn", "No borrowing records found, checking if item exists")
                checkItemExists(itemId)
            }
        }
    }

    private fun updateAllActiveBorrowingsToReturned(activeBorrowings: List<Borrowing>, itemId: String) {
        val returnTime = getCurrentTime()
        val totalBorrowings = activeBorrowings.size
        val completedUpdates = AtomicInteger(0)
        var hasFailure = false

        Log.d("ScanReturn", "Starting update of $totalBorrowings active borrowings with return time: $returnTime")

        val checkCompletion = { success: Boolean, borrowingId: String ->
            if (!success) {
                hasFailure = true
                Log.e("ScanReturn", "Failed to update borrowing: $borrowingId")
            } else {
                Log.d("ScanReturn", "Successfully updated borrowing: $borrowingId")
            }

            val completed = completedUpdates.incrementAndGet()
            Log.d("ScanReturn", "Completed updates: $completed / $totalBorrowings")

            if (completed == totalBorrowings) {
                if (hasFailure) {
                    Log.e("ScanReturn", "Some borrowing updates failed")
                    CustomDialog.alert(
                        context = this,
                        message = "Beberapa data peminjaman gagal diupdate",
                        onDismiss = { isProcessing = false }
                    )
                } else {
                    Log.d("ScanReturn", "All borrowing updates successful, updating item status")
                    updateItemStatusToAvailable(itemId)
                }
            }
        }

        activeBorrowings.forEach { borrowing ->
            Log.d("ScanReturn", "Updating borrowing ${borrowing.borrowing_id} from ${borrowing.status} to Dikembalikan")

            borrowingRepository.updateBorrowingStatus(
                borrowing.borrowing_id,
                "Dikembalikan",
                returnTime
            ) { success ->
                checkCompletion(success, borrowing.borrowing_id)
            }
        }
    }

    private fun updateItemStatusToAvailable(itemId: String) {
        Log.d("ScanReturn", "Updating item $itemId status to Tersedia")

        itemRepository.updateItemStatus(itemId, "Tersedia") { itemUpdated ->
            if (itemUpdated) {
                Log.d("ScanReturn", "Item $itemId status updated successfully")
                CustomDialog.success(
                    context = this,
                    message = getString(R.string.return_success_item_available),
                    onDismiss = {
                        Log.d("ScanReturn", "Return process completed successfully")
                        finish()
                    }
                )
            } else {
                Log.e("ScanReturn", "Failed to update item $itemId status")
                CustomDialog.alert(
                    context = this,
                    message = getString(R.string.return_success_item_update_failed),
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun checkItemExists(itemId: String) {
        itemRepository.getItemById(itemId) { item ->
            if (item != null) {
                CustomDialog.alert(
                    context = this,
                    message = getString(R.string.item_registered_not_borrowed, itemId),
                    onDismiss = { isProcessing = false }
                )
            } else {
                CustomDialog.alert(
                    context = this,
                    message = getString(R.string.item_not_registered, itemId),
                    onDismiss = { isProcessing = false }
                )
            }
        }
    }

    private fun getCurrentTime(): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(System.currentTimeMillis())
    }

    private fun startAutoCloseTimer() {
        shutdownHandler = Handler(mainLooper)
        shutdownRunnable = Runnable {
            if (!isDialogShowing) {
                isDialogShowing = true
                CustomDialog.alert(
                    context = this,
                    message = getString(R.string.scan_expired),
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