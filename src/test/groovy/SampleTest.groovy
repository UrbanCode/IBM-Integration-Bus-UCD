package src.test.groovy

import groovy.util.GroovyTestCase

import com.urbancode.sync.*;
import com.urbancode.plugin.*;

import spock.lang.Specification
import spock.lang.*

import com.urbancode.plugin.test.mocks.*;
import com.urbancode.plugin.test.data.*;

@Unroll
class SampleTest extends Specification{

    /*
        Constructor Tests
    */
    def "Rally Helper can instantiate without proxy settings"(){
        setup:
            def a = 3
            def b = 4
            println("Hello")
        expect:
            b > a
    };
}