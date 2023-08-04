package org.dice_research.enexa.transform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.rdf.test.ModelComparisonHelper;
import org.junit.Assert;
import org.junit.Test;
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
public class StreamingTransformatorTest {

    private Model expectedModel;
    private Lang outputLang;
    private Model[] inputModels;
    private Lang[] inputLangs;

    public StreamingTransformatorTest(Model expectedModel, Lang outputLang, Model[] inputModels, Lang[] inputLangs) {
        super();
        this.expectedModel = expectedModel;
        this.outputLang = outputLang;
        this.inputModels = inputModels;
        this.inputLangs = inputLangs;
        System.out.println("Testing with " + outputLang.toString()
                + " as output language and the following input languages: " + Arrays.toString(inputLangs));
    }

    @Test
    public void testCompInFileExt() {
        try {
            createFilesAndTest(true, true);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testFileExt() {
        try {
            createFilesAndTest(true, false);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testCompIn() {
        try {
            createFilesAndTest(false, true);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void test() {
        try {
            createFilesAndTest(false, false);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    public void createFilesAndTest(boolean fileExtensions, boolean compressedInput) throws Exception {
        // Create temporary file with given content
        Random random = new Random();
        File inputFiles[] = new File[inputModels.length];
        Compression[] compressions = new Compression[inputModels.length];
        StringBuilder endingBuilder = new StringBuilder();
        for (int i = 0; i < inputModels.length; ++i) {
            endingBuilder.delete(0, endingBuilder.length());
            if (fileExtensions) {
                endingBuilder.append('.');
                endingBuilder.append(inputLangs[i].getFileExtensions().get(0));
            }
            if (compressedInput) {
                if (random.nextBoolean()) {
                    endingBuilder.append(".bz2");
                    compressions[i] = Compression.BZIP2;
                } else {
                    endingBuilder.append(".gz");
                    compressions[i] = Compression.GZIP;
                }
            } else {
                compressions[i] = Compression.NONE;
            }
            inputFiles[i] = File.createTempFile("test-dataset-", endingBuilder.toString());
        }
        // Write Input files
        OutputStream out = null;
        for (int i = 0; i < inputModels.length; ++i) {
            try {
                out = new BufferedOutputStream(new FileOutputStream(inputFiles[i]));
                switch (compressions[i]) {
                case BZIP2: {
                    out = new BZip2CompressorOutputStream(out);
                    break;
                }
                case GZIP: {
                    out = new GzipCompressorOutputStream(out);
                    break;
                }
                case NONE: // falls through
                default:
                    break;
                }
                RDFDataMgr.write(out, inputModels[i], inputLangs[i]);
            } finally {
                IOUtils.closeQuietly(out);
            }
        }

        testTransformator(inputFiles, fileExtensions, Compression.NONE);
        testTransformator(inputFiles, fileExtensions, Compression.BZIP2);
        testTransformator(inputFiles, fileExtensions, Compression.GZIP);
    }

    public void testTransformator(File[] inputFiles, boolean fileExtensions, Compression compression) throws Exception {
        File outputFile = File.createTempFile("test-output-", "");
        try (StreamingTransformator transformator = StreamingTransformator.builder().setOutputFormat(outputLang)
                .setCompression(compression).setOutputFileName(outputFile.getName())
                .setOutputDirectory(outputFile.getParentFile()).build();) {
            if (fileExtensions) {
                transformator.addFiles2Stream(inputFiles);
            } else {
                for (int i = 0; i < inputLangs.length; ++i) {
                    transformator.addFile2Stream(inputFiles[i], inputLangs[i].getContentType().getContentTypeStr());
                }
            }
            // update output file after writing
            outputFile = transformator.getOutputFile();
        }

        Model readModel = ModelFactory.createDefaultModel();
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(outputFile));
            switch (compression) {
            case BZIP2: {
                is = new BZip2CompressorInputStream(is);
                break;
            }
            case GZIP: {
                is = new GzipCompressorInputStream(is);
                break;
            }
            case NONE: // falls through
            default:
                break;
            }
            RDFDataMgr.read(readModel, is, outputLang);
        } finally {
            IOUtils.closeQuietly(is);
        }
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

        Lang[] outputLanguages = new Lang[] { Lang.NTRIPLES, Lang.TTL, Lang.TRIG, Lang.NQUADS, Lang.RDFPROTO,
                Lang.RDFTHRIFT, Lang.TRIX };
        Lang[] inputSet1 = new Lang[] { Lang.NTRIPLES, Lang.TTL, Lang.JSONLD };
        Lang[] inputSet2 = new Lang[] { Lang.RDFXML, Lang.RDFJSON, Lang.N3 };

        List<Object[]> testConfigs = new ArrayList<Object[]>();
        for (int i = 0; i < outputLanguages.length; ++i) {
            testConfigs.add(new Object[] { expectedModel, outputLanguages[i], inputModels, inputSet1 });
            testConfigs.add(new Object[] { expectedModel, outputLanguages[i], inputModels, inputSet2 });
        }
        return testConfigs;
    }
}
