# Gson reads the model metadata by reflection: the classes and their field names
# must survive R8, or the bundle is rejected at startup and the search falls back
# to demo mode.
-keep class com.luckyprospector.app.inference.ModelMetadata { *; }
-keep class com.luckyprospector.app.inference.ModelInputMetadata { *; }
-keepattributes Signature, *Annotation*
