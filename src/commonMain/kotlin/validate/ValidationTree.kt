/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package validate

import model.GraphModel

class ValidationTree {
    private val tree = mutableMapOf<Validation, MutableList<Validation>>()
    private val roots = mutableListOf<Validation>()

    /**
     * Build a tree of validation dependencies using [Validation.dependsOn]
     */
    fun build(validators: List<Validation>) {
        clear()
        for (validation in validators) {
            val dependencies = validation.dependsOn()
            if (dependencies.isEmpty()) {
                roots.add(validation)
                continue
            }
            for (dependent in dependencies) {
                tree.getOrPut(dependent) { mutableListOf() }.add(validation)
            }
        }
    }

    /**
     * Check a [GraphModel] for validation [Issue]s.
     */
    fun validate(model: GraphModel): List<Issue> {
        require(roots.isNotEmpty()) { "Validation tree must be built before validating a GraphModel" }
        val issues = mutableListOf<Issue>()
        for (validation in roots) {
            validate(validation, model, issues)
        }
        return issues
    }

    /**
     * Check [model] against [validation] skipping child validators if an issue was found
     * to avoid duplicate or irrelevant issues.
     */
    private fun validate(validation: Validation, model: GraphModel, issues: MutableList<Issue>) {
        val start = issues.size
        validation.validate(model, issues)
        if (issues.size > start) {
            return
        }
        for (dependency in tree[validation] ?: return) {
            validate(dependency, model, issues)
        }
    }

    fun clear() {
        tree.clear()
        roots.clear()
    }
}
