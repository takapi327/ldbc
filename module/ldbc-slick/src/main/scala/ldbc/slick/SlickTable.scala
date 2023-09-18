/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick

import scala.deriving.Mirror
import scala.language.dynamics
import scala.reflect.ClassTag
import scala.annotation.targetName

import slick.ast.*
import slick.lifted.{ Rep, RepShape, ShapedValue, FlatShapeLevel, TupleShape, ProvenShape, ToTuple }
import slick.relational.RelationalProfile

import ldbc.core.*
import ldbc.core.interpreter.Tuples
import ldbc.slick.lifted.{ Tag, BaseTag, RefTag }
import ldbc.slick.ast.TypedTypeTuple

final class SlickTable[P <: Product](using
  mirror:   Mirror.ProductOf[P],
  classTag: ClassTag[P],
  tt:       ToTuple[mirror.MirroredElemTypes, mirror.MirroredElemTypes]
)(
  table:          Table[P],
  profile:        RelationalProfile,
  tableTag:       Tag,
  typedTypeTuple: TypedTypeTuple[mirror.MirroredElemTypes]
) extends Rep[P],
          Dynamic:

  private def tableIdentitySymbol: TableIdentitySymbol =
    SimpleTableIdentitySymbol(profile, "_", table._name)

  private lazy val tableNode =
    TableNode(None, table._name, tableIdentitySymbol, tableIdentitySymbol)(table)

  override def encodeRef(path: Node): SlickTable[P] =
    tableTag.taggedAs(path).asInstanceOf[SlickTable[P]]

  override def toNode = tableTag match {
    case tag: BaseTag =>
      val sym = new AnonSymbol
      TableExpansion(sym, tableNode, tag.taggedAs(Ref(sym)).*.toNode)
    case t: RefTag => t.path
  }

  @targetName("allColumnShape")
  private def * : ProvenShape[P] =
    val repColumns: Tuple.Map[mirror.MirroredElemTypes, Rep] =
      table.*.zip(typedTypeTuple)
        .map[Rep](
          [t] =>
            (tuple: t) =>
              val (column, typedType) = tuple.asInstanceOf[(Column[t], TypedType[t])]
              new Rep[t]:
                override def encodeRef(path: Node): Rep[t] =
                  Rep.forNode(path)(using typedType)

                val node = tableTag match
                  case r: RefTag => r.path
                  case _         => tableNode
                override def toNode = Select(node, FieldSymbol(column.label)(Seq.empty, typedType)) :@ typedType

                override def toString: String = (tableTag match
                  case r: RefTag => "(" + table._name + " " + r.path + ")"
                  case _         => table._name
                ) + "." + column.label
        )
        .asInstanceOf[Tuple.Map[mirror.MirroredElemTypes, Rep]]

    val tupleShape = new TupleShape[
      FlatShapeLevel,
      Tuple.Map[mirror.MirroredElemTypes, Rep],
      mirror.MirroredElemTypes,
      P
    ](
      table.all
        .map(column =>
          RepShape[FlatShapeLevel, Rep[SlickTable.Extract[column.type]], SlickTable.Extract[column.type]]
        ): _*
    )

    val shapedValue = new ShapedValue[Tuple.Map[mirror.MirroredElemTypes, Rep], mirror.MirroredElemTypes](
      repColumns,
      tupleShape
    )

    shapedValue <> (mirror.fromTuple, Tuple.fromProductTyped)

  def selectDynamic[Tag <: Singleton](tag: Tag)(using
    mirror:                                Mirror.ProductOf[P],
    index:                                 ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    tt: TypedType[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
  ): Rep[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
    val column = table.selectDynamic[Tag](tag)
    new Rep.TypedRep[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]:
      val node = tableTag match
        case r: RefTag => r.path
        case _         => tableNode

      override def toNode = Select(node, FieldSymbol(column.label)(Seq.empty, tt)) :@ tt
      override def toString: String = (tableTag match
        case r: RefTag => "(" + table._name + " " + r.path + ")"
        case _         => table._name
      ) + "." + column.label

object SlickTable:

  type Extract[T] = T match
    case Column[t] => t

  inline def apply[P <: Product](table: Table[P], profile: RelationalProfile)(using
    mirror:                             Mirror.ProductOf[P],
    classTag:                           ClassTag[P],
    tt:                                 ToTuple[mirror.MirroredElemTypes, mirror.MirroredElemTypes]
  ): SlickTable[P] =

    val typedTypeTuple = TypedTypeTuple.fold[mirror.MirroredElemTypes]

    val tableTag: Tag = new BaseTag:
      base =>
      override def taggedAs(path: Node) =
        val tag = new RefTag(path):
          override def taggedAs(path: Node) = base.taggedAs(path)
        new SlickTable[P](table, profile, tag, typedTypeTuple)

    new SlickTable[P](table, profile, tableTag, typedTypeTuple)
