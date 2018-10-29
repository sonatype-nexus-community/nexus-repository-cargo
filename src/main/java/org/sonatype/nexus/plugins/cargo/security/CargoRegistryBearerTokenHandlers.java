
package org.sonatype.nexus.plugins.cargo.security;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.entity.ContentType;
import org.apache.shiro.authz.AuthorizationException;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.security.SecurityHelper;

public class CargoRegistryBearerTokenHandlers {
    @Named
    public static class Get implements Handler {
        private final SecurityHelper securityHelper;
        private final CargoRegistryBearerTokenManager tokenManager;

        @Inject
        Get(CargoRegistryBearerTokenManager tokenManager, SecurityHelper securityHelper) {
            this.securityHelper = securityHelper;
            this.tokenManager = tokenManager;
        }

        @Override
        public Response handle(Context context) throws Exception {
            if (!this.securityHelper.subject().isAuthenticated()) {
                throw new AuthorizationException("Must be authenticated to obtain an API token");
            }

            // Clients should provide a JSON request that includes a name for
            // the new token. As Nexus BearerTokens only allow one token per
            // format, just ignore the requested name and return the "Default
            // Token".

            String token = this.tokenManager.getTokenForCurrentUser();
            Payload payload = new StringPayload("Bearer " + token,
                    StandardCharsets.UTF_8, ContentType.TEXT_PLAIN.getMimeType());
            return HttpResponses.ok(payload);
        }
    }

    @Named
    public static class Delete implements Handler {
        private final CargoRegistryBearerTokenManager tokenManager;

        @Inject
        Delete(CargoRegistryBearerTokenManager tokenManager) {
            this.tokenManager = tokenManager;
        }

        @Override
        public Response handle(Context context) throws Exception {
            this.tokenManager.deleteToken();
            return HttpResponses.ok(new Gson().toJson(new JsonObject()));
        }
    }
}
