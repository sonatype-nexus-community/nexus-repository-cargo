
package org.sonatype.nexus.plugins.cargo.git;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.common.hash.HashAlgorithm;

public class Constants {
    public final static List<HashAlgorithm> OBJECT_HASHES = Arrays.asList(HashAlgorithm.SHA1);
}
