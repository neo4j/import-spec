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

import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.neo4j.importer.v1.ImportSpecification;
import org.neo4j.importer.v1.actions.Action;
import org.neo4j.importer.v1.actions.ActionStage;
import org.neo4j.importer.v1.graph.Graphs;
import org.neo4j.importer.v1.sources.Source;
import org.neo4j.importer.v1.targets.CustomQueryTarget;
import org.neo4j.importer.v1.targets.KeyMapping;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;
import org.neo4j.importer.v1.targets.Target;
import org.neo4j.importer.v1.targets.Targets;

/**
 * {@link ImportPipeline} exposes a topologically-ordered set of {@link ImportStep},
 * based on the provided {@link ImportSpecification}, usually created with
 * {@link org.neo4j.importer.v1.ImportSpecificationDeserializer#deserialize(Reader)} or its variants.
 * <br><br>
 * <pre>
 *     var specification = org.neo4j.importer.v1.ImportSpecificationDeserializer.deserialize(aReader);
 *     var pipeline = @link ImportPipeline.of(specification);
 *     pipeline.forEach((step) -> {
 *          if (step instanceof SourceStep) {
 *              handleSource((SourceStep) step);
 *          }
 *          else if (step instanceof ActionStep) {
 *              handleAction((ActionStep) step);
 *          }
 *          else if (step instanceof TargetStep) {
 *              handleTarget((TargetStep) step);
 *          }
 *     });
 * </pre>
 * <br><br>
 * Since an {@link ImportStep} may have dependencies, which are either:<br><br>
 * - implicit like a {@link TargetStep} depending on a {@link SourceStep},
 * a {@link RelationshipTargetStep} depending on start/end {@link NodeTargetStep}s<br>
 * - and/or explicit (via {@link ImportStep#dependencies()}<br><br>
 * ... the pipeline guarantees that dependencies are *always* returned after their dependents.<br>
 * In particular, the dependencies of each {@link ActionStep} are resolved at pipeline construction, based on the
 * provided import specification and the corresponding {@link Action}'s {@link ActionStage}.
 */
public class ImportPipeline implements Iterable<ImportStep>, Serializable {

    private final Map<ImportStep, Set<ImportStep>> stepGraph;

    public static ImportPipeline of(ImportSpecification importSpecification) {
        var dependencyNameGraph = buildDependencyNameGraph(importSpecification);
        var dependencyGraph = resolveNames(importSpecification, dependencyNameGraph);
        return new ImportPipeline(dependencyGraph);
    }

    private ImportPipeline(Map<ImportStep, Set<ImportStep>> stepGraph) {
        this.stepGraph = stepGraph;
    }

    @Override
    public Iterator<ImportStep> iterator() {
        return stepGraph.keySet().iterator();
    }

    public ImportExecutionPlan executionPlan() {
        return ImportExecutionPlan.fromGraph(stepGraph);
    }

