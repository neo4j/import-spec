package builds

import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.toId
import jetbrains.buildServer.configs.kotlin.triggers.schedule
import jetbrains.buildServer.configs.kotlin.triggers.vcs

class NightlyBuild(name: String): Project({
    this.id(name.toId())
    this.name = name

    val complete = Empty("${name}-complete", "complete")

    val bts = sequential {
        dependentBuildType(SemgrepCheck("${name}-semgrep-check", "semgrep check"))
        dependentBuildType(complete)
    }

    bts.buildTypes().forEach {
        it.thisVcs()

        it.features {
            enableCommitStatusPublisher()
        }

        buildType(it)
    }

    complete.triggers {
        vcs { enabled = false }

        schedule {
            branchFilter = buildString {
                appendLine("+:main")
                appendLine("+:refs/heads/main")
            }
            schedulingPolicy = daily {
                hour = 7
                minute = 0
            }
            triggerBuild = always()
            withPendingChangesOnly = false
            enforceCleanCheckout = true
            enforceCleanCheckoutForDependencies = true
        }
    }

})