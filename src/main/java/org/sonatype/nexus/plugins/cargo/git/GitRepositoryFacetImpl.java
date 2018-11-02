/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo.git;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Preconditions;

import org.sonatype.nexus.plugins.cargo.GitRepositoryFacet;
import org.sonatype.nexus.plugins.cargo.git.assets.ComponentKindGitAttributes;
import org.sonatype.nexus.plugins.cargo.git.repo.Repository;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

@Named
public class GitRepositoryFacetImpl
        extends FacetSupport
        implements GitRepositoryFacet
{
    private final ComponentKindGitAttributes component_attributes;

    private final Repository.Builder builder;

    @Inject
    public GitRepositoryFacetImpl(ComponentKindGitAttributes component_attributes, Repository.Builder builder) {
        this.component_attributes = component_attributes;
        this.builder = builder;
    }

    @Override
    @Nonnull
    @TransactionalStoreMetadata
    public Repository createGitRepository(String repo_name) throws IOException {
        Preconditions.checkNotNull(repo_name, "getGitRepository called without a 'repo_name' token");
        Component component = this.component_attributes.createGitComponent(this.getRepository(), repo_name);

        StorageTx tx = UnitOfWork.currentTx();
        Bucket bucket = tx.findBucket(this.getRepository());
        StorageFacet storage_facet = this.getRepository().facet(StorageFacet.class);
        Repository repo = this.builder.setStorageFacet(storage_facet).setComponent(bucket, component).build();

        repo.create();
        return repo;
    }

    @Override
    @Nullable
    @TransactionalTouchMetadata
    public Repository getGitRepository(String repo_name) throws IOException {
        Preconditions.checkNotNull(repo_name, "getGitRepository called without a 'repo_name' token");

        Component component = this.component_attributes.findGitComponent(this.getRepository(), repo_name);
        if (component == null) {
            return null;
        }

        StorageTx tx = UnitOfWork.currentTx();
        Bucket bucket = tx.findBucket(this.getRepository());
        StorageFacet storage_facet = this.getRepository().facet(StorageFacet.class);
        return this.builder.setStorageFacet(storage_facet).setComponent(bucket, component).build();
    }
}
