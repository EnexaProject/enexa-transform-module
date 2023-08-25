package org.dice_research.enexa.transform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDF2OntologyTransformator implements Transformator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDF2OntologyTransformator.class);

    protected OWLOntology ontology;
    protected OWLOntologyManager manager;
    protected OWLDocumentFormat outputFormat;
    protected File outputFile;
    protected OutputStream outputStream;

    protected RDF2OntologyTransformator(OWLOntology ontology, OWLOntologyManager manager,
            OWLDocumentFormat outputFormat, File outputFile, OutputStream outputStream) {
        super();
        this.ontology = ontology;
        this.manager = manager;
        this.outputFormat = outputFormat;
        this.outputFile = outputFile;
        this.outputStream = outputStream;
    }

    public static OWLDocumentFormat getFormatForMediaType(String mediaType) {
        switch (mediaType) {
        case "application/owl+xml":
            return new OWLXMLDocumentFormat();
        case "text/owl-manchester":
            return new ManchesterSyntaxDocumentFormat();
        default:
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        try {
            if (ontology != null) {
                ontology.saveOntology(new OWLXMLDocumentFormat(), outputStream);
            }
        } catch (OWLOntologyStorageException e) {
            throw new IOException("Error while writing the ontology.", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    @Override
    public void addFile2Stream(File file, String contentType) throws IOException {
        try {
            String fileName = file.getName();
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                InputStream ins = in;
                if (fileName.endsWith(".gz")) {
                    ins = new GzipCompressorInputStream(ins);
                } else if (fileName.endsWith(".bz2")) {
                    ins = new BZip2CompressorInputStream(ins);
                }

                LOGGER.info("Adding file {} ...", file.toString());
                OWLOntology readOnt = manager.loadOntologyFromOntologyDocument(new StreamDocumentSource(ins));
                manager.addAxioms(ontology, readOnt.getAxioms());
            }
        } catch (OWLOntologyCreationException e) {
            throw new IOException(
                    "Error while reading the ontology \"" + (file == null ? "null" : file.toString()) + "\".", e);
        }
    }

    public static RDF2OntologyTransformator create(OWLDocumentFormat outputFormat, File outputFile,
            OutputStream outputStream) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.createOntology(IRI.create(outputFile));
        return new RDF2OntologyTransformator(ontology, manager, outputFormat, outputFile, outputStream);
    }

    public String getOutputFileName() {
        return outputFile.getName();
    }

    public File getOutputFile() {
        return outputFile;
    }
}
