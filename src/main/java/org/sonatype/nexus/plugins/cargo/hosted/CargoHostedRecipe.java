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
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.cargo.CargoFormat;
import org.sonatype.nexus.plugins.cargo.CargoRecipeSupport;
import org.sonatype.nexus.plugins.cargo.git.GitRepositoryHandlers;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.http.HttpHandlers;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.matchers.ActionMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

@Named(CargoHostedRecipe.NAME)
@Singleton
class CargoHostedRecipe
        extends CargoRecipeSupport
{
    public static final String NAME = "cargo-hosted";

    @Inject
    protected GitRepositoryHandlers.Default gitDefaultHandler;

    @Inject
    protected GitRepositoryHandlers.InfoRefs gitInfoRefsHandler;

    @Inject
    protected GitRepositoryHandlers.ReceivePackService gitReceivePackHandler;

    @Inject
    protected GitRepositoryHandlers.UploadPackService gitUploadPackHandler;

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

        // 404 anything else inside the index Git repo
        builder.route(new Route.Builder().matcher(new TokenMatcher("/{repo_name:index}/.*")).handler(timingHandler)
                .handler(securityHandler).handler(exceptionHandler).handler(conditionalRequestHandler)
                .handler(contentHeadersHandler).handler(unitOfWorkHandler).handler(gitDefaultHandler).create());

        // Crates.io API v1
        builder.route(new Route.Builder().matcher(new ActionMatcher(HttpMethods.GET)).handler(timingHandler)
                .handler(securityHandler).handler(exceptionHandler).handler(conditionalRequestHandler)
                .handler(partialFetchHandler).handler(contentHeadersHandler).handler(unitOfWorkHandler)
                .handler(new Handler()
                {

                    @Override
                    public Response handle(Context context) throws Exception {
                        return HttpResponses.notImplemented(context.toString());
                    }
                }).create());

        builder.defaultHandlers(HttpHandlers.notFound());
        facet.configure(builder.create());
        return facet;
    }

}
