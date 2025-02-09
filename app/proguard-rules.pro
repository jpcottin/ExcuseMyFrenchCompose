# Add project specific ProGuard rules here.

# Keep our model classes
-keep class com.example.excusemyfrenchcompose.data.model.** { *; }

# Keep @Composable annotation
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.animation.* <methods>;
}
# Needed for androidx.activity.compose.setContent
-keep class androidx.activity.ComponentActivity { *; }

# Keep Kotlin metadata
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, Signature
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlinx.serialization.** { *; }

# Keep Serialization related classes and members.
-keepclassmembers class com.example.excusemyfrenchcompose.** {
  kotlinx.serialization.KSerializer serializer(...);
}
# Keep anything that uses @Keep
-keep @interface androidx.annotation.Keep
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keep class * {
  @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
  @androidx.annotation.Keep <methods>;
}
#OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# R8 Optimization, helps with Compose performance
-assumenosideeffects class android.util.Log {
 public static *** d(...);
 public static *** v(...);
 public static *** i(...);
}