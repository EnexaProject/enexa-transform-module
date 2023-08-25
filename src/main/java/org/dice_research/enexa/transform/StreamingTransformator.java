package org.dice_research.enexa.transform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import javax.annotation.CheckForNull;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class allows the concatenation of one or several RDF files. Note that
 * the files are streamed to avoid a high memory consumption. Hence, no triples
 * are <b>not deduplicated</b>. During this process, the files can be
 * transformed into a different serialization language. As input formats, this
 * class basically supports all formats that are supported by the used Apache
 * Jena library. As output format, only Turtle, N-Triples, N-Quads and Trig are
 * supported since they can be streamed by the Jena library.
 * </p>
 * 
 * <p>
 * Input and output files can be compressed with BZIP2 or GZIP. However, the
 * class has to be able to identify the file format of the input files.
 * </p>
 * 
 * <p>
 * Note that the input because of the variety of input formats, this class has
 * to be able to identify the format of an input file. This is done by using
 * either the given media type or based on the file extensions in the given file
 * name. The content of the file is <b>not</b> used.
 * </p>
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class StreamingTransformator implements Transformator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingTransformator.class);

    /**
     * The output stream to which the RDF data is written.
     */
    protected StreamRDF outStream;
    /**
     * The lower-level output stream that is closed at the end of the process.
     */
    protected OutputStream out;
    /**
     * The output file to which the data is written.
     */
    protected File outputFile;

    /**
     * Constructor. <b>Note:</b> the class will take ownership of the given streams.
     * Hence, it will close them when this instance is closed.
     * 
     * @param outStream  the output stream to which the RDF data is written.
     * @param out        the lower-level output stream that is closed at the end of
     *                   the process.
     * @param outputFile the output file to which the data is written.
     */
    public StreamingTransformator(StreamRDF outStream, OutputStream out, File outputFile) {
        super();
        this.outStream = outStream;
        this.out = out;
        this.outputFile = outputFile;
    }

    /**
     * Add the given files to the target file. If the array contains directories,
     * all files and sub directories are added. Note that <b>all</b> files must be
     * RDF files.
     * 
     * <p>
     * Note: it is suggested to use {@link #addFile2Stream(File, String)} with an
     * explicit content type for each file.
     * </p>
     * 
     * @param files an array of RDF files that should be added to the output file
     * @throws IOException if an IO error occurs either during reading the given
     *                     files or writing the read data to the output file.
     */
    public void addFiles2Stream(File[] files) throws IOException {
        for (File file : files) {
            addFile2Stream(file);
        }
    }

    /**
     * Add the given file to the target file. If the given file is a directory, all
     * files and sub directories are added. Note that <b>all</b> files must be RDF
     * files.
     * 
     * <p>
     * Note: it is suggested to use {@link #addFile2Stream(File, String)} with an
     * explicit content type for each file.
     * </p>
     * 
     * @param file an RDF file or a directory with RDF files that should be added to
     *             the output file
     * @throws IOException if an IO error occurs either during reading the given
     *                     file(s) or writing the read data to the output file.
     */
    public void addFile2Stream(File file) throws IOException {
        if (file.isDirectory()) {
            addFiles2Stream(file.listFiles());
        } else {
            addFile2Stream(file, null);
        }
    }

    /**
     * Add the given file to the target file. The given content type is used to
     * guide the parsing of the file. If the given file is a directory, all files
     * and sub directories are added. Note that <b>all</b> files must be RDF files.
     * 
     * @param file        an RDF file or a directory with RDF files that should be
     *                    added to the output file
     * @param contentType the content type String expressing the RDF serialization
     *                    of the given file. The String is ignored if it is
     *                    {@code null}, which means that the identification of the
     *                    serialization is solely based on the file name.
     * @throws IOException if an IO error occurs either during reading the given
     *                     file(s) or writing the read data to the output file.
     */
    public void addFile2Stream(File file, @CheckForNull String contentType) throws IOException {
        if (file.isDirectory()) {
            addFile2Stream(file);
        }
        String fileURL = file.toURI().toURL().toString();
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            InputStream ins = in;
            if (fileURL.endsWith(".gz")) {
                ins = new GzipCompressorInputStream(ins);
                fileURL = fileURL.substring(0, fileURL.length() - 3);
            } else if (fileURL.endsWith(".bz2")) {
                ins = new BZip2CompressorInputStream(ins);
                fileURL = fileURL.substring(0, fileURL.length() - 4);
            }
            ContentType ct = WebContent.determineCT(contentType, null, fileURL);
            Lang lang = RDFLanguages.contentTypeToLang(ct);
            LOGGER.info("Adding file {} ...", file.toString());
            RDFParser.source(TypedInputStream.wrap(ins)).lang(lang).parse(outStream);
        }
    }

    /**
     * @return the outputFile
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * @param outputFile the outputFile to set
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void close() throws Exception {
        outStream.finish();
        out.close();
    }

}
