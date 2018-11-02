
package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.InflaterInputStream;

import org.eclipse.jgit.internal.storage.file.PackLock;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.PackedObjectInfo;
import org.sonatype.nexus.repository.storage.TempBlob;

public class PackParser extends org.eclipse.jgit.transport.PackParser {
    private final TempBlob pack_blob;
    private InputStream pack_stream;

    private final ObjectInserter inserter;
    private final ObjectReader reader;

    /** CRC-32 computation for objects that are appended onto the pack. */
    private final CRC32 crc = new CRC32();

    /** Checksum of the entire pack file. */
    private final MessageDigest packDigest = Constants.newMessageDigest();

    PackParser(Repository db, TempBlob pack_blob) {
        super(db.getObjectDatabase(), pack_blob.get());
        this.inserter = db.newObjectInserter();
        this.reader = db.newObjectReader();
        this.pack_blob = pack_blob;
        this.pack_stream = pack_blob.get();
    }

    @Override
    public PackLock parse(ProgressMonitor receiving, ProgressMonitor resolving) throws IOException {
        super.parse(receiving, resolving);

        // For any object that wasn't inflated during pack parsing, do so now so
        // all objects from the pack are inserted into the database.
        for (PackedObjectInfo info : this.getSortedObjectList(null)) {
            if (!this.reader.has(info)) {
                InputStream packed_obj = pack_blob.get();
                packed_obj.skip(info.getOffset());
                PackedObjectHeader header = new PackedObjectHeader(packed_obj);
                inserter.insert(header.getType(), header.getObjectSize(),
                        new InflaterInputStream(packed_obj));
            }
        }

        return null;
    }

    @Override
    protected void onPackHeader(long objCount) throws IOException {
    }

    @Override
    protected void onBeginWholeObject(long streamPosition, int type, long inflatedSize)
            throws IOException {
        this.crc.reset();
    }

    @Override
    protected void onEndWholeObject(PackedObjectInfo info) throws IOException {
        info.setCRC((int) crc.getValue());
    }

    @Override
    protected void onBeginOfsDelta(long deltaStreamPosition, long baseStreamPosition,
            long inflatedSize) throws IOException {
        this.crc.reset();
    }

    @Override
    protected void onBeginRefDelta(long deltaStreamPosition, AnyObjectId baseId, long inflatedSize)
            throws IOException {
        this.crc.reset();
    }

    @Override
    protected UnresolvedDelta onEndDelta() throws IOException {
        UnresolvedDelta delta = new UnresolvedDelta();
        delta.setCRC((int) this.crc.getValue());
        return delta;
    }

    @Override
    protected void onInflatedObjectData(PackedObjectInfo obj, int type_code, byte[] data)
            throws IOException {
        // Small objects and deltas will be fully inflated and resolved as the
        // pack is read. Since that work is already done for us, insert the
        // inflated object into the database.
        this.inserter.insert(type_code, data);
    }

    @Override
    protected void onObjectHeader(Source src, byte[] raw, int pos, int len) throws IOException {
        this.crc.update(raw, pos, len);
    }

    @Override
    protected void onObjectData(Source src, byte[] raw, int pos, int len) throws IOException {
        this.crc.update(raw, pos, len);
    }

    @Override
    protected void onStoreStream(byte[] raw, int pos, int len) throws IOException {
        // Objects are being stored individually so only the pack hash needs to be updated.
        packDigest.update(raw, pos, len);
    }

    @Override
    protected void onPackFooter(byte[] hash) throws IOException {
        if (!Arrays.equals(packDigest.digest(), hash)) {
            throw new IOException("Received corrupt pack");
        }
    }

    @Override
    protected ObjectTypeAndSize seekDatabase(PackedObjectInfo obj, ObjectTypeAndSize info)
            throws IOException {
        crc.reset();
        this.pack_stream = this.pack_blob.get();
        this.pack_stream.skip(obj.getOffset());
        return readObjectHeader(info);
    }

    @Override
    protected ObjectTypeAndSize seekDatabase(UnresolvedDelta delta, ObjectTypeAndSize info)
            throws IOException {
        crc.reset();
        this.pack_stream = this.pack_blob.get();
        this.pack_stream.skip(delta.getOffset());
        return readObjectHeader(info);
    }

    @Override
    protected int readDatabase(byte[] dst, int pos, int cnt) throws IOException {
        return this.pack_stream.read(dst, pos, cnt);
    }

    @Override
    protected boolean checkCRC(int oldCRC) {
        return oldCRC == (int) crc.getValue();
    }

    @Override
    protected boolean onAppendBase(int typeCode, byte[] data, PackedObjectInfo info)
            throws IOException {
        return false;
    }

    @Override
    protected void onEndThinPack() throws IOException {
        // Ignored.
    }
}
