package com.invoiceme.common.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Custom JSON Type for PostgreSQL JSONB columns
 * Extends Hypersistence JsonBinaryType for JSONB support
 */
public class JsonType extends JsonBinaryType {

    public static final JsonType INSTANCE = new JsonType();

    public JsonType() {
        super(new ObjectMapper());
    }

    public JsonType(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    public JsonType(org.hibernate.type.spi.TypeBootstrapContext typeBootstrapContext) {
        super(typeBootstrapContext);
    }
}
