/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License Version 1.0, which accompanies this
 * distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo.hosted;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.cargo.CargoFormat;
import org.sonatype.nexus.plugins.cargo.CargoRecipeSupport;
import org.sonatype.nexus.plugins.cargo.CargoRegistryFacet;
import org.sonatype.nexus.plugins.cargo.GitRepositoryFacet;
import org.sonatype.nexus.plugins.cargo.registry.v1.CargoRegistryV1Handlers;
import org.sonatype.nexus.plugins.cargo.security.CargoRegistryBearerTokenHandlers;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.http.HttpHandlers;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.matchers.ActionMatcher;
import org.sonatype.nexus.repository.view.matchers.LiteralMatcher;
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

@Named(CargoHostedRecipe.NAME)
@Singleton
class CargoHostedRecipe
        extends CargoRecipeSupport
{
    public static final String NAME = "cargo-hosted";

    @Inject
    protected Provider<CargoRegistryFacet> crateStorageFacet;

    @Inject
    protected Provider<GitRepositoryFacet> gitRepositoryFacet;

    @Inject
    protected CargoRegistryBearerTokenHandlers.Get tokenGetHandler;

    @Inject
    protected CargoRegistryBearerTokenHandlers.Delete tokenDeleteHandler;

    @Inject
    protected CargoRegistryV1Handlers.Publish cratePublishHandler;

    @Inject
    protected CargoRegistryV1Handlers.CrateDownload crateDownloadHandler;

    @Inject
    protected CargoRegistryV1Handlers.MetadataDownload metadataDownloadHandler;

    @Inject
    protected CargoRegistryV1Handlers.IndexInfoRefs gitInfoRefsHandler;

    @Inject
    protected CargoRegistryV1Handlers.IndexRecievePackService gitReceivePackHandler;

    @Inject
    protected CargoRegistryV1Handlers.IndexUploadPackService gitUploadPackHandler;

    @Inject
    CargoHostedRecipe(@Named(HostedType.NAME) final Type type, @Named(CargoFormat.NAME) final Format format) {
        super(type, format);
    }

    @Override
    public void apply(Repository repository) throws Exception {
        repository.attach(securityFacet.get());
        repository.attach(configure(viewFacet.get()));
        repository.attach(storageFacet.get());
        repository.attach(searchFacet.get());
        repository.attach(attributesFacet.get());
        repository.attach(componentMaintenanceFacet.get());
        repository.attach(crateStorageFacet.get());
        repository.attach(gitRepositoryFacet.get());
    }

    private ViewFacet configure(final ConfigurableViewFacet facet) {
        Router.Builder builder = new Router.Builder();

        // Git HTTP protocol reference discovery
        builder.route(new Route.Builder().matcher(new TokenMatcher("/{repo_name:index}/info/refs"))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(contentHeadersHandler).handler(unitOfWorkHandler)
                .handler(gitInfoRefsHandler).create());

        // Git client fetching objects over Smart HTTP protocol
        builder.route(new Route.Builder().matcher(new TokenMatcher("/{repo_name:index}/git-upload-pack"))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(contentHeadersHandler).handler(unitOfWorkHandler)
                .handler(gitUploadPackHandler).create());

        // Git client pushing objects over Smart HTTP protocol
        builder.route(new Route.Builder().matcher(new TokenMatcher("/{repo_name:index}/git-receive-pack"))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(contentHeadersHandler).handler(unitOfWorkHandler)
                .handler(gitUploadPackHandler).create());

        // Crates.io API v1
        builder.route(new Route.Builder()
                .matcher(LogicMatchers.and(new ActionMatcher(HttpMethods.GET), new LiteralMatcher("/me")))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(partialFetchHandler).handler(contentHeadersHandler)
                .handler(unitOfWorkHandler).handler(tokenGetHandler).create());

        builder.route(new Route.Builder()
                .matcher(LogicMatchers.and(new ActionMatcher(HttpMethods.GET), new LiteralMatcher("/token")))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(partialFetchHandler).handler(contentHeadersHandler)
                .handler(unitOfWorkHandler).handler(tokenGetHandler).create());

        builder.route(new Route.Builder()
                .matcher(LogicMatchers.and(new ActionMatcher(HttpMethods.DELETE), new LiteralMatcher("/token")))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(partialFetchHandler).handler(contentHeadersHandler)
                .handler(unitOfWorkHandler).handler(tokenDeleteHandler).create());

        builder.route(new Route.Builder()
                .matcher(
                        LogicMatchers.and(new ActionMatcher(HttpMethods.PUT), new LiteralMatcher("/api/v1/crates/new")))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(partialFetchHandler).handler(contentHeadersHandler)
                .handler(unitOfWorkHandler).handler(cratePublishHandler).create());

        builder.route(new Route.Builder()
                .matcher(LogicMatchers.and(new ActionMatcher(HttpMethods.HEAD, HttpMethods.GET),
                        new TokenMatcher("/api/v1/crates/{name:.+}/{version:.+}/download")))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(partialFetchHandler).handler(contentHeadersHandler)
                .handler(unitOfWorkHandler).handler(crateDownloadHandler).create());

        builder.route(new Route.Builder()
                .matcher(LogicMatchers.and(new ActionMatcher(HttpMethods.HEAD, HttpMethods.GET),
                        new TokenMatcher("/{name:.+}-{version:.+}.crate")))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(partialFetchHandler).handler(contentHeadersHandler)
                .handler(unitOfWorkHandler).handler(crateDownloadHandler).create());

        builder.route(new Route.Builder()
                .matcher(LogicMatchers.and(new ActionMatcher(HttpMethods.HEAD, HttpMethods.GET),
                        new TokenMatcher("/{name:.+}-{version:.+}.json")))
                .handler(timingHandler).handler(securityHandler).handler(exceptionHandler)
                .handler(conditionalRequestHandler).handler(partialFetchHandler).handler(contentHeadersHandler)
                .handler(unitOfWorkHandler).handler(metadataDownloadHandler).create());

        builder.defaultHandlers(HttpHandlers.notFound());
        facet.configure(builder.create());
        return facet;
    }

}
