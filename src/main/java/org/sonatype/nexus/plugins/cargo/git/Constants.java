/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo.git;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.common.hash.HashAlgorithm;

public class Constants
{
    public final static List<HashAlgorithm> OBJECT_HASHES = Arrays.asList(HashAlgorithm.SHA1);
}
