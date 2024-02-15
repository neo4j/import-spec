# Neo4j Import Specification Format

[![CI](https://github.com/neo4j/import-spec/actions/workflows/CI.yml/badge.svg)](https://github.com/neo4j/import-spec/actions/workflows/CI.yml)

This repository provides:

 - the schema for the import specification file
 - the corresponding Java deserializer

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

        try (Reader reader = createReader("json.spec")) {
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
