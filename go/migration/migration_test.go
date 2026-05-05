package migration_test

import (
	"embed"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/neo4j/graph-spec/go/migration"
	"github.com/neo4j/graph-spec/go/model"
	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testdata embed.FS

func TestV3ToGraphSpecMigration(t *testing.T) {
	raw, err := testdata.ReadFile("testdata/northwind.json")
	require.NoError(t, err)

	result, err := migration.ToGraphSpec(string(raw), migration.ModelTypeDataModel)
	require.NoError(t, err)
	require.NotEmpty(t, result.Mappings)

	resultBytes, err := json.Marshal(result)
	require.NoError(t, err)
	t.Log(fmt.Sprintf("Transformed graph: %v", string(resultBytes)))
}

func TestGraphSpecToV3Migration(t *testing.T) {
	raw, err := testdata.ReadFile("testdata/graph-spec-example.json")
	require.NoError(t, err)

	var graph model.GraphModel
	err = json.Unmarshal(raw, &graph)
	require.NoError(t, err)

	res, err := migration.FromGraphSpec(graph, migration.ModelTypeDataModel, migration.ModelVersionDataModelV30)
	require.NoError(t, err)
	require.NotNil(t, res)
	t.Log(fmt.Sprintf("Transformed graph: %v", res))
}
