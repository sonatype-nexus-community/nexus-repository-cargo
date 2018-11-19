
package org.sonatype.nexus.plugins.cargo.git.repo;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import org.eclipse.jgit.lib.Constants;

public class ObjectIdHashFunction implements HashFunction {
    private final int object_type;
    private final long object_size;

    public ObjectIdHashFunction(int object_type, long object_size) {
        this.object_type = object_type;
        this.object_size = object_size;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int bits() {
        return Hashing.sha1().bits();
    }

    @Override
    public HashCode hashBytes(ByteBuffer input) {
        return newHasher().putBytes(input).hash();
    }

    @Override
    public HashCode hashBytes(byte[] input) {
        return newHasher().putBytes(input).hash();
    }

    @Override
    public HashCode hashBytes(byte[] input, int off, int len) {
        return newHasher().putBytes(input, off, len).hash();
    }

    @Override
    public HashCode hashInt(int input) {
        return newHasher().putInt(input).hash();
    }

    @Override
    public HashCode hashLong(long input) {
        return newHasher().putLong(input).hash();
    }

    @Override
    public <T> HashCode hashObject(T instance, Funnel<? super T> funnel) {
        return newHasher().putObject(instance, funnel).hash();
    }

    @Override
    public HashCode hashString(CharSequence input, Charset charset) {
        return newHasher().putString(input, charset).hash();
    }

    @Override
    public HashCode hashUnencodedChars(CharSequence input) {
        return newHasher().putUnencodedChars(input).hash();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Hasher newHasher() {
        return putHeader(Hashing.sha1().newHasher());
    }

    @Override
    @SuppressWarnings("deprecation")
    public Hasher newHasher(int expectedInputSize) {
        return putHeader(Hashing.sha1().newHasher(expectedInputSize));
    }

    private Hasher putHeader(Hasher hash) {
        return hash.putBytes(Constants.encodedTypeString(this.object_type))
                .putByte((byte) ' ')
                .putBytes(Constants.encodeASCII(this.object_size))
                .putByte((byte) 0);
    }

}
