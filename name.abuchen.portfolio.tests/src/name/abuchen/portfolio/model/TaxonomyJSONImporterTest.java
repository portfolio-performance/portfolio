package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.TaxonomyJSONImporter.Operation;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TaxonomyJSONImporterTest
{
    private Client client;
    private Taxonomy taxonomy;
    private Security security;

    @Before
    public void setUp()
    {
        client = new Client();

        security = new Security();
        security.setName("Test Security");
        security.setIsin("US1234567890");
        security.setTickerSymbol("TEST");
        security.setCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(security);

        taxonomy = new Taxonomy("Test Taxonomy");
        Classification root = new Classification(null, "root", "Root");
        taxonomy.setRootNode(root);

        var classification = new Classification(root, "existing-id", "Existing Category");
        classification.setKey("existing-key");
        root.addChild(classification);

        client.addTaxonomy(taxonomy);
    }

    @Test
    public void testStraightforwardImport() throws IOException
    {
        String json = """
                        {
                          "categories": [
                            {
                              "name": "New Category",
                              "key": "new-key",
                              "description": "A new test category",
                              "color": "#FF0000"
                            }
                          ],
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "US1234567890",
                                "name": "Test Security"
                              },
                              "categories": [
                                {
                                  "key": "new-key",
                                  "weight": 50.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy);

        var importResult = importer.importTaxonomy(new StringReader(json));
        var changeEntries = importResult.getChanges();

        assertThat(importResult.hasChanges(), is(true));
        assertThat(changeEntries.size(), is(2));

        // check change entries

        assertThat(changeEntries, hasItem(allOf( //
                        hasProperty("operation", is(Operation.CREATE)), //
                        hasProperty("target", is(Classification.class)),
                        hasProperty("comment", containsString("New Category")))));

        assertThat(changeEntries, hasItem(allOf( //
                        hasProperty("operation", is(Operation.CREATE)),
                        hasProperty("target", is(Classification.Assignment.class)),
                        hasProperty("comment", containsString("New Category")),
                        hasProperty("comment", containsString("Test Security")),
                        hasProperty("comment", containsString(Values.WeightPercent.format(50_00))))));

        // verify the new category was created
        assertThat(taxonomy.getRoot().getChildren().size(), is(2));
        Classification newCategory = taxonomy.getRoot().getChildren().stream()
                        .filter(c -> "New Category".equals(c.getName())).findFirst().orElse(null);
        assertThat(newCategory, is(notNullValue()));
        assertThat(newCategory.getKey(), is("new-key"));
        assertThat(newCategory.getNote(), is("A new test category"));
        assertThat(newCategory.getColor(), is("#FF0000"));

        // verify the assignment exists on the new category
        assertThat(newCategory.getAssignments().size(), is(1));
        Classification.Assignment assignment = newCategory.getAssignments().get(0);
        assertThat(assignment.getInvestmentVehicle(), is(security));
        assertThat(assignment.getWeight(), is(Classification.ONE_HUNDRED_PERCENT / 2));
    }

    @Test
    public void testAddingClassificationAndAssignmentOnPathOnly() throws IOException
    {
        String json = """
                        {
                          "categories": [
                            {
                              "name": "Existing Category",
                              "children": [
                                {
                                  "name": "New Category",
                                  "description": "A child category"
                                }
                              ]
                            }
                          ],
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "US1234567890",
                                "name": "Test Security"
                              },
                              "categories": [
                                {
                                  "path": [ "Existing Category", "New Category" ],
                                  "weight": 50.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy);

        var importResult = importer.importTaxonomy(new StringReader(json));
        importResult.getChanges();

        // verify the new category was created
        assertThat(taxonomy.getRoot().getChildren().size(), is(1));

        var newClassification = taxonomy.getRoot().getChildren().getFirst().getChildren().getFirst();

        assertThat(newClassification.getName(), is("New Category"));
        assertThat(newClassification.getNote(), is("A child category"));
        assertThat(newClassification.getKey(), is(""));

        // verify the assignment exists on the new category
        assertThat(newClassification.getAssignments().size(), is(1));
        Classification.Assignment assignment = newClassification.getAssignments().getFirst();
        assertThat(assignment.getInvestmentVehicle(), is(security));
        assertThat(assignment.getWeight(), is(Classification.ONE_HUNDRED_PERCENT / 2));
    }

    @Test
    public void testUpdateExistingCategory() throws IOException
    {
        String json = """
                        {
                          "categories": [
                            {
                              "name": "Updated Name",
                              "key": "existing-key",
                              "description": "Updated description",
                              "color": "#00FF00"
                            }
                          ]
                        }
                        """;

        var existingCategory = taxonomy.getRoot().getChildren().stream().filter(c -> "existing-key".equals(c.getKey()))
                        .findFirst().orElse(null);
        assertThat(existingCategory, is(notNullValue()));

        var importer = new TaxonomyJSONImporter(client, taxonomy);
        importer.importTaxonomy(new StringReader(json));

        assertThat(existingCategory.getName(), is("Updated Name"));
        assertThat(existingCategory.getNote(), is("Updated description"));
        assertThat(existingCategory.getColor(), is("#00FF00"));
    }

    @Test
    public void testInstrumentNotFound() throws IOException
    {
        String json = """
                        {
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "UNKNOWN123456",
                                "name": "Unknown Security"
                              },
                              "categories": [
                                {
                                  "key": "existing-key",
                                  "weight": 100.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy);

        var result = importer.importTaxonomy(new StringReader(json));

        assertThat(result.getChanges(), hasItem(allOf( //
                        hasProperty("operation", is(Operation.SKIPPED)),
                        hasProperty("target", is(InvestmentVehicle.class)),
                        hasProperty("comment", containsString("Unknown Security")))));
    }

    @Test
    public void testAssignmentIsUpdated() throws IOException
    {
        String json = """
                        {
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "US1234567890"
                              },
                              "categories": [
                                {
                                  "key": "existing-key",
                                  "weight": 99.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        taxonomy.getRoot().getChildren().getFirst()
                        .addAssignment(new Assignment(security, Classification.ONE_HUNDRED_PERCENT));

        var importer = new TaxonomyJSONImporter(client, taxonomy);

        var result = importer.importTaxonomy(new StringReader(json));

        assertThat(result.getChanges(), hasItem(allOf( //
                        hasProperty("operation", is(Operation.UPDATE)), //
                        hasProperty("target", is(Assignment.class)))));

        assertThat(taxonomy.getRoot().getChildren().getFirst().getAssignments().getFirst().getWeight(), is(99_00));
    }

    @Test
    public void testClassificationIsNotFound() throws IOException
    {
        String json = """
                        {
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "US1234567890"
                              },
                              "categories": [
                                {
                                  "path": [ "hello" ],
                                  "weight": 99.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        taxonomy.getRoot().getChildren().getFirst()
                        .addAssignment(new Assignment(security, Classification.ONE_HUNDRED_PERCENT));

        var importer = new TaxonomyJSONImporter(client, taxonomy);

        var result = importer.importTaxonomy(new StringReader(json));

        assertThat(result.getChanges(), hasItem(allOf( //
                        hasProperty("operation", is(Operation.SKIPPED)), //
                        hasProperty("target", is(Assignment.class)))));
    }

    @Test
    public void testClassificationIsMovedBasedOnKey() throws IOException
    {
        String json = """
                        {
                          "categories": [
                            {
                              "name": "Parent Category",
                              "key": "parent-key",
                              "children": [
                                {
                                  "name": "Child Category",
                                  "key": "child-key",
                                  "description": "A child category"
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var classification = new Classification(taxonomy.getRoot(), "child-key", "Existing Child Category");
        classification.setKey("child-key");
        taxonomy.getRoot().addChild(classification);
        assertThat(taxonomy.getRoot().getChildren().size(), is(2));

        var importer = new TaxonomyJSONImporter(client, taxonomy);
        var result = importer.importTaxonomy(new StringReader(json));

        assertThat(result.hasChanges(), is(true));

        // child is moved to new parent below 'parent-key'
        assertThat(taxonomy.getRoot().getChildren().size(), is(2));
        var parentCategory = taxonomy.getRoot().getChildren().stream()
                        .filter(c -> "Parent Category".equals(c.getName())).findFirst().get();

        // should have created child category
        assertThat(parentCategory.getChildren().size(), is(1));
        var childCategory = parentCategory.getChildren().get(0);
        assertThat(childCategory.getName(), is("Child Category"));
        assertThat(childCategory.getKey(), is("child-key"));
        assertThat(childCategory.getNote(), is("A child category"));
    }

    @Test
    public void testWeightExceedsLimit() throws IOException
    {
        String json = """
                        {
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "US1234567890"
                              },
                              "categories": [
                                {
                                  "key": "existing-key",
                                  "weight": 60.0
                                },
                                {
                                  "key": "existing-key",
                                  "weight": 50.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy);

        var result = importer.importTaxonomy(new StringReader(json));

        // Should have an error for weight exceeding 100%
        assertThat(result.getChanges(), hasItem(allOf( //
                        hasProperty("operation", is(Operation.ERROR)),
                        hasProperty("comment", containsString("100%")))));
    }

    @Test(expected = IOException.class)
    public void testInvalidJSON() throws IOException
    {
        String invalidJson = "{ invalid json";

        TaxonomyJSONImporter importer = new TaxonomyJSONImporter(client, taxonomy);

        importer.importTaxonomy(new StringReader(invalidJson));
    }

    @Test(expected = IOException.class)
    public void testInvalidPath() throws IOException
    {
        String json = """
                        {
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "US1234567890"
                              },
                              "categories": [
                                {
                                  "path": "hello",
                                  "weight": 99.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        taxonomy.getRoot().getChildren().getFirst()
                        .addAssignment(new Assignment(security, Classification.ONE_HUNDRED_PERCENT));

        var importer = new TaxonomyJSONImporter(client, taxonomy);
        importer.importTaxonomy(new StringReader(json));
    }

    @Test
    public void testNestedCategories() throws IOException
    {
        String json = """
                        {
                          "categories": [
                            {
                              "name": "Parent Category",
                              "key": "parent-key",
                              "children": [
                                {
                                  "name": "Child Category",
                                  "key": "child-key",
                                  "description": "A child category"
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy);
        var result = importer.importTaxonomy(new StringReader(json));

        assertThat(result.hasChanges(), is(true));

        // should have created parent category
        assertThat(taxonomy.getRoot().getChildren().size(), is(2));
        var parentCategory = taxonomy.getRoot().getChildren().stream()
                        .filter(c -> "Parent Category".equals(c.getName())).findFirst().get();

        // should have created child category
        assertThat(parentCategory.getChildren().size(), is(1));
        var childCategory = parentCategory.getChildren().get(0);
        assertThat(childCategory.getName(), is("Child Category"));
        assertThat(childCategory.getKey(), is("child-key"));
        assertThat(childCategory.getNote(), is("A child category"));
    }

    @Test
    public void testThatParentIsNotMovedToItsChild() throws IOException
    {
        String json = """
                        {
                          "categories": [
                            {
                              "name": "Existing Category",
                              "children": [
                                {
                                  "name": "Child Category",
                                  "key": "existing-key"
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy);
        var result = importer.importTaxonomy(new StringReader(json));

        // should have created parent category
        assertThat(taxonomy.getRoot().getChildren().size(), is(1));
        var parentCategory = taxonomy.getRoot().getChildren().getFirst();
        assertThat(parentCategory.getChildren().size(), is(0));

        assertThat(result.getChanges(), hasItem(allOf( //
                        hasProperty("operation", is(Operation.ERROR)), //
                        hasProperty("target", is(Classification.class)))));

    }

    @Test
    public void testWarningForMultipleMatchingInstruments() throws IOException
    {
        // Add another security with the same ISIN to create multiple matches
        var duplicateSecurity = new Security();
        duplicateSecurity.setName("Duplicate Security");
        duplicateSecurity.setIsin("US1234567890");
        duplicateSecurity.setCurrencyCode(CurrencyUnit.EUR);
        client.addSecurity(duplicateSecurity);

        // assert the there are no assignments yet
        assertThat(taxonomy.getRoot().getChildren().getFirst().getAssignments().size(), is(0));

        String json = """
                        {
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "US1234567890",
                                "name": "Test Security"
                              },
                              "categories": [
                                {
                                  "key": "existing-key",
                                  "weight": 50.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy);
        var result = importer.importTaxonomy(new StringReader(json));

        // Should have a WARNING for multiple instruments found
        assertThat(result.getChanges(), hasItem(allOf( //
                        hasProperty("operation", is(Operation.WARNING)),
                        hasProperty("target", is(InvestmentVehicle.class)),
                        hasProperty("comment", containsString("2 instruments found")))));

        // assert the there are no assignments yet
        assertThat(taxonomy.getRoot().getChildren().getFirst().getAssignments().size(), is(2));

        for (var assignment : taxonomy.getRoot().getChildren().getFirst().getAssignments())
        {
            assertThat(assignment.getWeight(), is(Classification.ONE_HUNDRED_PERCENT / 2));
        }
    }

    @Test
    public void testDuplicateVehiclePreventionWithSameInstrument() throws IOException
    {
        String json = """
                        {
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "US1234567890"
                              },
                              "categories": [
                                {
                                  "key": "existing-key",
                                  "weight": 30.0
                                }
                              ]
                            },
                            {
                              "identifiers": {
                                "name": "Test Security"
                              },
                              "categories": [
                                {
                                  "key": "existing-key",
                                  "weight": 40.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy);
        var result = importer.importTaxonomy(new StringReader(json));

        // Should have a WARNING for ignoring duplicate assignment
        assertThat(result.getChanges(), hasItem(allOf( //
                        hasProperty("operation", is(Operation.WARNING)),
                        hasProperty("target", is(InvestmentVehicle.class)),
                        hasProperty("comment", containsString("Ignoring assignment")),
                        hasProperty("comment", containsString("matched by another entry from the JSON already")))));

        // Should only have one assignment created (not two)
        var assignmentCreateCount = result.getChanges().stream()
                        .filter(c -> c.getOperation() == Operation.CREATE && c.getTarget() == Assignment.class).count();
        assertThat(assignmentCreateCount, is(1L));

        // assert the there there is only one assignment yet
        assertThat(taxonomy.getRoot().getChildren().getFirst().getAssignments().size(), is(1));

        // assert that the weight is 30% as defined by the first entry
        assertThat(taxonomy.getRoot().getChildren().getFirst().getAssignments().getFirst().getWeight(),
                        is(Math.round(0.3f * Classification.ONE_HUNDRED_PERCENT)));
    }

    @Test
    public void testImprovedErrorHandlingWithUnknownName() throws IOException
    {
        String json = """
                        {
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "NONEXISTENT123"
                              },
                              "categories": [
                                {
                                  "key": "existing-key",
                                  "weight": "invalid_weight_format"
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy);
        var result = importer.importTaxonomy(new StringReader(json));

        // Should have a SKIPPED entry for instrument not found with fallback to
        // "unknown"
        assertThat(result.getChanges(), hasItem(allOf( //
                        hasProperty("operation", is(Operation.SKIPPED)),
                        hasProperty("target", is(InvestmentVehicle.class)),
                        hasProperty("comment", containsString("unknown")))));
    }

    @Test
    public void testPruneAbsentClassificationsPreservesUUIDs() throws IOException
    {
        // Set up existing taxonomy with categories
        var existingCategory1 = new Classification(taxonomy.getRoot(), "uuid1", "Category 1");
        existingCategory1.setKey("cat1");
        taxonomy.getRoot().addChild(existingCategory1);

        var existingCategory2 = new Classification(taxonomy.getRoot(), "uuid2", "Category 2");
        existingCategory2.setKey("cat2");
        taxonomy.getRoot().addChild(existingCategory2);

        String json = """
                        {
                          "categories": [
                            {
                              "name": "Category 1 Updated",
                              "key": "cat1",
                              "description": "Updated description"
                            },
                            {
                              "name": "Category 3",
                              "key": "cat3"
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy, false, true);
        var result = importer.importTaxonomy(new StringReader(json));

        assertThat(result.hasChanges(), is(true));

        // Category 1 should be updated but preserve UUID
        var updatedCategory = taxonomy.getRoot().getChildren().stream().filter(c -> "cat1".equals(c.getKey()))
                        .findFirst().orElse(null);
        assertThat(updatedCategory, is(notNullValue()));
        assertThat(updatedCategory.getId(), is("uuid1")); // UUID preserved
        assertThat(updatedCategory.getName(), is("Category 1 Updated"));
        assertThat(updatedCategory.getNote(), is("Updated description"));

        // Category 2 should be deleted (not in JSON)
        var deletedCategory = taxonomy.getRoot().getChildren().stream().filter(c -> "cat2".equals(c.getKey()))
                        .findFirst().orElse(null);
        assertThat(deletedCategory, is(org.hamcrest.core.IsNull.nullValue())); // Should
                                                                               // have
                                                                               // been
                                                                               // removed

        // Category 3 should be created
        var newCategory = taxonomy.getRoot().getChildren().stream().filter(c -> "cat3".equals(c.getKey())).findFirst()
                        .orElse(null);
        assertThat(newCategory, is(notNullValue()));

        // Check operations in result
        assertThat(result.getChanges(), hasItem(allOf(hasProperty("operation", is(Operation.DELETE)),
                        hasProperty("comment", containsString("Remove category")))));

        assertThat(result.getChanges(), hasItem(allOf(hasProperty("operation", is(Operation.CREATE)),
                        hasProperty("comment", containsString("Create new category")))));
    }

    @Test
    public void testPruneAbsentClassificationsOrderPreservation() throws IOException
    {
        // Set up existing taxonomy with categories in different order
        var existingCategory1 = new Classification(taxonomy.getRoot(), "uuid1", "B Category");
        existingCategory1.setKey("b");
        taxonomy.getRoot().addChild(existingCategory1);

        var existingCategory2 = new Classification(taxonomy.getRoot(), "uuid2", "A Category");
        existingCategory2.setKey("a");
        taxonomy.getRoot().addChild(existingCategory2);

        String json = """
                        {
                          "categories": [
                            {
                              "name": "A Category",
                              "key": "a"
                            },
                            {
                              "name": "B Category",
                              "key": "b"
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy, false, true);
        importer.importTaxonomy(new StringReader(json));

        // Check that order matches JSON: A, then B
        var children = taxonomy.getRoot().getChildren();
        assertThat(children.size(), is(2));
        assertThat(children.get(0).getKey(), is("a"));
        assertThat(children.get(1).getKey(), is("b"));

        // Check ranks
        assertThat(children.get(0).getRank(), is(0));
        assertThat(children.get(1).getRank(), is(1));
    }

    @Test
    public void testPruneAbsentClassificationsRemovesAssignmentsNotInJSON() throws IOException
    {
        // Add existing assignments
        var existingCategory = taxonomy.getRoot().getChildren().get(0); // "existing-key"
        existingCategory.addAssignment(new Assignment(security, Classification.ONE_HUNDRED_PERCENT));

        // Add another security
        var security2 = new Security();
        security2.setName("Security 2");
        security2.setIsin("US9876543210");
        client.addSecurity(security2);

        String json = """
                        {
                          "categories": [
                            {
                              "name": "Existing Category",
                              "key": "existing-key"
                            }
                          ],
                          "instruments": [
                            {
                              "identifiers": {
                                "isin": "US9876543210"
                              },
                              "categories": [
                                {
                                  "key": "existing-key",
                                  "weight": 75.0
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy, false, true);
        var result = importer.importTaxonomy(new StringReader(json));

        // Original security assignment should be deleted (not in JSON)
        assertThat(result.getChanges(), hasItem(allOf(hasProperty("operation", is(Operation.DELETE)),
                        hasProperty("comment", containsString("not in JSON")))));

        // New assignment should be created
        assertThat(result.getChanges(), hasItem(allOf(hasProperty("operation", is(Operation.CREATE)),
                        hasProperty("comment", containsString("Security 2")))));

        // Final state: only one assignment for security2
        assertThat(existingCategory.getAssignments().size(), is(1));
        assertThat(existingCategory.getAssignments().get(0).getInvestmentVehicle(), is(security2));
    }

    @Test
    public void testReplaceModeWithNestedCategories() throws IOException
    {
        // Set up existing nested structure
        var parentCategory = new Classification(taxonomy.getRoot(), "parent-uuid", "Parent");
        parentCategory.setKey("parent");
        taxonomy.getRoot().addChild(parentCategory);

        var childCategory1 = new Classification(parentCategory, "child1-uuid", "Child 1");
        childCategory1.setKey("child1");
        parentCategory.addChild(childCategory1);

        var childCategory2 = new Classification(parentCategory, "child2-uuid", "Child 2");
        childCategory2.setKey("child2");
        parentCategory.addChild(childCategory2);

        String json = """
                        {
                          "categories": [
                            {
                              "name": "Parent Updated",
                              "key": "parent",
                              "children": [
                                {
                                  "name": "Child 1",
                                  "key": "child1"
                                },
                                {
                                  "name": "New Child",
                                  "key": "newchild"
                                }
                              ]
                            }
                          ]
                        }
                        """;

        var importer = new TaxonomyJSONImporter(client, taxonomy, false, true);
        var result = importer.importTaxonomy(new StringReader(json));

        assertThat(result.hasChanges(), is(true));

        // Parent should be updated, UUID preserved
        var updatedParent = taxonomy.getRoot().getChildren().stream().filter(c -> "parent".equals(c.getKey()))
                        .findFirst().orElse(null);
        assertThat(updatedParent, is(notNullValue()));
        assertThat(updatedParent.getId(), is("parent-uuid"));
        assertThat(updatedParent.getName(), is("Parent Updated"));

        // Child1 should remain, UUID preserved
        var child1 = updatedParent.getChildren().stream().filter(c -> "child1".equals(c.getKey())).findFirst()
                        .orElse(null);
        assertThat(child1, is(notNullValue()));
        assertThat(child1.getId(), is("child1-uuid"));

        // Child2 should be removed
        assertThat(result.getChanges(), hasItem(allOf(hasProperty("operation", is(Operation.DELETE)),
                        hasProperty("comment", containsString("Child 2")))));

        // New child should be created
        var newChild = updatedParent.getChildren().stream().filter(c -> "newchild".equals(c.getKey())).findFirst()
                        .orElse(null);
        assertThat(newChild, is(notNullValue()));
        assertThat(newChild.getName(), is("New Child"));
    }

    @Test
    public void testUpdateModeDoesNotRemoveExistingCategories() throws IOException
    {
        // Set up existing taxonomy with categories
        var existingCategory1 = new Classification(taxonomy.getRoot(), "uuid1", "Category 1");
        existingCategory1.setKey("cat1");
        taxonomy.getRoot().addChild(existingCategory1);

        var existingCategory2 = new Classification(taxonomy.getRoot(), "uuid2", "Category 2");
        existingCategory2.setKey("cat2");
        taxonomy.getRoot().addChild(existingCategory2);

        String json = """
                        {
                          "categories": [
                            {
                              "name": "Category 3",
                              "key": "cat3"
                            }
                          ]
                        }
                        """;

        // Use UPDATE mode (not replace)
        var importer = new TaxonomyJSONImporter(client, taxonomy, false, false);
        var result = importer.importTaxonomy(new StringReader(json));

        assertThat(result.hasChanges(), is(true));

        // Both existing categories should still be present
        // Original existing + 2 added + 1 new
        assertThat(taxonomy.getRoot().getChildren().size(), is(4));

        // No DELETE operations should occur in UPDATE mode
        var deleteOperations = result.getChanges().stream().filter(c -> c.getOperation() == Operation.DELETE).count();
        assertThat(deleteOperations, is(0L));
    }
}
