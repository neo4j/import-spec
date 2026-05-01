package validation_test

import (
	"embed"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/neo-technology/go-graph-spec-test/go/model"
	"github.com/neo-technology/go-graph-spec-test/go/validation"
	"github.com/stretchr/testify/require"
)

//go:embed testdata/*.json
var testdata embed.FS

func TestValidate(t *testing.T) {
	raw, err := testdata.ReadFile("testdata/invalid-graph-model.json")
	require.NoError(t, err)

	var graph model.GraphModel
	err = json.Unmarshal(raw, &graph)
	require.NoError(t, err)

	res, err := validation.Validate(graph)
	require.NoError(t, err)

	t.Log(fmt.Sprintf("Validated graph: %v", res))
	require.Len(t, res, 2)

	msgs := make([]string, len(res))
	for i := range res {
		msgs[i] = res[i].Message
	}
	require.ElementsMatch(t, []string{
		"Node type constraint 'typeConstraint' must have exactly one property.",
		"Node existence constraint 'existenceConstraint' must have exactly one property.",
	}, msgs)
}
