package org.neo4j.importer.v1.distribution;

public class InvalidNeo4jVersionException extends RuntimeException {
    InvalidNeo4jVersionException(String message) {
        super(message);
    }
}
