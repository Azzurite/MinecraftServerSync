package name.azzurite.mcserver.config;

public class Version implements Comparable<Version> {

	private final String version;

	public Version(String version) {
		this.version = version;
	}

	@Override
	public int compareTo(Version o) {
		return version.compareTo(o.toString());
	}

	@Override
	public String toString() {
		return version;
	}
}
