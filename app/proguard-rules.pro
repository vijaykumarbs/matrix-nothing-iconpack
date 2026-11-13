# Keep the small UI layer (views are inflated by name from XML).
-keep class dev.vijaykumarbs.matrix.** { *; }

# No reflection, no serialization, no native code in this app.
-dontwarn androidx.**
-dontwarn kotlin.**
