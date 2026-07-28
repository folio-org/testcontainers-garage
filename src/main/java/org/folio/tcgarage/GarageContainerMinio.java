package org.folio.tcgarage;

import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainer for the docker.io/dxflrs/garage image;
 * <a href="https://garagehq.deuxfleurs.fr/">garage</a> is a
 * lightweight S3-compatible distributed object storage service.
 *
 * <p>{@code GarageContainerMinio} runs a single (non-distributed) node.
 *
 * <p>To ease the migration from testcontainers-localstack's {@code LocalStackContainer}
 * {@code GarageContainerMinio} provides the methods {@link #withServices(String...)},
 * {@link #getEndpoint()}, {@link #getAccessKey()}, {@link #getSecretKey()}, and {@link #getRegion()},
 * and uses AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY if provided (via {@link #withEnv(String, String)}
 * or similar method).
 *
 * <p>To ease the migration from testcontainers-minio's {@code MinIOContainer}
 * {@code GarageContainerMinio} provides the methods {@link #withUserName(String)},
 * {@link #withPassword(String)}, {@link #getUserName()}, {@link #getPassword()}, and {@link #getS3URL()},
 * and uses MINIO_ROOT_USER and MINIO_ROOT_PASSWORD if provided (via {@link #withEnv(String, String)}
 * or similar method).
 *
 * <p>Use {@link #getMinioClient()} or {@link #getMinioAsyncClient()}
 * or {@link #getMinioClientBuilder()} or {@link #getMinioAsyncClientBuilder()}
 * to get a MinIO client or a MinIO client builder that is pre-configured for the garage container.
 */
public class GarageContainerMinio extends AbstractGarageContainerMinio<GarageContainerMinio> {
  /**
   * Construct a Garage container from the dockerImageName.
   *
   * @param dockerImageName the full image name to use
   */
  public GarageContainerMinio(final String dockerImageName) {
    super(dockerImageName);
  }

  /**
   * Construct a Garage container from the dockerImageName.
   *
   * Only ports 3900 (S3 API) and 3903 (Garage Admin API) get exposed by default.
   *
   * @param dockerImageName the full image name to use
   */
  public GarageContainerMinio(final DockerImageName dockerImageName) {
    super(dockerImageName);
  }
}
