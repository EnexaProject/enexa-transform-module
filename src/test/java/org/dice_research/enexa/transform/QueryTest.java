package org.dice_research.enexa.transform;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.riot.Lang;
import org.dice_research.sparql.SparqlQueryUtils;
import org.dice_research.sparql.test.ConstructQueryTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class QueryTest extends ConstructQueryTest {
    
    public static final String META_DATA_GRAPH_IRI = "http://example.org/meta-graph";
    public static final String SECOND_GRAPH_IRI = "http://example.org/other-graph";

    public QueryTest(String storeContentResource, Lang storeContentLang, String expectedResultResource,
            Lang expectedResultLang, String query) {
        super(META_DATA_GRAPH_IRI, SECOND_GRAPH_IRI, storeContentResource, storeContentLang, expectedResultResource, expectedResultLang, query);
    }

    @Parameters
    public static Collection<Object[]> data() {
        final String MODULE_INSTANCE_VARIABLE = "?moduleInstance";
        final String MODULE_INSTANCE_IRI = "http://example.org/module/123";

        ParameterizedSparqlString query = SparqlQueryUtils.loadParameterizedQuery(QueryTest.class.getClassLoader(),
                "org/dice_research/enexa/transform/getParameters.query", StandardCharsets.UTF_8);
        query.setIri(MODULE_INSTANCE_VARIABLE, MODULE_INSTANCE_IRI);
        query.setIri("?graph", META_DATA_GRAPH_IRI);

        List<Object[]> testConfigs = new ArrayList<Object[]>();
        testConfigs.add(new Object[] { "org/dice_research/enexa/transform/testDataset.ttl", Lang.TURTLE,
                "org/dice_research/enexa/transform/expectedParameters.ttl", Lang.TURTLE, query.toString() });
        return testConfigs;
    }
}
