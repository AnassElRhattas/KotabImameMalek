plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.akherapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.akherapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-firestore:24.10.1")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.firebase:firebase-analytics:21.5.0")
    implementation ("com.airbnb.android:lottie:6.1.0")
    
    // Google Auth
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.auth:google-auth-library-oauth2-http:1.20.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")
    
    // Mise à jour des dépendances Google Play Services
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-phenotype:17.0.0")
    implementation("com.google.android.gms:play-services-basement:18.2.0")

    // Assurez-vous que ces versions sont cohérentes
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("androidx.multidex:multidex:2.0.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") {
        exclude(group = "com.android.support")
    }
    
    // Volley pour les requêtes HTTP
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation ("com.itextpdf:itextpdf:5.5.13.3")
    implementation ("com.itextpdf:itext7-core:7.2.5")
    implementation ("com.itextpdf:html2pdf:4.0.3")
    implementation ("com.google.android.gms:play-services-base:18.2.0")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")
}