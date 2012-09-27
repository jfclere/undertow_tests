/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.undertow.loadtest;

import io.undertow.server.HttpOpenListener;
import io.undertow.server.HttpTransferEncodingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ResourceLoader;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;

import javax.servlet.ServletException;

import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.ConnectedStreamChannel;

public class ServerMain {
	
	 private static Xnio xnio;
	 private static XnioWorker worker;
	 private static AcceptingChannel<? extends ConnectedStreamChannel> server;
	 
	 private static HttpOpenListener openListener;
	
	public static void main(String [ ] args) {
		xnio = Xnio.getInstance("nio");
        try {
			worker = xnio.createWorker(OptionMap.builder()
			        .set(Options.WORKER_WRITE_THREADS, 4)
			        .set(Options.WORKER_READ_THREADS, 4)
			        .set(Options.CONNECTION_HIGH_WATER, 1000000)
			        .set(Options.CONNECTION_LOW_WATER, 1000000)
			        .set(Options.WORKER_TASK_CORE_THREADS, 10)
			        .set(Options.WORKER_TASK_MAX_THREADS, 12)
			        .set(Options.TCP_NODELAY, true)
			        .set(Options.CORK, true)
			        .getMap());
		} catch (IllegalArgumentException e) {
			System.out.println("failed: " + e);
		} catch (IOException e) {
			System.out.println("failed: " + e);
		}
        OptionMap serverOptions = OptionMap.builder()
                .set(Options.WORKER_ACCEPT_THREADS, 4)
                .set(Options.TCP_NODELAY, true)
                .set(Options.REUSE_ADDRESSES, true)
                .getMap();
        openListener = new HttpOpenListener(new ByteBufferSlicePool(BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR, 8192, 8192 * 8192));
        ChannelListener acceptListener = ChannelListeners.openListenerAdapter(openListener);
        try {
			server = worker.createStreamServer(new InetSocketAddress(8080), acceptListener, serverOptions);
		} catch (IOException e) {
			System.out.println("failed: " + e);
		}
        server.resumeAccepts();

        final PathHandler root = new PathHandler();
        final ServletContainer container = ServletContainer.Factory.newInstance();

        ServletInfo s = new ServletInfo("SimpleServlet", SimpleServlet.class)
        		.addMapping("/SimpleServlet");


        DeploymentInfo builder = new DeploymentInfo()
        		.setClassLoader(ServerMain.class.getClassLoader())
                .setContextPath("/SimpleServlet")
                .setDeploymentName("SimpleServlet.war")
                .setResourceLoader(NOOP_RESOURCE_LOADER)
                .addServlet(s);
   
        DeploymentManager manager = container.addDeployment(builder);
        manager.deploy();
        try {
        	root.addPath(builder.getContextPath(), manager.start());
		} catch (ServletException e) {
			System.out.println("failed: " + e);
		}
        
        final HttpTransferEncodingHandler ph = new HttpTransferEncodingHandler();
        ph.setNext(root);
        openListener.setRootHandler(ph);

	}
	
    public static final ResourceLoader NOOP_RESOURCE_LOADER  = new ResourceLoader() {
        @Override
        public File getResource(final String resource) {
            return null;
        }
    };
}
