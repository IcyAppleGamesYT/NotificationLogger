# Add project specific ProGuard rules here.
-keep class com.discordnotificationlogger.database.** { *; }
-keep class com.discordnotificationlogger.service.** { *; }
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
