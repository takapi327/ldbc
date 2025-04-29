/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mcp

import cats.effect.*

import mcp.server.McpServer

import ldbc.build.Version

object StdioServer extends IOApp.Simple:

  override def run: IO[Unit] =
    McpServer
      .FastMcp[IO]("ldbc Document Server", Version.current)
      .addTool(Tools.docsTool)
      .start("stdio")
