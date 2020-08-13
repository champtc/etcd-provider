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
import io.etcd.jetcd.options.GetOption;


public class Etcd {
	private Client client;
	
	private KV kvClient;
	
	public Etcd(String endpoint) {
		this.client = Client.builder().endpoints(endpoint).build();
		this.kvClient = client.getKVClient();
	}
	
	public void put(String k, String v) throws InterruptedException, ExecutionException  {
		ByteSequence key = ByteSequence.from(k.getBytes());
		ByteSequence value = ByteSequence.from(v.getBytes());
		kvClient.put(key, value).get();
	}
	
	/**
	 * Returns a map containing the key value pair associated with <i> key </i>
	 * 
	 * @param key Key to return
	 * @return Map containing the key value pair
	 * 
	 */
	public Map<String, String> get(String key) throws InterruptedException, ExecutionException {
		return get(key, key);
	}
	
	/**
     * Returns a map containing all keys from
     * <i>beginRange</i> to <i>endRange</i> (exclusive).
     *
     * <p>
     * If end key is '\0', the range is all keys >= key.
     *
     * <p>
     * If both key and end key are '\0', it returns all keys.
     *
     * @param  beginRange Beginning key
     * @param  endRange End key
     * @return  Map of key value pairs
     */
	public Map<String, String> get(String beginRange, String endRange) throws InterruptedException, ExecutionException {
		GetResponse getResponse;
		ByteSequence key = ByteSequence.from(beginRange.getBytes());
		if(beginRange == endRange) {
			getResponse = kvClient.get(key).get();
		}
		else {
			ByteSequence endKey = ByteSequence.from(endRange.getBytes());
			GetOption option = GetOption.newBuilder().withRange(endKey).build();
			getResponse = kvClient.get(key, option).get();
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
	
	
	public void delete(String key) throws InterruptedException, ExecutionException {
		delete(key,key);
	}

	public void delete(String begin, String end) throws InterruptedException, ExecutionException {
		GetResponse getResponse;
		ByteSequence key = ByteSequence.from(begin.getBytes());
		if(begin == end) {
			getResponse = kvClient.get(key).get();
		} 
		else {
			ByteSequence endRange = ByteSequence.from(end.getBytes());
			GetOption option = GetOption.newBuilder().withRange(endRange).build();
			getResponse = kvClient.get(key,option).get();
		}
		for (KeyValue kv : getResponse.getKvs()) {
		        kvClient.delete(kv.getKey());
		}
	}
	
	//TODO
	public void watch(String begin, String end) {
		
	}
	
}
