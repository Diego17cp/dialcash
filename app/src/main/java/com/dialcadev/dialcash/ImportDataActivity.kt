package com.dialcadev.dialcash

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import javax.inject.Inject

@AndroidEntryPoint
class ImportDataActivity: AppCompatActivity() {
    private lateinit var binding: ImportDataActivityBinding

    @Inject
    lateinit var backupManager: BackupManager

    private val documentPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
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
    private fun setupListeners(){
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
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bundle = backupManager.importFromInputStream(inputStream)
                    backupManager.restoreFromBundle(bundle)
                }
                setResult(Activity.RESULT_OK)
                Toast.makeText(this@ImportDataActivity, "Data imported successfully", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                binding.btnImport.isEnabled = true
                binding.btnImport.text = "Import Data"
            }
        }
    }
}