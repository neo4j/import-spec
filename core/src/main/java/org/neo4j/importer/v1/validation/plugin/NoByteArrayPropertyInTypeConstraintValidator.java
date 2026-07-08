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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.targets.EntityTarget;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.PropertyType;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoByteArrayPropertyInTypeConstraintValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "NBYT-001";

    private final List<String> invalidPaths = new ArrayList<>();

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(NoDuplicatedTargetPropertyValidator.class, NoDanglingPropertyInTypeConstraintValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        var byteArrayProps = byteArrayProperties(target);
        if (byteArrayProps.isEmpty()) {
            return;
        }
        var typeConstraints = target.getSchema().getTypeConstraints();
        for (int i = 0; i < typeConstraints.size(); i++) {
            var typeConstraint = typeConstraints.get(i);
            if (!byteArrayProps.contains(typeConstraint.getProperty())) {
                continue;
            }
            invalidPaths.add(String.format("$.targets.nodes[%d].schema.type_constraints[%d].property", index, i));
        }
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        var byteArrayProps = byteArrayProperties(target);
        if (byteArrayProps.isEmpty()) {
            return;
        }
        var typeConstraints = target.getSchema().getTypeConstraints();
        for (int i = 0; i < typeConstraints.size(); i++) {
            var typeConstraint = typeConstraints.get(i);
            if (!byteArrayProps.contains(typeConstraint.getProperty())) {
                continue;
            }
            invalidPaths.add(
                    String.format("$.targets.relationships[%d].schema.type_constraints[%d].property", index, i));
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPaths.forEach(
                path -> builder.addError(path, ERROR_CODE, "BYTE_ARRAY is not supported in type constraints"));
        return !invalidPaths.isEmpty();
    }

    private static Set<String> byteArrayProperties(EntityTarget target) {
        return target.getProperties().stream()
                .filter(mapping -> PropertyType.BYTE_ARRAY.equals(mapping.getTargetPropertyType()))
                .map(PropertyMapping::getTargetProperty)
                .collect(Collectors.toSet());
    }
}
