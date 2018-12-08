package com.beatrate.diffview
import kotlinx.html.*
import kotlinx.html.dom.*
import kotlinx.html.stream.appendHTML
import java.io.File

//val myDiv = document.create.div {
//    p { +"text inside" }
//}

fun main(args: Array<String>) {
    val resetStyle = "margin:0; padding:0; border:0; outline:0;"
    val text = createHTMLDocument()
    File("index.html").printWriter().use {out ->
        out.appendHTML().html {
            body {
                //func with old strings
                div {
                    style = "width:200px;"
                    p {
                        style = "background-color:ffeef0; " + resetStyle
                        +"hello"
                    }
                    p {
                        style = "background-color:ffeef0; " + resetStyle
                        +"hello second"
                    }
                }

                //func determine position new strings

                //func with new strings
                div {
                    style = "width:200px;"
                    p {
                        style = "background-color:e6ffed; "  + resetStyle
                        +"hello"
                    }
                    p {
                        style = "background-color:e6ffed; "  + resetStyle
                        +"hello second"
                    }
                }
            }
        }
    }

}