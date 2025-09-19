package com.dialcadev.dialcash

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.dialcadev.dialcash.data.dto.OnboardingItem
import com.dialcadev.dialcash.databinding.OnboardingActivityBinding
import com.dialcadev.dialcash.onboarding.OnboardingAdapter
import com.dialcadev.dialcash.ui.onboarding.OnboardingViewModel
import com.google.android.material.imageview.ShapeableImageView
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {
    private val vm: OnboardingViewModel by viewModels()
    private lateinit var binding: OnboardingActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = OnboardingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.onboarding)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupViews()
    }

    private fun setupViews() {
        val pager = binding.viewPagerOnboarding
        val items = listOf(
            OnboardingItem(
                R.drawable.ic_3d_notepad,
                getString(R.string.onb_title_1),
                getString(R.string.onb_subtitle_1),
            ),
            OnboardingItem(
                R.drawable.ic_3d_wallet,
                getString(R.string.onb_title_2),
                getString(R.string.onb_subtitle_2),
            ),
            OnboardingItem(
                R.drawable.ic_3d_arrow_dart,
                getString(R.string.onb_title_3),
                getString(R.string.onb_subtitle_3),
            ),
            OnboardingItem(
                R.drawable.ic_3d_megaphone,
                getString(R.string.onb_title_4),
                getString(R.string.onb_subtitle_4),
            ),
            OnboardingItem(
                R.drawable.ic_3d_lock,
                getString(R.string.onb_title_5),
                getString(R.string.onb_subtitle_5),
            ),
            OnboardingItem(
                R.drawable.ic_3d_bulb,
                getString(R.string.onb_title_6),
                getString(R.string.onb_subtitle_6),
            )
        )
        val adapter = OnboardingAdapter(items)
        pager.adapter = adapter
        setupDotsIndicator(items.size)

        vm.isSeen.value.let { isSeen ->

        }
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                binding.btnNext.text = if (position == items.size - 1) "Finish" else "Next"
            }
        })
        binding.btnNext.setOnClickListener {
            if (pager.currentItem < items.size - 1) pager.currentItem = pager.currentItem + 1
            else {
                vm.markSeen()
                finishOnboarding()
            }
        }
        binding.btnSkip.setOnClickListener { finishOnboarding() }
        vm.isSeen.let { flow ->
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    vm.isSeen.collect { seen ->
                        if (seen) startMainAndFinish()
                    }
                }
            }
        }
    }

    private fun finishOnboarding() {
        vm.markSeen()
        startMainAndFinish()
    }

    private fun startMainAndFinish() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setupDotsIndicator(count: Int) {
        val container = binding.layoutIndicators
        container.removeAllViews()
        repeat(count) {
            val dot = ShapeableImageView(this).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.indicator_inactive))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 8, 0)
                }
            }
            container.addView(dot)
        }
        updateDots(0)
    }

    private fun updateDots(active: Int) {
        for (i in 0 until binding.layoutIndicators.childCount) {
            val iv = binding.layoutIndicators.getChildAt(i) as ShapeableImageView
            val res =
                if (i == active) R.drawable.indicator_active else R.drawable.indicator_inactive
            iv.setImageDrawable(ContextCompat.getDrawable(this, res))
        }
    }
}