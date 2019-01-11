
package org.sonatype.nexus.plugins.cargo;

import com.vdurmont.semver4j.Semver;

/* CrateCoordinates are a unique identifier for a specific crate instance.
 * Since cargo registries have a flat namespace, only the crate name and version
 * are required.
 */
public class CrateCoordinates {
    protected String name;
    protected Semver version;

    public CrateCoordinates(String name, Semver version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return this.name;
    }

    public Semver getVersion() {
        return this.version;
    }

    public String getFileBasename() {
        return String.format("%s-%s", this.name, this.version.getValue());
    }

    public String getIndexEntryPath() {
        if (name.length() == 1) {
            return String.format("1/%s", this.name);
        } else if (name.length() == 2) {
            return String.format("2/%s", this.name);
        } else if (name.length() == 3) {
            return String.format("3/%c/%s", this.name.charAt(0), this.name);
        } else {
            return String.format("%s/%s/%s", this.name.substring(0, 2), this.name.substring(2, 4),
                    this.name);
        }
    }
}
