# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jules/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If you use reflection or introspection add keeprules here.
# Please consult the ProGuard manual for further guidance.
-dontwarn org.jetbrains.kotlin.**

# Keep Kotlinx Serialization classes and their serializers
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class *$$serializer { *; }

# Keep companion objects of serializable classes
-keepclassmembers class * {
    @kotlinx.serialization.Serializable companion object;
}
# Keep static serializer() method if present (for older kotlinx.serialization versions or specific patterns)
-keepclassmembers class * {
    @kotlinx.serialization.Serializable public static fun serializer(...);
}

# For enums used with Kotlinx Serialization
-keepclassmembers enum * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# Keep Kotlin metadata for reflection if it's used implicitly by libraries or your code.
# Hilt and other annotation processors might rely on this.
-keep kotlin.Metadata

# General Hilt rules are usually handled by the Hilt Gradle plugin, but these are common additions.
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel
-keep class * implements dagger.hilt.EntryPoint
-keep @dagger.hilt.InstallIn class *
-keep @dagger.Module class *
-keep @dagger.hilt.DefineComponent class *
-keep @dagger.hilt.components.SingletonComponent class *
# Add other Hilt components if used directly

# Retrofit and OkHttp might need rules if using advanced features or if issues arise with obfuscation
# -keep interface retrofit2.Call
# -keep class retrofit2.Response
# -keep class com.squareup.okhttp3.** { *; }
# -dontwarn com.squareup.okhttp3.**
# -dontwarn retrofit2.**
# -keepattributes Signature
# -keepattributes InnerClasses
# -keepclasseswithmembers class * {
#    @retrofit2.http.* <methods>;
# }
