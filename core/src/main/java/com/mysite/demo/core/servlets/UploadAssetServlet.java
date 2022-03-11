
/**
 * AEM AssetsをS3のようなPS API Storageにするサーブレット
 * こんな感じでPS APIのoutputに指定ができる
 * https://aem1.cqsvr.com/bin/uploadasset?createVersion=true&destinationPath=/content/dam/tmp/psapi-nodejs-result.png
 * 
 * System consoleで設定 
 1. Sling Authentication ServiceでAllow Anonymous AccessをON (PID: org.apache.sling.engine.impl.auth.SlingAuthenticator)
 2. Apache Sling Login Admin WhitelistのWhitelist regexpでstorageservlet.coreを登録(PID: org.apache.sling.jcr.base.internal.LoginAdminWhitelist)
 */

// package com.adobe.tvdemokit.core.servlets;
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

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
// @Component(service = { Servlet.class })
@Component(service = { Servlet.class }, immediate = true, property = { "sling.servlet.paths=" + "/bin/uploadasset" })

@ServiceDescription("Simple Demo Servlet")
public class UploadAssetServlet extends SlingAllMethodsServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(UploadAssetServlet.class);

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Override
	protected void doGet(final SlingHttpServletRequest req,
			final SlingHttpServletResponse resp) throws ServletException, IOException {
		final Resource resource = req.getResource();
		resp.setContentType("text/plain");
		resp.getWriter().write("Message from getMethod in /bin/uploadasset servlet");
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
