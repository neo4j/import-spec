package builds

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerRegistryConnections
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.MavenBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.script

const val GITHUB_OWNER = "neo4j"
const val GITHUB_REPOSITORY = "import-spec"
val MAVEN_DEFAULT_ARGS = buildString {
  append("--no-transfer-progress ")
  append("--batch-mode ")
  append("--threads 1C ")
  append("-Dmaven.repo.local=%teamcity.build.checkoutDir%/.m2/repository ")
  append("-Dmaven.wagon.http.retryHandler.class=standard ")
  append("-Dmaven.wagon.http.retryHandler.timeout=60 ")
  append("-Dmaven.wagon.http.retryHandler.count=3 ")
  append(
      "-Dmaven.wagon.http.retryHandler.nonRetryableClasses=java.io.InterruptedIOException,java.net.UnknownHostException,java.net.ConnectException ")
}

val DEFAULT_JAVA_VERSION = JavaVersion.V_11
val LTS_JAVA_VERSION = JavaVersion.V_21

const val NODE_DOCKER_IMAGE = "%ecr-registry-connectors%:node-24-latest"

const val SEMGREP_DOCKER_IMAGE = "%ecr-registry-connectors%:semgrep-latest"

const val FULL_GITHUB_REPOSITORY = "$GITHUB_OWNER/$GITHUB_REPOSITORY"
const val GITHUB_URL = "https://github.com/$FULL_GITHUB_REPOSITORY"

// Look into Root Project's settings -> Connections
const val ECR_CONNECTION_ID_ENG = "PROJECT_EXT_124"
const val ECR_CONNECTION_ID_BUILD = "PROJECT_EXT_107"

enum class LinuxSize(val value: String) {
  SMALL("small"),
  LARGE("large")
}

enum class JavaVersion(val version: String, val dockerImage: String) {
  V_11(version = "11", dockerImage = "%ecr-registry-connectors%:jdk-11-latest"),
  V_17(version = "17", dockerImage = "%ecr-registry-connectors%:jdk-17-latest"),
  V_21(version = "21", dockerImage = "%ecr-registry-connectors%:jdk-21-latest"),
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

fun BuildFeatures.loginToECR() = dockerRegistryConnections {
  cleanupPushedImages = true
  loginToRegistry = on { dockerRegistryId = ECR_CONNECTION_ID_ENG }
  loginToRegistry = on { dockerRegistryId = ECR_CONNECTION_ID_BUILD }
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
    javaVersion: JavaVersion = DEFAULT_JAVA_VERSION,
    init: MavenBuildStep.() -> Unit
): MavenBuildStep {
  val maven =
      this.maven {
        localRepoScope = MavenBuildStep.RepositoryScope.MAVEN_DEFAULT

        dockerImagePlatform = MavenBuildStep.ImagePlatform.Linux
        dockerImage = javaVersion.dockerImage
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
