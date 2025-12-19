/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.importer.v1.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.neo4j.importer.v1.ImportSpecificationDeserializer;
import org.neo4j.importer.v1.pipeline.ImportExecutionPlan.ImportStepStage;
import org.neo4j.importer.v1.sources.JdbcSource;
import org.neo4j.importer.v1.targets.NodeKeyConstraint;
import org.neo4j.importer.v1.targets.NodeReference;
import org.neo4j.importer.v1.targets.NodeSchema;
import org.neo4j.importer.v1.targets.NodeTarget;
import org.neo4j.importer.v1.targets.PropertyMapping;
import org.neo4j.importer.v1.targets.RelationshipTarget;

class ImportExecutionPlanIT {

    @Test
    void exposes_execution_plan() throws Exception {
        try (InputStream stream = this.getClass().getResourceAsStream("/e2e/execution-plan/northwind.yaml")) {
            assertThat(stream).isNotNull();
            try (var reader = new InputStreamReader(stream)) {
                var pipeline = ImportPipeline.of(ImportSpecificationDeserializer.deserialize(reader));

                var executionPlan = pipeline.executionPlan();

                var groups = executionPlan.getGroups();
                assertThat(groups).hasSize(1);
                var group = groups.get(0);
                var stages = group.getStages();
                assertThat(stages).hasSize(3);
                var productSource = new SourceStep(new JdbcSource(
                        "products",
                        "northwind",
                        "SELECT productname, unitprice FROM products ORDER BY productname ASC"));
                var productInCategorySource = new SourceStep(
                        new JdbcSource(
                                "products_in_categories",
                                "northwind",
                                "SELECT p.productname AS productname, c.categoryname AS categoryname FROM products p NATURAL JOIN categories c ORDER BY p.productname ASC"));
                var categorySource = new SourceStep(new JdbcSource(
                        "categories",
                        "northwind",
                        "SELECT categoryname, description FROM categories ORDER BY categoryname ASC"));
                assertThat(stages.get(0))
                        .isEqualTo(new ImportStepStage(Set.of(categorySource, productInCategorySource, productSource)));
                var productNodeStep = new NodeTargetStep(
                        new NodeTarget(
                                true,
                                "product_nodes",
                                "products",
                                null,
                                null,
                                (ObjectNode) null,
                                List.of("Product"),
                                List.of(
                                        new PropertyMapping("productname", "name", null),
                                        new PropertyMapping("unitprice", "unitPrice", null)),
                                nodeSchema(new NodeKeyConstraint(
                                        "product_name_key_constraint", "Product", List.of("name"), null))),
                        Set.of(productSource));
                var categoryNodeStep = new NodeTargetStep(
                        new NodeTarget(
                                true,
                                "category_nodes",
                                "categories",
                                null,
                                null,
                                (ObjectNode) null,
                                List.of("Category"),
                                List.of(
                                        new PropertyMapping("categoryname", "name", null),
                                        new PropertyMapping("description", "description", null)),
                                nodeSchema(new NodeKeyConstraint(
                                        "category_name_key_constraint", "Category", List.of("name"), null))),
                        Set.of(categorySource));
                assertThat(stages.get(1)).isEqualTo(new ImportStepStage(Set.of(productNodeStep, categoryNodeStep)));
                assertThat(stages.get(2))
                        .isEqualTo(new ImportStepStage(Set.of(new RelationshipTargetStep(
                                new RelationshipTarget(
                                        true,
                                        "product_in_category_relationships",
                                        "products_in_categories",
                                        null,
                                        "BELONGS_TO_CATEGORY",
                                        null,
                                        null,
                                        (ObjectNode) null,
                                        new NodeReference("product_nodes"),
                                        new NodeReference("category_nodes"),
                                        null,
                                        null),
                                productNodeStep,
                                categoryNodeStep,
                                Set.of(productInCategorySource, productNodeStep, categoryNodeStep)))));
            }
        }
    }

