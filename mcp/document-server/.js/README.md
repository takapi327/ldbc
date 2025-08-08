# MCP Server for ldbc (Lepus Database Connectivity) Document

<div align="center">
  <img alt="ldbc" src="https://github.com/takapi327/ldbc/blob/master/lepus_logo.png">
</div>

[![Continuous Integration](https://github.com/takapi327/ldbc/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/takapi327/ldbc/actions/workflows/ci.yml)
[![MIT License](https://img.shields.io/badge/license-MIT-green)](https://en.wikipedia.org/wiki/MIT_License)
[![Typelevel Affiliate Project](https://img.shields.io/badge/typelevel-affiliate%20project-FF6169.svg)](https://typelevel.org/projects/affiliate/)
[![npm version](https://badge.fury.io/js/@ldbc%2Fmcp-document-server.svg)](https://badge.fury.io/js/@ldbc%2Fmcp-document-server)

A Model Context Protocol server that provides access to ldbc documents. This server allows the LLM to inspect ldbc documents, answer questions, and perform tutorials.

<video src="https://github.com/user-attachments/assets/a0c2a7a4-d5e7-4f91-bf69-833716d3efe5" muted="muted" controls="controls"></video>

## Installation

There are several ways to install and configure the MCP server:

### Visual Studio Code

To manually configure the MCP server for Visual Studio Code, add the following to your settings.json file (usually located in your user directory)

```json
{
    "files.autoSave": "afterDelay",
    "mcp": {        
        "inputs": [],
        "servers": {
            "mcp-ldbc-document-server": {
                "command": "npx",
                "args": [
                    "@ldbc/mcp-document-server"
                ],
                "env": {}
            }
        }
    }
}
```

### Claude Desktop

To manually configure the MCP server for Claude Desktop App, add the following to your claude_desktop_config.json file (typically located in your user directory):

```json
{
  "mcpServers": {
    "mcp-ldbc-document-server": {
      "command": "npx",
      "args": [
        "@ldbc/mcp-document-server"
      ],
      "env": {}
    }
  }
}
```

### Using NPM/PNPM

For manual installation:

```shell
# Using npm
npm install -g @ldbc/mcp-document-mysql

# Using pnpm
pnpm add -g @ldbc/mcp-document-mysql
```

After manual installation, you'll need to configure your LLM application to use the MCP server (see Configuration section below).

### Running from Local Repository

If you want to clone and run this MCP server directly from the source code, follow these steps:

1. Clone the repository

```shell
git clone git@github.com:takapi327/ldbc.git
```

2. Install dependencies

```shell
npm install
# or
pnpm install
```

3. Build the project

```shell
sbt docs/tlSite
sbt mcpDocumentServer/npmPackage
```

4. Configure Visual Studio Code

Add the following to the Visual Studio Code settings file (settings.json)

```json
{
    "files.autoSave": "afterDelay",
    "mcp": {        
        "inputs": [],
        "servers": {
            "mcp-ldbc-document-server": {
                "command": "/path/to/node",
                "args": [
                    "/full/path/to/ldbc/mcp/document-server/.js/target/scala-x.x.x/npm-package/main.js"
                ],
                "env": {}
            }
        }
    }
}
```

Replace:

- /path/to/node with the full path to your Node.js binary (find it with which node)
- /full/path/to/ldbc/mcp/document-server/... with the full path to where you cloned the repository

## Local Setup

Use [verdaccio](https://github.com/verdaccio/verdaccio) for local verification.

First, start verdaccio using docker.

```shell
docker compose up -d
```

After startup, users are created using the verdaccio server port. (* First time startup only)

```shell
npm adduser --registry http://localhost:4873
```

Then enter your username, password, and email address.

Build the code.

```shell
sbt mcpDocumentServer/npmPackage
sbt mcpDocumentServer/npmPackageNpmrc
```

Go to the generated npm-package directory.

```shell
cd mcp/document-server/.js/target/scala-x.x.x/npm-package
```

Upload against verdaccio.

```shell
npm publish --registry http://localhost:4873
```
