package migration

import (
	"encoding/json"
	"fmt"

	"github.com/neo4j/graph-spec/go/internal/bridge"
	"github.com/neo4j/graph-spec/go/model"
)

type ModelType string
type ModelVersion string

const (
	ModelTypeDataModel  ModelType = "data_model"
	ModelTypeImportSpec ModelType = "import_spec"
	ModelTypeGraphSpec  ModelType = "graph_spec"

	ModelVersionGraphSpecLatest ModelVersion = "1.0.0"
	ModelVersionDataModelV23    ModelVersion = "2.3.0"
	ModelVersionDataModelV24    ModelVersion = "2.4.0"
	ModelVersionDataModelV30    ModelVersion = "3.0.0"
	ModelVersionImportSpecV1    ModelVersion = "1.0.0"
)

// ToGraphSpec returns the provided [jsonModel] migrated to the latest graph model representation.
// The input model should be a JSON string matching the provided [modelType]. If the migration path
// from [modelType] to graph model is not supported or the input model is malformed an error will
// be returned.
func ToGraphSpec(jsonModel string, modelType ModelType) (model.GraphModel, error) {
	res, err := bridge.Call(bridge.Migrate, jsonModel, string(modelType), string(ModelTypeGraphSpec), string(ModelVersionGraphSpecLatest))
	if err != nil {
		return model.GraphModel{}, err
	}

	var graph model.GraphModel
	if err := json.Unmarshal([]byte(res), &graph); err != nil {
		return model.GraphModel{}, fmt.Errorf("failed to unmarshal into graph model: %s", err)
	}
	return graph, nil
}

// FromGraphSpec returns the provided graph-spec [model] migrated to the target model. The returned
// model is a JSON string. If the migration path from graph-spec to [targetType]:[targetVersion] is
// not supported or the input model is invalid an error will be returned.
func FromGraphSpec(model model.GraphModel, targetType ModelType, targetVersion ModelVersion) (string, error) {
	bytes, err := json.Marshal(model)
	if err != nil {
		return "", err
	}
	res, err := bridge.Call(bridge.Migrate, string(bytes), string(ModelTypeGraphSpec), string(targetType), string(targetVersion))
	if err != nil {
		return "", err
	}
	return res, nil
}
