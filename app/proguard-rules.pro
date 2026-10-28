-keepattributes Signature
-keepattributes *Annotation*
-keep class com.sfl.notifapp.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
