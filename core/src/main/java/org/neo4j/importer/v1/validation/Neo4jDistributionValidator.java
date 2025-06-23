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
package org.neo4j.importer.v1.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.importer.v1.distribution.Neo4jDistribution;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;

public class Neo4jDistributionValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "VERS-001";

    private final Neo4jDistribution neo4jDistribution;
    private final Map<String, List<String>> unsupportedPaths;

    public Neo4jDistributionValidator(Neo4jDistribution neo4jDistribution) {
        this.neo4jDistribution = neo4jDistribution;
        this.unsupportedPaths = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget nodeTarget) {
        var schema = nodeTarget.getSchema();
        var unsupportedFeatures = new ArrayList<String>();
        if (!schema.getTypeConstraints().isEmpty() && !neo4jDistribution.hasNodeTypeConstraints()) {
            unsupportedFeatures.add("type_constraints");
        }
        if (!schema.getKeyConstraints().isEmpty() && !neo4jDistribution.hasNodeKeyConstraints()) {
            unsupportedFeatures.add("key_constraints");
        }
        if (!schema.getUniqueConstraints().isEmpty() && !neo4jDistribution.hasNodeUniqueConstraints()) {
            unsupportedFeatures.add("unique_constraints");
        }
        if (!schema.getExistenceConstraints().isEmpty() && !neo4jDistribution.hasNodeExistenceConstraints()) {
            unsupportedFeatures.add("existence_constraints");
        }
        if (!schema.getRangeIndexes().isEmpty() && !neo4jDistribution.hasNodeRangeIndexes()) {
            unsupportedFeatures.add("range_indexes");
        }
        if (!schema.getTextIndexes().isEmpty() && !neo4jDistribution.hasNodeTextIndexes()) {
            unsupportedFeatures.add("text_indexes");
        }
        if (!schema.getPointIndexes().isEmpty() && !neo4jDistribution.hasNodePointIndexes()) {
            unsupportedFeatures.add("point_indexes");
        }
        if (!schema.getFullTextIndexes().isEmpty() && !neo4jDistribution.hasNodeFullTextIndexes()) {
            unsupportedFeatures.add("fulltext_indexes");
        }
        if (!schema.getVectorIndexes().isEmpty() && !neo4jDistribution.hasNodeVectorIndexes()) {
            unsupportedFeatures.add("vector_indexes");
        }

        if (!unsupportedFeatures.isEmpty()) {
            unsupportedPaths.put(String.format("$.targets.nodes[%d].schema", index), unsupportedFeatures);
        }
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget relationshipTarget) {
        var schema = relationshipTarget.getSchema();
        if (schema.isEmpty()) {
            return;
        }

        var unsupportedFeatures = new ArrayList<String>();
        if (!schema.getTypeConstraints().isEmpty() && !neo4jDistribution.hasRelationshipTypeConstraints()) {
            unsupportedFeatures.add("type_constraints");
        }
        if (!schema.getKeyConstraints().isEmpty() && !neo4jDistribution.hasRelationshipKeyConstraints()) {
            unsupportedFeatures.add("key_constraints");
        }
        if (!schema.getUniqueConstraints().isEmpty() && !neo4jDistribution.hasRelationshipUniqueConstraints()) {
            unsupportedFeatures.add("unique_constraints");
        }
        if (!schema.getExistenceConstraints().isEmpty() && !neo4jDistribution.hasRelationshipExistenceConstraints()) {
            unsupportedFeatures.add("existence_constraints");
        }
        if (!schema.getRangeIndexes().isEmpty() && !neo4jDistribution.hasRelationshipRangeIndexes()) {
            unsupportedFeatures.add("range_indexes");
        }
        if (!schema.getTextIndexes().isEmpty() && !neo4jDistribution.hasRelationshipTextIndexes()) {
            unsupportedFeatures.add("text_indexes");
        }
        if (!schema.getPointIndexes().isEmpty() && !neo4jDistribution.hasRelationshipPointIndexes()) {
            unsupportedFeatures.add("point_indexes");
        }
        if (!schema.getFullTextIndexes().isEmpty() && !neo4jDistribution.hasRelationshipFullTextIndexes()) {
            unsupportedFeatures.add("fulltext_indexes");
        }
        if (!schema.getVectorIndexes().isEmpty() && !neo4jDistribution.hasRelationshipVectorIndexes()) {
            unsupportedFeatures.add("vector_indexes");
        }

        if (!unsupportedFeatures.isEmpty()) {
            unsupportedPaths.put(String.format("$.targets.relationships[%d].schema", index), unsupportedFeatures);
        }
    }

    @Override
    public boolean report(SpecificationValidationResult.Builder builder) {
        unsupportedPaths.forEach((path, features) -> builder.addError(
                path,
                ERROR_CODE,
                String.format(
                        "%s are not supported by %s.", getFeaturesString(features), neo4jDistribution.toString())));
        return !unsupportedPaths.isEmpty();
    }

    private String getFeaturesString(List<String> unsupportedFeatures) {
        var sb = new StringBuilder();

        for (String feature : unsupportedFeatures) {
            sb.append(feature).append(", ");
        }

        return sb.substring(0, sb.length() - 2);
    }
}
