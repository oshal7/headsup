# Bujho Toh! — R8 rules.
# The app uses no reflection-based serialization (decks are parsed with org.json),
# so the default optimized rules are enough. Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
