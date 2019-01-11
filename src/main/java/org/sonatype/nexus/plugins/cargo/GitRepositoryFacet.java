
package org.sonatype.nexus.plugins.cargo;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.plugins.cargo.git.repo.Repository;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Facet.Exposed;

@Exposed
public interface GitRepositoryFacet extends Facet {
    @Nonnull
    public Repository createGitRepository(String name) throws IOException;

    @Nullable
    public Repository getGitRepository(String name) throws IOException;

    public void replaceFile(Repository repository, String branch, String entryPath, byte[] bytes)
            throws IOException;
}
