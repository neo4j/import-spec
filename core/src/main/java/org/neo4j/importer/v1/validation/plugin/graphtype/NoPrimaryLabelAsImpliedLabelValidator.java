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
package org.neo4j.importer.v1.validation.plugin.graphtype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.neo4j.importer.v1.Configuration;
import org.neo4j.importer.v1.config.Features;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

public class NoPrimaryLabelAsImpliedLabelValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "GTPL-002";

    private final Set<String> primaryLabels = new LinkedHashSet<>();

    private final Set<String> impliedLabels = new HashSet<>();

    private final Map<String, String> primaryLabelPaths = new LinkedHashMap<>();

    private final Map<String, List<String>> impliedLabelPaths = new LinkedHashMap<>();

    private final AtomicBoolean skipValidation = new AtomicBoolean(true);

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(NoRepeatedPrimaryLabelValidator.class);
    }

    @Override
    public void visitConfiguration(Configuration configuration) {
        skipValidation.set(!Features.isGraphTypeEnabled(configuration));
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget target) {
        if (skipValidation.get()) {
            return;
        }
        var path = String.format("$.targets.nodes[%d].labels", index);
        var labels = target.getLabels();
        if (labels.isEmpty()) {
            return; // invalid target, validated in JSON schema
        }
        var primaryLabel = labels.get(0);
        var impliedLabels = new LinkedHashSet<>(labels.subList(1, labels.size()));
        this.primaryLabels.add(primaryLabel);
        this.impliedLabels.addAll(impliedLabels);
        primaryLabelPaths.put(primaryLabel, path);
        impliedLabels.forEach(impliedLabel -> {
            impliedLabelPaths
                    .computeIfAbsent(impliedLabel, (e) -> new ArrayList<>())
                    .add(path);
        });
    }

    @Override
    public boolean report(Builder builder) {
        var misusedLabels = misusedPrimaryLabels();
        misusedLabels.stream()
                .map(misusedLabel -> Map.entry(misusedLabel, primaryLabelPaths.get(misusedLabel)))
                .forEachOrdered(entry -> {
                    var path = entry.getValue();
                    var misusedLabel = entry.getKey();
                    builder.addError(
                            path,
                            ERROR_CODE,
                            String.format(
                                    "Graph type cannot be generated: the primary label %s cannot also be an implied label (offending occurrences in %s)",
                                    misusedLabel, String.join(", ", impliedLabelPaths.get(misusedLabel))));
                });

        return !misusedLabels.isEmpty();
    }

    private Set<String> misusedPrimaryLabels() {
        var misusedLabels = new HashSet<>(primaryLabels);
        misusedLabels.retainAll(impliedLabels);
        return misusedLabels;
    }
}
