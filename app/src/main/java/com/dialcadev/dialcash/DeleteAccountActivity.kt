package com.dialcadev.dialcash

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dialcadev.dialcash.data.UserDataStore
import com.dialcadev.dialcash.databinding.DeleteAccountActivityBinding
import com.dialcadev.dialcash.ui.viewmodel.DeleteAccountViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class DeleteAccountActivity : AppCompatActivity() {
    lateinit var binding: DeleteAccountActivityBinding

    @Inject
    lateinit var userDataStore: UserDataStore
    private val viewModel: DeleteAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DeleteAccountActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.delete_account)) { v, insets ->
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
        binding.btnCancel.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.confirmCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.btnDelete.isEnabled = isChecked
        }
        binding.btnDelete.setOnClickListener { deleteAccount() }
    }
    private fun deleteAccount() {
        if (!binding.confirmCheckbox.isChecked) return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                userDataStore.clearUserProfile()
                viewModel.deleteData()
            }
            Toast.makeText(this@DeleteAccountActivity, getString(R.string.account_deleted_successfully), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}