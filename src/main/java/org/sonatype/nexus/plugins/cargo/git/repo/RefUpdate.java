
package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;

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
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

public class RefUpdate extends org.eclipse.jgit.lib.RefUpdate {
    private final Repository db;
    private final AssetKindRefAttributes asset_attributes;
    private final Bucket bucket;
    private final Component component;

    private Ref dstRef;
    private RevWalk rw;

    RefUpdate(Repository db, Ref ref) {
        super(ref);
        this.db = db;
        this.asset_attributes = db.getAssetAttributesRef();
        this.bucket = db.getBucket();
        this.component = db.getComponent();
    }

    @Override
    protected RefDatabase getRefDatabase() {
        return this.db.getRefDatabase();
    }

    @Override
    protected Repository getRepository() {
        return this.db;
    }

    @Override
    protected boolean tryLock(boolean deref) throws IOException {
        dstRef = getRef();
        if (deref)
            dstRef = dstRef.getLeaf();

        if (dstRef.isSymbolic())
            setOldObjectId(null);
        else
            setOldObjectId(dstRef.getObjectId());

        return true;
    }

    @Override
    protected void unlock() {
        // No state is held while "locked".
    }

    @Override
    public Result update(RevWalk walk) throws IOException {
        // RevUpdate is needed in doUpdate() but is not available there.
        try {
            rw = walk;
            return super.update(walk);
        } finally {
            rw = null;
        }
    }

    @Override
    @TransactionalStoreMetadata
    protected Result doUpdate(Result desired_result) throws IOException {
        ObjectIdRef newRef;
        RevObject obj = rw.parseAny(getNewObjectId());
        if (obj instanceof RevTag) {
            newRef = new ObjectIdRef.PeeledTag(
                    Ref.Storage.LOOSE,
                    dstRef.getName(),
                    getNewObjectId(),
                    rw.peel(obj).copy());
        } else {
            newRef = new ObjectIdRef.PeeledNonTag(
                    Ref.Storage.LOOSE,
                    dstRef.getName(),
                    getNewObjectId());
        }

        StorageTx tx = UnitOfWork.currentTx();
        Asset asset = asset_attributes.findRefAssetWithName(this.bucket, this.component,
                dstRef.getName());
        if (asset == null) {
            asset = asset_attributes.createRefAsset(this.bucket, this.component, dstRef.getName());
        }
        asset_attributes.setRef(asset, newRef);
        tx.saveAsset(asset);

        return desired_result;
    }

    @Override
    @TransactionalStoreMetadata
    protected Result doLink(String target) throws IOException {
        final SymbolicRef newRef = new SymbolicRef(
                dstRef.getName(),
                new ObjectIdRef.Unpeeled(
                        Ref.Storage.NEW,
                        target,
                        null));

        StorageTx tx = UnitOfWork.currentTx();
        Asset asset = asset_attributes.findRefAssetWithName(this.bucket, this.component,
                dstRef.getName());
        if (asset == null) {
            asset = asset_attributes.createRefAsset(this.bucket, this.component, dstRef.getName());
        }
        asset_attributes.setRef(asset, newRef);
        tx.saveAsset(asset);

        if (dstRef.getStorage() == Ref.Storage.NEW) {
            return Result.NEW;
        } else {
            return Result.FORCED;
        }
    }

    @Override
    @TransactionalStoreMetadata
    protected Result doDelete(Result desired_result) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();
        Asset asset = asset_attributes.findRefAssetWithName(this.bucket, this.component,
                dstRef.getName());
        if (asset == null)
            return Result.REJECTED_MISSING_OBJECT;

        tx.deleteAsset(asset);

        return desired_result;
    }
}
