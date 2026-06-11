package org.neo4j.importer.v1.validation.plugin.graphtype;

import org.neo4j.importer.v1.Configuration;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.validation.SpecificationValidationResult.Builder;
import org.neo4j.importer.v1.validation.SpecificationValidator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public class NoRepeatedTypeValidator implements SpecificationValidator {

    private static final String ERROR_CODE = "GTRT-001";

    private final Map<String, List<String>> typePaths = new LinkedHashMap<>();

    private final AtomicBoolean skipValidation = new AtomicBoolean(true);

    @Override
    public void visitConfiguration(Configuration configuration) {
        skipValidation.set(!GraphTypeFeature.isEnabled(configuration));
    }

    @Override
    public void visitRelationshipTarget(int index, RelationshipTarget target) {
        if (skipValidation.get()) {
            return;
        }
        var path = String.format("$.targets.relationships[%d].type", index);
        typePaths
                .computeIfAbsent(target.getType(), (e) -> new ArrayList<>())
                .add(path);
    }

    @Override
    public boolean report(Builder builder) {
        var repeatedTypes = typePaths
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .toList();
        repeatedTypes.forEach(entry -> {
            var repeatedType = entry.getKey();
            var paths = entry.getValue();
            builder.addError(
                    paths.get(0),
                    ERROR_CODE,
                    "Graph type cannot be generated: "
                    + "the type %s is defined more than once (offending occurrences in %s)"
                            .formatted(repeatedType, String.join(", ", paths.subList(1, paths.size()))));
        });
        return !repeatedTypes.isEmpty();
    }
}
