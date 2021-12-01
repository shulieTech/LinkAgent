/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.log4j.interceptor.utils;

import com.pamirs.attach.plugin.log4j.interceptor.v2.holder.Cache;
import com.pamirs.pradar.Pradar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

/**
 * @Auther: vernon
 * @Date: 2020/12/14 13:28
 * @Description:
 */
public class XmlUtils {
    private static Logger logger = LoggerFactory.getLogger(XmlUtils.class);

    public static String generator(String fileName, String bizShadowLogPath) {
        try {
            File file = new File(fileName);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 解析文档，形成文档树，也就是生成Document对象，此处这个file也可以http的url
            Document document = builder.parse(file);

            // 获得根节点
            Element rootElement = document.getDocumentElement();

            // 获得根节点下的所有子节点
            NodeList data = rootElement.getChildNodes();
            for (int i = 0; i < data.getLength(); i++) {
                Node childNode = data.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) childNode;
                    if ("appenders".equalsIgnoreCase(childElement.getNodeName())) {
                        NodeList innerNodes = childNode.getChildNodes();
                        for (int j = 0; j < innerNodes.getLength(); j++) {
                            Node innerChild = innerNodes.item(j);
                            if ("FILE".equalsIgnoreCase(innerChild.getNodeName())
                                    || "RollingFile".equalsIgnoreCase(innerChild.getNodeName())) {
                                Element oldElement = (Element) (innerChild);
                                Node newNode = oldElement.cloneNode(true);

                                Element newElement = (Element) (newNode);
                                String oldElementName = oldElement.getAttribute("name");
                                if (oldElementName.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
                                    continue;
                                }

                                String oldFilePath = oldElement.getAttribute("fileName");
                                newElement.setAttribute("name", Pradar.CLUSTER_TEST_PREFIX + oldElementName);
                                newElement.setAttribute("fileName", bizShadowLogPath + oldFilePath);
                                childNode.appendChild(newNode);
                                Cache.DocumentCache.put(Pradar.CLUSTER_TEST_PREFIX + oldElementName
                                        , newNode);
                            }
                        }
                    }
                    if ("loggers".equalsIgnoreCase(childElement.getNodeName())) {
                        NodeList innerNodes = childNode.getChildNodes();
                        for (int j = 0; j < innerNodes.getLength(); j++) {
                            Node innerChild = innerNodes.item(j);
                            if ("logger".equalsIgnoreCase(innerChild.getNodeName())
                                    || "root".equalsIgnoreCase(innerChild.getNodeName())) {
                                for (int k = 0; k < innerChild.getChildNodes().getLength(); k++) {
                                    Node lastestChild = innerChild.getChildNodes().item(k);
                                    if ("appender-ref".equalsIgnoreCase(lastestChild.getNodeName())) {
                                        Element lastestElememt = ((Element) lastestChild);
                                        String ref = lastestElememt.getAttribute("ref");
                                        if (ref.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
                                            continue;
                                        }
                                        if (Cache.DocumentCache.get(Pradar.CLUSTER_TEST_PREFIX + ref) != null) {
                                            Node newNode = lastestElememt.cloneNode(true);
                                            ((Element) newNode).setAttribute("ref", Pradar.CLUSTER_TEST_PREFIX + ref);
                                            innerChild.appendChild(newNode);
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
            return write(document, fileName);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;

    }

    public static String write(Document document, String filePath) {
        // 获得Transformer对象，用于输出文档
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            // 封装成DOMResource对象
            DOMSource domSource = new DOMSource(document);
            String newFile = generate(filePath, ".xml");
            File file = new File(newFile);
            if (file.exists()) {
                file.delete();
            }
            Result result = new StreamResult(newFile);        // 输出结果

            transformer.transform(domSource, result);
            return newFile;
        } catch (Exception e) {

        }

        return null;

    }

    static public String generate(String file, String suffix) {
        return file.substring(0, file.indexOf(suffix)) + "pradar-new" + suffix;
    }
}
