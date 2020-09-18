package ai.darklight.etcd.storage;

import org.eclipse.ecf.internal.provider.etcd.protocol.Etcd;

public class Storage {
	private Etcd etcd;
	
	private String storagePath = null;
	
	/**
	 * Allows easy access for storing key value pairs within ETCD
	 * using a specific storage path
	 * 
	 * @param storagePath path to store settings
	 * @param endpoint endpoint to connect to ETCD server
	 */
	public Storage(String settingPath, String endpoint) {
		etcd = new Etcd(endpoint);
		this.storagePath = verifySlash(settingPath);
	}
	
	/**
	 * Allows easy access for storing key value pairs within ETCD
	 * without setting a specific storage path.
	 * 
	 *<p> 
	 *Storage path can be set at any time using {@link #setStoragePath(String) setStoragePath}
	 * 
	 * @param endpoint endpoint to connect to ETCD server
	 */
	public Storage(String endpoint) {
		etcd = new Etcd(endpoint);
	}
	
	/**
	 * Puts the specified key value pair into ETCD at the current <i> storagePath </i>
	 * 
	 * @param key 
	 * @param value
	 */
	public void set(String key, String value) {
		etcd.put(storagePath+key, value);
	}
	
	/**
	 * Gets the value paired with <i> key </i> at the current storage path
	 * 
	 * @param key key paired with the desired value
	 * @return value paired with <i> key </i> or null if the pair does not exist
	 */
	public String get(String key) {
		String value = etcd.get(storagePath+key).get(storagePath+key);
		return value.isEmpty() ? value : null;
	}
	
	/**
	 * Sets the storage path
	 * 
	 * @param storagePath path to store settings
	 */
	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}
	
	private String verifySlash(String path) {
		if (!path.endsWith("/"))
			return path + "/";
		else
			return path;
	}
}
