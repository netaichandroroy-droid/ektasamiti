# Add project specific ProGuard rules here.
# Keep Room entities, DAOs, and Moshi/Kotlin serialization classes safe in release APK/AAB builds.

-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
}
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
