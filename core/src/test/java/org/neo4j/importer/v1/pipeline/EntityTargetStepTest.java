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
package org.neo4j.importer.v1.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.targets.EntityTargetExtension;

class EntityTargetStepTest {

    @Test
    void finds_matching_extensions() {
        var extension = new ExtensionOne();
        var step = stepWithExtensions(extension);

        var result = step.extension(ExtensionOne.class);

        assertThat(result)
                .overridingErrorMessage("expected to find matching extension")
                .isPresent()
                .contains(extension);
    }

    @Test
    void finds_no_extensions() {
        var step = stepWithoutExtensions();

        assertThat(step.extension(ExtensionOne.class))
                .overridingErrorMessage("expected not to find any extension")
                .isEmpty();
    }

    @Test
    void finds_no_matching_extensions() {
        var step = stepWithExtensions(new ExtensionTwo());

        assertThat(step.extension(ExtensionOne.class))
                .overridingErrorMessage("expected not to find any matching extension")
                .isEmpty();
    }

    private static EntityTargetStep stepWithoutExtensions() {
        return step(List.of());
    }

    private static EntityTargetStep stepWithExtensions(
            EntityTargetExtension extension, EntityTargetExtension... extensions) {
        return step(prepend(extension, extensions));
    }

    private static EntityTargetStep step(List<EntityTargetExtension> extensions) {
        var entityStep = mock(EntityTargetStep.class);
        when(entityStep.extensions()).thenReturn(extensions);
        when(entityStep.extension(any())).thenCallRealMethod();
        return entityStep;
    }

    private static List<EntityTargetExtension> prepend(
            EntityTargetExtension extension, EntityTargetExtension... extensions) {
        var result = new ArrayList<EntityTargetExtension>();
        result.add(extension);
        result.addAll(Arrays.asList(extensions));
        return List.copyOf(result);
    }

    static class ExtensionOne implements EntityTargetExtension {}

    static class ExtensionTwo implements EntityTargetExtension {}
}
