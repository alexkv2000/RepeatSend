package ru.kvo.Utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

@Component
@Slf4j
public class XmlBodyExtractor {

    /**
     * Извлекает содержимое тега Body с использованием DOM парсера
     */
    public String extractBodyWithDom(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return "";
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            // Защита от XXE атак
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            // Ищем тег Body (регистронезависимо)
            NodeList bodyNodes = doc.getElementsByTagName("Body");
            if (bodyNodes.getLength() > 0) {
                Element bodyElement = (Element) bodyNodes.item(0);
                return bodyElement.getTextContent();
            }

            // Если не нашли, пробуем найти в нижнем регистре
            bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                Element bodyElement = (Element) bodyNodes.item(0);
                return bodyElement.getTextContent();
            }

            return "";

        } catch (Exception e) {
            log.error("Ошибка при парсинге XML: {}", e.getMessage());
            // Пробуем regex как запасной вариант
            return extractBodyWithRegex(xml);
        }
    }

    /**
     * Извлекает содержимое тега Body с помощью регулярных выражений
     */
    public String extractBodyWithRegex(String xml) {
        // Пробуем разные паттерны для разных форматов XML
        String[] patterns = {
                "<Body[^>]*>(.*?)</Body>",
                "<body[^>]*>(.*?)</body>",
                "\"Body\"\\s*:\\s*\"(.*?)\"",
                "'Body'\\s*:\\s*'(.*?)'"
        };

        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern,
                    java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(xml);

            if (m.find()) {
                String content = m.group(1);
                // Убираем экранирование если нужно
                content = content.replace("\\\"", "\"")
                        .replace("\\'", "'")
                        .replace("\\\\", "\\");
                return content;
            }
        }

        return "";
    }
}