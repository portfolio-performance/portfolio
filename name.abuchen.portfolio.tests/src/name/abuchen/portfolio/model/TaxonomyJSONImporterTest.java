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
}
