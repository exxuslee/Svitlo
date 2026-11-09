-keepattributes *Annotation*, InnerClasses, EnclosingMethod

# Kotlinx Serialization
-dontwarn kotlinx.serialization.**
-keepclasseswithmembers class **$$serializer {
    *;
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

# WorkManager Workers
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# AndroidX Startup Initializers
-keep class * extends androidx.startup.Initializer {
    <init>();
}
