package org.eclipse.ecf.internal.provider.etcd.protocol;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.Watch.Listener;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;

public class Etcd {
	private Client client;
	private KV kvClient;
	private Watcher watcher = null;

	/**
	 * Initializes ETCD client connection
	 * 
	 * @param endpoint endpoint to connect to the ETCD server
	 */
	public Etcd(String endpoint) {
		this.client = Client.builder().endpoints(endpoint).build();
		this.kvClient = client.getKVClient();
	}
	
	/**
	 * CLoses the clients connection to ETCD
	 */
	public void close() {
		closeWatch();
		client.close();
	}
	
	/**
	 * Puts a key-value pair into ETCD.
	 * 
	 * @param k key to put
	 * @param v value to put
	 * @throws EtcdException
	 */
	public void put(String k, String v) throws EtcdException {
		put(k, v, 0);
	}
	
	/**
	 * Puts a key-value pair into ETCD. If <i> ttl </i> is 0 no ttl is attached to the kv pair
	 * 
	 * @param k key to put
	 * @param v value to put
	 * @param ttl time-to-live value in seconds
	 * @throws EtcdException
	 */
	public void put(String k, String v, int ttl) throws EtcdException  {
		ByteSequence key = ByteSequence.from(k.getBytes());
		ByteSequence value = ByteSequence.from(v.getBytes());
		
		try {
			if(ttl == 0) {
				kvClient.put(key, value).get();
			}
			else {
				LeaseGrantResponse leaseGrantResponse = client.getLeaseClient().grant(ttl).get();
				PutOption option = PutOption.newBuilder().withLeaseId(leaseGrantResponse.getID()).build();
				kvClient.put(key, value, option).get();
			}
		} catch (Exception e) {
			throw new EtcdException("Unable to put key " + key, e); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns a map containing the key value pair associated with <i> key </i>
	 * 
	 * @param key Key to return
	 * @return Map containing the key value pair
	 * @throws EtcdException
	 */
	public Map<String, String> get(String key) throws EtcdException{
		return get(key, false);
	}
	
	/**
     * Returns a map containing all key value pairs of keys
     * with the prefix <i> key </i> if <i> isRange </i> is true.
     * Otherwise returns the key value pair associated with <i> key </i>
     *
     * @param  key Key to get
     * @param  isRange Treat <i> key </i> as the prefix for a range request or not 
     * @return  Map of key value pairs
     * @throws EtcdException
     */
	public Map<String, String> get(String key, boolean isRange) throws EtcdException{
		GetResponse getResponse = null;
		ByteSequence keyBytes = ByteSequence.from(key.getBytes());
		if(!isRange) {
			try {
				getResponse = kvClient.get(keyBytes).get();
			} catch (Exception e) {
				throw new EtcdException("Unable to get key " + key, e); //$NON-NLS-1$
			}
		}
		else {
			GetOption option = GetOption.newBuilder().withPrefix(keyBytes).build();
			try {
				getResponse = kvClient.get(keyBytes, option).get();
			} catch (Exception e) {
				throw new EtcdException("Unable to get key " + key, e); //$NON-NLS-1$
			}
		}
		Map<String, String> keyValueMap = new HashMap<>();
		for (KeyValue kv : getResponse.getKvs()) {
		    keyValueMap.put(
		        kv.getKey().toString(Charset.forName("UTF-8")), //$NON-NLS-1$
		        kv.getValue().toString(Charset.forName("UTF-8")) //$NON-NLS-1$
		    );
		}
		return keyValueMap;
	}
	
	/**
	 * Deletes all key value pairs that begin with prefix <i> key </i>
	 * 
	 * If <i> key </i> is '\0', all key-value pairs will be deleted
	 * 
	 * @param key Key to delete
	 * @throws EtcdException
	 */
	public void delete(String key) throws EtcdException {
		ByteSequence keyBytes = ByteSequence.from(key.getBytes());
		GetOption option;
		
		//if key is \0, will delete everything
		if(key.equals("\0")) //$NON-NLS-1$
			option = GetOption.newBuilder().withRange(keyBytes).build();
		else
			option = GetOption.newBuilder().withPrefix(keyBytes).build();
		GetResponse getResponse;
		try {
			getResponse = kvClient.get(keyBytes,option).get();
			for (KeyValue kv : getResponse.getKvs()) {
				kvClient.delete(kv.getKey());
			}
		} catch (Exception e) {
			throw new EtcdException("Unable to delete key " + key, e); //$NON-NLS-1$
		}
	}
	
	/**
	 * Adds a watch to the specified key
	 * 
	 * @param key key to watch
	 * @param listener listener to attach to the watch
	 */
	public void watch(String key, Listener listener) {
		ByteSequence keyBytes = ByteSequence.from(key.getBytes());
		Watch watch = client.getWatchClient();
		this.watcher = watch.watch(keyBytes, listener);
	}
	
	/**
	 * CLoses watch request
	 */
	public void closeWatch() {
		if(this.watcher == null)
			return;
		this.watcher.close();
	}
	
	/**
	 * Checks if there is an active watcher
	 * 
	 * @return true if there is an active watcher.
	 */
	public boolean isActiveWatch() {
		return this.watcher == null ? false : true;
	}
	
}
