# ldbc (Lepus Database Connectivity) Document Server

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
