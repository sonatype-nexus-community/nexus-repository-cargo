/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jgit.lib.Repository;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Facet.Exposed;

@Exposed
public interface GitRepositoryFacet
        extends Facet
{
    @Nonnull
    public Repository createGitRepository(String name) throws IOException;

    @Nullable
    public Repository getGitRepository(String name) throws IOException;
}
