/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo;

import javax.inject.Inject;
import javax.inject.Provider;

import org.sonatype.nexus.plugins.cargo.security.CargoSecurityFacet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.handlers.BrowseUnsupportedHandler;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;

/**
 * Support for Cargo recipes.
 */
public abstract class CargoRecipeSupport
        extends RecipeSupport
{
    @Inject
    protected Provider<CargoSecurityFacet> securityFacet;

    @Inject
    protected Provider<ConfigurableViewFacet> viewFacet;

    @Inject
    protected Provider<StorageFacet> storageFacet;

    @Inject
    protected Provider<SearchFacet> searchFacet;

    @Inject
    protected Provider<AttributesFacet> attributesFacet;

    @Inject
    protected Provider<DefaultComponentMaintenanceImpl> componentMaintenanceFacet;

    @Inject
    protected Provider<HttpClientFacet> httpClientFacet;

    @Inject
    protected Provider<PurgeUnusedFacet> purgeUnusedFacet;

    @Inject
    protected Provider<NegativeCacheFacet> negativeCacheFacet;

    @Inject
    protected ExceptionHandler exceptionHandler;

    @Inject
    protected TimingHandler timingHandler;

    @Inject
    protected SecurityHandler securityHandler;

    @Inject
    protected PartialFetchHandler partialFetchHandler;

    @Inject
    protected ConditionalRequestHandler conditionalRequestHandler;

    @Inject
    protected ContentHeadersHandler contentHeadersHandler;

    @Inject
    protected UnitOfWorkHandler unitOfWorkHandler;

    @Inject
    protected BrowseUnsupportedHandler browseUnsupportedHandler;

    @Inject
    protected HandlerContributor handlerContributor;

    @Inject
    protected NegativeCacheHandler negativeCacheHandler;

    protected CargoRecipeSupport(final Type type, final Format format) {
        super(type, format);
    }
}
