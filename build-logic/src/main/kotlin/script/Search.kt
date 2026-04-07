/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package script

/**
 * A bracket depth aware indexOf
 */
fun indexOfAtDepth(text: String, searchTerm: String, targetDepth: Int, startIndex: Int = 0): Int {
    var currentDepth = 0
    var i = startIndex
    while (i < text.length) {
        val nextOpen = text.indexOf('{', i)
        val nextClose = text.indexOf('}', i)
        val nextBracket = when {
            nextOpen == -1 -> nextClose
            nextClose == -1 -> nextOpen
            else -> minOf(nextOpen, nextClose)
        }
        val segmentEnd = if (nextBracket == -1) text.length else nextBracket
        // Search the text between brackets
        if (currentDepth == targetDepth) {
            val searchStart = maxOf(i, startIndex)
            if (searchStart < segmentEnd) {
                val match = text.indexOf(searchTerm, searchStart)
                if (match != -1 && match < segmentEnd) {
                    return match
                }
            }
        }
        if (nextBracket == -1) {
            break
        }
        // Find the next bracket
        val bracketChar = text[nextBracket]
        if (bracketChar == '{') {
            if (currentDepth == targetDepth && nextBracket >= startIndex && text.startsWith(searchTerm, nextBracket)) {
                return nextBracket
            }
            currentDepth++
        } else {
            currentDepth--
            if (currentDepth == targetDepth && nextBracket >= startIndex && text.startsWith(searchTerm, nextBracket)) {
                return nextBracket
            }
        }
        i = nextBracket + 1
    }
    return -1
}
