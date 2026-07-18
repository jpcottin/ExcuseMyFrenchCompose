# Add project specific ProGuard rules here.
#
# Library keep rules (kotlinx-serialization, Compose, OkHttp, AndroidX) are bundled as
# consumer rules in the dependencies themselves, so no manual rules are needed for them.

# Strip debug/verbose/info logging from release builds
-assumenosideeffects class android.util.Log {
 public static *** d(...);
 public static *** v(...);
 public static *** i(...);
}
