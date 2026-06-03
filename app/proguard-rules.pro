# Add project specific ProGuard rules here.

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,allowdictionarywarnings class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** {
    static <fields>;
}
-keep interface kotlinx.serialization.** { *; }

# Keep all data models and API interfaces from being obfuscated 
# (This is what caused the API data to disappear/crash in the release build!)
-keep class com.pitchpulse.data.model.** { *; }
-keep class com.pitchpulse.data.remote.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }