package pl.mkubala.messBuster.model.domain.field

import org.specs2.mutable.Specification
import pl.mkubala.messBuster.domain.model.field.Field

object FieldSpec extends Specification {

  "Field's implicit validators" should {
    "contains all default validators for field without any validators" in {
      val fieldNode = <string name="stringFieldName"></string>
      val field = Field(fieldNode)

      field.implicitValidators must_== field.fieldType.defaultValidators
    }

    "contains all default validators for field with validators other kind of the default ones" in {
      val fieldNode = <string name="stringFieldName">
                        <validatesRegex pattern="[0-9]*"/>
                      </string>
      val field = Field(fieldNode)

      field.implicitValidators must_== field.fieldType.defaultValidators
    }

    "not contains already defined field's validators" in {
      val fieldNode = <string name="stringFieldName">
                        <validatesLength max="500"/>
                      </string>
      val field = Field(fieldNode)

      field.implicitValidators must_== Set()
    }

    "contains default validator if the defined one has not upper bound" in {
      val fieldNode = <string name="stringFieldName">
                        <validatesLength min="50"/>
                      </string>
      val field = Field(fieldNode)

      field.implicitValidators must_== field.fieldType.defaultValidators
    }
  }

}