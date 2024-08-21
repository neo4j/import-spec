import builds.Build
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.version

version = "2024.03"

project {
  params {
    text("osssonatypeorg-username", "%publish-username%")
    password("osssonatypeorg-password", "%publish-password%")
    password("signing-key-passphrase", "%publish-signing-key-password%")
    password("github-commit-status-token", "%github-token%")
    password("github-pull-request-token", "%github-token%")
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
