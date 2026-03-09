
import java.nio.charset.Charset
import kotlin.reflect.KClass

internal fun KClass<*>.resourceAsString(name: String) =
    java.getResourceAsStream(name)!!.readBytes().toString(Charset.defaultCharset())
