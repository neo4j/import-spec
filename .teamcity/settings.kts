import builds.Build
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.version

version = "2023.11"

project {
  params {
    text("osssonatypeorg-username", "neotechnology.buildserver")
    password("osssonatypeorg-password", "credentialsJSON:447fbec2-193a-4020-b6ea-9356899f1b37")
    password("signing-key-passphrase", "credentialsJSON:d60b2a55-17e4-4d4d-8d09-92bf428c1cdb")
    password("github-commit-status-token", "credentialsJSON:906f7eeb-0de1-4634-8335-92522fd87b19")
    password("github-pull-request-token", "credentialsJSON:906f7eeb-0de1-4634-8335-92522fd87b19")
  }

  subProject(
      Build(
          name = "main",
          branchFilter =
              """
                +:main
              """
                  .trimIndent(),
          triggerRules =
              """
                -:comment=^build.*release version.*:**
                -:comment=^build.*update version.*:**
              """
                  .trimIndent(),
          forPullRequests = false))
  subProject(
      Build(
          name = "pull-request",
          branchFilter =
              """
                +:pull/*
              """
                  .trimIndent(),
          forPullRequests = true))
}
