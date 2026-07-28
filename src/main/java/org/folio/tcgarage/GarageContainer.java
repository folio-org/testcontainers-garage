package org.folio.tcgarage;

import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainer for the docker.io/dxflrs/garage image;
 * <a href="https://garagehq.deuxfleurs.fr/">garage</a> is a
 * lightweight S3-compatible distributed object storage service.
 *
 * <p>{@code GarageContainer} runs a single (non-distributed) node.
 *
 * <p>To ease the migration from testcontainers-localstack's {@code LocalStackContainer}
 * {@code GarageContainer} provides the methods {@link #withServices(String...)},
 * {@link #getEndpoint()}, {@link #getAccessKey()}, {@link #getSecretKey()}, and {@link #getRegion()},
 * and uses AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY if provided (via {@link #withEnv(String, String)}
 * or similar method).
 *
 * <p>To ease the migration from testcontainers-minio's {@code MinIOContainer}
 * {@code GarageContainer} provides the methods {@link #withUserName(String)},
 * {@link #withPassword(String)}, {@link #getUserName()}, {@link #getPassword()}, and {@link #getS3URL()},
 * and uses MINIO_ROOT_USER and MINIO_ROOT_PASSWORD if provided (via {@link #withEnv(String, String)}
 * or similar method).
 *
 * <p>For AWS S3 client use {@link GarageContainerAws} that has methods
 * that return a client or a client builder that is pre-configured for the garage container.
 *
 * <p>For MinIO client and MinIO async client use {@link GarageContainerMinio} that has methods
 * that return a client or a client builder that is pre-configured for the garage container.
 *
 * @see GarageContainerAws#getS3Client()
 * @see GarageContainerAws#getS3ClientBuilder()
 * @see GarageContainerMinio#getMinioClient()
 * @see GarageContainerMinio#getMinioClientBuilder()
 * @see GarageContainerMinio#getMinioAsyncClient()
 * @see GarageContainerMinio#getMinioAsyncClientBuilder()
 */
public class GarageContainer extends AbstractGarageContainer<GarageContainer> {
  /**
   * Construct a Garage container from the dockerImageName.
   *
   * @param dockerImageName the full image name to use
   */
  public GarageContainer(final String dockerImageName) {
    super(dockerImageName);
  }

  /**
   * Construct a Garage container from the dockerImageName.
   *
   * Only ports 3900 (S3 API) and 3903 (Garage Admin API) get exposed by default.
   *
   * @param dockerImageName the full image name to use
   */
  public GarageContainer(final DockerImageName dockerImageName) {
    super(dockerImageName);
  }
}
