package builds

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport

private const val DRY_RUN = "dry-run"

class Release(id: String, name: String) :
    BuildType({
      this.id(id.toId())
      this.name = name

      templates(AbsoluteId("FetchSigningKey"))

      params {
        text(
            "releaseVersion",
            "",
            label = "Version to release",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false)
        text(
            "nextSnapshotVersion",
            "",
            label = "Next snapshot version",
            description = "Next snapshot version to set after release",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false)

        checkbox(
            DRY_RUN,
            "true",
            "Dry run?",
            description = "Whether to perform a dry run where nothing is published and released",
            display = ParameterDisplay.PROMPT,
            checked = "true",
            unchecked = "false")

        text("env.JRELEASER_DRY_RUN", "%$DRY_RUN%")

        password("env.JRELEASER_GITHUB_TOKEN", "%github-pull-request-token%")
        password("env.OSSSONATYPEORG_USERNAME", "%osssonatypeorg-username%")
        password("env.OSSSONATYPEORG_PASSWORD", "%osssonatypeorg-password%")
        password("env.SIGNING_KEY_PASSPHRASE", "%signing-key-passphrase%")
      }

      steps {
        setVersion("Set release version", "%releaseVersion%")

        runMaven(DEFAULT_JAVA_VERSION) {
          this.name = "Build versioned packages"
          goals = "deploy"
          runnerArgs = "$MAVEN_DEFAULT_ARGS -Ppublication -Dmaven.test.skip -Dspotless.skip"
        }

        commitAndPush(
            "Push release version",
            "build: release version %releaseVersion%",
            dryRunParameter = DRY_RUN)

        runMaven(DEFAULT_JAVA_VERSION) {
          this.name = "Release to Github"
          goals = "jreleaser:auto-config-release"
          runnerArgs = "$MAVEN_DEFAULT_ARGS -Prelease"
        }

        setVersion("Set next snapshot version", "%nextSnapshotVersion%")

        commitAndPush(
            "Push next snapshot version",
            "build: update version to %nextSnapshotVersion%",
            dryRunParameter = DRY_RUN)

        publishToMavenCentral("Publish to Maven Central", dryRunParameter = DRY_RUN)
      }

      dependencies {
        artifacts(AbsoluteId("Tools_ReleaseTool")) {
          buildRule = lastSuccessful()
          artifactRules = "rt.jar => lib"
        }
      }

      features { dockerSupport {} }

      requirements { runOnLinux(LinuxSize.SMALL) }
    })
