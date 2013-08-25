package pl.mkubala.xml

import org.specs2.mutable._

object NodeAttributesReaderTest extends Specification with NodeAttributesReader {

  "getStringAttr" should {

    "parse Some(\"someValue\")" in {
      val node = <label attr="someValue"/>
      node.getStringAttr("attr") must_== Some("someValue")
    }

    "parse None" in {
      val node = <label/>
      node.getStringAttr("attr") must_== None
    }

    "return default value" in {
      val node = <label/>
      node.getStringAttr("attr", Some("defaultValue")) must_== Some("defaultValue")
    }

  }

}