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

package org.sonatype.nexus.plugins.cargo.git.assets;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Named;

import com.google.common.collect.Iterables;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

@Named
public class ComponentKindGitAttributes
{
    final private String I_GROUP_GIT = "git";

    @TransactionalStoreMetadata
    public Component createGitComponent(Repository repository, String repo_name) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();
        Bucket bucket = tx.findBucket(repository);

        // This could be the first time the bucket is referenced which means it
        // doesn't actually exist in the database. Go ahead and save it to
        // force the record to be populated.
        tx.saveBucket(bucket);

        Component component = tx.createComponent(bucket, repository.getFormat());
        component.name(repo_name);
        component.group(I_GROUP_GIT);
        tx.saveComponent(component);

        return component;
    }

    @TransactionalTouchMetadata
    public Component findGitComponent(Repository repository, String repo_name) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        Iterable<Component> components =
                tx.findComponents(
                        Query.builder().where(ComponentEntityAdapter.P_GROUP).eq(I_GROUP_GIT)
                                .and(ComponentEntityAdapter.P_NAME).eq(repo_name).build(),
                        Collections.singletonList(repository));

        return Iterables.getOnlyElement(components, null);
    }
}
