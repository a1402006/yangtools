/*
 * Copyright (c) 2016 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yangtools.yang.data.util;

import com.google.common.base.Preconditions;
import java.net.URI;
import javax.annotation.Nonnull;

import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.codec.IdentityrefCodec;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class ModuleStringIdentityrefCodec
        extends AbstractModuleStringIdentityrefCodec
        implements IdentityrefCodec<String> {
    protected final SchemaContext context;
    protected final QNameModule parentModuleQname;

    public ModuleStringIdentityrefCodec(@Nonnull final SchemaContext context, @Nonnull final QNameModule parentModule) {
        this.context = Preconditions.checkNotNull(context);
        this.parentModuleQname = Preconditions.checkNotNull(parentModule);
    }

    @Override
    protected String prefixForNamespace(final URI namespace) {
        final Module module = context.findModuleByNamespaceAndRevision(namespace, null);
        return module == null ? null : module.getName();
    }
}
