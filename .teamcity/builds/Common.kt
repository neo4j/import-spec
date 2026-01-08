package builds

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.script

const val GITHUB_OWNER = "neo4j"
const val GITHUB_REPOSITORY = "import-spec"
const val MAVEN_DEFAULT_ARGS = "--no-transfer-progress --batch-mode --show-version"

const val DEFAULT_JAVA_VERSION = "17"
const val LTS_JAVA_VERSION = "21"

const val SEMGREP_DOCKER_IMAGE = "semgrep/semgrep:1.146.0"

enum class LinuxSize(val value: String) {
  SMALL("small"),
  LARGE("large")
}

fun Requirements.runOnLinux(size: LinuxSize = LinuxSize.SMALL) {
  startsWith("cloud.amazon.agent-name-prefix", "linux-${size.value}")
}

fun BuildType.thisVcs() = vcs {
  root(DslContext.settingsRoot)

  cleanCheckout = true
}

fun BuildFeatures.enableCommitStatusPublisher() = commitStatusPublisher {
  vcsRootExtId = DslContext.settingsRoot.id.toString()
  publisher = github {
    githubUrl = "https://api.github.com"
    authType = personalToken { token = "%github-commit-status-token%" }
  }
}

fun BuildFeatures.enablePullRequests() = pullRequests {
  vcsRootExtId = DslContext.settingsRoot.id.toString()
  provider = github {
    authType = token { token = "%github-pull-request-token%" }
    filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
  }
}

fun CompoundStage.dependentBuildType(bt: BuildType) =
    buildType(bt) {
      onDependencyCancel = FailureAction.CANCEL
      onDependencyFailure = FailureAction.FAIL_TO_START
    }

fun collectArtifacts(buildType: BuildType): BuildType {
  buildType.artifactRules =
      """
        +:target/staging-deploy => packages
    """
          .trimIndent()

  return buildType
}

fun BuildSteps.runMaven(
    javaVersion: String = DEFAULT_JAVA_VERSION,
    init: MavenBuildStep.() -> Unit
): MavenBuildStep {
  val maven =
      this.maven {
        dockerImagePlatform = MavenBuildStep.ImagePlatform.Linux
        dockerImage = "eclipse-temurin:${javaVersion}-jdk"
        dockerRunParameters = "--volume /var/run/docker.sock:/var/run/docker.sock"
      }

  init(maven)
  return maven
}

fun BuildSteps.setVersion(name: String, version: String): MavenBuildStep {
  return this.runMaven {
    this.name = name
    goals = "versions:set"
    runnerArgs = "$MAVEN_DEFAULT_ARGS -DnewVersion=$version -DgenerateBackupPoms=false"
  }
}

fun BuildSteps.commitAndPush(
    name: String,
    commitMessage: String,
    includeFiles: String = "\\*pom.xml",
    dryRunParameter: String = "dry-run"
): ScriptBuildStep {
  return this.script {
    this.name = name
    scriptContent =
        """
          #!/bin/bash -eu              
         
          git add $includeFiles
          git commit -m "$commitMessage"
          git push
        """
            .trimIndent()

    conditions { doesNotMatch(dryRunParameter, "true") }
  }
}

fun BuildSteps.publishToMavenCentral(
    name: String,
    dryRunParameter: String = "dry-run"
): ScriptBuildStep {
  return this.script {
    this.name = name

    scriptContent =
        """
            #!/bin/bash -exu
            
            DRY_RUN_OPTION=""
            if [ "%$dryRunParameter%" = "true" ]; then
                DRY_RUN_OPTION="--dry-run"
            fi

            ${'$'}{JAVA_HOME}/bin/java -jar lib/rt.jar ${'$'}{DRY_RUN_OPTION} --debug publish-to-maven-central \
            --group-id org.neo4j \
            --operator %teamcity.build.triggeredBy.username% \
            --repository-username ${'$'}{OSSSONATYPEORG_USERNAME} \
            --repository-password ${'$'}{OSSSONATYPEORG_PASSWORD} \
            --repository-path ./target/staging-deploy \
            --signing-key-passphrase "${'$'}{SIGNING_KEY_PASSPHRASE}" \
            --staging-profile-name org.neo4j
        """
            .trimIndent()

    dockerImage = "neo4jbuildservice/quality:general-java8"
    dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
    dockerRunParameters = "--volume %teamcity.build.checkoutDir%/signingkeysandbox:/root/.gnupg"
  }
}
