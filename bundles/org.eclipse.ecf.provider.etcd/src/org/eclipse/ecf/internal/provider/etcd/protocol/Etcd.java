package org.eclipse.ecf.internal.provider.etcd.protocol;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;

//import org.eclipse.ecf.internal.provider.etcd.protocol.EtcdException;


public class Etcd {
	private Client client;
	
	private KV kvClient;
	
	public Etcd(String endpoint) {
		this.client = Client.builder().endpoints(endpoint).build();
		this.kvClient = client.getKVClient();

	}
	
	public void put(String k, String v) throws EtcdException {
		put(k, v, 0);
	}
	
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
	 * 
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
	 * @param key Key to delete
	 */
	public void delete(String key) throws InterruptedException, ExecutionException {
		ByteSequence keyBtyes = ByteSequence.from(key.getBytes());
		GetOption option = GetOption.newBuilder().withPrefix(keyBtyes).build();
		GetResponse getResponse = kvClient.get(keyBtyes,option).get();
		for (KeyValue kv : getResponse.getKvs()) {
		        kvClient.delete(kv.getKey());
		}
	}
	
	//TODO
	public void watch(String begin, String end) {
		
	}
	
}
