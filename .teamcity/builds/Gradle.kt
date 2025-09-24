package builds

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.toId

class Gradle(
    id: String,
    name: String,
    goals: String,
    args: String? = null,
    javaVersion: String = DEFAULT_JAVA_VERSION,
    size: LinuxSize = LinuxSize.SMALL
) :
    BuildType({
      this.id(id.toId())
      this.name = name

      steps {
        runGradle(javaVersion) {
            this.tasks = goals
            this.jvmArgs = "$GRADLE_DEFAULT_ARGS ${args ?: ""}"
        }
      }

      features { dockerSupport {} }

      requirements { runOnLinux(size) }
    })