    @Test
    void exposes_execution_plan_with_relationships_sharing_common_nodes() throws Exception {
        try (InputStream stream =
                this.getClass().getResourceAsStream("/e2e/execution-plan/northwind-rels-with-common-nodes.yaml")) {
            assertThat(stream).isNotNull();
            try (var reader = new InputStreamReader(stream)) {
                var pipeline = ImportPipeline.of(ImportSpecificationDeserializer.deserialize(reader));

                var executionPlan = pipeline.executionPlan();

                var groups = executionPlan.getGroups();
                assertThat(groups).hasSize(1);
                var group = groups.get(0);
                var stages = group.getStages();
                assertThat(stages).hasSize(4);
                assertThat(stages.get(0).getSteps()).hasSize(5);
                var customerSource = new SourceStep(
                        new JdbcSource(
                                "customers",
                                "northwind",
                                "SELECT customer_id, first_name, last_name FROM customers ORDER BY first_name ASC, last_name ASC"));
                var customerNodeStep = new NodeTargetStep(
                        new NodeTarget(
                                true,
                                "customer_nodes",
                                "customers",
                                null,
                                null,
                                (ObjectNode) null,
                                List.of("Customer"),
                                List.of(
                                        new PropertyMapping("customer_id", "id", null),
                                        new PropertyMapping("first_name", "first_name", null),
                                        new PropertyMapping("last_name", "last_name", null)),
                                nodeSchema(new NodeKeyConstraint(
                                        "customer_id_key_constraint", "Customer", List.of("id"), null))),
                        Set.of(customerSource));
                assertThat(stages.get(1).getSteps()).hasSize(3).contains(customerNodeStep);
                var customerBoughtProductSource = new SourceStep(
                        new JdbcSource(
                                "customers_bought_products",
                                "northwind",
                                "SELECT p.productname AS productname, c.customer_id AS customer_id FROM products p NATURAL JOIN customers c ORDER BY p.productname ASC"));
                var productNodeStep = new NodeTargetStep(
                        new NodeTarget(
                                true,
                                "product_nodes",
                                "products",
                                null,
                                null,
                                (ObjectNode) null,
                                List.of("Product"),
                                List.of(
                                        new PropertyMapping("productname", "name", null),
                                        new PropertyMapping("unitprice", "unitPrice", null)),
                                nodeSchema(new NodeKeyConstraint(
                                        "product_name_key_constraint", "Product", List.of("name"), null))),
                        Set.of(new SourceStep(new JdbcSource(
                                "products",
                                "northwind",
                                "SELECT productname, unitprice FROM products ORDER BY productname ASC"))));
                var categoryNodeStep = new NodeTargetStep(
                        new NodeTarget(
                                true,
                                "category_nodes",
                                "categories",
                                null,
                                null,
                                (ObjectNode) null,
                                List.of("Category"),
                                List.of(
                                        new PropertyMapping("categoryname", "name", null),
                                        new PropertyMapping("description", "description", null)),
                                nodeSchema(new NodeKeyConstraint(
                                        "category_name_key_constraint", "Category", List.of("name"), null))),
                        Set.of(new SourceStep(new JdbcSource(
                                "categories",
                                "northwind",
                                "SELECT categoryname, description FROM categories ORDER BY categoryname ASC"))));
                var customersBoughtProductsStep = new RelationshipTargetStep(
                        new RelationshipTarget(
                                true,
                                "customers_bought_products_relationships",
                                "customers_bought_products",
                                null,
                                "BOUGHT",
                                null,
                                null,
                                (ObjectNode) null,
                                new NodeReference("customer_nodes"),
                                new NodeReference("product_nodes"),
                                null,
                                null),
                        customerNodeStep,
                        productNodeStep,
                        Set.of(customerBoughtProductSource, productNodeStep, customerNodeStep));
                assertThat(stages.get(2).getSteps()).hasSize(1).contains(customersBoughtProductsStep);
                var productInCategorySource = new SourceStep(
                        new JdbcSource(
                                "products_in_categories",
                                "northwind",
                                "SELECT p.productname AS productname, c.categoryname AS categoryname FROM products p NATURAL JOIN categories c ORDER BY p.productname ASC"));
                assertThat(stages.get(3).getSteps())
                        .hasSize(1)
                        .contains(new RelationshipTargetStep(
                                new RelationshipTarget(
                                        true,
                                        "product_in_category_relationships",
                                        "products_in_categories",
                                        null,
                                        "BELONGS_TO_CATEGORY",
                                        null,
                                        null,
                                        (ObjectNode) null,
                                        new NodeReference("product_nodes"),
                                        new NodeReference("category_nodes"),
                                        null,
                                        null),
                                productNodeStep,
                                categoryNodeStep,
                                // customersBoughtProductsStep is listed because it overlaps with this relationship
                                Set.of(
                                        customersBoughtProductsStep,
                                        productInCategorySource,
                                        productNodeStep,
                                        categoryNodeStep)));
            }
        }
    }

