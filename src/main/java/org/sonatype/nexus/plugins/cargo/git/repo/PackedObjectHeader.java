
package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;
import java.io.InputStream;

public class PackedObjectHeader {
    private final int type_code;
    private long size;

    public PackedObjectHeader(InputStream src) throws IOException {
        int c = src.read();

        this.type_code = (c >> 4) & 7;
        this.size = c & 15;

        int shift = 4;
        while ((c & 0x80) != 0) {
            c = src.read();
            this.size += ((long) (c & 0x7f)) << shift;
            shift += 7;
        }
    }

    public int getType() {
        return this.type_code;
    }

    public long getObjectSize() {
        return this.size;
    }
}
