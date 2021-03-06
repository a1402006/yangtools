/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yangtools.yang.stmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.TypeUtils;

public class TypedefConstraintsTest {

    @Test
    public void decimalRangeConstraintsTest() throws SourceException,
            FileNotFoundException, ReactorException, URISyntaxException {
        final SchemaContext context = StmtTestUtils
                .parseYangSources("/stmt-test/constraints");

        assertNotNull(context);

        final Set<TypeDefinition<?>> typeDefinitions = context.getTypeDefinitions();
        assertNotNull(typeDefinitions);
        assertEquals(1, typeDefinitions.size());

        final TypeDefinition<?> myDecimal = typeDefinitions.iterator().next();

        assertNotNull(myDecimal);
        assertTrue(myDecimal instanceof DecimalTypeDefinition);

        final List<RangeConstraint> rangeConstraints = ((DecimalTypeDefinition) myDecimal)
                .getRangeConstraints();

        assertNotNull(rangeConstraints);
        assertEquals(1, rangeConstraints.size());

        final DataSchemaNode dataNode = context.getDataChildByName(QName.create("urn:opendaylight.foo", "2013-10-08", "id-decimal64"));
        assertNotNull(dataNode);
        assertTrue(dataNode instanceof LeafSchemaNode);

        final LeafSchemaNode leafDecimal = (LeafSchemaNode) dataNode;
        final TypeDefinition<?> type = leafDecimal.getType();

        assertTrue(type instanceof DecimalTypeDefinition);
        final DecimalTypeDefinition decType = (DecimalTypeDefinition) type;

        final List<RangeConstraint> decRangeConstraints = decType.getRangeConstraints();

        assertEquals(1, decRangeConstraints.size());

        final RangeConstraint range = decRangeConstraints.iterator().next();
        assertEquals(new BigDecimal(1.5), range.getMin());
        assertEquals(new BigDecimal(5.5), range.getMax());

        assertTrue(decType.getQName().getModule().equals(leafDecimal.getQName().getModule()));
        assertTrue(decType.getQName().getLocalName().equals(TypeUtils.DECIMAL64));
        assertNull(decType.getBaseType());
    }

}
