# Go GraphSpec

This package contains the Go GraphSpec Library. It consists of:

* **Go-native GraphSpec types** - These are automatically generated from the Kotlin source of truth models
* **Methods for migration and validation** - Enables migration between older model types and latest GraphSpec model, as 
    well as validation of a given GraphSpec model. These methods call into the Kotlin source of truth methods via a
    Kotlin/Native library.

## Usage

> TODO: Update this when first release from main repo

This library can be imported into your Go project via the standard `go get`: 

```shell
# Use SSH for git to avoid having to authenticate when fetching this library
git config --global url."git@github.com:".insteadOf "https://github.com/" 

# Fetch Go library
go get github.com/neo4j/graph-spec/go@vx.y.z
```

## Contributing

### Building and testing on MacOS
As above, the migration and validation methods rely on Kotlin/Native static libraries that are built from the Kotlin 
code. MacOS requires an environment configuration to allow specific linker flags used by the Kotlin/Native binary.

To prevent Go from blocking the `-force_load` flag the following environment variable needs to be set:
```
CGO_LDFLAGS_ALLOW="-Wl,-force_load,.*"
```

E.g. to run tests from the `go/` directory:
```
CGO_LDFLAGS_ALLOW="-Wl,-force_load,.*" go test ./... 
```

#### Why is this needed?
* macOS (Darwin): Uses the Apple linker (ld64), which is aggressive at "dead-code stripping." `-Wl,-force_load` is used 
  to ensure all Kotlin symbols are included in the final binary. For security, the Go toolchain blocks this specific 
  flag by default unless it is added to the allowlist.

* Linux: Uses standard linkers (bfd or gold) that do not require "force-load" instructions to resolve these symbols. 
  Consequently, no security allowlist is triggered.

### Generating Kotlin/Native libraries

```shell
./go/scripts/generate-kotlin-native-libs.sh
```


### Generating Go types

```shell
./go/scripts/generate-go-models.sh
```

The GraphModel Go struct and associated structs are automatically generated from the Kotlin source of truth. They live
in the `go/models` package. The pipeline for generating these types is:

_Kotlin_ → _JSON Schema_ → _Go_

Some temporary sanitising is needed to ensure there are no issues relating to Go enum variable names when generating
Neo4jTypes. The script handles this.

#### Generating JSON schema

If you want to update the JSON schema separately you can run the individual Gradle command from the repo root:

```shell
./gradlew :generateGraphModelJsonSchema
```

JSON Schema is generated automatically from the Kotlin source of truth via the `kotlinx-schema` library.

#### Go type generation

The Go structs are generated using the [schemancer library](https://github.com/Southclaws/schemancer). This library was chosen as it correctly handled 
polymorphism (e.g. mapping types). Other type radiation libraries such as modelina and quicktype condense down all 
polymorphic types into a single super struct.
