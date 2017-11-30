/*******************************************************************************
 * Copyright (c) 2014 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.internal.provider.etcd;

import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainerFactory;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.util.LogHelper;
import org.eclipse.ecf.core.util.SystemLogService;
import org.eclipse.ecf.discovery.IDiscoveryAdvertiser;
import org.eclipse.ecf.discovery.IDiscoveryLocator;
import org.eclipse.ecf.provider.etcd.EtcdDiscoveryContainer;
import org.eclipse.ecf.provider.etcd.EtcdDiscoveryContainerConfig;
import org.eclipse.ecf.provider.etcd.EtcdDiscoveryContainerInstantiator;
import org.eclipse.ecf.provider.etcd.identity.EtcdNamespace;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings("rawtypes")
public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "org.eclipse.ecf.provider.etcd"; //$NON-NLS-1$

	private static Activator plugin;

	public static Activator getDefault() {
		return plugin;
	}

	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	public Activator() {
		plugin = this;
	}

	private ContainerTypeDescription ctd = new ContainerTypeDescription(EtcdDiscoveryContainerInstantiator.NAME,
			new EtcdDiscoveryContainerInstantiator(), "Etcd Discovery Container", true, false); //$NON-NLS-1$

	// Logging
	private ServiceTracker logServiceTracker = null;
	private LogService logService = null;
	private Object logServiceTrackerLock = new Object();

	@SuppressWarnings("unchecked")
	public void start(BundleContext ctxt) throws Exception {
		context = ctxt;

		// Register Namespace and ContainerTypeDescription first
		context.registerService(Namespace.class, new EtcdNamespace(), null);
		context.registerService(ContainerTypeDescription.class, ctd, null);

		try {
			EtcdDiscoveryContainerConfig config = null;
			String hostname = System.getProperty(EtcdDiscoveryContainerConfig.ETCD_TARGETID_HOSTNAME_PROP);
			if (hostname == null) {
				LogUtility.logError("createEtcdDiscoveryContainer", DebugOptions.DEBUG, Activator.class, //$NON-NLS-1$
						"No etcd service hostname configured via system property=" //$NON-NLS-1$
								+ EtcdDiscoveryContainerConfig.ETCD_TARGETID_HOSTNAME_PROP
								+ ".  No EtcdDiscoveryContainer created"); //$NON-NLS-1$
				return;
			}
			config = new EtcdDiscoveryContainerConfig();
			EtcdDiscoveryContainer etcd = null;
			if (config != null)
				etcd = getContainer(config);
			if (etcd != null) {
				etcd.connect(null, null);
				final Hashtable props = new Hashtable();
				props.put(IDiscoveryLocator.CONTAINER_NAME, EtcdDiscoveryContainerInstantiator.NAME);
				props.put(Constants.SERVICE_RANKING, new Integer(500));
				context.registerService(
						new String[] { IDiscoveryAdvertiser.class.getName(), IDiscoveryLocator.class.getName() }, etcd,
						props);
			}
		} catch (Exception e1) {
			LogUtility.logError("start", DebugOptions.DEBUG, this.getClass(), "Etcd container creation failed", e1); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	public void stop(BundleContext context) throws Exception {
		if (cfTracker != null) {
			cfTracker.close();
			cfTracker = null;
		}
		if (container != null) {
			container.disconnect();
			container = null;
		}
		synchronized (logServiceTrackerLock) {
			if (logServiceTracker != null) {
				logServiceTracker.close();
				logServiceTracker = null;
				logService = null;
			}
		}
		context = null;
		plugin = null;
	}

	private EtcdDiscoveryContainer container;

	synchronized EtcdDiscoveryContainer getContainer(EtcdDiscoveryContainerConfig config)
			throws ContainerCreateException {
		if (context == null)
			return null;
		if (container == null)
			container = (EtcdDiscoveryContainer) getContainerFactory().createContainer(ctd,
					(Object[]) new Object[] { config });
		return container;
	}

	private ServiceTracker cfTracker;

	@SuppressWarnings("unchecked")
	synchronized IContainerFactory getContainerFactory() {
		BundleContext ctxt = context;
		if (ctxt == null)
			return null;
		if (cfTracker == null) {
			cfTracker = new ServiceTracker(ctxt, IContainerFactory.class, null);
			cfTracker.open();
		}
		return (IContainerFactory) cfTracker.getService();
	}

	@SuppressWarnings("unchecked")
	public LogService getLogService() {
		if (context == null)
			return null;
		synchronized (logServiceTrackerLock) {
			if (logServiceTracker == null) {
				logServiceTracker = new ServiceTracker(context, LogService.class.getName(), null);
				logServiceTracker.open();
			}
			logService = (LogService) logServiceTracker.getService();
			if (logService == null)
				logService = new SystemLogService(PLUGIN_ID);
			return logService;
		}
	}

	public void log(IStatus status) {
		if (logService == null)
			logService = getLogService();
		if (logService != null)
			logService.log(null, LogHelper.getLogCode(status), LogHelper.getLogMessage(status), status.getException());
	}

	public void log(ServiceReference sr, IStatus status) {
		log(sr, LogHelper.getLogCode(status), LogHelper.getLogMessage(status), status.getException());
	}

	public void log(ServiceReference sr, int level, String message, Throwable t) {
		if (logService == null)
			logService = getLogService();
		if (logService != null)
			logService.log(sr, level, message, t);
	}

}
