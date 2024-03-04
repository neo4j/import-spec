# Neo4j Import Specification Format

[![CI](https://github.com/neo4j/import-spec/actions/workflows/CI.yml/badge.svg)](https://github.com/neo4j/import-spec/actions/workflows/CI.yml)

## Scope

This library provides a uniform configuration facade for tools running imports to Neo4j.
In particular, it offers:

 - a user-friendly configuration surface (in JSON or YAML), called import specification, backed by a JSON schema
 - the Java equivalent of the import specification, a.k.a. `org.neo4j.importer.v1.ImportSpecification`
 - validation plugins (soon)
 - pre-processing plugins (soon)

The library does **NOT**:

 - implement any actual import to Neo4j (although some end-to-end tests just do that)
 - expose any configuration to locate a Neo4j instance to import data to

## Getting Started

Save the following import specification into `spec.json`:

```json
{
    "config": {
      "key": "value"
    },
    "sources": [{
        "name": "my-bigquery-source",
        "type": "bigquery",
        "query": "SELECT id, name FROM my.table"
    }],
    "targets": {
        "queries": [{
            "name": "my-query",
            "source": "my-bigquery-source",
            "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
        }]
    },
    "actions": [{
        "name": "my-http-action",
        "type": "http",
        "method": "get",
        "url": "https://example.com"
    }]
}
```

You can then deserialize it and run your import logic accordingly:

```java
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.targets.Targets;

import java.io.Reader;

class GettingStarted {

    public static void main(String... args) {

        try (Reader reader = createReader("spec.json")) {
            var importSpecification = ImportSpecificationDeserializer.deserialize(reader);

            var config = importSpecification.getConfiguration();
            var sources = importSpecification.getSources();
            var actions = importSpecification.getActions();
            var targets = importSpecification.getTargets();
            var nodeTargets = targets.getNodes();
            var relationshipTargets = targets.getRelationships();
            var customQueryTargets = targets.getCustomQueries();
        }
    }
}
```

## Prerequisites

 - Maven
 - JDK 21 (21 is used for tests, 11 for production sources)

## Contributing

If you have not configured access to the `build-resources` dependency of the licensing plugin, the build will fail as follows:

```shell
Execution check-licenses of goal com.mycila:license-maven-plugin:4.3:check failed: Plugin com.mycila:license-maven-plugin:4.3 or one of its dependencies could not be resolved: Failed to collect dependencies at com.mycila:license-maven-plugin:jar:4.3 -> org.neo4j.connectors:build-resources:jar:1.0.0: Failed to read artifact descriptor for org.neo4j.connectors:build-resources:jar:1.0.0: The following artifacts could not be resolved: org.neo4j.connectors:build-resources:pom:1.0.0 (absent): Could not transfer artifact org.neo4j.connectors:build-resources:pom:1.0.0 from/to github (https://maven.pkg.github.com/neo4j/connectors-build-resources): status code: 401, reason phrase: Unauthorized (401)
```

### Working for Neo4j
Make sure to add the right server entry to your own `~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>github</id>
        <username>USERNAME</username>
        <password>PASSWORD_OR_TOKEN</password>
    </server>
</servers>
```

### Working outside Neo4j
You will have to build the project by disabling the licensing profile with `-P'!licensing'`, as in the following example:

```shell
mvn verify -P'!licensing'
```
