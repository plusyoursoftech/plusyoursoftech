/**
 * Copyright (C) 2017 Alfresco Software Limited.
 * <p/>
 * This file is part of the Alfresco SDK project.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alfresco.api.platforms;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * A demonstration Java controller for the Hello World Web Script.
 *
 * @author martin.bergljung@alfresco.com
 * @since 2.1.0
 */
public class ExtractDataWebScript extends AbstractWebScript {
	private static Log logger = LogFactory.getLog(ExtractDataWebScript.class);

	@Autowired
	private NodeService nodeService;
	@Autowired
	private PermissionService permissionService;

	@Autowired
	private ContentService contentService;

	StringBuilder listOfNodeInfo = new StringBuilder();

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		String nodeId = req.getParameter("nodeId");
		System.out.println("<-------------------nodeId--------------------------------------------------> : " + nodeId);
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE + "/" + nodeId);
		getNodeDetails(nodeRef);
		System.out.println("listOfNodeInfo : " + listOfNodeInfo);
		uploadFile(nodeRef, "Report_" + System.currentTimeMillis() + ".csv", listOfNodeInfo.toString());
	}

	private void getNodeDetails(NodeRef nodeRef) {
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
		System.out.println(" childAssocs -> " + childAssocs);

		if (childAssocs != null) {
			childAssocs.forEach(childRef -> {
				NodeRef nr = childRef.getChildRef();
				System.out.println(" nr -> " + nr);
				QName nodeType = nodeService.getType(nr);
				System.out.println(" nodeType -> " + nodeType);
				if (nodeType.equals(ContentModel.TYPE_CONTENT)) {
					String nodeId = nr.getId();
					listOfNodeInfo.append(nodeId);
					listOfNodeInfo.append(",");

					Serializable name = nodeService.getProperty(nr, ContentModel.PROP_NAME);
					listOfNodeInfo.append(name);
					listOfNodeInfo.append(",");

					Serializable title = nodeService.getProperty(nr, ContentModel.PROP_TITLE);
					listOfNodeInfo.append(title != null ? title : "");
					listOfNodeInfo.append(",");

					Serializable desc = nodeService.getProperty(nr, ContentModel.PROP_DESCRIPTION);
					listOfNodeInfo.append(desc != null ? desc : "");
					listOfNodeInfo.append(",");

					Date created = (Date) nodeService.getProperty(nr, ContentModel.PROP_CREATED);
					;
					SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
					String formatCreated = ft.format(created);

					listOfNodeInfo.append(formatCreated);
					listOfNodeInfo.append(",");

					Serializable creator = nodeService.getProperty(nr, ContentModel.PROP_CREATOR);
					listOfNodeInfo.append(creator);
					listOfNodeInfo.append(",");

					Date modified = (Date) nodeService.getProperty(nr, ContentModel.PROP_MODIFIED);
					
					formatCreated = ft.format(modified);

					listOfNodeInfo.append(formatCreated);
					listOfNodeInfo.append(",");

					Serializable modifier = nodeService.getProperty(nr, ContentModel.PROP_MODIFIER);
					listOfNodeInfo.append(modifier);
					listOfNodeInfo.append(",");

					ContentData contentData = (ContentData) nodeService.getProperty(nr, ContentModel.PROP_CONTENT);
					String infoUrl = contentData.getInfoUrl();
					listOfNodeInfo.append(infoUrl);
					listOfNodeInfo.append(",");
					System.out.println(" listOfNodeInfo row -> " + listOfNodeInfo);
					String displayPathofANode = getDisplayPath(nr);
					listOfNodeInfo.append(displayPathofANode);
					listOfNodeInfo.append("\n");
				} else {
					getNodeDetails(nr);
				}
			});
		}
	}

	private void uploadFile(NodeRef parentNodeRef, String fileName, String bodyData) {

		Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
		props.put(ContentModel.PROP_NAME, fileName);
		NodeRef node = nodeService.createNode(parentNodeRef, ContentModel.ASSOC_CONTAINS,
				QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, fileName), ContentModel.TYPE_CONTENT, props)
				.getChildRef();
		ContentWriter writer = contentService.getWriter(node, ContentModel.PROP_CONTENT, true);
		writer.setEncoding("UTF-8");
		writer.setMimetype("text/csv");

		writer.putContent(bodyData);

	}

	private String getDisplayPath(final NodeRef nodeRef) {
		return AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<String>() {
			@Override
			public String doWork() {
				final Path nodePath = nodeService.getPath(nodeRef);
				return nodePath.toDisplayPath(nodeService, permissionService);
			}
		}, AuthenticationUtil.getSystemUserName());
	}
}