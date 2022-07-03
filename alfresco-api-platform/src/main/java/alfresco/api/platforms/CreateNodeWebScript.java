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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * A demonstration Java controller for the Hello World Web Script.
 *
 * @author martin.bergljung@alfresco.com
 * @since 2.1.0
 */
public class CreateNodeWebScript extends AbstractWebScript {
	private static Log logger = LogFactory.getLog(CreateNodeWebScript.class);

	@Autowired
	private NodeService nodeService;
	
	@Autowired
	private ContentService contentService;
	@Autowired
	@Qualifier("policyBehaviourFilter")
	private BehaviourFilter behaviourFilter;
	@Autowired
	private FileFolderService fileFolderService;
	StringBuilder listOfNodeInfo = new StringBuilder();

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		// TODO Auto-generated method stub
		String reportNodeId = req.getParameter("reportNodeId");
		String destNodeId = req.getParameter("destNodeId");
		System.out.println("<-------------------reportNodeId--------------------------------------------------> : "
				+ reportNodeId);
		System.out.println(
				"<-------------------destNodeId--------------------------------------------------> : " + destNodeId);
		NodeRef reportNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE + "/" + reportNodeId);
		NodeRef destNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE + "/" + destNodeId);
		ContentReader contentReader = contentService.getReader(reportNodeRef, ContentModel.PROP_CONTENT);
		InputStream contentInputStream = contentReader.getContentInputStream();

		Reader reader = new InputStreamReader(contentInputStream);
		BufferedReader br = new BufferedReader(reader);
		String line = "";
		String splitBy = ",";
		while ((line = br.readLine()) != null) // returns a Boolean value
		{
			String[] nodeProperties = line.split(splitBy); // use comma as separator
			
			listOfNodeInfo.append(nodeProperties);
			String createNode = createNode(destNodeRef, nodeProperties);
			listOfNodeInfo.append(",");
			listOfNodeInfo.append(createNode);
			listOfNodeInfo.append("\n");
		}
		uploadFile(destNodeRef, "Report_" + System.currentTimeMillis() + ".csv", listOfNodeInfo.toString());
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

	private String createNode(NodeRef nodeRef, String[] nodeProperties) {
		Map<QName, Serializable> properties = getProperties(nodeProperties);
		System.out.println("Create Node Properties: " + properties);
		behaviourFilter.disableBehaviour(nodeRef, ContentModel.ASPECT_AUDITABLE);
		nodeRef = createFolderStructure(nodeProperties[9], nodeRef);
		ChildAssociationRef createNode = nodeService.createNode(nodeRef, ContentModel.ASSOC_CONTAINS,
				ContentModel.TYPE_CONTENT, ContentModel.TYPE_CONTENT, properties);

		return createNode.getChildRef().getId();
	}

	private NodeRef createFolderStructure(String manifestKey, NodeRef nodeRef) {
		String[] folderPath = manifestKey.split("/");

		for (int start = 1; start < folderPath.length; start++) {
			String folder = folderPath[start];
			System.out.println("folder: " + folder);
			nodeRef = createFolder(nodeRef, folder);
		}
		return nodeRef;
	}

	private NodeRef createFolder(NodeRef nodeRef, String folder) {
		try {
			FileInfo appInfo = fileFolderService.create(nodeRef, folder, ContentModel.TYPE_FOLDER);
			nodeRef = appInfo.getNodeRef();

		} catch (Exception e) {
			nodeRef = fileFolderService.searchSimple(nodeRef, folder);
		}
		return nodeRef;
	}

	private Map<QName, Serializable> getProperties(String[] nodeProperties) {
		Map<QName, Serializable> properties = new HashMap<>();
		properties.put(ContentModel.PROP_NAME, nodeProperties[1]);
		properties.put(ContentModel.PROP_TITLE, nodeProperties[2]);
		properties.put(ContentModel.PROP_DESCRIPTION, nodeProperties[3]);
		properties.put(ContentModel.PROP_CREATED, nodeProperties[4]);
		properties.put(ContentModel.PROP_CREATOR, nodeProperties[5]);
		properties.put(ContentModel.PROP_MODIFIED, nodeProperties[6]);
		properties.put(ContentModel.PROP_MODIFIER, nodeProperties[7]);
		properties.put(ContentModel.PROP_CONTENT, nodeProperties[8]);
		return properties;
	}
}