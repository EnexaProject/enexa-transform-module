package org.dice_research.enexa.transform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.dice_research.enexa.vocab.IANAMediaType;
import org.junit.Assert;
import org.junit.Test;

/**
 * An abstract test for {@link Transformator} classes that simply generates
 * several files with varying serializations and concatenates them in different
 * setups (with/without input compression, output compression and explicit file
 * extensions).
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public abstract class AbstractTransformatorTest {

    protected Model expectedModel;
    protected String outputFormatIri;
    protected Model[] inputModels;
    protected String[] inputFormatIris;
    protected Lang[] inputLangs;

    public AbstractTransformatorTest(Model expectedModel, String outputFormatIri, Model[] inputModels,
            String[] inputFormatIris) {
        super();
        this.expectedModel = expectedModel;
        this.outputFormatIri = outputFormatIri;
        this.inputModels = inputModels;
        this.inputFormatIris = inputFormatIris;
        this.inputLangs = new Lang[inputFormatIris.length];
        // Print information about this test
        System.out.println("Test output format: " + IANAMediaType.iri2ContentType(outputFormatIri.toString()));
        System.out.print("     input formats: ");
        for (int i = 0; i < inputFormatIris.length; ++i) {
            inputLangs[i] = IANAMediaType.iri2Lang(inputFormatIris[i]);
            Assert.assertNotNull("Input format cannot be represented as Lang instance.", inputLangs[i]);
            if (i > 0) {
                System.out.print("                    ");
            }
            System.out.println(IANAMediaType.iri2ContentType(inputFormatIris[i]));
        }
    }

//    @Test
//    public void testCompInFileExt() {
//        try {
//            createFilesAndTest(true, true);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Assert.fail();
//        }
//    }
//
//    @Test
//    public void testFileExt() {
//        try {
//            createFilesAndTest(true, false);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Assert.fail();
//        }
//    }
//
//    @Test
//    public void testCompIn() {
//        try {
//            createFilesAndTest(false, true);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Assert.fail();
//        }
//    }

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
        try (Transformator transformator = new TransformatorBuilder().setOutputFormat(outputFormatIri)
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
            compareModels(is, outputFormatIri, expectedModel);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    protected abstract void compareModels(InputStream is, String outputFormatIri, Model expectedModel);

}
