package bridge

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import validate.ValidationTree
import validate.Validations
import kotlin.experimental.ExperimentalNativeApi

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = false
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("validate")
fun validate(inputJson: CPointer<ByteVar>?, outputBuffer: CPointer<ByteVar>?, bufferSize: Int) =
    invokeBridge(inputJson, outputBuffer = outputBuffer, bufferSize = bufferSize) { input ->
        val graphModel = GraphSpec.Json.decodeFromString(content = input[0])
        val validation = ValidationTree()
        validation.build(Validations.all)
        val issues = validation.validate(graphModel)
        json.encodeToString(issues)
    }
