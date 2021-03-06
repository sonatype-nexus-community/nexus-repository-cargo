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

package org.sonatype.nexus.plugins.cargo.registry.assets;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Named;

import com.google.common.collect.Iterables;
import com.vdurmont.semver4j.Semver;

import org.sonatype.nexus.plugins.cargo.CrateCoordinates;
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
public class ComponentKindCrateAttributes
{
    final private String I_GROUP_CRATE = "crates";

    @TransactionalStoreMetadata
    public Component create(Repository repository, CrateCoordinates coords) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();
        Bucket bucket = tx.findBucket(repository);

        // This could be the first time the bucket is referenced which means it
        // doesn't actually exist in the database. Go ahead and save it to
        // force the record to be populated.
        tx.saveBucket(bucket);

        Component component = tx.createComponent(bucket, repository.getFormat());
        component.group(I_GROUP_CRATE);
        component.name(coords.getName());
        component.version(coords.getVersion().getValue());
        tx.saveComponent(component);

        return component;
    }

    @TransactionalTouchMetadata
    public Component find(Repository repository, CrateCoordinates coords) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        Iterable<Component> components =
                tx.findComponents(
                        Query.builder().where(ComponentEntityAdapter.P_GROUP).eq(I_GROUP_CRATE)
                                .and(ComponentEntityAdapter.P_NAME).eq(coords.getName())
                                .and(ComponentEntityAdapter.P_VERSION).eq(coords.getVersion().getValue()).build(),
                        Collections.singletonList(repository));

        return Iterables.getOnlyElement(components, null);
    }

    @TransactionalTouchMetadata
    public Iterable<Component> findAllVersions(Repository repository, String crateName) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        return tx.findComponents(
                Query.builder()
                        .where(ComponentEntityAdapter.P_GROUP).eq(I_GROUP_CRATE)
                        .and(ComponentEntityAdapter.P_NAME).eq(crateName)
                        .build(),
                Collections.singletonList(repository));
    }

    public CrateCoordinates getCoordinates(Component component) {
        return new CrateCoordinates(component.name(), new Semver(component.version()));
    }
}
