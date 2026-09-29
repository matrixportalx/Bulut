# kotlinx.serialization: @Serializable sınıfların üretilmiş serializer'ları
# yalnızca yansıma ile bulunuyor; R8 onları kullanılmamış sanıp atıyor.
-keepclassmembers class tr.bulut.** {
    *** Companion;
}
-keepclasseswithmembers class tr.bulut.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class tr.bulut.**$$serializer { *; }
