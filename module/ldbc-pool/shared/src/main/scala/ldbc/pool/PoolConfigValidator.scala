/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.effect.MonadThrow

/** Validates a [[ConnectionPoolConfig]] and reports every problem. */
object PoolConfigValidator:

  private def validateMinConnections(c: ConnectionPoolConfig): List[String] =
    if c.minConnections < 0 then List(s"minConnections cannot be less than 0, value: ${ c.minConnections }") else Nil

  private def validateMaxConnections(c: ConnectionPoolConfig): List[String] =
    if c.maxConnections <= 0 then List(s"maxConnections cannot be less than 1, value: ${ c.maxConnections }") else Nil

  private def validateMinMaxConnections(c: ConnectionPoolConfig): List[String] =
    if c.minConnections > c.maxConnections then
      List(s"minConnections (${ c.minConnections }) cannot be greater than maxConnections (${ c.maxConnections })")
    else Nil

  private def validateConnectionTimeout(c: ConnectionPoolConfig): List[String] =
    if c.connectionTimeout < 250.milliseconds then
      List(s"connectionTimeout cannot be less than 250ms, value: ${ c.connectionTimeout }")
    else Nil

  private def validateValidationTimeout(c: ConnectionPoolConfig): List[String] =
    if c.validationTimeout < 250.milliseconds then
      List(s"validationTimeout cannot be less than 250ms, value: ${ c.validationTimeout }")
    else Nil

  private def validateIdleTimeout(c: ConnectionPoolConfig): List[String] =
    if c.idleTimeout < Duration.Zero then List(s"idleTimeout cannot be negative, value: ${ c.idleTimeout }") else Nil

  private def validateMaxLifetime(c: ConnectionPoolConfig): List[String] =
    if c.maxLifetime < 30.seconds then List(s"maxLifetime cannot be less than 30 seconds, value: ${ c.maxLifetime }")
    else Nil

  private def validateMaintenanceInterval(c: ConnectionPoolConfig): List[String] =
    if c.maintenanceInterval <= Duration.Zero then
      List(s"maintenanceInterval cannot be less than 1 second, value: ${ c.maintenanceInterval }")
    else Nil

  private def validateLeakDetectionThreshold(c: ConnectionPoolConfig): List[String] =
    c.leakDetectionThreshold match
      case Some(threshold) =>
        if threshold < 2.seconds then List(s"leakDetectionThreshold cannot be less than 2 seconds, value: $threshold")
        else if threshold > c.maxLifetime then
          List(s"leakDetectionThreshold ($threshold) cannot be greater than maxLifetime (${ c.maxLifetime })")
        else Nil
      case None => Nil

  private def validateLogicalRelationship(c: ConnectionPoolConfig): List[String] =
    if c.idleTimeout > Duration.Zero && c.idleTimeout > c.maxLifetime then
      List(s"idleTimeout (${ c.idleTimeout }) cannot be greater than maxLifetime (${ c.maxLifetime })")
    else Nil

  private def validateAliveBypassWindow(c: ConnectionPoolConfig): List[String] =
    if c.aliveBypassWindow < Duration.Zero then
      List(s"aliveBypassWindow cannot be negative, value: ${ c.aliveBypassWindow }")
    else Nil

  private def validateKeepaliveTime(c: ConnectionPoolConfig): List[String] =
    c.keepaliveTime match
      case Some(keepalive) =>
        if keepalive < 30.seconds then List(s"keepaliveTime cannot be less than 30 seconds, value: $keepalive")
        else if keepalive >= c.maxLifetime then
          List(s"keepaliveTime ($keepalive) must be less than maxLifetime (${ c.maxLifetime })")
        else Nil
      case None => Nil

  /** Collects every validation error for `config` (empty when the config is valid). */
  def errors(config: ConnectionPoolConfig): List[String] =
    validateMinConnections(config) ++
      validateMaxConnections(config) ++
      validateMinMaxConnections(config) ++
      validateConnectionTimeout(config) ++
      validateValidationTimeout(config) ++
      validateIdleTimeout(config) ++
      validateMaxLifetime(config) ++
      validateMaintenanceInterval(config) ++
      validateLeakDetectionThreshold(config) ++
      validateLogicalRelationship(config) ++
      validateAliveBypassWindow(config) ++
      validateKeepaliveTime(config)

  /** Raises an `IllegalArgumentException` if `config` is invalid, otherwise succeeds. */
  def validate[F[_]](config: ConnectionPoolConfig)(using F: MonadThrow[F]): F[Unit] =
    errors(config) match
      case Nil  => F.unit
      case errs =>
        F.raiseError(new IllegalArgumentException(s"Configuration validation failed:\n${ errs.mkString("\n") }"))
