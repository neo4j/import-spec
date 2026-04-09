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
package model.extension

import kotlinx.js.JsPlainObject

@JsExport
@JsPlainObject
external interface ExtensionValueJs {
    val type: String
}

fun ExtensionValue.toJs(): ExtensionValueJs = when (this) {
    is StringValue -> toJs()
    is BooleanValue -> toJs()
    is LongValue -> toJs()
    is DoubleValue -> toJs()
    is ListValue -> toJs()
    is MapValue -> toJs()
}

fun ExtensionValueJs.toClass(): ExtensionValue = when (this.type) {
    ExtensionType.STRING -> (this as StringValueJs).toClass()
    ExtensionType.BOOLEAN -> (this as BooleanValueJs).toClass()
    ExtensionType.LONG -> (this as LongValueJs).toClass()
    ExtensionType.DOUBLE -> (this as DoubleValueJs).toClass()
    ExtensionType.LIST -> (this as ListValueJs).toClass()
    ExtensionType.MAP -> (this as MapValueJs).toClass()
    else -> error("Unexpected extension type: ${this.type}")
}
