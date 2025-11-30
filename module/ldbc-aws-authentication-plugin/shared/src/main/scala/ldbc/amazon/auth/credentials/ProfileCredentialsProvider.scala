/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.time.Instant

import cats.MonadThrow

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*

import fs2.io.file.{Files, Path}

import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.*

import ProfileCredentialsProvider.*

final class ProfileCredentialsProvider[F[_]: SystemProperties: Files: Concurrent](
                                                                                   profileName: String,
  cacheRef: Ref[F, Option[(ProfileFile, AwsCredentials)]],
  semaphore: Semaphore[F]
)(using ev: MonadThrow[F]) extends AwsCredentialsProvider[F]:

  override def resolveCredentials(): F[AwsCredentials] =
    for
      currentFile <- loadFile
      cached      <- cacheRef.get
      credentials <- cached match
        case Some((cachedFile, creds)) if cachedFile.lastModified == currentFile.lastModified => ev.pure(creds)
        case _ => updateCredentials(currentFile)
    yield credentials
  
  private def loadFile: F[ProfileFile] =
    for
      homeOpt <- SystemProperties[F].get("user.home")
      home <- ev.fromOption(homeOpt, new SdkClientException(""))
      credentialsPath = Path(s"$home/.aws/credentials")
      exists   <- Files[F].exists(credentialsPath)
      _ <- ev.raiseUnless(exists)(new SdkClientException(s"File not found: $credentialsPath"))
      content  <- Files[F].readUtf8(credentialsPath).compile.string
      lastMod  <- Files[F].getLastModifiedTime(credentialsPath)
      profiles <- ev.fromEither(parseProfiles(content))
    yield ProfileFile(profiles, Instant.ofEpochMilli(lastMod.toMillis))

  private def parseProfiles(content: String): Either[Throwable, Map[String, Profile]] =
    val profilePattern = "\\[(?:profile\\s+)?(.+)\\]".r
    val propertyPattern = "^\\s*([^=]+)\\s*=\\s*(.+)\\s*$".r

    val lines = content.linesIterator.toList

    case class State(
                      currentProfile: Option[String] = None,
                      profiles: Map[String, Map[String, String]] = Map.empty
                    )

    val finalState = lines.foldLeft(State()): (state, line) =>
      line.trim match
        case _ if line.trim.isEmpty || line.trim.startsWith("#") =>
          state
        case profilePattern(name) =>
          State(Some(name.trim), state.profiles + (name.trim -> Map.empty))
        case propertyPattern(key, value) =>
          state.currentProfile match
            case Some(profile) =>
              val updatedProps = state.profiles.getOrElse(profile, Map.empty) + (key.trim -> value.trim)
              state.copy(profiles = state.profiles + (profile -> updatedProps))
            case None => state
        case _ => state

    Right(finalState.profiles.map: (name, props) =>
      name -> Profile(name, props)
    )

  private def parseStaticCredentials(props: Map[String, String]): Option[AwsCredentials] =
    for
      accessKeyId <- props.get("aws_access_key_id")
      secretAccessKey <- props.get("aws_secret_access_key")
    yield props.get("aws_session_token") match {
      case Some(sessionToken) =>
        AwsSessionCredentials(
          accessKeyId         = accessKeyId,
          secretAccessKey     = secretAccessKey,
          sessionToken        = sessionToken,
          validateCredentials = false,
          providerName        = None,
          accountId           = None,
          expirationTime      = None
        )
      case None =>
        AwsBasicCredentials(
          accessKeyId         = accessKeyId,
          secretAccessKey     = secretAccessKey,
          validateCredentials = false,
          providerName        = None,
          accountId           = None,
          expirationTime      = None
        )
    }

  private def extractCredentials(profileFile: ProfileFile): F[AwsCredentials] =
    for
      profile <- ev.fromOption(
        profileFile.profiles.get(profileName),
        new SdkClientException("")
      )
      credentials <- ev.fromOption(
        parseStaticCredentials(profile.properties),
        new SdkClientException("")
      )
    yield credentials

  private def updateCredentials(profileFile: ProfileFile): F[AwsCredentials] =
    semaphore.permit.use { _ =>
      for
        cached <- cacheRef.get
        credentials <- cached match
          case Some((cachedFile, creds)) if cachedFile.lastModified == profileFile.lastModified => ev.pure(creds)
          case _ =>
            for
              creds <- extractCredentials(profileFile)
              _ <- cacheRef.set(Some((profileFile, creds)))
            yield creds
      yield credentials
    }
 
object ProfileCredentialsProvider:

  final case class Profile(
                            name: String,
                            properties: Map[String, String]
                          )

  final case class ProfileFile(
                                profiles: Map[String, Profile],
                                lastModified: Instant
                              )

  def default[F[_]: SystemProperties: Files: Concurrent](profileName: String = "default"): F[ProfileCredentialsProvider[F]] =
    for
      cacheRef <- Ref.of[F, Option[(ProfileFile, AwsCredentials)]](None)
      semaphore <- Semaphore[F](1)
    yield new ProfileCredentialsProvider[F](profileName, cacheRef, semaphore)
