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

package org.sonatype.nexus.plugins.cargo.git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpHeaders;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PacketLineOut;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RefAdvertiser;
import org.eclipse.jgit.transport.UploadPack;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.plugins.cargo.GitRepositoryFacet;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.ActionMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.User;

public class GitRepositoryHandlers
{
    /** Name of the git-upload-pack service. */
    public static final String UPLOAD_PACK = "git-upload-pack";

    /** Content type supplied by the client to the /info/refs?service=git-upload-pack handler. */
    public static final String UPLOAD_PACK_ADVERTISEMENT_TYPE = "application/x-git-upload-pack-advertisement";

    /** Content type supplied by the client to the /git-upload-pack handler. */
    public static final String UPLOAD_PACK_REQUEST_TYPE = "application/x-git-upload-pack-request";

    /** Content type returned from the /git-upload-pack handler. */
    public static final String UPLOAD_PACK_RESULT_TYPE = "application/x-git-upload-pack-result";

    /** Name of the git-receive-pack service. */
    public static final String RECEIVE_PACK = "git-receive-pack";

    /** Content type supplied by the client to the /info/refs?service=git-receive-pack handler. */
    public static final String RECEIVE_PACK_ADVERTISEMENT_TYPE = "application/x-git-receive-pack-advertisement";

    /** Content type supplied by the client to the /git-receive-pack handler. */
    public static final String RECEIVE_PACK_REQUEST_TYPE = "application/x-git-receive-pack-request";

    /** Content type returned from the /git-receive-pack handler. */
    public static final String RECEIVE_PACK_RESULT_TYPE = "application/x-git-receive-pack-result";

    private final static String[] GET_REQUEST_METHODS = {HttpMethods.GET, HttpMethods.HEAD};

    private final static String[] PUT_REQUEST_METHODS = {HttpMethods.POST};

    private static abstract class GitHandlerSupport
            extends ComponentSupport
            implements Handler
    {
        protected Repository getGitRepository(Context context) throws IOException {
            TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
            String repo_name = state.getTokens().get("repo_name");

            GitRepositoryFacet facet = context.getRepository().facet(GitRepositoryFacet.class);
            return facet.getGitRepository(repo_name);
        }

        protected static Response setUncachedResponse(Response response) {
            return new Response.Builder().copy(response).header(HttpHeaders.EXPIRES, "Fri, 01 Jan 1980 00:00:00 GMT")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, max-age=0, must-revalidate").build();
        }

        protected static Response buildBadRequestResponse(String[] allowed_methods) {
            return setUncachedResponse(new Response.Builder().copy(HttpResponses.badRequest())
                    .header(HttpHeaders.ALLOW, String.join(", ", allowed_methods)).build());
        };
    }

    @Named
    public static class Default
            extends GitHandlerSupport
    {
        @Override
        public Response handle(Context context) throws Exception {
            return HttpResponses.notFound();
        }
    };

    @Named
    public static class InfoRefs
            extends GitHandlerSupport
    {
        @Override
        public Response handle(Context context) throws Exception {
            if (!new ActionMatcher(GET_REQUEST_METHODS).matches(context)) {
                return buildBadRequestResponse(GET_REQUEST_METHODS);
            }

            String requested_service = context.getRequest().getParameters().get("service");
            if (requested_service == null) {
                return HttpResponses.forbidden("git over dump-http is not supported");
            }
            else if (requested_service.equalsIgnoreCase(UPLOAD_PACK)) {
                return handleSmartUploadPackService(context);
            }
            else if (requested_service.equalsIgnoreCase(RECEIVE_PACK)) {
                return handleSmartReceivePackService(context);
            }
            else {
                return HttpResponses.forbidden("unknown service");
            }
        }

        @TransactionalTouchMetadata
        public Response handleSmartUploadPackService(Context context) throws Exception {
            ByteArrayOutputStream out_bytes = new ByteArrayOutputStream();
            final PacketLineOut packet_line_out = new PacketLineOut(out_bytes);

            packet_line_out.writeString("# service=" + UPLOAD_PACK + "\n");
            packet_line_out.end();

            Repository gitRepo = getGitRepository(context);
            if (gitRepo == null)
                return HttpResponses.notFound("unknown repository");

            UploadPack service = new UploadPack(gitRepo);
            service.setBiDirectionalPipe(false);
            service.sendAdvertisedRefs(new RefAdvertiser.PacketLineOutRefAdvertiser(packet_line_out));

            Payload payload = new BytesPayload(out_bytes.toByteArray(), UPLOAD_PACK_ADVERTISEMENT_TYPE);
            return HttpResponses.ok(payload);
        }

        @TransactionalTouchMetadata
        public Response handleSmartReceivePackService(Context context) throws Exception {
            ByteArrayOutputStream out_bytes = new ByteArrayOutputStream();
            final PacketLineOut packet_line_out = new PacketLineOut(out_bytes);

            packet_line_out.writeString("# service=" + RECEIVE_PACK + "\n");
            packet_line_out.end();

            Repository gitRepo = getGitRepository(context);
            if (gitRepo == null)
                return HttpResponses.notFound("unknown repository");

            ReceivePack service = new ReceivePack(gitRepo);
            service.setBiDirectionalPipe(false);
            service.sendAdvertisedRefs(new RefAdvertiser.PacketLineOutRefAdvertiser(packet_line_out));

            Payload payload = new BytesPayload(out_bytes.toByteArray(), RECEIVE_PACK_ADVERTISEMENT_TYPE);
            return HttpResponses.ok(payload);
        }

    };