    private static Map<QualifiedName, Set<QualifiedName>> buildDependencyNameGraph(
            ImportSpecification importSpecification) {
        Map<QualifiedName, Set<QualifiedName>> dependencyGraph = new HashMap<>();
        Targets targets = importSpecification.getTargets();
        var activeTargets = targets.getAll().stream().filter(Target::isActive).collect(Collectors.toList());
        var activeSources =
                activeTargets.stream().map(Target::getSource).distinct().collect(Collectors.toList());
        var sources = new LinkedHashSet<QualifiedName>(activeSources.size());
        activeSources.forEach(source -> {
            var qualifiedName = QualifiedName.ofSource(source);
            dependencyGraph.put(qualifiedName, Set.of());
            sources.add(qualifiedName);
        });
        Map<ActionStage, Set<QualifiedName>> indexedActions = importSpecification.getActions().stream()
                .filter(Action::isActive)
                .collect(Collectors.groupingBy(
                        Action::getStage,
                        Collectors.mapping(
                                action -> QualifiedName.ofAction(action.getName()),
                                Collectors.toCollection(LinkedHashSet::new))));
        var indexedTargets = activeTargets.stream().collect(Collectors.toMap(Target::getName, Function.identity()));
        var startActions = indexedActions.getOrDefault(ActionStage.START, Set.of());
        var preNodeActions = indexedActions.getOrDefault(ActionStage.PRE_NODES, Set.of());
        var nodeTargets = new LinkedHashSet<QualifiedName>(targets.getNodes().size());
        var preRelationshipActions = indexedActions.getOrDefault(ActionStage.PRE_RELATIONSHIPS, Set.of());
        var relationshipTargets =
                new LinkedHashSet<QualifiedName>(targets.getRelationships().size());
        var preQueryActions = indexedActions.getOrDefault(ActionStage.PRE_QUERIES, Set.of());
        var queryTargets =
                new LinkedHashSet<QualifiedName>(targets.getCustomQueries().size());
        activeTargets.forEach(target -> {
            var targetName = target.getName();
            var dependencies = new HashSet<QualifiedName>();
            dependencies.add(QualifiedName.ofSource(target.getSource()));
            dependencies.addAll(target.getDependencies().stream()
                    .map(name -> QualifiedName.ofTarget(indexedTargets.get(name)))
                    .collect(Collectors.toSet()));
            dependencies.addAll(startActions);
            if (target instanceof NodeTarget) {
                dependencies.addAll(preNodeActions);
                var qualifiedName = QualifiedName.ofNodeTarget(targetName);
                dependencyGraph.put(qualifiedName, dependencies);
                nodeTargets.add(qualifiedName);
            } else if (target instanceof RelationshipTarget) {
                dependencies.addAll(preRelationshipActions);
                var relationshipTarget = (RelationshipTarget) target;
                dependencies.add(QualifiedName.ofNodeTarget(
                        relationshipTarget.getStartNodeReference().getName()));
                dependencies.add(QualifiedName.ofNodeTarget(
                        relationshipTarget.getEndNodeReference().getName()));
                var qualifiedName = QualifiedName.ofRelationshipTarget(targetName);
                dependencyGraph.put(qualifiedName, dependencies);
                relationshipTargets.add(qualifiedName);
            } else if (target instanceof CustomQueryTarget) {
                dependencies.addAll(preQueryActions);
                var qualifiedName = QualifiedName.ofQueryTarget(targetName);
                dependencyGraph.put(qualifiedName, dependencies);
                queryTargets.add(qualifiedName);
            } else {
                throw new RuntimeException(
                        String.format("Unknown type %s of target: %s", target.getClass(), targetName));
            }
        });
        Map<ActionStage, Set<QualifiedName>> dependenciesByStage = Map.of(
                ActionStage.START,
                Set.of(),
                ActionStage.POST_SOURCES,
                sources,
                ActionStage.PRE_NODES,
                Set.of(),
                ActionStage.POST_NODES,
                nodeTargets,
                ActionStage.PRE_RELATIONSHIPS,
                Set.of(),
                ActionStage.POST_RELATIONSHIPS,
                relationshipTargets,
                ActionStage.PRE_QUERIES,
                Set.of(),
                ActionStage.POST_QUERIES,
                queryTargets,
                ActionStage.END,
                concat(
                        sources,
                        nodeTargets,
                        relationshipTargets,
                        queryTargets,
                        indexedActions.getOrDefault(ActionStage.START, Set.of()),
                        indexedActions.getOrDefault(ActionStage.POST_SOURCES, Set.of()),
                        indexedActions.getOrDefault(ActionStage.PRE_NODES, Set.of()),
                        indexedActions.getOrDefault(ActionStage.POST_NODES, Set.of()),
                        indexedActions.getOrDefault(ActionStage.PRE_RELATIONSHIPS, Set.of()),
                        indexedActions.getOrDefault(ActionStage.POST_RELATIONSHIPS, Set.of()),
                        indexedActions.getOrDefault(ActionStage.PRE_QUERIES, Set.of()),
                        indexedActions.getOrDefault(ActionStage.POST_QUERIES, Set.of())));
        indexedActions.forEach((stage, actions) -> {
            var dependencies = dependenciesByStage.get(stage);
            actions.forEach(action -> dependencyGraph.put(action, dependencies));
        });
        return dependencyGraph;
    }

