package com.dialcadev.dialcash

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.backup.BackupManager
import com.dialcadev.dialcash.databinding.ImportDataActivityBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import javax.inject.Inject

@AndroidEntryPoint
class ImportDataActivity : AppCompatActivity() {
    private lateinit var binding: ImportDataActivityBinding

    @Inject
    lateinit var backupManager: BackupManager

    private val documentPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleSelectedFile(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ImportDataActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.import_data_layout)) { v, insets ->
            val systemBars =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.confirmCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.btnImport.isEnabled = isChecked
        }
        binding.btnImport.setOnClickListener { openFilePicker() }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        documentPicker.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                binding.btnImport.isEnabled = false
                binding.btnImport.text = "Importing..."
                val mime = contentResolver.getType(uri)
                val name = queryDisplayName(uri)
                val lowerName = name?.lowercase() ?: ""
                if (!lowerName.endsWith(".json") && !lowerName.endsWith(".backup")) {
                    Toast.makeText(
                        this@ImportDataActivity,
                        "Please select a valid .json or .backup file",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnImport.isEnabled = true
                    binding.btnImport.text = "Import Data"
                    return@launch
                }
                contentResolver.openInputStream(uri)?.use { rawStream ->
                    val bis =
                        if (rawStream is BufferedInputStream) rawStream else BufferedInputStream(
                            rawStream
                        )
                    bis.mark(2048)
                    val headerBytes = ByteArray(1024)
                    val read = bis.read(headerBytes)
                    val header = if (read > 0) String(
                        headerBytes,
                        0,
                        read,
                        Charsets.UTF_8
                    ).trimStart() else ""
                    bis.reset()

                    if (!header.startsWith("{") && !header.startsWith("[")) {
                        Toast.makeText(
                            this@ImportDataActivity,
                            "Selected file is not a valid JSON backup.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.btnImport.isEnabled = true
                        binding.btnImport.text = "Import Data"
                        return@use
                    }

                    // If header looks ok, proceed to import
                    val bundle = try {
                        backupManager.importFromInputStream(bis)
                    } catch (e: IllegalArgumentException) {
                        throw e
                    }

                    backupManager.restoreFromBundle(bundle)
                } ?: run {
                    Toast.makeText(
                        this@ImportDataActivity,
                        "Failed to open selected file.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnImport.isEnabled = true
                    binding.btnImport.text = "Import Data"
                    return@launch
                }

                setResult(Activity.RESULT_OK)
                Toast.makeText(
                    this@ImportDataActivity,
                    "Data imported successfully",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                binding.btnImport.isEnabled = true
                binding.btnImport.text = "Import Data"
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        var displayName: String? = null
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) displayName = cursor.getString(idx)
                }
            }
        return displayName
    }
}