package com.budjb.rabbitmq.test.converter

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass

import com.budjb.rabbitmq.converter.IntegerMessageConverter
import com.budjb.rabbitmq.converter.MessageConverter
import com.budjb.rabbitmq.converter.MessageConverterManager
import com.budjb.rabbitmq.converter.StringMessageConverter

import org.springframework.context.ApplicationContext

import spock.lang.Specification

class MessageConverterManagerSpec extends Specification {
    MessageConverterManager messageConverterManager
    GrailsApplication grailsApplication
    ApplicationContext applicationContext

    def setup() {
        grailsApplication = Mock(GrailsApplication)
        applicationContext = Mock(ApplicationContext)

        messageConverterManager = new MessageConverterManager()
        messageConverterManager.grailsApplication = grailsApplication
        messageConverterManager.applicationContext = applicationContext
    }

    def 'Ensure setGrailsApplication(GrailsApplication) sets the property correctly'() {
        setup:
        GrailsApplication grailsApplication = Mock(GrailsApplication)

        when:
        messageConverterManager.setGrailsApplication(grailsApplication)

        then:
        messageConverterManager.grailsApplication == grailsApplication
    }

    def 'Validate registering and retrieving message converters'() {
        setup:
        StringMessageConverter stringMessageConverter = Mock(StringMessageConverter)
        IntegerMessageConverter integerMessageConverter = Mock(IntegerMessageConverter)

        when:
        messageConverterManager.registerMessageConverter(integerMessageConverter)
        messageConverterManager.registerMessageConverter(stringMessageConverter)

        then:
        messageConverterManager.messageConverters.size() == 2

        when:
        List converters = messageConverterManager.getMessageConverters()

        then:
        converters.size() == 2
        converters == [integerMessageConverter, stringMessageConverter]
    }

    def 'Validate that all built-in converters can convert from bytes through the manager'() {
        setup:
        grailsApplication.getArtefacts('MessageConverter') >> []
        messageConverterManager.load()

        expect:
        messageConverterManager.convertFromBytes(a) == b

        where:
        a                                                                       || b
        [102, 111, 111, 98, 97, 114] as byte[]                                  || "foobar"
        [49, 50, 51, 52] as byte[]                                              || 1234
        [91, 34, 102, 111, 111, 34, 44, 34, 98, 97, 114, 34, 93] as byte[]      || ["foo", "bar"]
        [123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125] as byte[]    || ["foo": "bar"]
    }

    def 'Validate that all built-in converters can convert to bytes through the manager'() {
        setup:
        grailsApplication.getArtefacts('MessageConverter') >> []
        messageConverterManager.load()

        String inside = "foobar"

        expect:
        messageConverterManager.convertToBytes(a) == b

        where:
        a               || b
        "foobar"        || [102, 111, 111, 98, 97, 114] as byte[]
        1234            || [49, 50, 51, 52] as byte[]
        ["foo", "bar"]  || [91, 34, 102, 111, 111, 34, 44, 34, 98, 97, 114, 34, 93] as byte[]
        ["foo": "bar"]  || [123, 34, 102, 111, 111, 34, 58, 34, 98, 97, 114, 34, 125] as byte[]
        "${'foobar'}"   || [102, 111, 111, 98, 97, 114] as byte[]
    }

    def 'Ensure message converters are freed up with reset() is called'() {
        setup:
        grailsApplication.getArtefacts('MessageConverter') >> []
        messageConverterManager.load()

        when:
        messageConverterManager.reset()

        then:
        messageConverterManager.getMessageConverters().size() == 0
    }

    def 'If Grails reports custom message converters, make sure they\'re loaded'() {
        setup:
        ExpandoMockConverter customConverter = new ExpandoMockConverter()
        GrailsClass grailsClass = Mock(GrailsClass)
        grailsClass.getFullName() >> "ExpandoMockConverter"
        applicationContext.getBean("ExpandoMockConverter") >> customConverter
        grailsApplication.getArtefacts('MessageConverter') >> [grailsClass]

        when:
        messageConverterManager.load()

        then:
        messageConverterManager.getMessageConverters().size() == 6
        messageConverterManager.getMessageConverters().contains(customConverter) == true

        when:
        Object convertedFromBytes = messageConverterManager.convertFromBytes([102, 111, 111, 98, 97, 114, 98, 97, 122] as byte[])
        byte[] convertedFromObject = messageConverterManager.convertToBytes(new Expando())

        then:
        convertedFromBytes instanceof Expando
        convertedFromObject == [102, 111, 111, 98, 97, 114, 98, 97, 122] as byte[]
    }

    class ExpandoMockConverter extends MessageConverter {
        public String getContentType() {
            return null
        }

        public boolean canConvertFrom() {
            return true
        }

        public boolean canConvertTo() {
            return true
        }

        public byte[] convertFrom(Object source) {
            if (source instanceof Expando) {
                return [102, 111, 111, 98, 97, 114, 98, 97, 122]
            }
            return null
        }

        public Object convertTo(byte[] source) {
            if (source == [102, 111, 111, 98, 97, 114, 98, 97, 122] as byte[]) {
                return new Expando()
            }
            return null
        }
    }
}
