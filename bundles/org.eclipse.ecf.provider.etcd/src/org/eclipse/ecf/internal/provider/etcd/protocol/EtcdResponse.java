/*******************************************************************************
 * Copyright (c) 2014 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.internal.provider.etcd.protocol;

public abstract class EtcdResponse extends EtcdProtocol {
	public abstract boolean isError();

	public abstract EtcdSuccessResponse getSuccessResponse();

	public abstract EtcdErrorResponse getErrorResponse();
}
