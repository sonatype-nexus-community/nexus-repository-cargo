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

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Preconditions;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.plugins.cargo.GitRepositoryFacet;
import org.sonatype.nexus.plugins.cargo.git.assets.ComponentKindGitAttributes;
import org.sonatype.nexus.plugins.cargo.git.repo.Repository;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

@Named
public class GitRepositoryFacetImpl
        extends FacetSupport
        implements GitRepositoryFacet
{
    private final EmailManager emailManager;

    private final ComponentKindGitAttributes component_attributes;

    private final Repository.Builder builder;

    @Inject
    public GitRepositoryFacetImpl(EmailManager emailManager,
                                  ComponentKindGitAttributes component_attributes,
                                  Repository.Builder builder)
    {
        this.emailManager = emailManager;
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

    @Override
    @TransactionalTouchMetadata
    @TransactionalTouchBlob
    @TransactionalStoreMetadata
    @TransactionalStoreBlob
    public void replaceFile(Repository repository, String branch, String entryPath, byte[] bytes) throws IOException {
        // Calculate the ObjectId for the new contents and insert it into the object index.
        ObjectInserter ins = repository.newObjectInserter();
        AnyObjectId configObjId = ins.idFor(Constants.OBJ_BLOB, bytes);
        ins.insert(Constants.OBJ_BLOB, bytes);
        ins.flush();

        // Start with a blank tree.
        DirCache dirCache = DirCache.newInCore();
        RevCommit parent = null;

        // If there is a current HEAD, read that commit's tree. It will become
        // our parent.
        AnyObjectId headId = repository.resolve(branch + "^{commit}"); //$NON-NLS-1$
        if (headId != null) {
            try (RevWalk revWalk = new RevWalk(repository)) {
                parent = revWalk.parseCommit(headId);
                DirCacheBuilder builder = dirCache.builder();
                builder.addTree(null, DirCacheEntry.STAGE_0, repository.newObjectReader(), parent.getTree());
                builder.finish();
            }
        }

        // Update entryPath in the tree to point to the new ObjectId.
        DirCacheEditor editor = dirCache.editor();
        editor.add(new DirCacheEditor.PathEdit(entryPath)
        {
            @Override
            public void apply(DirCacheEntry ent) {
                ent.setFileMode(FileMode.REGULAR_FILE);
                ent.setObjectId(configObjId);
            }
        });
        editor.finish();
        AnyObjectId indexTreeId = dirCache.writeTree(ins);

        // Check for empty commits
        if (parent != null && indexTreeId.equals(parent.getTree())) {
            return;
        }

        // Create a Commit object, populate it and write it
        PersonIdent adminIdent = new PersonIdent("Nexus System", this.emailManager.getConfiguration().getFromAddress());
        CommitBuilder commit = new CommitBuilder();
        commit.setCommitter(adminIdent);
        commit.setAuthor(adminIdent);
        commit.setMessage("Update " + entryPath);
        if (parent != null) {
            commit.setParentIds(parent);
        }
        commit.setTreeId(indexTreeId);
        AnyObjectId commitId = ins.insert(commit);
        ins.flush();

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit revCommit = revWalk.parseCommit(commitId);
            RefUpdate headUpdate = repository.updateRef(branch);
            headUpdate.setNewObjectId(commitId);
            String reflogMsgPrefix = parent == null ? "commit (initial): " : "commit: ";
            headUpdate.setRefLogMessage(reflogMsgPrefix + revCommit.getShortMessage(), false);

            if (headId != null)
                headUpdate.setExpectedOldObjectId(headId);
            else
                headUpdate.setExpectedOldObjectId(ObjectId.zeroId());
            headUpdate.forceUpdate();
        }
    }
}
