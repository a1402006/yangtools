/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.codec.xml;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.SchemaUtils;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

/**
 * A {@link NormalizedNodeStreamWriter} which translates the events into an
 * {@link XMLStreamWriter}, resulting in a RFC 6020 XML encoding.
 */
public final class XMLStreamNormalizedNodeStreamWriter implements NormalizedNodeStreamWriter {
    private static final XmlStreamUtils UTILS = XmlStreamUtils.create(XmlUtils.DEFAULT_XML_CODEC_PROVIDER);

    private final Deque<Object> schemaStack = new ArrayDeque<>();
    private final XMLStreamWriter writer;
    private final DataNodeContainer root;

    private XMLStreamNormalizedNodeStreamWriter(final XMLStreamWriter writer, final SchemaContext context, final SchemaPath path) {
        this.writer = Preconditions.checkNotNull(writer);

        DataNodeContainer current = context;
        for (QName qname : path.getPathFromRoot()) {
            final DataSchemaNode child = current.getDataChildByName(qname);
            Preconditions.checkArgument(child instanceof DataNodeContainer);
            current = (DataNodeContainer) child;
        }

        this.root = current;
    }

    /**
     * Create a new writer with the specified context as its root.
     *
     * @param writer Output {@link XMLStreamWriter}
     * @param context Associated {@link SchemaContext}.
     * @return A new {@link NormalizedNodeStreamWriter}
     */
    public static NormalizedNodeStreamWriter create(final XMLStreamWriter writer, final SchemaContext context) {
        return create( writer, context, SchemaPath.ROOT);
    }

    /**
     * Create a new writer with the specified context and rooted in the specified schema path
     *
     * @param writer Output {@link XMLStreamWriter}
     * @param context Associated {@link SchemaContext}.
     *
     * @return A new {@link NormalizedNodeStreamWriter}
     */
    public static NormalizedNodeStreamWriter create(final XMLStreamWriter writer, final SchemaContext context, final SchemaPath path) {
        final Boolean repairing = (Boolean) writer.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
        Preconditions.checkArgument(repairing == true, "XML Stream Writer has to be repairing namespaces");
        return new XMLStreamNormalizedNodeStreamWriter(writer, context, path);
    }

    private final Object getParent() {
        if (schemaStack.isEmpty()) {
            return root;
        }
        return schemaStack.peek();
    }

    private final SchemaNode getSchema(final PathArgument name) {
        final Object parent = getParent();
        Preconditions.checkState(parent instanceof DataNodeContainer);

        final QName qname = name.getNodeType();
        final SchemaNode schema = ((DataNodeContainer)parent).getDataChildByName(qname);
        Preconditions.checkArgument(schema != null, "Could not find schema for node %s", qname);
        return schema;
    }

    private void push(final Object schema) {
        schemaStack.push(schema);
    }

    private void writeElement(final QName qname, final TypeDefinition<?> type, final Object value) throws IOException {
        final String ns = qname.getNamespace().toString();

        try {
            if (value != null) {
                writer.writeStartElement(ns, qname.getLocalName());
                UTILS.writeValue(writer, type, value);
                writer.writeEndElement();
            } else {
                writer.writeEmptyElement(ns, qname.getLocalName());
            }
        } catch (XMLStreamException e) {
            throw new IOException("Failed to emit element", e);
        }
    }

    private void startElement(final QName qname) throws IOException {
        try {
            writer.writeStartElement(qname.getNamespace().toString(), qname.getLocalName());
        } catch (XMLStreamException e) {
            throw new IOException("Failed to start element", e);
        }
    }

    private void startList(final NodeIdentifier name) {
        final SchemaNode schema = getSchema(name);
        Preconditions.checkArgument(schema instanceof ListSchemaNode, "Node %s is not a list", schema.getPath());
        push(schema);
    }

    private void startListItem(final PathArgument name) throws IOException {
        final Object schema = getParent();
        Preconditions.checkArgument(schema instanceof ListSchemaNode, "List item is not appropriate");
        startElement(name.getNodeType());
        push(schema);
    }