    @Named
    public static class UploadPackService
            extends GitHandlerSupport
    {
        @Override
        @TransactionalTouchMetadata
        @TransactionalTouchBlob
        public Response handle(Context context) throws Exception {
            if (!new ActionMatcher(PUT_REQUEST_METHODS).matches(context)) {
                this.createLogger().debug("Wrong upload-pack action: {}", context.getRequest());
                return buildBadRequestResponse(PUT_REQUEST_METHODS);
            }

            if (!context.getRequest().getPayload().getContentType().equals(UPLOAD_PACK_REQUEST_TYPE)) {
                this.createLogger().debug("Wrong upload-pack content type: {}",
                        context.getRequest().getPayload().getContentType());
                return HttpResponses.badRequest();
            }

            Repository gitRepo = getGitRepository(context);
            if (gitRepo == null)
                return HttpResponses.notFound("unknown repository");

            UploadPack service = new UploadPack(gitRepo);
            service.setBiDirectionalPipe(false);

            ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
            service.upload(context.getRequest().getPayload().openInputStream(), outputBytes, null);

            Payload payload = new BytesPayload(outputBytes.toByteArray(), UPLOAD_PACK_RESULT_TYPE);
            return HttpResponses.ok(payload);
        }
    };

    @Named
    public static class ReceivePackService
            extends GitHandlerSupport
    {
        private final SecuritySystem securitySytem;

        @Inject
        public ReceivePackService(SecuritySystem securitySystem) {
            this.securitySytem = securitySystem;
        }

        @Override
        @TransactionalStoreBlob
        @TransactionalStoreMetadata
        public Response handle(Context context) throws Exception {
            if (!new ActionMatcher(PUT_REQUEST_METHODS).matches(context)) {
                this.createLogger().debug("Wrong receive-pack action: {}", context.getRequest());
                return buildBadRequestResponse(PUT_REQUEST_METHODS);
            }

            if (!context.getRequest().getPayload().getContentType().equals(RECEIVE_PACK_REQUEST_TYPE)) {
                this.createLogger().debug("Wrong receive-pack content type: {}",
                        context.getRequest().getPayload().getContentType());
                return HttpResponses.badRequest();
            }

            Repository gitRepo = getGitRepository(context);
            if (gitRepo == null)
                return HttpResponses.notFound("unknown repository");

            ReceivePack service = new ReceivePack(gitRepo);
            service.setBiDirectionalPipe(false);
            service.setEchoCommandFailures(true);

            // Figure out the user who is making this request so reflog identification can be
            // set.
            User user = this.securitySytem.currentUser();
            PersonIdent userPerson = new PersonIdent(user.getName(), user.getEmailAddress());
            service.setRefLogIdent(userPerson);

            ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
            service.receive(context.getRequest().getPayload().openInputStream(), outputBytes, null);
            Payload payload = new BytesPayload(outputBytes.toByteArray(), RECEIVE_PACK_RESULT_TYPE);
            return HttpResponses.ok(payload);
        }
    };

}
