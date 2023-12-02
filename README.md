# gitignore-parser

[![](https://jitpack.io/v/Fs02/gitignore-parser.svg)](https://jitpack.io/#Fs02/gitignore-parser)

A spec-compliant gitignore parser for Kotlin/JVM.

Kotlin port from: https://github.com/mherrmann/gitignore_parser

## Download

```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Fs02:gitignore-parser:1.0.0'
}
```

## Usage

Suppose `/home/michael/project/.gitignore` contains the following:

```
# Ignore Gradle project-specific cache directory
.gradle

# Ignore Gradle build output directory
build
```

Then:

```kotlin
import com.github.fs02.gitignore.parser.Gitignore
import java.nio.file.Path

fun main() {
    // Example usage
    val gitignorePath = Path.of("/home/project/.gitignore")
    val gitignore = Gitignore(gitignorePath)
    println("Is Ignored: ${gitignore.match(Path.of("/home/project/build"))}")
}
```
