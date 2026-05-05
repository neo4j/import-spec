package model

import (
	"bytes"
	"encoding/json"
	"fmt"
)

type ExtensionValueUnion interface {
	ExtensionValueType() string
	isExtensionValue()
}

type ExtensionValue struct {
	ExtensionValueUnion
}

func (w ExtensionValue) MarshalJSON() ([]byte, error) {
	if w.ExtensionValueUnion == nil {
		return []byte("null"), nil
	}
	return json.Marshal(w.ExtensionValueUnion)
}

func (w *ExtensionValue) UnmarshalJSON(data []byte) error {
	data = bytes.TrimSpace(data)
	if bytes.Equal(data, []byte("null")) {
		w.ExtensionValueUnion = nil
		return nil
	}

	var peek struct {
		Type string `json:"type"`
	}
	if err := json.Unmarshal(data, &peek); err != nil {
		return fmt.Errorf("ExtensionValue: invalid JSON: %w", err)
	}
	if peek.Type == "" {
		return fmt.Errorf("ExtensionValue: missing discriminator field %q", "type")
	}

	var v ExtensionValueUnion
	switch peek.Type {
	case "String":
		v = &String{}
	case "Boolean":
		v = &Boolean{}
	case "Long":
		v = &Long{}
	case "Double":
		v = &Double{}
	case "List":
		v = &List{}
	case "Map":
		v = &Map{}
	default:
		return fmt.Errorf("ExtensionValue: unknown type %q", peek.Type)
	}

	if err := json.Unmarshal(data, v); err != nil {
		return fmt.Errorf("ExtensionValue: invalid %q payload: %w", peek.Type, err)
	}

	w.ExtensionValueUnion = v
	return nil
}

type String struct {
	Type  string `json:"type"`
	Value string `json:"value"`
}

func (String) isExtensionValue() {}

func (String) ExtensionValueType() string { return "String" }

type Boolean struct {
	Type  string `json:"type"`
	Value bool   `json:"value"`
}

func (Boolean) isExtensionValue() {}

func (Boolean) ExtensionValueType() string { return "Boolean" }

type Long struct {
	Type  string `json:"type"`
	Value int    `json:"value"`
}

func (Long) isExtensionValue() {}

func (Long) ExtensionValueType() string { return "Long" }

type Double struct {
	Type  string  `json:"type"`
	Value float64 `json:"value"`
}

func (Double) isExtensionValue() {}

func (Double) ExtensionValueType() string { return "Double" }

type List struct {
	Type  string           `json:"type"`
	Value []ExtensionValue `json:"value"`
}

func (List) isExtensionValue() {}

func (List) ExtensionValueType() string { return "List" }

type Map struct {
	Type  string                    `json:"type"`
	Value map[string]ExtensionValue `json:"value"`
}

func (Map) isExtensionValue() {}

func (Map) ExtensionValueType() string { return "Map" }

type NodeDisplay struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	X          float64                   `json:"x"`
	Y          float64                   `json:"y"`
}

type Display struct {
	Nodes map[string]NodeDisplay `json:"nodes,omitempty"`
}

type ForeignKeyReference struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	Fields     []string                  `json:"fields,omitempty"`
	Table      string                    `json:"table"`
}

type ForeignKey struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	Fields     []string                  `json:"fields"`
	References ForeignKeyReference       `json:"references"`
}

type MappingMode string

const (
	MappingModeMerge  MappingMode = "MERGE"
	MappingModeCreate MappingMode = "CREATE"
)

var MappingModeValues = []MappingMode{
	MappingModeMerge,
	MappingModeCreate,
}

type PropertyMapping struct {
	Field string `json:"field"`
}

type TargetMapping struct {
	Label      *string                    `json:"label,omitempty"`
	Node       *string                    `json:"node,omitempty"`
	Properties map[string]PropertyMapping `json:"properties,omitempty"`
}

type MappingUnion interface {
	MappingType() string
	isMapping()
}

type Mapping struct {
	MappingUnion
}

func (w Mapping) MarshalJSON() ([]byte, error) {
	if w.MappingUnion == nil {
		return []byte("null"), nil
	}
	return json.Marshal(w.MappingUnion)
}

func (w *Mapping) UnmarshalJSON(data []byte) error {
	data = bytes.TrimSpace(data)
	if bytes.Equal(data, []byte("null")) {
		w.MappingUnion = nil
		return nil
	}

	var peek struct {
		Type string `json:"type"`
	}
	if err := json.Unmarshal(data, &peek); err != nil {
		return fmt.Errorf("Mapping: invalid JSON: %w", err)
	}
	if peek.Type == "" {
		return fmt.Errorf("Mapping: missing discriminator field %q", "type")
	}

	var v MappingUnion
	switch peek.Type {
	case "NodeMapping":
		v = &NodeMapping{}
	case "RelationshipMapping":
		v = &RelationshipMapping{}
	case "QueryMapping":
		v = &QueryMapping{}
	case "LabelMapping":
		v = &LabelMapping{}
	default:
		return fmt.Errorf("Mapping: unknown type %q", peek.Type)
	}

	if err := json.Unmarshal(data, v); err != nil {
		return fmt.Errorf("Mapping: invalid %q payload: %w", peek.Type, err)
	}

	w.MappingUnion = v
	return nil
}

