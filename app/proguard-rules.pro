# kotlinx.serialization keeps generated serializers via its bundled consumer rules.
# Ktor references optional SLF4J classes that are not on the classpath.
-dontwarn org.slf4j.**
