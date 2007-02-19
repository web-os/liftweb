package net.liftweb.mapper

/*                                                *\
 (c) 2006-2007 WorldWide Conferencing, LLC
 Distributed under an Apache License
 http://www.apache.org/licenses/LICENSE-2.0
\*                                                 */

import scala.collection.mutable._
import java.lang.reflect.Method
import java.sql.{ResultSet, Types}
import scala.xml.{Elem, Text, Node, NodeSeq}


trait MappedField[T <: Any,O] {
  def ignoreField = false
  def defaultValue : T
  
  /**
   * Get the JDBC SQL Type for this field
   */
  def getTargetSQLType(field : String) : int
  
  private var _dirty_? = false
  def dirty_? = !db_index_? && _dirty_?
  protected def dirty_?(b : boolean) = _dirty_? = b

  /**
    * override this method in indexed fields to indicate that the field has been saved
    */
  def db_index_field_indicates_saved_? = false
      
  def owner : Mapper[O]
  final def safe_? : boolean = {
    owner.safe_?
  }
  
  def write_permission_? = false
  def read_permission_? = false
  
  /**
   * pascal-style assignment for syntactic sugar
   */
  def :=(v : T) : T = {
    if (safe_? || write_permission_?) i_set_!(v)
    v
  }
  
  /**
    * Assignment from the underlying type.  It's ugly, but:<br />
    * field() = new_value <br />
    * field := new_value <br />
    * field set new_value <br />
    * field.set(new_value) <br />
    * are all the same
    */
  def update(v: T) {
    this := v
  }
  
  private var _name : String = null
  
  final def i_name_! = {_name}
  
  final def name = synchronized {
    if (_name == null) owner.checkNames
    _name
  }
  final def setName_!(newName : String) : String = {
    if(safe_?) _name = newName.toLowerCase
    _name
  }
  
  def displayName = name
  
  def resetDirty {
    if (safe_?) dirty_?(false)
  }
  
  def db_display_? = true
  
  /**
  * pascal-style assignment for syntactic sugar
  */
  def ::=(v : Any) : T
 
  /**
    * Create an input field for the item
    */
  def i : NodeSeq = {
    // <input type='text' name={S.ae({s => this ::= s(0)})} value={get.toString}/>
    <span>FIXME</span>
  }
  
  def set(value : T) : T = {
    if (safe_? || write_permission_?) i_set_!(value)
    else throw new Exception("Do not have permissions to set this field")
  }
  
  protected def i_set_!(value : T) : T
  
  def buildSetActualValue(accessor : Method, inst : AnyRef, columnName : String) : (Mapper[O], AnyRef) => unit 
  protected def getField(inst : Mapper[O], meth : Method) = meth.invoke(inst, null).asInstanceOf[MappedField[T,O]];
  
  def get : T = {
    if (safe_? || read_permission_?) i_get_!
    else i_obscure_!(i_get_!)
  }
  
  protected def i_get_! : T
  
  // def changed_? = {i_get_! != defaultValue}
  
  protected def i_obscure_!(in : T) : T
  
  def asString = displayName + "=" + get match {
    case null => ""
    case v @ _ => v.toString}
  
  def db_column_count = 1
  
  def db_column_names(in : String) = List(in.toLowerCase)
  
  def db_index_? = false

  def db_autogenerated_index_? = false
  
  def getJDBCFriendly(field : String) : Object
  
  override def toString : String = {
    val t = get
    if (t == null) "" else t.toString
  }
  
  def sws_validate : List[ValidationIssues[T,O]] = Nil

  def convertToJDBCFriendly(value: T): Object

  def asHtml : Node = Text(asString)
}

object MappedField {
  implicit def mapToType[T, A](in : MappedField[T, A]) : T = in.get
  implicit def mapFromOption[T, A](in : Option[MappedField[T, A]]) : MappedField[T,A] = in.get
}

case class ValidationIssues[T,O](field : MappedField[T,O], msg : String)

trait IndexedField[O] {
  def convertKey(in : String) : Option[O]
  def convertKey(in : int) : Option[O]
  def convertKey(in: long): Option[O]
  def convertKey(in : AnyRef) : Option[O];
  def makeKeyJDBCFriendly(in : O) : AnyRef
  def db_display_? = false
}

trait ForeignKey[O] {
  def defined_? : boolean
}

