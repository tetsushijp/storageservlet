/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.mysite.demo.core.servlets;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.LoginException;
import com.day.cq.dam.api.AssetManager;

import javax.jcr.Session;
import javax.jcr.Binary;
import javax.jcr.ValueFactory;

import com.day.cq.dam.api.Asset;
import java.io.InputStream;
import javax.servlet.ServletOutputStream;

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
// @Component(service = { Servlet.class })
@Component(service = { Servlet.class }, immediate = true, property = { "sling.servlet.paths=" + "/bin/downloadasset" })

@ServiceDescription("Simple Download Asset Servlet")
public class DownloadAssetServlet extends SlingAllMethodsServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadAssetServlet.class);

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Override
	protected void doGet(final SlingHttpServletRequest req,
			final SlingHttpServletResponse resp) throws ServletException, IOException {
		final Resource resource = req.getResource();

		ResourceResolver resourceResolver = req.getResourceResolver();

		// http://localhost:4502/bin/downloadasset?assetPath=/content/dam/storageservlet/AdobeStock_1.jpg
		String assetFilePath = req.getParameter("assetPath");
		// Resource imgRes =
		// resourceResolver.getResource("/content/dam/storageservlet/AdobeStock_1.jpg");
		Resource imgRes = resourceResolver.getResource(assetFilePath);
		Asset imgAsset = imgRes.adaptTo(Asset.class);
		Resource original = imgAsset.getOriginal();
		String mimeType = imgAsset.getMimeType();
		InputStream stream = original.adaptTo(InputStream.class);
		long imgSize = imgAsset.getOriginal().getSize();
		String fileName = imgAsset.getName();
		/////////////////
		resp.setContentType(mimeType != null ? mimeType : "application/octet-stream");
		resp.setContentLength((int) imgSize);
		// resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName +
		// "\"");

		ServletOutputStream os = resp.getOutputStream();
		byte[] bufferData = new byte[1024];
		int read = 0;
		while ((read = stream.read(bufferData)) != -1) {
			os.write(bufferData, 0, read);
		}
		os.flush();
		os.close();
		stream.close();

		/////////////////
		// resp.setContentType("text/plain");
		// resp.getWriter().write("Message from getMethod in /bin/downloaddasset servlet
		///////////////// 2");

		// resp.getWriter().write("Title = " +
		// resource.getValueMap().get(JcrConstants.JCR_TITLE));
	}

	@SuppressWarnings("all")
	@Override
	protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		try {
			LOGGER.info(" == START Asset upload ==");
			String destinationPath = request.getParameter("destinationPath");
			boolean createVersion = Boolean.valueOf(request.getParameter("createVersion"));
			LOGGER.info(" dest path ==> " + destinationPath);
			LOGGER.info(" cre version ==> " + createVersion);
			ResourceResolver resourceResolver = getResourceResolver();
			AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
			Session session = resourceResolver.adaptTo(Session.class);
			ValueFactory factory = session.getValueFactory();
			Binary binary = factory.createBinary(request.getInputStream());
			LOGGER.info(" binary size ==> " + binary.getSize() + " ");
			assetManager.createOrUpdateAsset(destinationPath, binary, "image/png", true, createVersion, "revisionLabel",
					"revisionComment");
			resourceResolver.commit();
			binary.dispose();
		} catch (Exception exception) {
			LOGGER.error("Error creating Asset", exception);
		}
	}

	@SuppressWarnings("all")
	private ResourceResolver getResourceResolver() throws LoginException {
		return resourceResolverFactory.getAdministrativeResourceResolver(null);
	}

}
