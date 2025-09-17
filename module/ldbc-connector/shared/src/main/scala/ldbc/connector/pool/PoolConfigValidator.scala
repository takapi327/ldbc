/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*
import scala.io.AnsiColor

import cats.data.{ Validated, ValidatedNel }
import cats.syntax.all.*

import cats.effect.std.Console
import cats.effect.Async

import ldbc.connector.MySQLConfig

object PoolConfigValidator:

  private[pool] def validateMinConnections(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.minConnections < 0 then
      s"minConnections cannot be less than 0, value: ${ config.minConnections }".invalidNel
    else ().validNel

  private[pool] def validateMaxConnections(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.maxConnections <= 0 then
      s"maxConnections cannot be less than 1, value: ${ config.maxConnections }".invalidNel
    else ().validNel

  private[pool] def validateMinMaxConnections(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.minConnections > config.maxConnections then
      s"minConnections (${ config.minConnections }) cannot be greater than maxConnections (${ config.maxConnections })".invalidNel
    else ().validNel

  private[pool] def validateConnectionTimeout(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.connectionTimeout <= Duration.Zero then
      s"connectionTimeout cannot be less than 250ms, value: ${ config.connectionTimeout }".invalidNel
    // Minimum recommended values (similar to HikariConfig's SOFT_TIMEOUT_FLOOR)
    else if config.connectionTimeout < 250.milliseconds then
      s"connectionTimeout cannot be less than 250ms, value: ${ config.connectionTimeout }".invalidNel
    else ().validNel

  private[pool] def validateValidationTimeout(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.validationTimeout <= Duration.Zero then
      s"validationTimeout cannot be less than 250ms, value: ${ config.validationTimeout }".invalidNel
    else if config.validationTimeout < 250.milliseconds then
      s"validationTimeout cannot be less than 250ms, value: ${ config.validationTimeout }".invalidNel
    else ().validNel

  private[pool] def validateIdleTimeout(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.idleTimeout < Duration.Zero then
      s"idleTimeout cannot be negative, value: ${ config.idleTimeout }".invalidNel
    else ().validNel

  private[pool] def validateMaxLifetime(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.maxLifetime <= Duration.Zero then
      s"maxLifetime cannot be less than 30 seconds, value: ${ config.maxLifetime }".invalidNel
    else if config.maxLifetime < 30.seconds then
      s"maxLifetime cannot be less than 30 seconds, value: ${ config.maxLifetime }".invalidNel
    else ().validNel

  private[pool] def validateMaintenanceInterval(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.maintenanceInterval <= Duration.Zero then
      s"maintenanceInterval cannot be less than 1 second, value: ${ config.maintenanceInterval }".invalidNel
    else ().validNel

  private[pool] def validateLeakDetectionThreshold(config: MySQLConfig): ValidatedNel[String, Unit] =
    config.leakDetectionThreshold match
      case Some(threshold) =>
        if threshold < 2.seconds then
          s"leakDetectionThreshold cannot be less than 2 seconds, value: $threshold".invalidNel
        else if threshold > config.maxLifetime then
          s"leakDetectionThreshold ($threshold) cannot be greater than maxLifetime (${ config.maxLifetime })".invalidNel
        else ().validNel
      case None => ().validNel

  private[pool] def validateLogicalRelationship(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.idleTimeout > Duration.Zero && config.idleTimeout > config.maxLifetime then
      s"idleTimeout (${ config.idleTimeout }) cannot be greater than maxLifetime (${ config.maxLifetime })".invalidNel
    else ().validNel

  private[pool] def validateUser(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.user == null || config.user.isEmpty then "user cannot be null or empty".invalidNel
    else ().validNel

  private[pool] def validateHost(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.host == null || config.host.isEmpty then "host cannot be null or empty".invalidNel
    else ().validNel

  private[pool] def validatePort(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.port <= 0 || config.port > 65535 then
      s"port must be between 1 and 65535, value: ${ config.port }".invalidNel
    else ().validNel

  private[pool] def validateAliveBypassWindow(config: MySQLConfig): ValidatedNel[String, Unit] =
    if config.aliveBypassWindow < Duration.Zero then
      s"aliveBypassWindow cannot be negative, value: ${ config.aliveBypassWindow }".invalidNel
    else ().validNel

  private[pool] def validateKeepaliveTime(config: MySQLConfig): ValidatedNel[String, Unit] =
    config.keepaliveTime match
      case Some(keepalive) =>
        if keepalive < 30.seconds then s"keepaliveTime cannot be less than 30 seconds, value: $keepalive".invalidNel
        else if keepalive >= config.maxLifetime then
          s"keepaliveTime ($keepalive) must be less than maxLifetime (${ config.maxLifetime })".invalidNel
        else ().validNel
      case None => ().validNel

  def validate[F[_]: Async: Console](config: MySQLConfig): F[Unit] =
    val validations = (
      validateMinConnections(config),
      validateMaxConnections(config),
      validateMinMaxConnections(config),
      validateConnectionTimeout(config),
      validateValidationTimeout(config),
      validateIdleTimeout(config),
      validateMaxLifetime(config),
      validateMaintenanceInterval(config),
      validateLeakDetectionThreshold(config),
      validateLogicalRelationship(config),
      validateUser(config),
      validateHost(config),
      validatePort(config),
      validateAliveBypassWindow(config),
      validateKeepaliveTime(config)
    ).mapN((_, _, _, _, _, _, _, _, _, _, _, _, _, _, _) => ())

    validations match
      case Validated.Valid(_) =>
        if config.maxConnections > 100 && config.debug then
          Console[F].println(
            s"${ AnsiColor.YELLOW } [WARN] ${ AnsiColor.RESET } maxConnections (${ config.maxConnections }) is unusually high, consider reducing it for better performance."
          )
        else Async[F].unit
      case Validated.Invalid(errors) =>
        Async[F].raiseError(
          new IllegalArgumentException(s"Configuration validation failed:\n${ errors.toList.mkString("\n") }")
        )
