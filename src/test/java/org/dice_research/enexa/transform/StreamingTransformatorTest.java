package org.dice_research.enexa.transform;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.enexa.vocab.IANAMediaType;
import org.dice_research.rdf.test.ModelComparisonHelper;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * A test for the {@link StreamingTransformator} class that simply generates 3
 * files with varying serializations and concatenates them in different setups
 * (with/without input compression, output compression and explicit file
 * extensions).
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
@RunWith(Parameterized.class)
public class StreamingTransformatorTest extends AbstractTransformatorTest {

    public StreamingTransformatorTest(Model expectedModel, String outputFormatIri, Model[] inputModels,
            String[] inputFormatIris) {
        super(expectedModel, outputFormatIri, inputModels, inputFormatIris);
    }

    @Override
    protected void compareModels(InputStream is, String outputFormatIri, Model expectedModel) {
        Model readModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(readModel, is, IANAMediaType.iri2Lang(outputFormatIri));
        ModelComparisonHelper.assertModelsEqual(expectedModel, readModel);
    }

    @Parameters
    public static Collection<Object[]> data() {
        Model modelA = ModelFactory.createDefaultModel();
        modelA.add(modelA.createResource("http://example.org/mA/e1"), RDF.type,
                modelA.createResource("http://example.org/mA/c1"));
        modelA.add(modelA.createResource("http://example.org/mA/e2"), RDF.type,
                modelA.createResource("http://example.org/mA/c1"));
        modelA.add(modelA.createResource("http://example.org/mA/e3"), RDF.type,
                modelA.createResource("http://example.org/mA/c2"));

        Model modelB = ModelFactory.createDefaultModel();
        modelB.add(modelB.createResource("http://example.org/mA/e1"), modelB.createProperty("http://example.org/mB/p1"),
                modelB.createResource("http://example.org/mA/e2"));
        modelB.add(modelB.createResource("http://example.org/mA/e2"), modelB.createProperty("http://example.org/mB/p1"),
                modelB.createResource("http://example.org/mA/e3"));
        modelB.add(modelB.createResource("http://example.org/mA/e3"), modelB.createProperty("http://example.org/mB/p1"),
                modelB.createResource("http://example.org/mA/e4"));

        Model modelC = ModelFactory.createDefaultModel();
        modelC.add(modelC.createResource("http://example.org/mC/e1"), modelC.createProperty("http://example.org/mC/p1"),
                modelC.createResource("http://example.org/mC/e2"));
        modelC.add(modelC.createResource("http://example.org/mC/e2"), modelC.createProperty("http://example.org/mC/p1"),
                modelC.createResource("http://example.org/mC/e3"));
        modelC.add(modelC.createResource("http://example.org/mC/e3"), modelC.createProperty("http://example.org/mC/p1"),
                modelC.createResource("http://example.org/mC/e4"));

        Model[] inputModels = new Model[] { modelA, modelB, modelC };

        Model expectedModel = ModelFactory.createDefaultModel();
        for (int i = 0; i < inputModels.length; ++i) {
            expectedModel.add(inputModels[i]);
        }

        String[] outputLanguages = new String[] { IANAMediaType.lang2Iri(Lang.NTRIPLES),
                IANAMediaType.lang2Iri(Lang.TTL), IANAMediaType.lang2Iri(Lang.TRIG),
                IANAMediaType.lang2Iri(Lang.NQUADS), IANAMediaType.lang2Iri(Lang.RDFPROTO),
                IANAMediaType.lang2Iri(Lang.RDFTHRIFT), IANAMediaType.lang2Iri(Lang.TRIX) };
        String[] inputSet1 = new String[] { IANAMediaType.lang2Iri(Lang.NTRIPLES), IANAMediaType.lang2Iri(Lang.TTL),
                IANAMediaType.lang2Iri(Lang.JSONLD) };
        String[] inputSet2 = new String[] { IANAMediaType.lang2Iri(Lang.RDFXML), IANAMediaType.lang2Iri(Lang.RDFJSON),
                IANAMediaType.lang2Iri(Lang.N3) };

        List<Object[]> testConfigs = new ArrayList<Object[]>();
        for (int i = 0; i < outputLanguages.length; ++i) {
            testConfigs.add(new Object[] { expectedModel, outputLanguages[i], inputModels, inputSet1 });
            testConfigs.add(new Object[] { expectedModel, outputLanguages[i], inputModels, inputSet2 });
        }
        return testConfigs;
    }
}
