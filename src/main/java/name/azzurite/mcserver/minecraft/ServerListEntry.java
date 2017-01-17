package name.azzurite.mcserver.minecraft;

import java.util.Map;

import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;

class ServerListEntry {

	private static final String NAME_KEY = "name";

	private static final String ICON_KEY = "icon";

	private static final String IP_KEY = "ip";

	private static final String HIDE_ADDRESS_KEY = "hideAddress";

	private static final String ACCEPT_TEXTURES_KEY = "acceptTextures";

	private String name;

	private String icon;

	private String ip;

	private Byte hideAddress;

	private Byte acceptTextures;

	ServerListEntry(String name, String ip) {
		this.name = name;
		this.ip = ip;
	}

	ServerListEntry(Map<String, Tag<?>> tagData) {
		if (tagData.containsKey(NAME_KEY)) {
			name = ((StringTag) tagData.get(NAME_KEY)).getValue();
		}
		if (tagData.containsKey(ICON_KEY)) {
			icon = ((StringTag) tagData.get(ICON_KEY)).getValue();
		}
		if (tagData.containsKey(IP_KEY)) {
			ip = ((StringTag) tagData.get(IP_KEY)).getValue();
		}
		if (tagData.containsKey(HIDE_ADDRESS_KEY)) {
			hideAddress = ((ByteTag) tagData.get(HIDE_ADDRESS_KEY)).getValue();
		}
		if (tagData.containsKey(ACCEPT_TEXTURES_KEY)) {
			acceptTextures = ((ByteTag) tagData.get(ACCEPT_TEXTURES_KEY)).getValue();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Byte getHideAddress() {
		return hideAddress;
	}

	public void setHideAddress(Byte hideAddress) {
		this.hideAddress = hideAddress;
	}

	public Byte getAcceptTextures() {
		return acceptTextures;
	}

	public void setAcceptTextures(Byte acceptTextures) {
		this.acceptTextures = acceptTextures;
	}

	public CompoundTag toTag() {
		CompoundMap map = new CompoundMap();
		if (name != null) {
			map.put(NAME_KEY, new StringTag(NAME_KEY, name));
		}
		if (icon != null) {
			map.put(ICON_KEY, new StringTag(ICON_KEY, icon));
		}
		if (ip != null) {
			map.put(IP_KEY, new StringTag(IP_KEY, ip));
		}
		if (hideAddress != null) {
			map.put(HIDE_ADDRESS_KEY, new ByteTag(HIDE_ADDRESS_KEY, hideAddress));
		}
		if (acceptTextures != null) {
			map.put(ACCEPT_TEXTURES_KEY, new ByteTag(ACCEPT_TEXTURES_KEY, acceptTextures));
		}
		return new CompoundTag("", map);
	}

}
