/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mcp

import cats.syntax.all.*

import io.circe.*

import cats.effect.*
import fs2.Stream
import fs2.io.file.{Files, Path}

import mcp.schema.*

case class DirContents(dirs: List[String], files: List[String])

object Tools:

  private val resourcesDir: String = "mcp-document-server/docs"

  /**
   * Helper function to list contents of a directory
   *
   * @param dirPath
   *   Path to the directory
   * @return
   *   A list of directories and files in the directory
   */
  def listDirContents(dirPath: String): IO[DirContents] =
    Files[IO]
      .list(Path(dirPath))
      .evalMap { path =>
        val fileName = path.fileName.toString
        Files[IO].isDirectory(path).map { isDir =>
          if isDir then
            Some(Left(fileName + "/"))
          else if fileName.endsWith(".md") then
            Some(Right(fileName))
          else
            None
        }
      }
      .collect { case Some(entry) => entry }
      .compile
      .fold((List.empty[String], List.empty[String])) {
        case ((dirs, files), Left(dir)) => (dir :: dirs, files)
        case ((dirs, files), Right(file)) => (dirs, file :: files)
      }
      .map { case (dirs, files) =>
        DirContents(dirs.sorted, files.sorted)
      }
      .handleErrorWith { error =>         
        IO.raiseError(error)
      }

  private def handleDirectory(fullPath: Path, docPath: String): IO[String] =

    listDirContents(fullPath.toString).flatMap { contents =>
      val dirs = contents.dirs
      val files = contents.files

      // Create a header section that displays the contents of the directory
      val dirListing = List(
        s"Directory contents of $docPath:",
        "",
        if (dirs.nonEmpty) "Subdirectories:" else "No subdirectories.",
        dirs.map(d => s"- $d").mkString("\n"),
        "",
        if (files.nonEmpty) "Files in this directory:" else "No files in this directory.",
        files.map(f => s"- $f").mkString("\n"),
        "",
        "---",
        "",
        "Contents of all files in this directory:",
        ""
      ).mkString("\n")

      // Read and concatenate the contents of all files
      files.traverse { file =>
        val filePath = fullPath.resolve(file)
        Files[IO].readUtf8(filePath).compile.string.map { content =>
          s"\n\n# $file\n\n$content"
        }
      }.map { fileContents =>
        dirListing + fileContents.mkString
      }
    }

  /**
   * Helper function to read MD files from a path
   *
   * @param docPath
   *   Path to the document
   * @return
   *   The content of the document as a string
   */
  def readMdContent(docPath: String): IO[String] =
    val fullPath = Path(PathUtils.fromPackageRoot(s"$resourcesDir/$docPath"))
    Files[IO].exists(fullPath).flatMap { exists =>
      if !exists then
        IO.raiseError(new Exception(s"Path not found: $docPath"))
      else
        Files[IO].isDirectory(fullPath).flatMap { isDirectory =>
          if isDirectory then
            handleDirectory(fullPath, docPath)
          else
            // For files, read the contents
            Files[IO].readUtf8(fullPath).compile.string
        }
    }.handleErrorWith { error =>
      IO.raiseError(new Exception(s"Path not found: $docPath"))
    }

  /**
   * Helper function to read file from resources directory
   *
   * @param docPath
   *   Path to the document in resources
   * @return
   *   The content of the document as a string
   */
  def readResourceFile(docPath: String): IO[String] =
    val resourcePath = s"$resourcesDir/$docPath"

    Option(getClass.getClassLoader.getResourceAsStream(resourcePath)) match
      case Some(is) =>
        Stream.bracket(IO(is))(is => IO(is.close()))
          .flatMap(is => fs2.io.readInputStream(IO(is), 8192, closeAfterUse = false))
          .through(fs2.text.utf8.decode)
          .compile
          .string
      case None =>
        IO.raiseError(new Exception(s"Resource not found: $resourcePath"))

  /**
   * Helper function to find nearest existing directory and its contents
   *
   * @param docPath
   *   Path to the document
   * @param availablePaths
   *   List of available paths
   * @return
   *   The content of the nearest existing directory as a string
   */
  def findNearestDirectory(docPath: String, availablePaths: String): IO[String] =
    // Split a path into parts
    val parts = docPath.split("/").filterNot(_.isEmpty).toList

    // Recursively try the parent directory
    def tryParentDirs(remainingParts: List[String]): IO[String] =
      if remainingParts.isEmpty then
        // Returns a list of roots if the parent directory is not found
        IO.pure(
          s"""Path "$docPath" not found.
             |Here are all available paths:
             |
             |$availablePaths""".stripMargin
        )
      else
        val testPath = remainingParts.mkString("/")
        val fullPath = Path(resourcesDir).resolve(testPath)

        Files[IO].exists(fullPath).flatMap { exists =>
          if !exists then
            tryParentDirs(remainingParts.init)
          else
            Files[IO].isDirectory(fullPath).flatMap { isDir =>
              if isDir then
                // If a directory is found, return its contents
                listDirContents(fullPath.toString).map { contents =>
                  val dirs = contents.dirs
                  val files = contents.files

                  List(
                    s"""Path "$docPath" not found.""",
                    s"""Here are the available paths in "$testPath":""",
                    "",
                    if (dirs.nonEmpty) "Directories:" else "No subdirectories.",
                    dirs.map(d => s"- $testPath/$d").mkString("\n"),
                    "",
                    if (files.nonEmpty) "Files:" else "No files.",
                    files.map(f => s"- $testPath/$f").mkString("\n")
                  ).mkString("\n")
                }
              else
                // If the path found is not a directory, try the parent
                tryParentDirs(remainingParts.init)
            }
        }.handleErrorWith { _ =>
          // If an error occurs, try the parent directory
          tryParentDirs(remainingParts.init)
        }

    tryParentDirs(parts)
    
  private val documentTree =
    """
      |├── README.md
      |├── en
      |│ ├── directory.conf
      |│ ├── examples
      |│ │ ├── HikariCP.md
      |│ │ ├── Http4s.md
      |│ │ ├── Otel.md
      |│ │ ├── directory.conf
      |│ │ └── index.md
      |│ ├── index.md
      |│ ├── migration-notes.md
      |│ ├── qa
      |│ │ ├── How-do-I-add-processing-before-and-after-the-connection-is-established.md
      |│ │ ├── How-do-I-use-my-own-type.md
      |│ │ ├── How-do-I-use-nested-models.md
      |│ │ ├── How-to-change-column-names-in-Query-Builder.md
      |│ │ ├── How-to-change-the-format-of-column-names-using-the-schema.md
      |│ │ ├── How-to-define-complex-queries-with-plain-queries.md
      |│ │ ├── How-to-handle-multiple-databases.md
      |│ │ ├── How-to-perform-DDL-with-schema.md
      |│ │ ├── How-to-use-scala-connector-connection-pool.md
      |│ │ ├── How-to-use-with-ZIO.md
      |│ │ ├── Implicit-search-problem-too-large.md
      |│ │ ├── Is-there-a-function-to-limit-the-number-of-concurrent-queries.md
      |│ │ ├── Is-there-a-way-to-stream-query-results-asynchronously.md
      |│ │ ├── What-is-ldbc.md
      |│ │ ├── What-is-the-difference-between-a-Java-connect-and-a-Scala-connector.md
      |│ │ ├── Which-dependencies-should-I-set.md
      |│ │ ├── directory.conf
      |│ │ └── index.md
      |│ ├── reference
      |│ │ ├── Connector.md
      |│ │ ├── Performance.md
      |│ │ ├── directory.conf
      |│ │ └── index.md
      |│ └── tutorial
      |│     ├── Connection.md
      |│     ├── Custom-Data-Type.md
      |│     ├── Database-Operations.md
      |│     ├── Error-Handling.md
      |│     ├── Logging.md
      |│     ├── Parameterized-Queries.md
      |│     ├── Query-Builder.md
      |│     ├── Schema-Code-Generation.md
      |│     ├── Schema.md
      |│     ├── Selecting-Data.md
      |│     ├── Setup.md
      |│     ├── Simple-Program.md
      |│     ├── Updating-Data.md
      |│     ├── directory.conf
      |│     └── index.md
      |├── index.md
      |├── ja
      |│ ├── directory.conf
      |│ ├── examples
      |│ │ ├── HikariCP.md
      |│ │ ├── Http4s.md
      |│ │ ├── Otel.md
      |│ │ ├── directory.conf
      |│ │ └── index.md
      |│ ├── index.md
      |│ ├── migration-notes.md
      |│ ├── qa
      |│ │ ├── How-do-I-add-processing-before-and-after-the-connection-is-established.md
      |│ │ ├── How-do-I-use-my-own-type.md
      |│ │ ├── How-do-I-use-nested-models.md
      |│ │ ├── How-to-change-column-names-in-Query-Builder.md
      |│ │ ├── How-to-change-the-format-of-column-names-using-the-schema.md
      |│ │ ├── How-to-define-complex-queries-with-plain-queries.md
      |│ │ ├── How-to-handle-multiple-databases.md
      |│ │ ├── How-to-perform-DDL-with-schema.md
      |│ │ ├── How-to-use-scala-connector-connection-pool.md
      |│ │ ├── How-to-use-with-ZIO.md
      |│ │ ├── Implicit-search-problem-too-large.md
      |│ │ ├── Is-there-a-function-to-limit-the-number-of-concurrent-queries.md
      |│ │ ├── Is-there-a-way-to-stream-query-results-asynchronously.md
      |│ │ ├── What-is-ldbc.md
      |│ │ ├── What-is-the-difference-between-a-Java-connect-and-a-Scala-connector.md
      |│ │ ├── Which-dependencies-should-I-set.md
      |│ │ ├── directory.conf
      |│ │ └── index.md
      |│ ├── reference
      |│ │ ├── Connector.md
      |│ │ ├── Performance.md
      |│ │ ├── directory.conf
      |│ │ └── index.md
      |│ └── tutorial
      |│     ├── Connection.md
      |│     ├── Custom-Data-Type.md
      |│     ├── Database-Operations.md
      |│     ├── Error-Handling.md
      |│     ├── Logging.md
      |│     ├── Parameterized-Queries.md
      |│     ├── Query-Builder.md
      |│     ├── Schema-Code-Generation.md
      |│     ├── Schema.md
      |│     ├── Selecting-Data.md
      |│     ├── Setup.md
      |│     ├── Simple-Program.md
      |│     ├── Updating-Data.md
      |│     ├── directory.conf
      |│     └── index.md
      |└── older-versions.md
      |""".stripMargin

  case class DocsInput(
    @Description(s"One or more documentation paths to fetch\\nAvailable paths:\\n$documentTree") paths: List[String]
  ) derives JsonSchema
  object DocsInput:
    given Decoder[DocsInput] = Decoder.derived[DocsInput]

  def docsTool = McpSchema.Tool[IO, DocsInput](
    "ldbcDocs",
    "Get ldbc (Lepus Database Connectivity) documentation. Request paths to explore the docs. References contain API docs. Other paths contain guides. The user doesn\\'t know about files and directories. This is your internal knowledge the user can\\'t read. If the user asks about a feature check general docs as well as reference docs for that feature. ",
    request =>
      for
        results <- IO.traverse(request.paths) { path =>
          readMdContent(path).map { content =>
            (path, content)
          }
        }
        output = results.map { case (path, content) =>
          s"## $path\n\n$content\n\n---\n"
        }.mkString("\n")
      yield McpSchema.CallToolResult.success(
        McpSchema.Content.text(output) :: Nil
      )
  )
