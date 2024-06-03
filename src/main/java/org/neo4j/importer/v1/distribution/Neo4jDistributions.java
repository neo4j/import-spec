package org.neo4j.importer.v1.distribution;

import java.util.Locale;
import java.util.Objects;

public class Neo4jDistributions {

    private Neo4jDistributions() {}

    public static Edition community() {
        return Edition.COMMUNITY;
    }

    public static Edition enterprise() {
        return Edition.ENTERPRISE;
    }

    public static Edition aura() {
        return Edition.AURA;
    }

    public enum Edition {
        COMMUNITY,
        ENTERPRISE,
        AURA;

        public Neo4jDistribution of(String version) {
            return new Neo4jDistribution(this, Version.of(version));
        }
    }

    protected static class Version {
        private final int major;
        private final int minor;

        private Version(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        public static Version of(String version) {
            if (version.toLowerCase(Locale.ROOT).endsWith("-aura")) {
                version = version.substring(0, version.length() - 5);
            }

            String[] parts = version.split("\\.");

            int major;
            try {
                major = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                throw new InvalidNeo4jVersionException(
                        String.format("Major version number %s is invalid. Must be a number.", parts[0]));
            }

            int minor;
            try {
                minor = Integer.parseInt(parts[1]);
            } catch (IndexOutOfBoundsException e) {
                throw new InvalidNeo4jVersionException("Minor version should be specified. Examples: 5.9, 5.17");
            } catch (NumberFormatException e) {
                throw new InvalidNeo4jVersionException(
                        String.format("Minor version number %s is invalid. Must be a number.", parts[1]));
            }

            if (major < 0 || minor < 0) {
                throw new InvalidNeo4jVersionException(
                        String.format("Version string %s can not contain a negative number.", version));
            }

            return new Version(major, minor);
        }

        public boolean isLargerThanOrEqual(String version) {
            var givenVersion = of(version);
            return this.major > givenVersion.major
                    || (this.major == givenVersion.major && (this.minor >= givenVersion.minor));
        }

        @Override
        public String toString() {
            return String.format("%s.%s", major, minor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Version)) return false;
            Version that = (Version) o;
            return Objects.equals(major, that.major) && Objects.equals(minor, that.minor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor);
        }
    }
}