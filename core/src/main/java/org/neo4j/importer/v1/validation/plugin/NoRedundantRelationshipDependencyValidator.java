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
package org.neo4j.importer.v1.validation.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoRedundantRelationshipDependencyValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "NRRD-001";

    private final Map<String, List<RedundantDependency>> redundantDependencies = new HashMap<>();

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingActiveNodeReferenceValidator.class,
                NoDanglingDependsOnValidator.class,
                NoDuplicatedDependencyValidator.class);
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var path = String.format("$.targets.relationships[%d].dependencies", index);
        Set<String> explicitDependencies = new HashSet<>(target.getDependencies());
        var startNodeRef = target.getStartNodeReference();
        if (explicitDependencies.contains(startNodeRef)) {
            var redundantDependency = new RedundantDependency(startNodeRef, NodeReferenceType.START);
            redundantDependencies.computeIfAbsent(path, k -> new ArrayList<>()).add(redundantDependency);
        }
        var endNodeRef = target.getEndNodeReference();
        if (explicitDependencies.contains(endNodeRef)) {
            var redundantDependency = new RedundantDependency(endNodeRef, NodeReferenceType.END);
            redundantDependencies.computeIfAbsent(path, k -> new ArrayList<>()).add(redundantDependency);
        }
        SpecificationValidator.super.visitRelationshipTarget(index, target);
    }

    @Override
    public boolean report(Builder builder) {
        redundantDependencies.forEach((path, redundancies) -> {
            redundancies.forEach(redundancy -> {
                var referenceType = redundancy.getReferenceType();
                builder.addError(
                        path,
                        ERROR_CODE,
                        String.format(
                                "%s \"%s\" is defined as an explicit dependency *and* as %s %s node reference, remove it from dependencies",
                                path,
                                redundancy.getName(),
                                referenceType == NodeReferenceType.START ? "a" : "an",
                                referenceType.name().toLowerCase(Locale.ROOT)));
            });
        });
        return !redundantDependencies.isEmpty();
    }

    private static class RedundantDependency {
        private final String name;

        private final NodeReferenceType referenceType;

        public RedundantDependency(String name, NodeReferenceType referenceType) {
            this.name = name;
            this.referenceType = referenceType;
        }

        public String getName() {
            return name;
        }

        public NodeReferenceType getReferenceType() {
            return referenceType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RedundantDependency)) return false;
            RedundantDependency that = (RedundantDependency) o;
            return Objects.equals(name, that.name) && referenceType == that.referenceType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, referenceType);
        }
    }

    private enum NodeReferenceType {
        START,
        END
    }
}
