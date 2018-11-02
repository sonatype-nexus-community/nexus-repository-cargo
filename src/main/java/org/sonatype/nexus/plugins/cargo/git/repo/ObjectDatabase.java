/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo.git.repo;

public class ObjectDatabase
        extends org.eclipse.jgit.lib.ObjectDatabase
{
    private final Repository db;

    ObjectDatabase(Repository db) {
        this.db = db;
    }

    @Override
    public ObjectReader newReader() {
        return new ObjectReader(db);
    }

    @Override
    public ObjectInserter newInserter() {
        return new ObjectInserter(db);
    }

    @Override
    public void close() {

    }
}
