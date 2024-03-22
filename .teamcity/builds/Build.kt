package builds

import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.toId
import jetbrains.buildServer.configs.kotlin.triggers.vcs

class Build(
    name: String,
    branchFilter: String,
    forPullRequests: Boolean,
    triggerRules: String? = null
) :
    Project({
      this.id(name.toId())
      this.name = name

      val complete = Empty("${name}-complete", "complete")

      val bts = sequential {
        if (forPullRequests)
            buildType(WhiteListCheck("${name}-whitelist-check", "white-list check"))
        if (forPullRequests) dependentBuildType(PRCheck("${name}-pr-check", "pr check"))
        parallel {
          listOf("11", "17", "21").forEach { java ->
            dependentBuildType(
                Maven(
                    "${name}-build-${java}",
                    "build - java ${java}",
                    "sortpom:verify license:check spotless:check compile",
                    "-DspotlessFiles=src/main/java/.*.java",
                    java))
          }
        }
        dependentBuildType(
            Maven(
                "${name}-test",
                "test",
                "verify",
                javaVersion = LTS_JAVA_VERSION,
                size = LinuxSize.LARGE))
        dependentBuildType(complete)
        if (!forPullRequests)
            collectArtifacts(dependentBuildType(Release("${name}-release", "release")))
      }

      bts.buildTypes().forEach {
        it.thisVcs()

        it.features {
          enableCommitStatusPublisher()
          if (forPullRequests) enablePullRequests()
        }

        buildType(it)
      }

      complete.triggers {
        vcs {
          this.branchFilter = branchFilter
          this.triggerRules = triggerRules
        }
      }
    })
