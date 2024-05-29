package org.neo4j.importer.v1.validation;

import org.neo4j.importer.v1.Neo4jDistribution;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;

import java.util.ArrayList;
import java.util.List;

public class Neo4jVersionValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "VERS-001";

    private final Neo4jDistribution neo4jDistribution;
    private final List<String> unsupportedPaths;

    public Neo4jVersionValidator(Neo4jDistribution neo4jDistribution) {
        this.neo4jDistribution = neo4jDistribution;
        this.unsupportedPaths = new ArrayList<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget nodeTarget) {
        var schema = nodeTarget.getSchema();
        if (schema == null) {
            return;
        }

        if (!isEmpty(schema.getTypeConstraints()) && !neo4jDistribution.hasNodeTypeConstraints()) {
            unsupportedPaths.add(String.format("$.targets.nodes[%d].schema.type_constraints", index));
        }
        if (!isEmpty(schema.getKeyConstraints()) && !neo4jDistribution.hasNodeKeyConstraints()) {
            unsupportedPaths.add(String.format("$.targets.nodes[%d].schema.key_constraints", index));
        }
        if (!isEmpty(schema.getUniqueConstraints()) && !neo4jDistribution.hasNodeUniqueConstraints()) {
            unsupportedPaths.add(String.format("$.targets.nodes[%d].schema.unique_constraints", index));
        }
        if (!isEmpty(schema.getExistenceConstraints()) && !neo4jDistribution.hasNodeExistenceConstraints()) {
            unsupportedPaths.add(String.format("$.targets.nodes[%d].schema.existence_constraints", index));
        }
        if (!isEmpty(schema.getRangeIndexes()) && !neo4jDistribution.hasNodeRangeIndexes()) {
            unsupportedPaths.add(String.format("$.targets.nodes[%d].schema.range_indexes", index));
        }
        if (!isEmpty(schema.getTextIndexes()) && !neo4jDistribution.hasNodeTextIndexes()) {
            unsupportedPaths.add(String.format("$.targets.nodes[%d].schema.text_indexes", index));
        }
        if (!isEmpty(schema.getPointIndexes()) && !neo4jDistribution.hasNodePointIndexes()) {
            unsupportedPaths.add(String.format("$.targets.nodes[%d].schema.text_indexes", index));
        }
        if (!isEmpty(schema.getFullTextIndexes()) && !neo4jDistribution.hasNodeFullTextIndexes()) {
            unsupportedPaths.add(String.format("$.targets.nodes[%d].schema.fulltext_indexes", index));
        }
        if (!isEmpty(schema.getVectorIndexes()) && !neo4jDistribution.hasNodeVectorIndexes()) {
            unsupportedPaths.add(String.format("$.targets.nodes[%d].schema.vector_indexes", index));
        }
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget relationshipTarget) {
        var schema = relationshipTarget.getSchema();
        if (schema == null) {
            return;
        }

        if (!isEmpty(schema.getTypeConstraints()) && !neo4jDistribution.hasRelationshipTypeConstraints()) {
            unsupportedPaths.add(String.format("$.targets.relationships[%d].schema.type_constraints", index));
        }
        if (!isEmpty(schema.getKeyConstraints()) && !neo4jDistribution.hasRelationshipKeyConstraints()) {
            unsupportedPaths.add(String.format("$.targets.relationships[%d].schema.key_constraints", index));
        }
        if (!isEmpty(schema.getUniqueConstraints()) && !neo4jDistribution.hasRelationshipUniqueConstraints()) {
            unsupportedPaths.add(String.format("$.targets.relationships[%d].schema.unique_constraints", index));
        }
        if (!isEmpty(schema.getExistenceConstraints()) && !neo4jDistribution.hasRelationshipExistenceConstraints()) {
            unsupportedPaths.add(String.format("$.targets.relationships[%d].schema.existence_constraints", index));
        }
        if (!isEmpty(schema.getRangeIndexes()) && !neo4jDistribution.hasRelationshipRangeIndexes()) {
            unsupportedPaths.add(String.format("$.targets.relationships[%d].schema.range_indexes", index));
        }
        if (!isEmpty(schema.getTextIndexes()) && !neo4jDistribution.hasRelationshipTextIndexes()) {
            unsupportedPaths.add(String.format("$.targets.relationships[%d].schema.text_indexes", index));
        }
        if (!isEmpty(schema.getPointIndexes()) && !neo4jDistribution.hasRelationshipPointIndexes()) {
            unsupportedPaths.add(String.format("$.targets.relationships[%d].schema.text_indexes", index));
        }
        if (!isEmpty(schema.getFullTextIndexes()) && !neo4jDistribution.hasRelationshipFullTextIndexes()) {
            unsupportedPaths.add(String.format("$.targets.relationships[%d].schema.fulltext_indexes", index));
        }
        if (!isEmpty(schema.getVectorIndexes()) && !neo4jDistribution.hasRelationshipVectorIndexes()) {
            unsupportedPaths.add(String.format("$.targets.relationships[%d].schema.vector_indexes", index));
        }
    }

    @Override
    public boolean report(SpecificationValidationResult.Builder builder) {
        unsupportedPaths.forEach(path -> builder.addError(
                path,
                ERROR_CODE,
                String.format("%s is not supported by version %s", path, neo4jDistribution.toString())));
        return !unsupportedPaths.isEmpty();
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
