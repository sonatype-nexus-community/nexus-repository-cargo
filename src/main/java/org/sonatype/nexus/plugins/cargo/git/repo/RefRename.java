
package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;

public class RefRename extends org.eclipse.jgit.lib.RefRename {
    RefRename(RefUpdate src, RefUpdate dst) {
        super(src, dst);
    }

    @Override
    protected Result doRename() throws IOException {
        destination.setExpectedOldObjectId(ObjectId.zeroId());
        destination.setNewObjectId(source.getRef().getObjectId());
        switch (destination.update()) {
            case NEW:
                source.delete();
                return Result.RENAMED;

            default:
                return destination.getResult();
        }
    }
}