type NodeMapping struct {
	Keys       []string                   `json:"keys,omitempty"`
	MatchLabel *string                    `json:"matchLabel,omitempty"`
	Mode       *MappingMode               `json:"mode,omitempty"`
	Node       string                     `json:"node"`
	Properties map[string]PropertyMapping `json:"properties"`
	Table      string                     `json:"table"`
	Type       string                     `json:"type"`
}

func (NodeMapping) isMapping() {}

func (NodeMapping) MappingType() string { return "NodeMapping" }

type RelationshipMapping struct {
	From         TargetMapping              `json:"from"`
	Keys         []string                   `json:"keys,omitempty"`
	MatchLabel   *string                    `json:"matchLabel,omitempty"`
	Mode         *MappingMode               `json:"mode,omitempty"`
	Properties   map[string]PropertyMapping `json:"properties,omitempty"`
	Relationship string                     `json:"relationship"`
	Table        string                     `json:"table"`
	To           TargetMapping              `json:"to"`
	Type         string                     `json:"type"`
}

func (RelationshipMapping) isMapping() {}

func (RelationshipMapping) MappingType() string { return "RelationshipMapping" }

type QueryMapping struct {
	Query string `json:"query"`
	Table string `json:"table"`
	Type  string `json:"type"`
}

func (QueryMapping) isMapping() {}

func (QueryMapping) MappingType() string { return "QueryMapping" }

type LabelMapping struct {
	Field string `json:"field"`
	Table string `json:"table"`
	Type  string `json:"type"`
}

func (LabelMapping) isMapping() {}

func (LabelMapping) MappingType() string { return "LabelMapping" }

type Labels struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	Identifier *string                   `json:"identifier,omitempty"`
	Implied    []string                  `json:"implied,omitempty"`
	Optional   []string                  `json:"optional,omitempty"`
}

type NodeConstraint struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	Label      string                    `json:"label"`
	Properties []string                  `json:"properties"`
	Type       string                    `json:"type"`
}

type NodeIndex struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	Labels     []string                  `json:"labels"`
	Options    map[string]ExtensionValue `json:"options,omitempty"`
	Properties []string                  `json:"properties"`
	Type       string                    `json:"type"`
}

type Neo4jType string

const (
	Neo4jTypeAny               Neo4jType = "ANY"
	Neo4jTypeBoolean           Neo4jType = "BOOLEAN"
	Neo4jTypeListBoolean       Neo4jType = "LIST<BOOLEAN>"
	Neo4jTypeDate              Neo4jType = "DATE"
	Neo4jTypeListDate          Neo4jType = "LIST<DATE>"
	Neo4jTypeDuration          Neo4jType = "DURATION"
	Neo4jTypeListDuration      Neo4jType = "LIST<DURATION>"
	Neo4jTypeFloat32           Neo4jType = "FLOAT32"
	Neo4jTypeListFloat32       Neo4jType = "LIST<FLOAT32>"
	Neo4jTypeFloat             Neo4jType = "FLOAT"
	Neo4jTypeListFloat         Neo4jType = "LIST<FLOAT>"
	Neo4jTypeInteger8          Neo4jType = "INTEGER8"
	Neo4jTypeListInteger8      Neo4jType = "LIST<INTEGER8>"
	Neo4jTypeInteger16         Neo4jType = "INTEGER16"
	Neo4jTypeListInteger16     Neo4jType = "LIST<INTEGER16>"
	Neo4jTypeInteger32         Neo4jType = "INTEGER32"
	Neo4jTypeListInteger32     Neo4jType = "LIST<INTEGER32>"
	Neo4jTypeInteger           Neo4jType = "INTEGER"
	Neo4jTypeListInteger       Neo4jType = "LIST<INTEGER>"
	Neo4jTypeLocalDatetime     Neo4jType = "LOCAL DATETIME"
	Neo4jTypeListLocalDatetime Neo4jType = "LIST<LOCAL DATETIME>"
	Neo4jTypeLocalTime         Neo4jType = "LOCAL TIME"
	Neo4jTypeListLocalTime     Neo4jType = "LIST<LOCAL TIME>"
	Neo4jTypePoint             Neo4jType = "POINT"
	Neo4jTypeListPoint         Neo4jType = "LIST<POINT>"
	Neo4jTypeString            Neo4jType = "STRING"
	Neo4jTypeListString        Neo4jType = "LIST<STRING>"
	Neo4jTypeVectorFloat       Neo4jType = "VECTOR<FLOAT>"
	Neo4jTypeVectorFloat32     Neo4jType = "VECTOR<FLOAT32>"
	Neo4jTypeVectorInteger     Neo4jType = "VECTOR<INTEGER>"
	Neo4jTypeVectorInteger32   Neo4jType = "VECTOR<INTEGER32>"
	Neo4jTypeVectorInteger16   Neo4jType = "VECTOR<INTEGER16>"
	Neo4jTypeVectorInteger8    Neo4jType = "VECTOR<INTEGER8>"
	Neo4jTypeZonedDatetime     Neo4jType = "ZONED DATETIME"
	Neo4jTypeListZonedDatetime Neo4jType = "LIST<ZONED DATETIME>"
	Neo4jTypeZonedTime         Neo4jType = "ZONED TIME"
	Neo4jTypeListZonedTime     Neo4jType = "LIST<ZONED TIME>"
	Neo4jTypeUUID              Neo4jType = "UUID"
)

