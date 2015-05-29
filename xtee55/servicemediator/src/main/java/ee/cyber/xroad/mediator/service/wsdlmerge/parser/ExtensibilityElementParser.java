package ee.cyber.xroad.mediator.service.wsdlmerge.parser;

import java.util.List;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

abstract class ExtensibilityElementParser {
    private List<?> extensibilityElements;
    private QName searchableElement;

    ExtensibilityElementParser(List<?> extensibilityElements,
            QName searchableElement) {
        this.extensibilityElements = extensibilityElements;
        this.searchableElement = searchableElement;
    }

    String parse() {
        for (Object eachExt : extensibilityElements) {
            ExtensibilityElement extElement = (ExtensibilityElement) eachExt;

            if (searchableElement.equals(extElement.getElementType())) {
                return parseContent(extElement);
            }
        }

        return null;
    }

    protected abstract String parseContent(
            ExtensibilityElement element);
}
