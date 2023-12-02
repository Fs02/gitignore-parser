import fs02.gitignore.parser.Gitignore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitignoreTest {

    @Test
    fun testSimple() {
        val gitignore = Gitignore(
            "__pycache__/\n" +
                    "*.py[cod]",
            Path.of("/home/michael")
        )
        assertFalse(gitignore.match("/home/michael/main.py"))
        assertTrue(gitignore.match("/home/michael/main.pyc"))
        assertTrue(gitignore.match("/home/michael/dir/main.pyc"))
        assertTrue(gitignore.match("/home/michael/__pycache__"))
    }

    @Test
    fun testIncompleteFilename() {
        val gitignore = Gitignore("o.py",  Path.of("/home/michael/.gitignore"))
        assertTrue(gitignore.match("/home/michael/o.py"))
        assertFalse(gitignore.match("/home/michael/foo.py"))
        assertFalse(gitignore.match("/home/michael/o.pyc"))
        assertTrue(gitignore.match("/home/michael/dir/o.py"))
        assertFalse(gitignore.match("/home/michael/dir/foo.py"))
        assertFalse(gitignore.match("/home/michael/dir/o.pyc"))
    }

    @Test
    fun testWildcard() {
        val gitignore = Gitignore(
            "hello.*",
            Path.of("/home/michael")
        )
        assertTrue(gitignore.match("/home/michael/hello.txt"))
        assertTrue(gitignore.match("/home/michael/hello.foobar/"))
        assertTrue(gitignore.match("/home/michael/dir/hello.txt"))
        assertTrue(gitignore.match("/home/michael/hello."))
        assertFalse(gitignore.match("/home/michael/hello"))
        assertFalse(gitignore.match("/home/michael/helloX"))
    }

    // ... (similar conversion for other test cases)

    @Test
    fun testSlashInRangeDoesNotMatchDirs() {
        val gitignore = Gitignore("abc[X-Z/]def", Path.of("/home/michael"))
        assertFalse(gitignore.match("/home/michael/abcdef"))
        assertTrue(gitignore.match("/home/michael/abcXdef"))
        assertTrue(gitignore.match("/home/michael/abcYdef"))
        assertTrue(gitignore.match("/home/michael/abcZdef"))
        assertFalse(gitignore.match("/home/michael/abc/def"))
        assertFalse(gitignore.match("/home/michael/abcXYZdef"))
    }

    @Test
    fun testSymlinkToAnotherDirectory() {
        val projectDir = createTempDirectory()
        val anotherDir = createTempDirectory()

        val gitignore = Gitignore("link", projectDir)

        // Create a symlink to another directory.
        val link = projectDir.resolve("link")
        val target = anotherDir.resolve("target")
        target.toFile().mkdirs()
        link.toFile().deleteOnExit()
        Files.createSymbolicLink(link, target)

        // Check the intended behavior
        assertTrue(gitignore.match(link))
    }
}