    @Test
    void exposes_execution_plan_with_reverse_relationships_sharing_common_nodes() throws Exception {
        try (InputStream stream =
                this.getClass().getResourceAsStream("/e2e/execution-plan/northwind-with-reverse-rels.yaml")) {
            assertThat(stream).isNotNull();
            try (var reader = new InputStreamReader(stream)) {
                var pipeline = ImportPipeline.of(ImportSpecificationDeserializer.deserialize(reader));

                var executionPlan = pipeline.executionPlan();

                var groups = executionPlan.getGroups();
                assertThat(groups).hasSize(1);
                var group = groups.get(0);
                var stages = group.getStages();
                assertThat(stages).hasSize(4);
                var productNodeStep = new NodeTargetStep(
                        new NodeTarget(
                                true,
                                "product_nodes",
                                "products",
                                null,
                                null,
                                (ObjectNode) null,
                                List.of("Product"),
                                List.of(
                                        new PropertyMapping("productname", "name", null),
                                        new PropertyMapping("unitprice", "unitPrice", null)),
                                nodeSchema(new NodeKeyConstraint(
                                        "product_name_key_constraint", "Product", List.of("name"), null))),
                        Set.of(new SourceStep(new JdbcSource(
                                "products",
                                "northwind",
                                "SELECT productname, unitprice FROM products ORDER BY productname ASC"))));
                var categoryNodeStep = new NodeTargetStep(
                        new NodeTarget(
                                true,
                                "category_nodes",
                                "categories",
                                null,
                                null,
                                (ObjectNode) null,
                                List.of("Category"),
                                List.of(
                                        new PropertyMapping("categoryname", "name", null),
                                        new PropertyMapping("description", "description", null)),
                                nodeSchema(new NodeKeyConstraint(
                                        "category_name_key_constraint", "Category", List.of("name"), null))),
                        Set.of(new SourceStep(new JdbcSource(
                                "categories",
                                "northwind",
                                "SELECT categoryname, description FROM categories ORDER BY categoryname ASC"))));
                assertThat(stages.get(1).getSteps()).hasSize(2).contains(productNodeStep, categoryNodeStep);
                var productInCategorySource = new SourceStep(
                        new JdbcSource(
                                "products_in_categories",
                                "northwind",
                                "SELECT p.productname AS productname, c.categoryname AS categoryname FROM products p NATURAL JOIN categories c ORDER BY p.productname ASC"));

                RelationshipTargetStep categoryIncludesProduct = new RelationshipTargetStep(
                        new RelationshipTarget(
                                true,
                                "category_of_product_relationships",
                                "products_in_categories",
                                null,
                                "CATEGORY_INCLUDES_PRODUCT",
                                null,
                                null,
                                (ObjectNode) null,
                                new NodeReference("category_nodes"),
                                new NodeReference("product_nodes"),
                                null,
                                null),
                        categoryNodeStep,
                        productNodeStep,
                        Set.of(productInCategorySource, productNodeStep, categoryNodeStep));
                assertThat(stages.get(2).getSteps()).hasSize(1).contains(categoryIncludesProduct);
                assertThat(stages.get(3).getSteps())
                        .hasSize(1)
                        .contains(new RelationshipTargetStep(
                                new RelationshipTarget(
                                        true,
                                        "product_in_category_relationships",
                                        "products_in_categories",
                                        null,
                                        "BELONGS_TO_CATEGORY",
                                        null,
                                        null,
                                        (ObjectNode) null,
                                        new NodeReference("product_nodes"),
                                        new NodeReference("category_nodes"),
                                        null,
                                        null),
                                productNodeStep,
                                categoryNodeStep,
                                Set.of(
                                        categoryIncludesProduct,
                                        productInCategorySource,
                                        productNodeStep,
                                        categoryNodeStep)));
            }
        }
    }

    private NodeSchema nodeSchema(NodeKeyConstraint key) {
        return new NodeSchema(null, List.of(key), null, null, null, null, null, null, null);
    }
}
