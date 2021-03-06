/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.zipkin;

import java.net.InetAddress;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.util.InetUtils;

import com.twitter.zipkin.gen.Endpoint;

/**
 * An {@link EndpointLocator} that tries to find local service information from a
 * {@link DiscoveryClient}.
 *
 * @author Dave Syer
 *
 */
public class DiscoveryClientEndpointLocator implements EndpointLocator {

	private DiscoveryClient client;

	public DiscoveryClientEndpointLocator(DiscoveryClient client) {
		this.client = client;
	}

	@Override
	public Endpoint locate(Span span) {
		ServiceInstance instance = this.client.getLocalServiceInstance();
		return new Endpoint(getIpAddress(instance),
				new Integer(instance.getPort()).shortValue(), instance.getServiceId());
	}

	private int getIpAddress(ServiceInstance instance) {
		try {
			InetAddress address = InetAddress.getByName(instance.getHost());
			return InetUtils.convert(address).getIpAddressAsInt();
		}
		catch (Exception e) {
			return 0;
		}
	}

}
