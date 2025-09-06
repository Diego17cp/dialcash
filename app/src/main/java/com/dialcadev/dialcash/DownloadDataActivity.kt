package com.dialcadev.dialcash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.backup.BackupManager
import com.dialcadev.dialcash.databinding.DownloadDataActivityBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadDataActivity : AppCompatActivity() {
    private lateinit var binding: DownloadDataActivityBinding

    @Inject
    lateinit var backupManager: BackupManager

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            uri?.let { saveBackupToUri(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DownloadDataActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.download_data_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
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
            binding.btnDownload.isEnabled = isChecked
        }
        binding.btnDownload.setOnClickListener { downloadBackup() }
    }

    private fun downloadBackup() {
        if (!binding.confirmCheckbox.isChecked) {
            Toast.makeText(this, "Please confirm to proceed", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = "dialcash_data_${System.currentTimeMillis()}.backup"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, fileName)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "*/*"))
        }


        try {
            createDocumentLauncher.launch(fileName)
        } catch (e: Exception) {
            Toast.makeText(this, "Error searching other apps", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun saveBackupToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                binding.btnDownload.isEnabled = false
                binding.btnDownload.text = "Generating backup..."

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    backupManager.exportToOutputStream(outputStream)
                }

                Toast.makeText(
                    this@DownloadDataActivity,
                    "Backup saved successfully",
                    Toast.LENGTH_LONG
                ).show()
                onBackPressedDispatcher.onBackPressed()
            } catch (e: Exception) {
                Toast.makeText(
                    this@DownloadDataActivity,
                    "Failed to save backup: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnDownload.isEnabled = true
                binding.btnDownload.text = "Download Data"
            }
        }
    }

}