    @Override
    public void leafNode(final NodeIdentifier name, final Object value) throws IOException {
        final SchemaNode schema = getSchema(name);

        Preconditions.checkArgument(schema instanceof LeafSchemaNode, "Node %s is not a leaf", schema.getPath());
        final TypeDefinition<?> type = ((LeafSchemaNode) schema).getType();
        writeElement(schema.getQName(), type, value);
    }

    @Override
    public void startLeafSet(final NodeIdentifier name, final int childSizeHint) {
        final SchemaNode schema = getSchema(name);

        Preconditions.checkArgument(schema instanceof LeafListSchemaNode, "Node %s is not a leaf-list", schema.getPath());
        push(schema);
    }

    @Override
    public void leafSetEntryNode(final Object value) throws IOException {
        final Object parent = getParent();

        Preconditions.checkArgument(parent instanceof LeafListSchemaNode, "Not currently in a leaf-list");
        final LeafListSchemaNode schema = (LeafListSchemaNode) parent;
        writeElement(schema.getQName(), schema.getType(), value);
    }

    @Override
    public void startContainerNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        final SchemaNode schema = getSchema(name);
        final QName qname = schema.getQName();

        Preconditions.checkArgument(schema instanceof ContainerSchemaNode, "Node %s is not a container", schema.getPath());
        startElement(qname);
        push(schema);
    }

    @Override
    public void startUnkeyedList(final NodeIdentifier name, final int childSizeHint) {
        startList(name);
    }

    @Override
    public void startUnkeyedListItem(final NodeIdentifier name, final int childSizeHint) throws IOException {
        startListItem(name);
    }

    @Override
    public void startMapNode(final NodeIdentifier name, final int childSizeHint) {
        startList(name);
    }

    @Override
    public void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint) throws IOException {
        startListItem(identifier);
    }

    @Override
    public void startOrderedMapNode(final NodeIdentifier name, final int childSizeHint) {
        startList(name);
    }

    @Override
    public void startChoiceNode(final NodeIdentifier name, final int childSizeHint) {
        final SchemaNode schema = getSchema(name);

        Preconditions.checkArgument(schema instanceof ChoiceNode, "Node %s is not a choice", schema.getPath());
        push(schema);
    }

    @Override
    public void startAugmentationNode(final AugmentationIdentifier identifier) throws IOException {
        final Object parent = getParent();

        Preconditions.checkArgument(parent instanceof AugmentationTarget, "Augmentation not allowed under %s", parent);
        final AugmentationSchema schema = SchemaUtils.findSchemaForAugment((AugmentationTarget) parent, identifier.getPossibleChildNames());
        push(schema);
    }

    @Override
    public void anyxmlNode(final NodeIdentifier name, final Object value) throws IOException {
        final SchemaNode schema = getSchema(name);

        Preconditions.checkArgument(schema instanceof AnyXmlSchemaNode, "Node %s is not anyxml", schema.getPath());

        final QName qname = schema.getQName();
        final String ns = qname.getNamespace().toString();

        try {
            if (value != null) {
                writer.writeStartElement(ns, qname.getLocalName());
                UTILS.writeValue(writer, (Node<?>)value, schema);
                writer.writeEndElement();
            } else {
                writer.writeEmptyElement(ns, qname.getLocalName());
            }
        } catch (XMLStreamException e) {
            throw new IOException("Failed to emit element", e);
        }
    }

    @Override
    public void endNode() throws IOException {
        final Object schema = schemaStack.pop();

        try {
            if (schema instanceof ListSchemaNode) {
                // For lists, we only emit end element on the inner frame
                final Object parent = getParent();
                if (parent == schema) {
                    writer.writeEndElement();
                }
            } else if (schema instanceof ContainerSchemaNode) {
                // Emit container end element
                writer.writeEndElement();
            }
        } catch (XMLStreamException e) {
            throw new IOException("Failed to end element", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to close writer", e);
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            writer.flush();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to flush writer", e);
        }
    }
}
