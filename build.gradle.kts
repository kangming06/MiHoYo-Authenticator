buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://androidx.dev/snapshots/latest/artifacts/repository")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
