package org.eclipse.ecf.tests.provider.etcd;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ecf.discovery.IDiscoveryAdvertiser;
import org.eclipse.ecf.discovery.IDiscoveryLocator;
import org.eclipse.ecf.discovery.IServiceInfo;
import org.eclipse.ecf.discovery.IServiceProperties;
import org.eclipse.ecf.discovery.identity.IServiceID;
import org.eclipse.ecf.internal.provider.etcd.protocol.Etcd;
import org.eclipse.ecf.internal.provider.etcd.protocol.EtcdSuccessResponse;
import org.eclipse.ecf.provider.etcd.EtcdDiscoveryContainerInstantiator;
import org.eclipse.ecf.provider.etcd.EtcdServiceInfo;
import org.eclipse.ecf.provider.etcd.identity.EtcdNamespace;
import org.eclipse.ecf.tests.discovery.AbstractDiscoveryTest;
import org.eclipse.ecf.tests.discovery.Activator;
import org.json.JSONException;

import io.etcd.jetcd.Watch;
import io.etcd.jetcd.Watch.Listener;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchEvent.EventType;

public class DiscoveryTest extends AbstractDiscoveryTest {
	
	public Etcd etcd = null;

	public DiscoveryTest() {
		super(EtcdDiscoveryContainerInstantiator.NAME);
	}

	@Override
	protected void setUp() throws Exception {
		new EtcdNamespace();
		etcd = new Etcd("http://localhost:2379");
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		etcd.close();
		super.tearDown();
	}

	@Override
	protected IDiscoveryLocator getDiscoveryLocator() {
		return Activator.getDefault().getDiscoveryLocator(containerUnderTest);
	}

	@Override
	protected IDiscoveryAdvertiser getDiscoveryAdvertiser() {
		return Activator.getDefault()
				.getDiscoveryAdvertiser(containerUnderTest);
	}

	protected void deleteAll() throws Exception {
		etcd.delete("\0");
	}

	public void testAdvertiseServiceInfo() throws Exception {
		IDiscoveryAdvertiser advertiser = getDiscoveryAdvertiser();
		advertiser.registerService(this.serviceInfo);
		// sleep for while
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		advertiser.unregisterAllServices();
		// sleep for while
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void testLocatorServiceInfo() throws Exception {
		IDiscoveryAdvertiser advertiser = getDiscoveryAdvertiser();
		advertiser.registerService(this.serviceInfo);
		// sleep for while
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		IDiscoveryLocator locator = getDiscoveryLocator();
		IServiceInfo[] serviceInfos = locator.getServices();
		assertTrue(serviceInfos.length > 0);

		advertiser.unregisterAllServices();
		// sleep for while
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		locator = getDiscoveryLocator();
		serviceInfos = locator.getServices();
		assertTrue(serviceInfos.length == 0);
	}
	

	public void testGetRequestSucceed() throws Exception {
		etcd.put("foo", "bar", 4);
		Map<String, String> map = etcd.get("foo", false);
		map.clear();
	}
		
//// 	ETCDv3 API has a flat key space so there is no recursive requests. Need to replace with a range request?
//	public void testGetRequestSucceedRecursive() throws Exception {
//		System.out.println("testGetRequestSucceedRecursive(" + GET_SUCCEED
//				+ ")");
//		EtcdResponse response = new EtcdGetRequest(GET_SUCCEED, true).execute();
//		assertFalse(response.isError());
//		System.out.println("testGetRequestSucceedRecursive(response="
//				+ response.getSuccessResponse() + ")");
//	}


	
	//A put request is always successful
	public void testCreateSucceed() throws Exception {
		etcd.put("foo", "bar", 3);
		//etcd.put("foo", "bar");
		
		Map<String, EventType> watchEvents = new HashMap<>();
		
		Listener listener = Watch.listener(response -> {
			for(WatchEvent event : response.getEvents()) {
				EventType eventType = event.getEventType();
				try {
					String json = new String(event.getKeyValue().getValue().getBytes());
					EtcdSuccessResponse successReponse = new EtcdSuccessResponse(json, new HashMap<>());
					synchronized (watchEvents) {
						watchEvents.put("Event", eventType);//$NON-NLS-1$
						watchEvents.notifyAll();					
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		});
		
		etcd.watch("foo", listener);
		//Thread.sleep(1000);
		//etcd.delete("foo");
		//etcd.closeWatch();
		
		try {
			synchronized (watchEvents) {
				if (watchEvents.isEmpty()) {
					watchEvents.wait(20000);
				}
			}
			assertFalse(watchEvents.isEmpty());
			if(watchEvents.get("Event") == EventType.DELETE) { //$NON-NLS-1$
				etcd.closeWatch("foo");
			}
		} catch (InterruptedException e) {
			//logEtcdError("watchJob.run", "Unexpected exception in watch job", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void testSerializeAndDeserializeServiceInfo() throws Exception {

		EtcdServiceInfo sinfo = new EtcdServiceInfo(serviceInfo);

		String s = sinfo.serializeToJsonString();
		assertNotNull(s);

		EtcdServiceInfo newSinfo = EtcdServiceInfo.deserializeFromString(s);

		assertNotNull(newSinfo);

		IServiceID sid1 = sinfo.getServiceID();
		IServiceID sid2 = newSinfo.getServiceID();
		assertTrue(sid1.getServiceTypeID().equals(sid2.getServiceTypeID()));
		assertTrue(sid1.equals(sid2));
		assertTrue(sinfo.getLocation().equals(newSinfo.getLocation()));
		assertTrue(sinfo.getServiceName().equals(newSinfo.getServiceName()));
		assertTrue(sinfo.getPriority() == newSinfo.getPriority());
		assertTrue(sinfo.getWeight() == newSinfo.getWeight());
		assertTrue(sinfo.getTTL() == newSinfo.getTTL());
		// get and compare service properties
		IServiceProperties sp1 = sinfo.getServiceProperties();
		IServiceProperties sp2 = newSinfo.getServiceProperties();
		assertTrue(sp1.size() == sp2.size());
		for (Enumeration<?> e1 = sp1.getPropertyNames(); e1.hasMoreElements();) {
			String key = (String) e1.nextElement();
			assertTrue(foundKey(sp2.getPropertyNames(), key));
			// try bytes
			byte[] b1 = sp1.getPropertyBytes(key);
			if (b1 != null) {
				compareByteArray(b1, sp2.getPropertyBytes(key));
			} else {
				String s1 = sp1.getPropertyString(key);
				if (s1 != null) {
					assertTrue(s1.equals(sp2.getPropertyString(key)));
				} else {
					Object o1 = sp1.getProperty(key);
					Object o2 = sp2.getProperty(key);
					assertTrue(o1.getClass().equals(o2.getClass()));
					assertTrue(o1.equals(o2));
				}
			}
		}
	}

	void compareByteArray(byte[] b1, byte[] b2) {
		// compare size
		assertTrue(b1.length == b2.length);
		for (int i = 0; i < b1.length; i++)
			if (b1[i] != b2[i])
				fail("bytes i=" + i + " of b1=" + Arrays.asList(b1) + " b2="
						+ Arrays.asList(b2) + " not equal");
	}

	boolean foundKey(Enumeration<?> e, String key) {
		for (; e.hasMoreElements();) {
			String el = (String) e.nextElement();
			if (key.equals(el))
				return true;
		}
		return false;
	}
	
	public void testDeleteAll() throws Exception {
		deleteAll();
	}
}
