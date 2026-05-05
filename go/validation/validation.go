package validation

import (
	"encoding/json"
	"fmt"

	"github.com/neo4j/graph-spec/go/internal/bridge"
	"github.com/neo4j/graph-spec/go/model"
)

type Issue struct {
	Code    string            `json:"code"`
	Message string            `json:"message"`
	Path    string            `json:"path"`
	Details map[string]string `json:"details"`
}

func Validate(model model.GraphModel) ([]Issue, error) {
	bytes, err := json.Marshal(model)
	if err != nil {
		return nil, err
	}
	res, err := bridge.Call(bridge.Validate, string(bytes))
	if err != nil {
		return nil, err
	}
	var issues []Issue
	if err := json.Unmarshal([]byte(res), &issues); err != nil {
		return nil, fmt.Errorf("error parsing validation issues: %w", err)
	}
	return issues, nil
}
