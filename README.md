# Neo4j Import Specification Format

## Scope

This library provides a uniform configuration facade for tools running imports to Neo4j.
In particular, it offers:

- a user-friendly configuration surface (in JSON or YAML), called import specification, backed by a JSON schema
- the Java equivalent of the import specification, a.k.a. `org.neo4j.importer.v1.ImportSpecification`
- source plugins (see [`SourceProvider` Service Provider Interface](https://github.com/neo4j/import-spec/blob/main/src/main/java/org/neo4j/importer/v1/sources/SourceProvider.java))
- action plugins (see [`ActionProvider` Service Provider Interface](https://github.com/neo4j/import-spec/blob/main/src/main/java/org/neo4j/importer/v1/actions/ActionProvider.java) and [built-in plugins](https://github.com/neo4j/import-spec/tree/main/src/main/java/org/neo4j/importer/v1/actions/plugin))
- validation plugins (see [`SpecificationValidator` Service Provider Interface](https://github.com/neo4j/import-spec/blob/main/src/main/java/org/neo4j/importer/v1/validation/SpecificationValidator.java) and [built-in plugins](https://github.com/neo4j/import-spec/tree/main/src/main/java/org/neo4j/importer/v1/validation/plugin))

The library does **NOT**:

- define any sources, you need to define and register at least one (see [examples](https://github.com/neo4j/import-spec/tree/main/src/test/java/org/neo4j/importer/v1/sources/))
- implement any actual import to Neo4j (although some end-to-end tests just do that)
- expose any configuration to locate a Neo4j instance to import data to

## Getting Started

First, [implement](https://github.com/neo4j/import-spec/blob/main/src/test/java/org/neo4j/importer/v1/sources/BigQuerySourceProvider.java) and [register](https://github.com/neo4j/import-spec/blob/main/src/test/resources/META-INF/services/org.neo4j.importer.v1.sources.SourceProvider#L1) a source provider for BigQuery.

Then, save the following import specification into `spec.json`:

```json
{
  "version": "1",
  "config": {
    "key": "value"
  },
  "sources": [
    {
      "name": "my-bigquery-source",
      "type": "bigquery",
      "query": "SELECT id, name FROM my.table"
    }
  ],
  "targets": {
    "queries": [
      {
        "name": "my-query",
        "source": "my-bigquery-source",
        "query": "UNWIND $rows AS row CREATE (n:ANode) SET n = row"
      }
    ]
  },
  "actions": [
    {
      "name": "my-cypher-action",
      "type": "cypher",
      "stage": "start",
      "query": "CREATE CONSTRAINT a_node_id FOR (n:ANode) REQUIRE n.id IS UNIQUE"
    }
  ]
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
