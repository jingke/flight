# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.flightbooking.app.data.remote.dto.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
