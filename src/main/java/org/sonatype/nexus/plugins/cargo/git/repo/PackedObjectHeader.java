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

package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;
import java.io.InputStream;

public class PackedObjectHeader
{
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
