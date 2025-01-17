package org.neo4j.importer.v1.validation.plugin;

import org.neo4j.importer.v1.targets.KeyMapping;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidator;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NoDanglingNodeReferenceKeyMappingValidator implements SpecificationValidator {
    private static final String ERROR_CODE = "DANG-444"; // todo: find the correct error code

    private final Map<String, List<NodeKeyConstraint>> nodeNameKeyConstraintMap;
    private final Map<String, String> invalidKeyMappings;

    public NoDanglingNodeReferenceKeyMappingValidator() {
        this.nodeNameKeyConstraintMap = new HashMap<>();
        this.invalidKeyMappings = new HashMap<>();
    }

    @Override
    public Set<Class<? extends SpecificationValidator>> requires() {
        return Set.of(
                NoDanglingActiveNodeReferenceValidator.class,
                NoDanglingPropertyInKeyConstraintValidator.class,
                NoDanglingNodeReferenceKeyMappingTargetPropertyValidator.class);
    }

    @Override
    public void visitNodeTarget(int index, NodeTarget nodeTarget) {
        nodeNameKeyConstraintMap.put(
                nodeTarget.getName(), nodeTarget.getSchema().getKeyConstraints());
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget relationshipTarget) {
        visitKeyMappings(
                String.format("$.targets.relationships[%d].start_node_reference.key_mappings", index),
                relationshipTarget.getStartNodeReference());
        visitKeyMappings(
                String.format("$.targets.relationships[%d].end_node_reference.key_mappings", index),
                relationshipTarget.getEndNodeReference());
    }

    @Override
    public boolean report(Builder builder) {
        invalidKeyMappings.forEach((path, message) -> {
            builder.addError(path, ERROR_CODE, message);
        });
        return invalidKeyMappings.isEmpty();
    }

    private void visitKeyMappings(String path, NodeReference nodeReference) {
        if (nodeReference.getKeyMappings() != null) {
            boolean passState = false;
            var failMessages = new ArrayList<String>();

            var targetProperties = nodeReference.getKeyMappings().stream()
                    .map(KeyMapping::getTargetProperty)
                    .collect(Collectors.toList());

            var nodeKeyConstraints = nodeNameKeyConstraintMap.get(nodeReference.getName());
            if (nodeKeyConstraints != null) {
                for (NodeKeyConstraint nodeKeyConstraint : nodeKeyConstraints) {
                    var notIncluded = new ArrayList<String>();
                    for (String keyConstraintProperty : nodeKeyConstraint.getProperties()) {
                        if (!targetProperties.contains(keyConstraintProperty)) {
                            notIncluded.add(keyConstraintProperty);
                        }
                    }
                    if (notIncluded.isEmpty()) {
                        passState = true;
                        break;
                    }

                    failMessages.add(String.format("mapping to %s is missing", String.join(", ", notIncluded)));
                }
            }

            if (!passState) {
                invalidKeyMappings.put(path, String.join(" or ", failMessages));
            }
        }
    }
}

// targetProperties [1,3]
// keyConstraintProperties [1,2,3]
// notIncluded [2]

// mapping to 2 is missing
