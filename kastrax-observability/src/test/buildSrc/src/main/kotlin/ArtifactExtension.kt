import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class ArtifactExtension @Inject constructor(@Suppress("UNUSED_PARAMETER") objectFactory: ObjectFactory) {
    var name: String = "Proto.Actor"
    var description: String = "Ultra-fast, distributed, cross-platform actors."
    var version: String = "0.1.0"
}
