
package org.sonatype.nexus.plugins.cargo.hosted;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.cargo.CargoFormat;
import org.sonatype.nexus.plugins.cargo.CargoRecipeSupport;
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

@Named(CargoHostedRecipe.NAME)
@Singleton
class CargoHostedRecipe extends CargoRecipeSupport {
    public static final String NAME = "cargo-hosted";

    @Inject
    CargoHostedRecipe(@Named(HostedType.NAME) final Type type,
            @Named(CargoFormat.NAME) final Format format) {
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

        builder.route(new Route.Builder()
                .matcher(new ActionMatcher(HttpMethods.GET))
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(exceptionHandler)
                .handler(conditionalRequestHandler)
                .handler(partialFetchHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(new Handler() {

                    @Override
                    public Response handle(Context context) throws Exception {
                        return HttpResponses.notImplemented(context.toString());
                    }
                })
                .create());

        builder.defaultHandlers(HttpHandlers.notFound());
        facet.configure(builder.create());
        return facet;
    }

}
