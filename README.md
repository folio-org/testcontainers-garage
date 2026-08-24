# testcontainers-garage

Copyright (C) 2026 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0.
See the file "LICENSE" for more information.

## Introduction

The testcontainers-garage classes implement a
[testcontainer](https://java.testcontainers.org/) for the
[`docker.io/dxflrs/garage`](https://hub.docker.com/r/dxflrs/garage) image;
[garage](https://garagehq.deuxfleurs.fr/) is a lightweight S3-compatible distributed object storage service.

A testcontainers-garage class runs a single (non-distributed) node.

## LocalStackContainer

To ease the migration from testcontainers-localstack's `LocalStackContainer`
the methods `withServices(String...)`, `getEndpoint()`, `getAccessKey()`,
`getSecretKey()`, and `getRegion()` are provided, and the environment variables
`AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` are used if provided
(via `withEnv(String, String)` or similar method).

## MinIOContainer

To ease the migration from testcontainers-minio's `MinIOContainer`
the methods `withUserName(String)`, `withPassword(String)`, `getUserName()`,
`getPassword()`, and `getS3URL()` are provided, and the environment variables
`MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD` are used if provided 
(via `withEnv(String, String)` or similar method).

## AWS S3 client

For AWS S3 client use `GarageContainerAws` that has methods
that return a client or a client builder that is pre-configured for the garage container.

## MinIO client

For MinIO client and MinIO async client use `GarageContainerMinio` that has methods
that return a client or a client builder that is pre-configured for the garage container.
