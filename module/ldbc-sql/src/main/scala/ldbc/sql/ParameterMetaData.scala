/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import ldbc.core.JdbcType

/** An object that can be used to get information about the types and properties for each parameter marker in a
  * PreparedStatement object. For some queries and driver implementations, the data that would be returned by a
  * ParameterMetaData object may not be available until the PreparedStatement has been executed.
  *
  * Some driver implementations may not be able to provide information about the types and properties for each parameter
  * marker in a CallableStatement object.
  *
  * @tparam F
  *   The effect type
  */
trait ParameterMetaData[F[_]]:

  /** Retrieves the number of parameters in the PreparedStatement object for which this ParameterMetaData object
    * contains information.
    *
    * @return
    *   the number of parameters
    */
  def getParameterCount(): F[Int]

  /** Retrieves whether null values are allowed in the designated parameter.
    *
    * @param param
    *   the first parameter is 1, the second is 2, ...
    * @return
    *   the nullability status of the given parameter; one of ParameterMetaData.parameterNoNulls,
    *   ParameterMetaData.parameterNullable, or ParameterMetaData.parameterNullableUnknown
    */
  def isNullable(param: Int): F[Option[ParameterMetaData.Parameter]]

  /** Retrieves whether values for the designated parameter can be signed numbers.
    *
    * @param param
    *   the first parameter is 1, the second is 2, ...
    * @return
    *
    * true if so; false otherwise
    */
  def isSigned(param: Int): F[Boolean]

  /** Retrieves the designated parameter's specified column size.
    *
    * The returned value represents the maximum column size for the given parameter. For numeric data, this is the
    * maximum precision. For character data, this is the length in characters. For datetime datatypes, this is the
    * length in characters of the String representation (assuming the maximum allowed precision of the fractional
    * seconds component). For binary data, this is the length in bytes. For the ROWID datatype, this is the length in
    * bytes. 0 is returned for data types where the column size is not applicable.
    *
    * @param param
    *   the first parameter is 1, the second is 2, ...
    * @return
    *   precision
    */
  def getPrecision(param: Int): F[Int]

  /** Retrieves the designated parameter's number of digits to right of the decimal point. 0 is returned for data types
    * where the scale is not applicable.
    *
    * @param param
    *   the first parameter is 1, the second is 2, ...
    * @return
    *   scale
    */
  def getScale(param: Int): F[Int]

  /** Retrieves the designated parameter's SQL type.
    *
    * @param param
    *   the first parameter is 1, the second is 2, ...
    * @return
    *   SQL type from [[ldbc.core.JdbcType]]
    */
  def getParameterType(param: Int): F[JdbcType]

  /** Retrieves the designated parameter's database-specific type name.
    *
    * @param param
    *   the first parameter is 1, the second is 2, ...
    * @return
    *   type the name used by the database. If the parameter type is a user-defined type, then a fully-qualified type
    *   name is returned.
    */
  def getParameterTypeName(param: Int): F[String]

  /** Retrieves the fully-qualified name of the Java class whose instances should be passed to the method
    * PreparedStatement.setObject.
    *
    * @param param
    *   the first parameter is 1, the second is 2, ...
    * @return
    *   the fully-qualified name of the class in the Java programming language that would be used by the method
    *   PreparedStatement.setObject to set the value in the specified parameter. This is the class name used for custom
    *   mapping.
    */
  def getParameterClassName(param: Int): F[String]

  /** Retrieves the designated parameter's mode.
    *
    * @param param
    *   the first parameter is 1, the second is 2, ...
    * @return
    *   mode of the parameter; one of ParameterMetaData.parameterModeIn, ParameterMetaData.parameterModeOut, or
    *   ParameterMetaData.parameterModeInOut ParameterMetaData.parameterModeUnknown.
    */
  def getParameterMode(param: Int): F[Option[ParameterMetaData.Mode]]

object ParameterMetaData:

  enum Parameter(val code: Int):
    case NO_NULLS         extends Parameter(java.sql.ParameterMetaData.parameterNoNulls)
    case NULLABLE         extends Parameter(java.sql.ParameterMetaData.parameterNullable)
    case NULLABLE_UNKNOWN extends Parameter(java.sql.ParameterMetaData.parameterNullableUnknown)

  enum Mode(val code: Int):
    case UNKNOWN extends Mode(java.sql.ParameterMetaData.parameterModeUnknown)
    case IN      extends Mode(java.sql.ParameterMetaData.parameterModeIn)
    case IN_OUT  extends Mode(java.sql.ParameterMetaData.parameterModeInOut)
    case OUT     extends Mode(java.sql.ParameterMetaData.parameterModeOut)
