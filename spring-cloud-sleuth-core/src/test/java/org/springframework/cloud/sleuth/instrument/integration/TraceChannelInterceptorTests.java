/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.sleuth.instrument.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.cloud.sleuth.Trace.NOT_SAMPLED_NAME;
import static org.springframework.cloud.sleuth.Trace.SPAN_ID_NAME;
import static org.springframework.cloud.sleuth.Trace.TRACE_ID_NAME;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.sleuth.Trace;
import org.springframework.cloud.sleuth.TraceContextHolder;
import org.springframework.cloud.sleuth.TraceScope;
import org.springframework.cloud.sleuth.instrument.integration.TraceChannelInterceptorTests.App;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
@IntegrationTest
@DirtiesContext
public class TraceChannelInterceptorTests implements MessageHandler {

	@Autowired
	@Qualifier("channel")
	private DirectChannel channel;

	@Autowired
	private Trace trace;

	private Message<?> message;

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		this.message = message;
	}

	@Before
	public void init() {
		this.channel.subscribe(this);
	}

	@After
	public void close() {
		TraceContextHolder.removeCurrentSpan();
		this.channel.unsubscribe(this);
	}

	@Test
	public void testNoSpanCreation() {
		this.channel.send(MessageBuilder.withPayload("hi").setHeader(NOT_SAMPLED_NAME, "")
				.build());
		assertNotNull("message was null", this.message);

		String spanId = this.message.getHeaders().get(SPAN_ID_NAME, String.class);
		assertNull("spanId was not null", spanId);
	}

	@Test
	public void testSpanCreation() {
		this.channel.send(MessageBuilder.withPayload("hi").build());
		assertNotNull("message was null", this.message);

		String spanId = this.message.getHeaders().get(SPAN_ID_NAME, String.class);
		assertNotNull("spanId was null", spanId);

		String traceId = this.message.getHeaders().get(TRACE_ID_NAME, String.class);
		assertNotNull("traceId was null", traceId);
		assertNull(TraceContextHolder.getCurrentSpan());
	}

	@Test
	public void testHeaderCreation() {
		TraceScope traceScope = this.trace.startSpan("testSendMessage",
				new AlwaysSampler(), null);
		this.channel.send(MessageBuilder.withPayload("hi").build());
		traceScope.close();
		assertNotNull("message was null", this.message);

		String spanId = this.message.getHeaders().get(SPAN_ID_NAME, String.class);
		assertNotNull("spanId was null", spanId);

		String traceId = this.message.getHeaders().get(TRACE_ID_NAME, String.class);
		assertNotNull("traceId was null", traceId);
		assertNull(TraceContextHolder.getCurrentSpan());
	}

	@Configuration
	@EnableAutoConfiguration
	static class App {

		@Bean
		public DirectChannel channel() {
			return new DirectChannel();
		}

		@Bean
		public AlwaysSampler alwaysSampler() {
			return new AlwaysSampler();
		}

	}
}
