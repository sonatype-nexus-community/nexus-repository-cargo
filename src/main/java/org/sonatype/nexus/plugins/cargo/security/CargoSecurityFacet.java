/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo.security;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.SecurityFacetSupport;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.security.BreadActions;

/**
 * Cargo format security facet.
 */
@Named
public class CargoSecurityFacet
        extends SecurityFacetSupport
{
    @Inject
    public CargoSecurityFacet(final CargoFormatSecurityContributor securityResource,
                              @Named("simple") final VariableResolverAdapter variableResolverAdapter,
                              final ContentPermissionChecker contentPermissionChecker)
    {
        super(securityResource, variableResolverAdapter, contentPermissionChecker);
    }

    // This needs to exist so that Configuration is imported correctly. The
    // bundle will fail to load due to Guice being unable to find the class otherwise.
    @Override
    protected void doValidate(Configuration configuration) throws Exception {
        super.doValidate(configuration);
    }

    @Override
    protected String action(Request request) {
        if (request.getAction().equals(HttpMethods.POST) &&
                request.getPath().equals("/index/git-upload-pack")) {
            // Git Smart HTTP protocol uses a POST request for reading the repo so
            // the client can set Git pack-format messages to indicate what objects
            // it wants.
            return BreadActions.READ;
        } else if (request.getAction().equals(HttpMethods.POST) &&
                request.getPath().equals("/index/git-receive-pack")) {
            // Normally, the index will only be written to by Nexus. If a user
            // has edit permissions, let them commit arbitrary things to the
            // index anyway.
            return BreadActions.EDIT;
        } else {
            return super.action(request);
        }
    }
}
