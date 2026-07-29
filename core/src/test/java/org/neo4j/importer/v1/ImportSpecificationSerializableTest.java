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
package org.neo4j.importer.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import org.junit.jupiter.api.Test;

class ImportSpecificationSerializableTest {

    @Test
    void all_declared_types_reachable_from_import_specification_are_serializable() {
        var reachableTypes = new LinkedHashSet<Class<?>>();
        collectReachableTypesFrom(ImportSpecification.class, false, reachableTypes);

        var faultyTypes = reachableTypes.stream()
                .filter(type -> !Serializable.class.isAssignableFrom(type))
                .map(Class::getName)
                .collect(Collectors.toList());
        var delimiter = "\n\t- ";
        assertThat(faultyTypes)
                .overridingErrorMessage(
                        "Found ImportSpecification types missing the Serializable inheritance:%s%s",
                        delimiter, String.join(delimiter, faultyTypes))
                .isEmpty();
    }

    private static void collectReachableTypesFrom(Type type, boolean variableType, Set<Class<?>> reachableTypes) {
        if (type instanceof ParameterizedType) {
            collectParameterizedType((ParameterizedType) type, reachableTypes);
            return;
        }
        if (type instanceof WildcardType) {
            var wildcardType = (WildcardType) type;
            Arrays.stream(wildcardType.getUpperBounds())
                    .forEach(bound -> collectReachableTypesFrom(bound, true, reachableTypes));
            return;
        }
        if (type instanceof TypeVariable<?>) {
            var typeVariable = (TypeVariable<?>) type;
            Arrays.stream(typeVariable.getBounds())
                    .forEach(bound -> collectReachableTypesFrom(bound, true, reachableTypes));
            return;
        }
        if (type instanceof GenericArrayType) {
            var arrayType = (GenericArrayType) type;
            collectReachableTypesFrom(arrayType.getGenericComponentType(), false, reachableTypes);
            return;
        }
        if (type instanceof Class<?>) {
            collectClass((Class<?>) type, variableType, reachableTypes);
        }
    }

    private static void collectParameterizedType(ParameterizedType type, Set<Class<?>> reachableTypes) {
        collectReachableTypesFrom(type.getRawType(), false, reachableTypes);

        var rawType = type.getRawType();
        var arbitraryMapValue = rawType instanceof Class<?> && Map.class.isAssignableFrom((Class<?>) rawType);
        Arrays.stream(type.getActualTypeArguments())
                .forEach(typeArgument -> collectReachableTypesFrom(typeArgument, arbitraryMapValue, reachableTypes));
    }

    private static void collectClass(Class<?> type, boolean variableType, Set<Class<?>> reachableTypes) {
        if (type.isPrimitive() || Void.TYPE.equals(type)) {
            return;
        }
        if (type.isArray()) {
            collectClass(type.getComponentType(), false, reachableTypes);
            return;
        }
        if (Object.class.equals(type) && variableType) {
            return;
        }
        if (isContainerContract(type)) {
            return;
        }
        if (reachableTypes.contains(type)) {
            return;
        }

        reachableTypes.add(type);

        if (!isImportSpecType(type)) {
            return;
        }

        collectSuperclass(type, reachableTypes);
        collectInterfaces(type, reachableTypes);
        collectFields(type, reachableTypes);
        collectConcreteSubtypes(type, reachableTypes);
    }

    private static void collectSuperclass(Class<?> type, Set<Class<?>> reachableTypes) {
        var superclass = type.getGenericSuperclass();
        if (superclass == null || Object.class.equals(superclass)) {
            return;
        }
        collectReachableTypesFrom(superclass, false, reachableTypes);
    }

    private static void collectInterfaces(Class<?> type, Set<Class<?>> reachableTypes) {
        Arrays.stream(type.getGenericInterfaces())
                .filter(ImportSpecificationSerializableTest::isImportSpecType)
                .forEach(interfaceType -> collectReachableTypesFrom(interfaceType, false, reachableTypes));
    }

    private static void collectFields(Class<?> type, Set<Class<?>> reachableTypes) {
        Arrays.stream(type.getDeclaredFields())
                .filter(field -> !isIgnored(field))
                .forEach(field -> collectReachableTypesFrom(field.getGenericType(), false, reachableTypes));
    }

    private static void collectConcreteSubtypes(Class<?> type, Set<Class<?>> reachableTypes) {
        if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            return;
        }
        productionClasses().stream()
                .filter(candidate -> !candidate.equals(type))
                .filter(type::isAssignableFrom)
                .filter(candidate -> !candidate.isInterface())
                .filter(candidate -> !Modifier.isAbstract(candidate.getModifiers()))
                .sorted(Comparator.comparing(Class::getName))
                .forEach(candidate -> collectReachableTypesFrom(candidate, false, reachableTypes));
    }

    private static boolean isIgnored(Field field) {
        var modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic();
    }

    private static boolean isContainerContract(Class<?> type) {
        return type.isInterface() && (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type));
    }

    private static boolean isImportSpecType(Type type) {
        if (type instanceof Class<?>) {
            return isImportSpecType((Class<?>) type);
        }
        if (type instanceof ParameterizedType) {
            return isImportSpecType(((ParameterizedType) type).getRawType());
        }
        return false;
    }

    private static boolean isImportSpecType(Class<?> type) {
        return type.getName().startsWith(ImportSpecification.class.getPackageName() + ".");
    }

    private static Set<Class<?>> productionClasses() {
        try {
            var location = ImportSpecification.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            var root = Path.of(location);
            if (Files.isDirectory(root)) {
                return productionClassesFromDirectory(root);
            }
            return productionClassesFromJar(root);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Unable to find import-spec production classes", e);
        }
    }

    private static Set<Class<?>> productionClassesFromDirectory(Path root) throws IOException {
        var packageRoot =
                root.resolve(ImportSpecification.class.getPackageName().replace('.', '/'));
        if (!Files.exists(packageRoot)) {
            return Set.of();
        }
        try (var files = Files.walk(packageRoot)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .map(root::relativize)
                    .map(ImportSpecificationSerializableTest::toClassName)
                    .map(ImportSpecificationSerializableTest::loadClass)
                    .collect(Collectors.toSet());
        }
    }

    private static Set<Class<?>> productionClassesFromJar(Path jarPath) throws IOException {
        try (var jar = new JarFile(jarPath.toFile())) {
            return jar.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith(
                            ImportSpecification.class.getPackageName().replace('.', '/')))
                    .filter(name -> name.endsWith(".class"))
                    .map(name -> name.replace('/', '.').replaceAll("\\.class$", ""))
                    .map(ImportSpecificationSerializableTest::loadClass)
                    .collect(Collectors.toSet());
        }
    }

    private static String toClassName(Path path) {
        return path.toString().replace(File.separatorChar, '.').replaceAll("\\.class$", "");
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name, false, ImportSpecificationSerializableTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load " + name, e);
        }
    }
}
