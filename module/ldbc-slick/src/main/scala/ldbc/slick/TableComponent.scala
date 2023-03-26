/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.slick

import scala.language.dynamics
import scala.deriving.Mirror
import scala.reflect.ClassTag

import slick.ast.{ Node, TableNode, TableIdentitySymbol, SimpleTableIdentitySymbol, Select, FieldSymbol, AnonSymbol, Ref, TableExpansion }
import slick.lifted.{ ToTuple, Rep, FlatShapeLevel, ProvenShape, TupleShape, RepShape, ShapedValue }
import slick.relational.RelationalProfile

import ldbc.core.{ Key, Column, DataType, Table }
import ldbc.core.attribute.Attribute
import ldbc.core.interpreter.*
import ldbc.slick.lifted.{ Tag, BaseTag, RefTag }
import ldbc.slick.syntax.TableSyntax

private[ldbc] trait TableComponent:
  self: RelationalProfile =>

  object SlickTable extends Dynamic:

    type Extract[T] = T match
      case Column[t] => t

    type RepColumnType[Type] = TypedColumn[Type] & Rep[Type]

    private case class Impl[P <: Product](
      tag: Tag,
      name: String,
      keyDefinitions: Seq[Key]
    )(using
      mirror:   Mirror.ProductOf[P],
      classTag: ClassTag[P],
      tt:       ToTuple[mirror.MirroredElemTypes, mirror.MirroredElemTypes]
    )(columns: Tuple.Map[mirror.MirroredElemTypes, TypedColumn]) extends SlickTable[P]:

      override def encodeRef(path: Node): SlickTable[P] =
        tag.taggedAs(path).asInstanceOf[SlickTable[P]]

      private def tableIdentitySymbol: TableIdentitySymbol =
        SimpleTableIdentitySymbol(self, None.getOrElse("_"), name)

      override lazy val tableNode =
        TableNode(None, name, tableIdentitySymbol, tableIdentitySymbol)(this)

      override def allColumnShape: ProvenShape[P] =
        val tupleShape = new TupleShape[FlatShapeLevel, Tuple.Map[mirror.MirroredElemTypes, RepColumnType], mirror.MirroredElemTypes, P](
          columns.productIterator.map(v => {
            RepShape[FlatShapeLevel, Rep[Extract[v.type]], Extract[v.type]]
          }).toList: _*
        )
        val repColumns: Tuple.Map[mirror.MirroredElemTypes, RepColumnType] = Tuple.fromArray(columns.productIterator.map(v => {
          val column = v.asInstanceOf[TypedColumn[?]]
          new TypedColumn[Extract[column.type]] with Rep[Extract[column.type]]:
            override def label: String = column.label

            override def dataType: DataType[Extract[column.type]] = column.dataType

            override def comment: Option[String] = column.comment

            override def attributes: Seq[Attribute[Extract[column.type]]] = column.attributes

            override def typedType = column.typedType

            override def encodeRef(path: Node): Rep[Extract[column.type]] = Rep.forNode(path)(using column.typedType)

            override def toNode =
              Select((tag match
                case r: RefTag => r.path
                case _ => tableNode),
                FieldSymbol(label)(Seq.empty, typedType)
              ) :@ typedType

            override def toString = (tag match
              case r: RefTag => "(" + name + " " + r.path + ")"
              case _ => name
            ) + "." + label
        }).toArray).asInstanceOf[Tuple.Map[mirror.MirroredElemTypes, RepColumnType]]
        val shapedValue = new ShapedValue[Tuple.Map[mirror.MirroredElemTypes, RepColumnType], mirror.MirroredElemTypes](repColumns, tupleShape)
        shapedValue <> (
          v => mirror.fromTuple(v.asInstanceOf[mirror.MirroredElemTypes]),
          Tuple.fromProductTyped
        )

      override def toNode = tag match {
        case _: BaseTag =>
          val sym = new AnonSymbol
          TableExpansion(sym, tableNode, tag.taggedAs(Ref(sym)).allColumnShape.toNode)
        case t: RefTag => t.path
      }

      override def selectDynamic[Tag <: Singleton](
        tag: Tag
      )(using
        mirror: Mirror.ProductOf[P],
        index: ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
      ): Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
        columns
          .productElement(index.value)
          .asInstanceOf[Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]

      override def selectDynamic(label: "*"): List[Tuple.Union[Tuple.Map[Any *: NonEmptyTuple, Column]]] =
        columns.toList.asInstanceOf[List[Tuple.Union[Tuple.Map[Any *: NonEmptyTuple, Column]]]]

      override def keys(func: ldbc.core.Table[P] => Seq[Key]): ldbc.core.Table[P] =
        this.copy(keyDefinitions = this.keyDefinitions ++ func(this))(columns)

    def applyDynamic[P <: Product](using
     mirror: Mirror.ProductOf[P],
     converter: ColumnTupleConverter[mirror.MirroredElemTypes, TypedColumn],
     classTag: ClassTag[P],
     tt: ToTuple[mirror.MirroredElemTypes, mirror.MirroredElemTypes]
    )(nameApply: "apply")(name: String)(
      columns: ColumnTuples[mirror.MirroredElemTypes, TypedColumn]
    ): SlickTable[P] =

      val tableTag: Tag = new BaseTag {
        base =>
        def taggedAs(path: Node): SlickTable[?] = fromTupleMap[P](new RefTag(path) {
          def taggedAs(path: Node) = base.taggedAs(path)
        }, name, ColumnTupleConverter.convert(columns))
      }
      fromTupleMap[P](tableTag, name, ColumnTupleConverter.convert(columns))

    private def fromTupleMap[P <: Product](using
     mirror: Mirror.ProductOf[P],
     classTag: scala.reflect.ClassTag[P],
     tt: slick.lifted.ToTuple[mirror.MirroredElemTypes, mirror.MirroredElemTypes]
    )(
      tableTag: ldbc.slick.lifted.Tag,
      _name: String,
      columns: Tuple.Map[mirror.MirroredElemTypes, TypedColumn]
    ): SlickTable[P] =
      Impl[P](tableTag, _name, Seq.empty)(columns)
