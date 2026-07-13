# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.noahlin.nikonpicturecontrol.** {
    *** Companion;
}
-keepclasseswithmembers class com.noahlin.nikonpicturecontrol.** {
    kotlinx.serialization.KSerializer serializer(...);
}
