package fs02.gitignore.parser

import java.io.Reader
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.relativeTo

class Gitignore(reader: Reader, path: Path, baseDir: Path = path.parent) {
    private val rules = mutableListOf<IgnoreRule>()

    constructor(str: String, baseDir: Path): this(str.reader(), baseDir.resolve(".gitignore"), baseDir)
    constructor(path: Path, baseDir: Path = path.parent): this(path.toFile().bufferedReader(), baseDir.resolve(".gitignore"), baseDir)

    init {
        reader.useLines { lines ->
            lines.forEachIndexed { index, line ->
                val rule = ruleFromPattern(line.trim(), baseDir, Pair(path, index + 1))
                if (rule != null) {
                    rules.add(rule)
                }
            }
        }
    }

    fun match(path: String): Boolean {
        return match(Path.of(path))
    }

    fun match(path: Path): Boolean {
        return if (rules.none { it.negation }) {
            rules.any { it.match(path) }
        } else {
            handleNegation(path, rules)
        }
    }

    private fun handleNegation(filePath: Path, rules: List<IgnoreRule>): Boolean {
        for (rule in rules.reversed()) {
            if (rule.match(filePath)) {
                return !rule.negation
            }
        }
        return false
    }

    private fun ruleFromPattern(pattern: String, basePath: Path?, source: Pair<Path, Int>?): IgnoreRule? {
        if (basePath != null && basePath != basePath.toAbsolutePath().normalize()) {
            throw IllegalArgumentException("Base path must be absolute")
        }

        if (pattern.isBlank() || pattern[0] == '#') {
            return null
        }

        val negation = pattern.startsWith('!')
        val strippedPattern = if (negation) pattern.substring(1) else pattern

        val directoryOnly = strippedPattern.endsWith("/")
        var anchored = "/" in strippedPattern.dropLast(1)

        var adjustedPattern = strippedPattern
            .replace(Regex("([^/])\\*{2,}"), "$1*")
            .replace(Regex("\\*{2,}([^/])"), "*$1")

        if (adjustedPattern.startsWith("/")) {
            adjustedPattern = adjustedPattern.substring(1)
        }
        if (adjustedPattern.startsWith("**")) {
            adjustedPattern = adjustedPattern.substring(2)
            anchored = false
        }
        if (adjustedPattern.startsWith("/")) {
            adjustedPattern = adjustedPattern.substring(1)
        }
        if (adjustedPattern.endsWith("/")) {
            adjustedPattern = adjustedPattern.substring(0, adjustedPattern.length - 1)
        }
        if (adjustedPattern.startsWith("\\") && adjustedPattern[1] in listOf('#', '!')) {
            adjustedPattern = adjustedPattern.substring(1)
        }
        var i = adjustedPattern.length - 1
        var stripTrailingSpaces = true
        while (i > 1 && adjustedPattern[i] == ' ') {
            if (adjustedPattern[i - 1] == '\\') {
                adjustedPattern = adjustedPattern.substring(0, i - 1) + adjustedPattern.substring(i)
                i -= 1
                stripTrailingSpaces = false
            } else {
                if (stripTrailingSpaces) {
                    adjustedPattern = adjustedPattern.substring(0, i)
                }
            }
            i -= 1
        }

        val regex = fnmatchPathnameToRegex(adjustedPattern, directoryOnly, negation, anchored)
        return IgnoreRule(
            pattern = pattern,
            regex = regex,
            negation = negation,
            directoryOnly = directoryOnly,
            anchored = anchored,
            basePath = normalizePath(basePath),
            source = source
        )
    }

    private fun fnmatchPathnameToRegex(
        pattern: String,
        directoryOnly: Boolean,
        negation: Boolean,
        anchored: Boolean
    ): Regex {
        var i = 0
        val n = pattern.length

        val seps = listOf(
            Regex.escape(FileSystems.getDefault().separator.toString())
        )
        val sepsGroup = "([" + seps.joinToString("|") + "])"
        val nonsep = "[^${seps.joinToString("|")}]"

        val res = mutableListOf<String>()
        while (i < n) {
            val c = pattern[i]
            i += 1
            when (c) {
                '*' -> {
                    try {
                        if (pattern[i] == '*') {
                            i += 1
                            if (i < n && pattern[i] == '/') {
                                i += 1
                                res.add("(.*$sepsGroup)?")
                            } else {
                                res.add(".*")
                            }
                        } else {
                            res.add("$nonsep*")
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        res.add("$nonsep*")
                    }
                }
                '?' -> res.add(nonsep)
                '/' -> res.add(sepsGroup)
                '[' -> {
                    var j = i
                    if (j < n && pattern[j] == '!') {
                        j += 1
                    }
                    if (j < n && pattern[j] == ']') {
                        j += 1
                    }
                    while (j < n && pattern[j] != ']') {
                        j += 1
                    }
                    if (j >= n) {
                        res.add("\\[")
                    } else {
                        var stuff = pattern.substring(i, j).replace("\\", "\\\\").replace("/", "")
                        i = j + 1
                        if (stuff[0] == '!') {
                            stuff = "^" + stuff.substring(1)
                        } else if (stuff[0] == '^') {
                            stuff = "\\" + stuff
                        }
                        res.add("[$stuff]")
                    }
                }
                else -> res.add(Regex.escape(c.toString()))
            }
        }
        if (anchored) {
            res.add(0, "^")
        } else {
            res.add(0, "(^|$sepsGroup)")
        }
        if (!directoryOnly) {
            res.add("$")
        } else if (directoryOnly && negation) {
            res.add("/$")
        } else {
            res.add("($|\\/)") // You may need to adjust this line based on your exact requirements
        }
        return Regex(res.joinToString(""))
    }

    private fun normalizePath(path: Path?): Path {
        return path?.toAbsolutePath()?.normalize() ?: Path.of("")
    }

    companion object {
        private data class IgnoreRule(
            val pattern: String,
            val regex: Regex,
            val negation: Boolean,
            val directoryOnly: Boolean,
            val anchored: Boolean,
            val basePath: Path?,
            val source: Pair<Path, Int>?
        ) {
            fun match(absPath: Path): Boolean {
                val relPath = if (basePath != null) absPath.normalize().relativeTo(basePath).toString() else absPath.toString()
                return if (negation && absPath.toString().endsWith("/") && !directoryOnly) {
                    regex.containsMatchIn("$relPath/")
                } else {
                    regex.containsMatchIn(relPath)
                }
            }
        }
    }
}