var Neo4jTypeValues = []Neo4jType{
	Neo4jTypeAny,
	Neo4jTypeBoolean,
	Neo4jTypeListBoolean,
	Neo4jTypeDate,
	Neo4jTypeListDate,
	Neo4jTypeDuration,
	Neo4jTypeListDuration,
	Neo4jTypeFloat32,
	Neo4jTypeListFloat32,
	Neo4jTypeFloat,
	Neo4jTypeListFloat,
	Neo4jTypeInteger8,
	Neo4jTypeListInteger8,
	Neo4jTypeInteger16,
	Neo4jTypeListInteger16,
	Neo4jTypeInteger32,
	Neo4jTypeListInteger32,
	Neo4jTypeInteger,
	Neo4jTypeListInteger,
	Neo4jTypeLocalDatetime,
	Neo4jTypeListLocalDatetime,
	Neo4jTypeLocalTime,
	Neo4jTypeListLocalTime,
	Neo4jTypePoint,
	Neo4jTypeListPoint,
	Neo4jTypeString,
	Neo4jTypeListString,
	Neo4jTypeVectorFloat,
	Neo4jTypeVectorFloat32,
	Neo4jTypeVectorInteger,
	Neo4jTypeVectorInteger32,
	Neo4jTypeVectorInteger16,
	Neo4jTypeVectorInteger8,
	Neo4jTypeZonedDatetime,
	Neo4jTypeListZonedDatetime,
	Neo4jTypeZonedTime,
	Neo4jTypeListZonedTime,
	Neo4jTypeUUID,
}

type Property struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	Name       *string                   `json:"name,omitempty"`
	Nullable   *bool                     `json:"nullable,omitempty"`
	Type       *Neo4jType                `json:"type,omitempty"`
	Unique     *bool                     `json:"unique,omitempty"`
}

type Node struct {
	Constraints map[string]NodeConstraint `json:"constraints,omitempty"`
	Extensions  map[string]ExtensionValue `json:"extensions,omitempty"`
	Indexes     map[string]NodeIndex      `json:"indexes,omitempty"`
	Labels      *Labels                   `json:"labels,omitempty"`
	Name        *string                   `json:"name,omitempty"`
	Properties  map[string]Property       `json:"properties,omitempty"`
}

type RelationshipConstraint struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	Options    map[string]ExtensionValue `json:"options,omitempty"`
	Properties []string                  `json:"properties"`
	Type       string                    `json:"type"`
}

type RelationshipIndex struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	Options    map[string]ExtensionValue `json:"options,omitempty"`
	Properties []string                  `json:"properties"`
	Type       string                    `json:"type"`
}

type RelationshipTarget struct {
	Label    *string `json:"label,omitempty"`
	Node     *string `json:"node,omitempty"`
	Property *string `json:"property,omitempty"`
}

type Relationship struct {
	Constraints map[string]RelationshipConstraint `json:"constraints,omitempty"`
	Extensions  map[string]ExtensionValue         `json:"extensions,omitempty"`
	From        RelationshipTarget                `json:"from"`
	Indexes     map[string]RelationshipIndex      `json:"indexes,omitempty"`
	Name        *string                           `json:"name,omitempty"`
	Properties  map[string]Property               `json:"properties,omitempty"`
	To          RelationshipTarget                `json:"to"`
	Type        string                            `json:"type"`
}

type TableField struct {
	Extensions map[string]ExtensionValue `json:"extensions,omitempty"`
	Name       *string                   `json:"name,omitempty"`
	Size       *int                      `json:"size,omitempty"`
	Suggested  *Neo4jType                `json:"suggested,omitempty"`
	Supported  []Neo4jType               `json:"supported,omitempty"`
	Type       *string                   `json:"type,omitempty"`
}

type Table struct {
	Extensions  map[string]ExtensionValue `json:"extensions,omitempty"`
	Fields      map[string]TableField     `json:"fields,omitempty"`
	ForeignKeys map[string]ForeignKey     `json:"foreignKeys,omitempty"`
	PrimaryKeys []string                  `json:"primaryKeys,omitempty"`
	Source      string                    `json:"source"`
}

type GraphModel struct {
	Display       *Display                `json:"display,omitempty"`
	Mappings      []Mapping               `json:"mappings,omitempty"`
	Nodes         map[string]Node         `json:"nodes,omitempty"`
	Relationships map[string]Relationship `json:"relationships,omitempty"`
	Tables        map[string]Table        `json:"tables,omitempty"`
	Version       string                  `json:"version"`
}
