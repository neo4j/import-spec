package org.neo4j.importer.v1.validation.plugin;

import org.neo4j.importer.v1.targets.KeyMapping;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidator;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NoDanglingNodeReferenceKeyMappingTargetPropertyValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DANG-333"; //todo: find the correct error code

    private final Map<String, Set<String>> nodeTargets;
    private final Map<String, String> invalidPathToKeyMappings;

    public NoDanglingNodeReferenceKeyMappingTargetPropertyValidator() {
        this.nodeTargets = new LinkedHashMap<>();
        this.invalidPathToKeyMappings = new LinkedHashMap<>();
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget nodeTarget) {
        nodeTargets.put(
                nodeTarget.getName(),
                nodeTarget.getProperties().stream()
                        .map(PropertyMapping::getTargetProperty)
                        .collect(Collectors.toSet()));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget relationshipTarget) {
        NodeReference startNodeRef = relationshipTarget.getStartNodeReference();
        if (startNodeRef.getKeyMappings() != null) {
            Set<String> targetProperties = nodeTargets.get(startNodeRef.getName());
            for (int i = 0; i < startNodeRef.getKeyMappings().size(); i++) {
                KeyMapping keyMapping = startNodeRef.getKeyMappings().get(i);
                if (!targetProperties.contains(keyMapping.getTargetProperty())) {
                    invalidPathToKeyMappings.put(
                            String.format(
                                    "$.targets.relationships[%d].start_node_reference.key_mappings[%d].target_property",
                                    index, i),
                            keyMapping.getTargetProperty());
                }
            }
        }
        NodeReference endNodeRef = relationshipTarget.getStartNodeReference();
        if (endNodeRef.getKeyMappings() != null) {
            Set<String> targetProperties = nodeTargets.get(endNodeRef.getName());
            for (int i = 0; i < endNodeRef.getKeyMappings().size(); i++) {
                KeyMapping keyMapping = endNodeRef.getKeyMappings().get(i);
                if (!targetProperties.contains(keyMapping.getTargetProperty())) {
                    invalidPathToKeyMappings.put(
                            String.format(
                                    "$.targets.relationships[%d].end_node_reference.key_mappings[%d].target_property",
                                    index, i),
                            keyMapping.getTargetProperty());
                }
            }
        }
    }

    @Override
    public boolean report(Builder builder) {
        invalidPathToKeyMappings.forEach((path, invalidNodeReference) -> {
            builder.addError(
                    path,
                    ERROR_CODE,
                    String.format("%s refers to a non-existing node target \"%s\".", path, invalidNodeReference));
        });
        return !invalidPathToKeyMappings.isEmpty();
    }
}
