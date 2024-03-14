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

import java.util.Objects;

public final class SpecificationWarning {

    private final String elementPath;
    private final String code;
    private final String message;

    public SpecificationWarning(String elementPath, String code, String message) {
        this.elementPath = elementPath;
        this.code = code;
        this.message = message;
    }

    public String getElementPath() {
        return elementPath;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        SpecificationWarning that = (SpecificationWarning) object;
        return Objects.equals(elementPath, that.elementPath)
                && Objects.equals(code, that.code)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementPath, code, message);
    }

    @Override
    public String toString() {
        return "SpecificationWarning{" + "elementPath='"
                + elementPath + '\'' + ", code='"
                + code + '\'' + ", message='"
                + message + '\'' + '}';
    }
}
