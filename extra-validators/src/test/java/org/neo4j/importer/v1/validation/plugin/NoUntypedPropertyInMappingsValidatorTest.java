package org.neo4j.importer.v1.validation.plugin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.validation.InvalidSpecificationException;
import org.neo4j.importer.v1.validation.SpecificationValidators;

class NoUntypedPropertyInMappingsValidatorTest {

    @Test
    void reports_validation_failure_for_untyped_property_in_mappings() throws Exception {
        // Given
        NoUntypedPropertyInMappingsValidator validator = new NoUntypedPropertyInMappingsValidator();

        try (var reader = new InputStreamReader(
                this.getClass().getResourceAsStream("/specs/untyped_property_mappings_spec.yaml"))) {
            var importSpec = ImportSpecificationDeserializer.deserialize(reader);
            var wrapper = SpecificationValidators.of(validator);

            // When / Then
            assertThatThrownBy(() -> wrapper.validate(importSpec))
                    .isInstanceOf(InvalidSpecificationException.class)
                    .hasMessageContainingAll(
                            "[TYPE-002][$.targets.nodes[0].properties[0].target_property_type] $.targets.nodes[0].properties[0].target_property_type \"id\" refers to an untyped property",
                            "[TYPE-002][$.targets.relationships[0].properties[0].target_property_type] $.targets.relationships[0].properties[0].target_property_type \"property1\" refers to an untyped property");
        }
    }
}
