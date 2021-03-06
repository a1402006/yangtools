/*
 * Copyright (c) 2016 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.codec.gson;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.data.util.AbstractStringUnionCodec;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;

final class JSONStringUnionCodec extends AbstractStringUnionCodec implements JSONCodec<Object> {
    private final JSONCodecFactory codecFactory;

    JSONStringUnionCodec(final DataSchemaNode schema, final UnionTypeDefinition typeDefinition,
                         final JSONCodecFactory codecFactory) {
        super(schema, typeDefinition);
        this.codecFactory = Preconditions.checkNotNull(codecFactory);
    }

    @Override
    public void serializeToWriter(JsonWriter writer, Object value) throws IOException {
        writer.value(serialize(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Codec<String, Object> codecFor(final TypeDefinition<?> type) {
        return (Codec<String, Object>) codecFactory.codecFor(schema, type);
    }
}