    private static Map<ImportStep, Set<ImportStep>> resolveNames(
            ImportSpecification importSpecification, Map<QualifiedName, Set<QualifiedName>> dependencyGraph) {
        var indexedSources = importSpecification.getSources().stream()
                .collect(Collectors.toMap(Source::getName, Function.identity()));
        var indexedNodeTargets = importSpecification.getTargets().getNodes().stream()
                .collect(Collectors.toMap(Target::getName, Function.identity()));
        var indexedRelationshipTargets = importSpecification.getTargets().getRelationships().stream()
                .collect(Collectors.toMap(Target::getName, Function.identity()));
        var indexedQueryTargets = importSpecification.getTargets().getCustomQueries().stream()
                .collect(Collectors.toMap(Target::getName, Function.identity()));
        var indexedActions = importSpecification.getActions().stream()
                .collect(Collectors.toMap(Action::getName, Function.identity()));
        var processedNodeSteps = new HashMap<String, NodeTargetStep>();
        var processedSteps = new HashMap<QualifiedName, ImportStep>();
        var result = new LinkedHashMap<ImportStep, Set<ImportStep>>();
        Graphs.runTopologicalSort(dependencyGraph).forEach(qualifiedName -> {
            // if A depends on B, B is guaranteed to be iterated on/mapped before A
            String name = qualifiedName.getValue();
            var dependencySteps = dependencyGraph.getOrDefault(qualifiedName, Set.of()).stream()
                    .map(processedSteps::get)
                    .collect(Collectors.toSet());
            var nameType = qualifiedName.getType();
            switch (nameType) {
                case SOURCE:
                    var sourceStep = new SourceStep(indexedSources.get(name));
                    processedSteps.put(qualifiedName, sourceStep);
                    result.put(sourceStep, Set.of());
                    break;
                case NODE_TARGET:
                    var nodeStep = new NodeTargetStep(indexedNodeTargets.get(name), dependencySteps);
                    processedSteps.put(qualifiedName, nodeStep);
                    processedNodeSteps.put(name, nodeStep);
                    result.put(nodeStep, dependencySteps);
                    break;
                case RELATIONSHIP_TARGET:
                    var relationshipTarget = indexedRelationshipTargets.get(name);
                    var startNode = relationshipTarget.getStartNodeReference();
                    var endNode = relationshipTarget.getEndNodeReference();
                    var startNodeStep = redefineRelationshipNode(
                            processedNodeSteps.get(startNode.getName()), startNode.getKeyMappings());
                    var endNodeStep = redefineRelationshipNode(
                            processedNodeSteps.get(endNode.getName()), endNode.getKeyMappings());
                    var relationshipStep =
                            new RelationshipTargetStep(relationshipTarget, startNodeStep, endNodeStep, dependencySteps);
                    processedSteps.put(qualifiedName, relationshipStep);
                    var allDependencies = new HashSet<>(dependencySteps);
                    allDependencies.add(startNodeStep);
                    allDependencies.add(endNodeStep);
                    result.put(relationshipStep, allDependencies);
                    break;
                case QUERY_TARGET:
                    var queryTarget = indexedQueryTargets.get(name);
                    var queryStep = new CustomQueryTargetStep(queryTarget, dependencySteps);
                    processedSteps.put(qualifiedName, queryStep);
                    result.put(queryStep, dependencySteps);
                    break;
                case ACTION:
                    var action = indexedActions.get(name);
                    var actionStep = new ActionStep(action, dependencySteps);
                    processedSteps.put(qualifiedName, actionStep);
                    result.put(actionStep, dependencySteps);
                    break;
                default:
                    throw new RuntimeException("Unknown import task type: " + nameType);
            }
        });
        return result;
    }

