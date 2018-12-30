package com.beatrate.diffview

import com.beatrate.diffview.common.*
import org.junit.Test
import java.io.File
import java.lang.IllegalArgumentException

class VisualizerTest {
    @Test
    fun test() {
        val content =
""" public class HelloWorld {

     public static void main(String[] args) {
-        // Prints "Hello, World" to the terminal window.
-        System.out.println("Hello, World");
+        // Prints "Hello, World"
+        System.out.print("Hello, ");
+        System.out.println("World");
     }

 }"""
        val lines = content.lines().map {line ->
            val kind = when {
                line.isBlank() || line.first() == ' '  -> LineKind.REGULAR
                line.first() == '-' -> LineKind.DELETED
                line.first() == '+' -> LineKind.ADDED
                else -> throw IllegalArgumentException("line")
            }
            Line(kind, if (line.isEmpty()) line else line.substring(1))
        }
        val hunk = Hunk(LineRange(19, 8), LineRange(19, 9), lines)
        val originalFile = File("src/test/resources/HelloWorld.java")

        // Test here...
    }
}