package org.dice_research.enexa.transform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.WebContent;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.enexa.transform.vocab.TransformVocab;
import org.dice_research.enexa.utils.EnexaPathUtils;
import org.dice_research.enexa.vocab.ENEXA;
import org.dice_research.enexa.vocab.IANAMediaType;
import org.dice_research.rdf.RdfHelper;
import org.dice_research.sparql.SparqlQueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper of the {@link StreamingTransformator} class that makes it an ENEXA
 * module.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class EnexaTransformator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnexaTransformator.class);

    protected Resource experiment;
    protected String endpoint;
    protected String metaGraph;
    protected Resource moduleInstance;
    protected String sharedDir;
    protected String outputDir;
    protected String enexaServiceUrl;
    protected Model parameterModel;
    protected List<Resource> sourceFiles;
    protected Resource targetMediaResource;
    protected File outputFile;
    protected String enexaFileLocation;

    public EnexaTransformator() throws IllegalStateException {
        experiment = ResourceFactory.createResource(getEnvVariable("ENEXA_EXPERIMENT_IRI"));
        endpoint = getEnvVariable("ENEXA_META_DATA_ENDPOINT");
        metaGraph = getEnvVariable("ENEXA_META_DATA_GRAPH");
        moduleInstance = ResourceFactory.createResource(getEnvVariable("ENEXA_MODULE_INSTANCE_IRI"));
        sharedDir = getEnvVariable("ENEXA_SHARED_DIRECTORY");
        if (!sharedDir.endsWith(File.separator)) {
            sharedDir += File.separator;
        }
        outputDir = getEnvVariable("ENEXA_MODULE_INSTANCE_DIRECTORY");
        enexaServiceUrl = getEnvVariable("ENEXA_SERVICE_URL");
        if (!enexaServiceUrl.endsWith("/")) {
            enexaServiceUrl += "/";
        }
    }

    protected void requestParameters() throws IllegalArgumentException {
        parameterModel = queryParameterModel(endpoint, metaGraph, moduleInstance.getURI());
        LOGGER.debug("Parameter Model: {}", parameterModel);
        sourceFiles = RdfHelper.getObjectResources(parameterModel, moduleInstance, TransformVocab.input);
        if (sourceFiles.size() == 0) {
            throw new IllegalArgumentException(
                    "No input files defined (parameter IRI:" + TransformVocab.input.getURI() + ").");
        }
        targetMediaResource = RdfHelper.getObjectResource(parameterModel, moduleInstance,
                TransformVocab.outputMediaType);
        if (targetMediaResource == null) {
            throw new IllegalArgumentException("The output media type has not been defined (parameter IRI:"
                    + TransformVocab.outputMediaType.getURI() + ").");
        }
        if (!targetMediaResource.isURIResource()) {
            throw new IllegalArgumentException(
                    "The output media type is not an IRI (" + targetMediaResource.toString() + ").");
        }
    }

    protected void executeTransformation() throws Exception {
        try (Transformator transformator = new TransformatorBuilder().setOutputFormat(targetMediaResource.getURI())
                // .setCompression(compression)
                // .setOutputFileName(outputFile.getName())
                .setOutputDirectory(new File(outputDir)).build();) {
            for (Resource sourceFile : sourceFiles) {
                addFile(sourceFile, parameterModel, sharedDir, transformator);
            }
            // update output file after writing
            outputFile = transformator.getOutputFile();
        }
        enexaFileLocation = EnexaPathUtils.translateLocal2EnexaPath(outputFile, sharedDir);
    }

    protected void sendFileMetadata() throws IOException {
        Model metadata = ModelFactory.createDefaultModel();
        Resource fileResource = metadata.createResource();
        metadata.add(fileResource, RDF.type, metadata.createResource("http://www.w3.org/ns/prov#Entity"));
        metadata.add(fileResource, ENEXA.experiment, experiment);
        metadata.add(fileResource, ENEXA.location, enexaFileLocation);
        metadata.add(fileResource, metadata.createProperty("http://www.w3.org/ns/prov#wasGeneratedBy"), moduleInstance);
        metadata.add(fileResource, DCAT.mediaType, targetMediaResource);
        try {
            metadata.addLiteral(fileResource, DCAT.byteSize, outputFile.length());
        } catch (SecurityException e) {
            LOGGER.warn("Couldn't determine the size of " + outputFile.toString(), e);
        }
        // Add the direct connection that the generated file is the output of this
        // module instance
        metadata.add(moduleInstance, TransformVocab.output, fileResource);

        if (sendRequest(enexaServiceUrl + "add-resource", metadata) != null) {
            LOGGER.info("This module seems to have been successful.");
        }
    }

    public static void main(String[] args) {
        // 1. init
        EnexaTransformator module = null;
        try {
            module = new EnexaTransformator();
        } catch (Exception e) {
            LOGGER.error("Error during initialization. Aborting.", e);
            System.exit(-1);
        }
        // 2. get parameters from SPARQL endpoint
        try {
            module.requestParameters();
        } catch (Exception e) {
            LOGGER.error("Received invalid parameters. Aborting.", e);
            System.exit(-1);
        }
        // 3. create transformer
        try {
            module.executeTransformation();
        } catch (Exception e) {
            LOGGER.error("Got an exception while transforming files. Aborting.", e);
            System.exit(-1);
        }
        // 4. write file metadata
        try {
            module.sendFileMetadata();
        } catch (Exception e) {
            LOGGER.error("Exception while sending meta data of generated file. Aborting.", e);
            System.exit(-1);
        }
    }

    /**
     * Returns the {@link Lang} instance for the output file, or {@code null} if the
     * given media type is not known or the language is not supported for being
     * streamed.
     * 
     * @param targetMediaResource
     * @return
     */
    @SuppressWarnings("unused")
    @Deprecated
    private static Lang getOutputLang(Resource targetMediaResource) {
        Lang outputLang = IANAMediaType.resource2Lang(targetMediaResource);
        // If this is one of the languages we can handle...
        if (Lang.NTRIPLES.equals(outputLang) || Lang.TURTLE.equals(outputLang) || Lang.NQUADS.equals(outputLang)
                || Lang.TRIG.equals(outputLang)) {
            return outputLang;
        }
        throw new IllegalArgumentException("Got an unsupported media type \"" + targetMediaResource.toString() + "\".");
    }

    /**
     * A simple helper method that returns the value of the environmental variable
     * with the given key or dies horribly with an {@link IllegalStateException}.
     * 
     * @param key the variable name for which the value should be retrieved
     * @return the value of the given variable
     * @throws IllegalStateException if the key is not known
     */
    private static String getEnvVariable(String key) throws IllegalStateException {
        String value = System.getenv(key);
        if (value == null) {
            String msg = "Couldn't get value of variable " + key + ". The environment is not correctly set up.";
            LOGGER.error(msg);
            throw new IllegalStateException(msg);
        } else {
            return value;
        }
    }

    /**
     * Adds the given source file to the given transformator, which should add it ot
     * the output file.
     * 
     * @param sourceFile     the resource representing the source file in the meta
     *                       data graph
     * @param parameterModel the part of the meta data graph that has been retrieved
     *                       and should contain additional information about the
     *                       given source file
     * @param sharedDir      the local path of the shared directory
     * @param transformator  the transformator to which the given file should be
     *                       added
     * @throws IOException in case of an IO error while reading the file
     */
    protected static void addFile(Resource sourceFile, Model parameterModel, String sharedDir,
            Transformator transformator) throws IOException {
        String enexaPath = RdfHelper.getStringValue(parameterModel, sourceFile, ENEXA.location);
        Resource mediaTypeResource = RdfHelper.getObjectResource(parameterModel, sourceFile, DCAT.mediaType);
        transformator.addFile2Stream(new File(EnexaPathUtils.translateEnexa2LocalPath(enexaPath, sharedDir)),
                IANAMediaType.resource2ContentType(mediaTypeResource));
    }

    /**
     * This method queries the parameters from the meta data graph in the given meta
     * data endpoint.
     * 
     * @param metaDataEndpoint the SPARQL endpoint hosting the meta data graph
     * @param metaDataGraph    the graph containing the meta data
     * @param moduleInstance   the IRI of the ENEXA module instance
     * @return an RDF model containing the queried sub graph
     * @throws IllegalStateException in case an error occurs while querying the
     *                               model
     */
    protected static Model queryParameterModel(String metaDataEndpoint, String metaDataGraph, String moduleInstance) {
        ParameterizedSparqlString query = null;
        try (QueryExecutionFactory queryExecFactory = new QueryExecutionFactoryHttp(metaDataEndpoint)) {
            query = SparqlQueryUtils.loadParameterizedQuery(EnexaTransformator.class.getClassLoader(),
                    "org/dice_research/enexa/transform/getParameters.query", StandardCharsets.UTF_8);
            query.setIri("?moduleInstance", moduleInstance);
            query.setIri("?graph", metaDataGraph);
            LOGGER.debug("SPARQL query for getting paramters: {}" + query.toString());
            try (QueryExecution qe = queryExecFactory.createQueryExecution(query.asQuery());) {
                return qe.execConstruct();
            }
        } catch (Exception e) {
            String msg = "Error while requesting parameter values. query: "
                    + ((query == null) ? "no query generated" : query.toString());
            LOGGER.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * This method sends an HTTP POST request to the given URL appending the given
     * RDF data serialized using JSON-LD.
     * 
     * @param url  the URL to which the data should be sent
     * @param data the RDF data that should be sent within the request body
     * @return the RDF model that has been received as response
     * @throws IOException
     */
    protected static Model sendRequest(String url, Model data) throws IOException {
        HttpPost request = new HttpPost(url);
        request.addHeader(HttpHeaders.ACCEPT, WebContent.contentTypeJSONLD);
        if (data != null) {
            try (StringWriter writer = new StringWriter()) {
                request.addHeader(HttpHeaders.CONTENT_TYPE, WebContent.contentTypeJSONLD);
                RDFDataMgr.write(writer, data, Lang.JSONLD);
                request.setEntity(new StringEntity(writer.toString()));
            }
        }
        try (CloseableHttpClient client = HttpClients.createDefault();) {
            final Result result = client.execute(request, response -> {
                Model model = null;
                if (response.getCode() >= 200 && response.getCode() < 300) {
                    try (InputStream is = response.getEntity().getContent()) {
                        model = ModelFactory.createDefaultModel();
                        if (is.available() > 0) {
                            RDFDataMgr.read(model, is, "", Lang.JSONLD);
                        } else {
                            LOGGER.warn("Answer of ENEXA service is empty. Maybe this is the test environment?");
                        }
                    }
                } else {
                    LOGGER.error("Wrong response status while sending \"{}\": {} ", url, new StatusLine(response));
                }
                return new Result(response.getCode(), model);
            });
            return result.getContent();
        }
    }

    /**
     * A simple helper class to get the status code and RDF content from the HTTP
     * response.
     * 
     * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
     *
     */
    public static class Result {
        final protected int status;
        final protected Model content;

        public Result(int status, Model content) {
            super();
            this.status = status;
            this.content = content;
        }

        /**
         * @return the status
         */
        public int getStatus() {
            return status;
        }

        /**
         * @return the content
         */
        public Model getContent() {
            return content;
        }

    }
}
