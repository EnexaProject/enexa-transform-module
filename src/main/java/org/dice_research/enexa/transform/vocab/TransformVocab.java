package org.dice_research.enexa.transform.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class TransformVocab {

    protected static final String uri = "http://w3id.org/dice-research/enexa/module/transform/";

    /**
     * returns the URI for this schema
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return uri;
    }

    protected static final Resource resource(String local) {
            Resource tmp = ResourceFactory.createResource(uri + local);
            return tmp;

    }

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

    // Properties sorted alphabetically
    public static final Property input = property("parameter/input");
    public static final Property outputMediaType = property("parameter/outputMediaType");
}
