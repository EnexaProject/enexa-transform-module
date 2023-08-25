package org.dice_research.enexa.transform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.enexa.vocab.IANAMediaType;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * A test for the {@link RDF2OntologyTransformator} class that simply generates
 * 3 files with varying serializations and concatenates them in different setups
 * (with/without input compression, output compression and explicit file
 * extensions).
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
@RunWith(Parameterized.class)
public class RDF2OntologyTransformatorTest extends AbstractTransformatorTest {

    public RDF2OntologyTransformatorTest(Model expectedModel, String outputFormatIri, Model[] inputModels,
            String[] inputFormatIris) {
        super(expectedModel, outputFormatIri, inputModels, inputFormatIris);
    }

    @Override
    protected void compareModels(InputStream is, String outputFormatIri, Model expectedModel) {
        try {
            File expectedFile = File.createTempFile("test-expected-result-", ".nt");
            try (Writer writer = new FileWriter(expectedFile, StandardCharsets.UTF_8)) {
                expectedModel.write(writer, "Turtle");
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology expectedOntology = manager.loadOntologyFromOntologyDocument(expectedFile);
            File expectedFile2 = File.createTempFile("test-expected-result-", ".nt");
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(expectedFile2))) {
                manager.saveOntology(expectedOntology, out);
            }
            try (InputStream in = new BufferedInputStream(new FileInputStream(expectedFile2))) {
                expectedOntology = manager.loadOntologyFromOntologyDocument(in);
            }

            OWLOntology readOnt = manager.loadOntologyFromOntologyDocument(is);

            for (OWLAxiom a : expectedOntology.getAxioms()) {
                Assert.assertTrue("Read ontology does not contain the expected axiom " + a.toString(),
                        readOnt.containsAxiom(a));
            }
            for (OWLAxiom a : readOnt.getAxioms()) {
                Assert.assertTrue("Read ontology contains the additional axiom " + a.toString(),
                        expectedOntology.containsAxiom(a));
            }
        } catch (IOException | OWLOntologyStorageException | OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
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

        String[] outputLanguages = new String[] { IANAMediaType.contentType2Iri("application/owl+xml"),
                IANAMediaType.contentType2Iri("text/owl-manchester") };
        String[] inputSet1 = new String[] { IANAMediaType.lang2Iri(Lang.NTRIPLES), IANAMediaType.lang2Iri(Lang.TTL),
                IANAMediaType.lang2Iri(Lang.RDFXML) };

        List<Object[]> testConfigs = new ArrayList<Object[]>();
        for (int i = 0; i < outputLanguages.length; ++i) {
            testConfigs.add(new Object[] { expectedModel, outputLanguages[i], inputModels, inputSet1 });
        }
        return testConfigs;
    }
}
