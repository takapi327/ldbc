/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*
import scala.io.AnsiColor

import cats.syntax.all.*

import cats.effect.std.Console
import cats.effect.Async

import ldbc.connector.MySQLConfig

object PoolConfigValidator:

  private[pool] def validateMinConnections[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.minConnections < 0 then
      Async[F].raiseError(
        new IllegalArgumentException(s"minConnections cannot be less than 0, value: ${ config.minConnections }")
      )
    else Async[F].unit

  private[pool] def validateMaxConnections[F[_]: Async: Console](config: MySQLConfig): F[Unit] =
    if config.maxConnections <= 0 then
      Async[F].raiseError(
        new IllegalArgumentException(s"maxConnections cannot be less than 1, value: ${ config.maxConnections }")
      )
    else if config.maxConnections > 100 && config.debug then
      Console[F].println(
        s"${ AnsiColor.YELLOW } [WARN] ${ AnsiColor.RESET } maxConnections (${ config.maxConnections }) is unusually high, consider reducing it for better performance."
      )
    else Async[F].unit

  private[pool] def validateMinMaxConnections[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.minConnections > config.maxConnections then
      Async[F].raiseError(
        new IllegalArgumentException(
          s"minConnections (${ config.minConnections }) cannot be greater than maxConnections (${ config.maxConnections })"
        )
      )
    else Async[F].unit

  private[pool] def validateConnectionTimeout[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.connectionTimeout <= Duration.Zero then
      Async[F].raiseError(
        new IllegalArgumentException(
          s"connectionTimeout cannot be less than 250ms, value: ${ config.connectionTimeout }"
        )
      )
    // Minimum recommended values (similar to HikariConfig's SOFT_TIMEOUT_FLOOR)
    else if config.connectionTimeout < 250.milliseconds then
      Async[F].raiseError(
        new IllegalArgumentException(
          s"connectionTimeout cannot be less than 250ms, value: ${ config.connectionTimeout }"
        )
      )
    else Async[F].unit

  private[pool] def validateValidationTimeout[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.validationTimeout <= Duration.Zero then
      Async[F].raiseError(
        new IllegalArgumentException(
          s"validationTimeout cannot be less than 250ms, value: ${ config.validationTimeout }"
        )
      )
    else if config.validationTimeout < 250.milliseconds then
      Async[F].raiseError(
        new IllegalArgumentException(
          s"validationTimeout cannot be less than 250ms, value: ${ config.validationTimeout }"
        )
      )
    else Async[F].unit

  private[pool] def validateIdleTimeout[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.idleTimeout < Duration.Zero then
      Async[F].raiseError(
        new IllegalArgumentException(s"idleTimeout cannot be negative, value: ${ config.idleTimeout }")
      )
    else Async[F].unit

  private[pool] def validateMaxLifetime[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.maxLifetime <= Duration.Zero then
      Async[F].raiseError(
        new IllegalArgumentException(s"maxLifetime cannot be less than 30 seconds, value: ${ config.maxLifetime }")
      )
    else if config.maxLifetime < 30.seconds then
      Async[F].raiseError(
        new IllegalArgumentException(s"maxLifetime cannot be less than 30 seconds, value: ${ config.maxLifetime }")
      )
    else Async[F].unit

  private[pool] def validateMaintenanceInterval[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.maintenanceInterval <= Duration.Zero then
      Async[F].raiseError(
        new IllegalArgumentException(
          s"maintenanceInterval cannot be less than 1 second, value: ${ config.maintenanceInterval }"
        )
      )
    else Async[F].unit

  private[pool] def validateLeakDetectionThreshold[F[_]: Async](config: MySQLConfig): F[Unit] =
    config.leakDetectionThreshold match
      case Some(threshold) =>
        if threshold < 2.seconds then
          Async[F].raiseError(
            new IllegalArgumentException(s"leakDetectionThreshold cannot be less than 2 seconds, value: $threshold")
          )
        else if threshold > config.maxLifetime then
          Async[F].raiseError(
            new IllegalArgumentException(
              s"leakDetectionThreshold ($threshold) cannot be greater than maxLifetime (${ config.maxLifetime })"
            )
          )
        else Async[F].unit
      case None => Async[F].unit

  private[pool] def validateLogicalRelationship[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.idleTimeout > Duration.Zero && config.idleTimeout > config.maxLifetime then
      Async[F].raiseError(
        new IllegalArgumentException(
          s"idleTimeout (${ config.idleTimeout }) cannot be greater than maxLifetime (${ config.maxLifetime })"
        )
      )
    else Async[F].unit

  private[pool] def validateUser[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.user == null || config.user.isEmpty then
      Async[F].raiseError(new IllegalArgumentException("user cannot be null or empty"))
    else Async[F].unit

  private[pool] def validateHost[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.host == null || config.host.isEmpty then
      Async[F].raiseError(new IllegalArgumentException("host cannot be null or empty"))
    else Async[F].unit

  private[pool] def validatePort[F[_]: Async](config: MySQLConfig): F[Unit] =
    if config.port <= 0 || config.port > 65535 then
      Async[F].raiseError(new IllegalArgumentException(s"port must be between 1 and 65535, value: ${ config.port }"))
    else Async[F].unit

  def validate[F[_]: Async: Console](config: MySQLConfig): F[Unit] =
    for
      _ <- validateMinConnections(config)
      _ <- validateMaxConnections(config)
      _ <- validateMinMaxConnections(config)
      _ <- validateConnectionTimeout(config)
      _ <- validateValidationTimeout(config)
      _ <- validateIdleTimeout(config)
      _ <- validateMaxLifetime(config)
      _ <- validateMaintenanceInterval(config)
      _ <- validateLeakDetectionThreshold(config)
      _ <- validateLogicalRelationship(config)
      _ <- validateUser(config)
      _ <- validateHost(config)
      _ <- validatePort(config)
    yield ()
