/*******************************************************************************
 * Copyright (c) 2014 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.etcd;

/*******************************************************************************
 * Copyright (c) 2014 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.json.JSONObject;

public class EtcdResponse extends AbstractEtcdResponse {

	private Map<String, Object> responseHeaders;

	private final String action;
	private final EtcdNode etcdNode;
	private final EtcdNode previousNode;

	public EtcdResponse(String json, Map<String, Object> headers)
			throws Exception {
		JSONObject jsonObject = new JSONObject(json);
		this.action = jsonObject.getString(ACTION_KEY);
		Assert.isNotNull(this.action,
				"action field in response must not be null"); //$NON-NLS-1$
		this.etcdNode = new EtcdNode(jsonObject.getJSONObject(NODE_KEY));
		JSONObject jobj = jsonObject.optJSONObject(PREVIOUSNODE_KEY);
		this.previousNode = (jobj != null) ? new EtcdNode(jobj) : null;
		this.responseHeaders = headers;
	}

	public Map<String, Object> getHeaders() {
		return responseHeaders;
	}

	public String getAction() {
		return action;
	}

	public EtcdNode getNode() {
		return etcdNode;
	}

	public EtcdNode getPreviousNode() {
		return previousNode;
	}

	@Override
	public String toString() {
		return "EtcdResponse [responseHeaders=" + responseHeaders + ", action=" //$NON-NLS-1$ //$NON-NLS-2$
				+ action + ", etcdNode=" + etcdNode + ", previousNode=" //$NON-NLS-1$ //$NON-NLS-2$
				+ previousNode + "]"; //$NON-NLS-1$
	}

}
