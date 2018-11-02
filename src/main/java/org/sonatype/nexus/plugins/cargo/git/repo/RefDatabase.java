
package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.SymbolicRef;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.sonatype.nexus.plugins.cargo.git.assets.AssetKindRefAttributes;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;

public class RefDatabase extends org.eclipse.jgit.lib.RefDatabase {
    private final Repository db;
    private final AssetKindRefAttributes asset_attributes;

    private final Bucket bucket;
    private final Component component;

    @Inject
    RefDatabase(Repository db) {
        this.db = db;
        this.asset_attributes = db.getAssetAttributesRef();
        this.bucket = db.getBucket();
        this.component = db.getComponent();
    }

    @Override
    public void create() throws IOException {
        // Nothing to do.
    }

    @Override
    public void close() {
        // Nothing to do.
    }

    @Override
    public Ref exactRef(String name) throws IOException {
        Asset asset = asset_attributes.findRefAssetWithName(this.bucket, this.component, name);
        if (asset == null)
            return null;

        return asset_attributes.getRef(this.bucket, this.component, asset);
    }

    @Override
    public Ref getRef(String name) throws IOException {
        for (String prefix : SEARCH_PATH) {
            Ref ref = exactRef(prefix + name);
            if (ref != null)
                return ref;
        }
        return null;
    }

    @Override
    @TransactionalTouchMetadata
    public Map<String, Ref> getRefs(String prefix) throws IOException {
        Map<String, Ref> refsByName = new HashMap<>();
        for (Asset asset : asset_attributes.browseRefAssetsByPrefix(this.bucket, this.component,
                prefix)) {
            Ref ref = asset_attributes.getRef(this.bucket, this.component, asset);
            if (ref != null)
                refsByName.put(StringUtils.remove(asset.name(), prefix), ref);
        }

        return refsByName;
    }

    @Override
    @NonNull
    @TransactionalTouchMetadata
    public List<Ref> getRefsByPrefix(String prefix) throws IOException {
        List<Ref> refs = new ArrayList<Ref>();
        for (Asset asset : asset_attributes.browseRefAssetsByPrefix(this.bucket, this.component,
                prefix)) {
            Ref ref = asset_attributes.getRef(this.bucket, this.component, asset);
            if (ref != null)
                refs.add(ref);
        }
        return refs;
    }

    @Override
    public RefUpdate newUpdate(String name, boolean detach) throws IOException {
        boolean detachingSymbolicRef = false;
        Ref ref = exactRef(name);
        if (ref == null)
            ref = new ObjectIdRef.Unpeeled(Ref.Storage.NEW, name, null);
        else
            detachingSymbolicRef = detach && ref.isSymbolic();

        RefUpdate update = new RefUpdate(this.db, ref);
        if (detachingSymbolicRef)
            update.setDetachingSymbolicRef();

        return update;
    }

    @Override
    public RefRename newRename(String from_name, String to_name) throws IOException {
        RefUpdate src = newUpdate(from_name, true);
        RefUpdate dst = newUpdate(to_name, true);
        return new RefRename(src, dst);
    }

    @Override
    public Ref peel(Ref ref) throws IOException {
        final Ref leaf = ref.getLeaf();
        if (leaf.isPeeled() || leaf.getObjectId() == null)
            return ref;

        ObjectIdRef newLeaf = null;
        try (RevWalk rw = new RevWalk(this.db)) {
            RevObject obj = rw.parseAny(leaf.getObjectId());
            if (obj instanceof RevTag) {
                newLeaf = new ObjectIdRef.PeeledTag(
                        leaf.getStorage(),
                        leaf.getName(),
                        leaf.getObjectId(),
                        rw.peel(obj).copy());
            } else {
                newLeaf = new ObjectIdRef.PeeledNonTag(
                        leaf.getStorage(),
                        leaf.getName(),
                        leaf.getObjectId());
            }
        }

        return recreate(ref, newLeaf);
    }

    private static Ref recreate(Ref old, ObjectIdRef leaf) {
        if (old.isSymbolic()) {
            Ref dst = recreate(old.getTarget(), leaf);
            return new SymbolicRef(old.getName(), dst);
        }
        return leaf;
    }

    @Override
    public List<Ref> getAdditionalRefs() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public boolean isNameConflicting(String name) throws IOException {
        // Cannot be nested within an existing reference.
        int lastSlash = name.lastIndexOf('/');
        while (0 < lastSlash) {
            String needle = name.substring(0, lastSlash);
            if (asset_attributes.findRefAssetWithName(this.bucket, this.component, needle) != null)
                return true;
            lastSlash = name.lastIndexOf('/', lastSlash - 1);
        }

        // Cannot be the container of an existing reference.
        String prefix = name + '/';
        if (getRefsByPrefix(prefix).size() != 0)
            return true;

        return false;
    }

}