    // this redefines the mapping for key properties.
    // start/end node targets are defined against their specific source.
    // relationship targets are likely defined against another source, so the source field names for the key properties
    // of start/end nodes are likely to be different from the names of the source fields defined in the original targets
    private static NodeTargetStep redefineRelationshipNode(NodeTargetStep nodeTarget, List<KeyMapping> keyMappings) {
        if (keyMappings.isEmpty()) {
            return nodeTarget;
        }
        var target = nodeTarget.target();
        List<PropertyMapping> mappings = overwriteKeyProperties(target.getProperties(), keyMappings);
        return new NodeTargetStep(
                new NodeTarget(
                        target.isActive(),
                        target.getName(),
                        target.getSource(),
                        target.getDependencies(),
                        target.getWriteMode(),
                        target.getExtensions(),
                        target.getLabels(),
                        mappings,
                        target.getSchema()),
                nodeTarget.dependencies());
    }

    @SafeVarargs
    private static <T> Set<T> concat(Set<T>... sets) {
        return Arrays.stream(sets).flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static class QualifiedName {
        private final ContainerType type;
        private final String value;

        private QualifiedName(ContainerType type, String value) {
            this.type = type;
            this.value = value;
        }

        public static QualifiedName ofSource(String name) {
            return new QualifiedName(ContainerType.SOURCE, name);
        }

        public static QualifiedName ofNodeTarget(String name) {
            return new QualifiedName(ContainerType.NODE_TARGET, name);
        }

        public static QualifiedName ofRelationshipTarget(String name) {
            return new QualifiedName(ContainerType.RELATIONSHIP_TARGET, name);
        }

        public static QualifiedName ofQueryTarget(String name) {
            return new QualifiedName(ContainerType.QUERY_TARGET, name);
        }

        public static QualifiedName ofAction(String name) {
            return new QualifiedName(ContainerType.ACTION, name);
        }

        public static QualifiedName ofTarget(Target target) {
            return new QualifiedName(ContainerType.typeOf(target), target.getName());
        }

        public ContainerType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof QualifiedName)) return false;
            QualifiedName that = (QualifiedName) o;
            return type == that.type && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }
    }

    private static List<PropertyMapping> overwriteKeyProperties(
            List<PropertyMapping> properties, List<KeyMapping> keyMappings) {
        if (keyMappings.isEmpty()) {
            return properties;
        }
        Map<String, String> keys =
                keyMappings.stream().collect(Collectors.toMap(KeyMapping::getNodeProperty, KeyMapping::getSourceField));
        List<PropertyMapping> result = new ArrayList<>(properties.size());
        for (PropertyMapping mapping : properties) {
            String targetProperty = mapping.getTargetProperty();
            String sourceFieldForKey = keys.get(targetProperty);
            if (sourceFieldForKey != null) {
                result.add(new PropertyMapping(sourceFieldForKey, targetProperty, mapping.getTargetPropertyType()));
            } else {
                result.add(mapping);
            }
        }
        return result;
    }

    private enum ContainerType {
        SOURCE,
        NODE_TARGET,
        RELATIONSHIP_TARGET,
        QUERY_TARGET,
        ACTION;

        public static ContainerType typeOf(Target target) {
            if (target instanceof NodeTarget) {
                return NODE_TARGET;
            }
            if (target instanceof RelationshipTarget) {
                return RELATIONSHIP_TARGET;
            }
            if (target instanceof CustomQueryTarget) {
                return QUERY_TARGET;
            }
            throw new IllegalArgumentException(
                    String.format("Unexpected type %s of target %s ", target.getClass(), target.getName()));
        }
    }
}
