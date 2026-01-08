// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.28" apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.6" apply false
}