
package org.sonatype.nexus.plugins.cargo.git.repo;

public class ObjectDatabase extends org.eclipse.jgit.lib.ObjectDatabase {
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
