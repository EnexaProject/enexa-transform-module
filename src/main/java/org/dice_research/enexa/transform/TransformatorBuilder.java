package org.dice_research.enexa.transform;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.dice_research.enexa.vocab.IANAMediaType;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformatorBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformatorBuilder.class);

    protected String outputFileName = null;
    protected File outputDirectory = null;
    protected Compression compression = Compression.NONE;
    protected String outputFormatIri = null;
    protected Lang outputFormatJena = null;
    protected OWLDocumentFormat outputFormatOwl = null;

    public Transformator build() throws IOException {
        if (outputFormatIri == null) {
            LOGGER.error("The given output format is null.");
            throw new IllegalArgumentException("The given output format is null.");
        }

        // Try to understand the output format
        outputFormatJena = IANAMediaType.iri2Lang(outputFormatIri);
        if (outputFormatJena == null) {
            outputFormatOwl = RDF2OntologyTransformator
                    .getFormatForMediaType(IANAMediaType.iri2ContentType(outputFormatIri));
        }
        if (outputFormatJena == null && outputFormatOwl == null) {
            String msg = "The given output format \"" + outputFormatIri + "\" is unknown.";
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        File outputFile = createOutputFile();

        // Create parent directoy if it doesn't exist
        if (outputDirectory != null && !outputDirectory.exists() && !outputDirectory.mkdirs()) {
            String msg = "Couldn't create the non-existing parent directory " + outputDirectory.toString()
                    + " for the output file. ";
            LOGGER.error(msg);
            throw new IOException(msg);
        }

        OutputStream fout = null;
        try {
            fout = createOutputStream(outputFile);
            if (outputFormatJena != null) {
                return new StreamingTransformator(createRDFStream(fout), fout, outputFile);
            } else {
                return RDF2OntologyTransformator.create(outputFormatOwl, outputFile, fout);
            }
        } catch (Throwable e) {
            // If the stream is open, try to close it
            IOUtils.closeQuietly(fout);
            throw new IOException("Error while building transformator.", e);
        }
    }

    protected File createOutputFile() {
        // Create File object for the output file
        StringBuilder outputFileBuilder = new StringBuilder();
        // Add parent path if it exists
        if (outputDirectory != null) {
            outputFileBuilder.append(outputDirectory.getAbsolutePath());
            outputFileBuilder.append(File.separator);
        }
        // Add file name (or choose a random name)
        outputFileBuilder.append(
                (outputFileName != null) ? outputFileName : Integer.toString(Math.abs((new Random()).nextInt())));
        // Add file extension of the output language
        String fileExtension = getFileExtension();
        if (fileExtension != null && !fileExtension.isEmpty()) {
            outputFileBuilder.append('.');
            outputFileBuilder.append(fileExtension);
        }
        // Add file extension if we use compression
        switch (compression) {
        case BZIP2: {
            outputFileBuilder.append(".bz2");
            break;
        }
        case GZIP: {
            outputFileBuilder.append(".gz");
            break;
        }
        case NONE: // falls through
        default:
            break;
        }
        return new File(outputFileBuilder.toString());
    }

    protected String getFileExtension() {
        if (outputFormatJena != null) {
            List<String> extensions = outputFormatJena.getFileExtensions();
            return extensions.size() > 0 ? extensions.get(0) : null;
        } else if (outputFormatIri.endsWith("application/owl+xml")) {
            return "owl";
        } else if (outputFormatIri.endsWith("text/owl-manchester")) {
            return "omn";
        } else {
            throw new IllegalStateException("There is no file extension defined for the format " + outputFormatIri);
        }
    }

    protected OutputStream createOutputStream(File outputFile) throws IOException {
        OutputStream fout = null;
        try {
            fout = new BufferedOutputStream(new FileOutputStream(outputFile));
            // Add compression if needed
            switch (compression) {
            case BZIP2: {
                fout = new BZip2CompressorOutputStream(fout);
                break;
            }
            case GZIP: {
                fout = new GzipCompressorOutputStream(fout);
                break;
            }
            case NONE: // falls through
            default:
                break;
            }

            return fout;
        } catch (Throwable e) {
            // If the stream is open, try to close it
            IOUtils.closeQuietly(fout);
            throw e;
        }
    }

    protected StreamRDF createRDFStream(OutputStream fout) {
        // Create RDF stream
        StreamRDF outStream = StreamRDFWriter.getWriterStream(fout, outputFormatJena);
        outStream.start();
        return outStream;
    }

    /**
     * @return the outputFormat
     */
    public String getOutputFormat() {
        return outputFormatIri;
    }

    /**
     * @param outputFormat the outputFormat to set
     */
    public TransformatorBuilder setOutputFormat(String outputFormatIri) {
        this.outputFormatIri = outputFormatIri;
        return this;
    }

    /**
     * @return the compression
     */
    public Compression getCompression() {
        return compression;
    }

    /**
     * @param compression the compression to set
     */
    public TransformatorBuilder setCompression(Compression compression) {
        this.compression = compression;
        return this;
    }

    /**
     * @return the outputFileName
     */
    public String getOutputFileName() {
        return outputFileName;
    }

    /**
     * @param outputFileName the outputFileName to set
     */
    public TransformatorBuilder setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
        return this;
    }

    /**
     * @return the outputDirectory
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * @param outputDirectory the outputDirectory to set
     */
    public TransformatorBuilder setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }
}
