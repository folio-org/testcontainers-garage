package org.folio.tcgarage;

import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Testcontainer for the docker.io/dxflrs/garage image;
 * <a href="https://garagehq.deuxfleurs.fr/">garage</a> is a
 * lightweight S3-compatible distributed object storage service.
 *
 * <p>{@code AbstractGarageContainerAws} runs a single (non-distributed) node.
 *
 * <p>To ease the migration from testcontainers-localstack's {@code LocalStackContainer}
 * {@code AbstractGarageContainerAws} provides the methods {@link #withServices(String...)},
 * {@link #getEndpoint()}, {@link #getAccessKey()}, {@link #getSecretKey()}, and {@link #getRegion()},
 * and uses AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY if provided (via {@link #withEnv(String, String)}
 * or similar method).
 *
 * <p>To ease the migration from testcontainers-minio's {@code MinIOContainer}
 * {@code AbstractGarageContainerAws} provides the methods {@link #withUserName(String)},
 * {@link #withPassword(String)}, {@link #getUserName()}, {@link #getPassword()}, and {@link #getS3URL()},
 * and uses MINIO_ROOT_USER and MINIO_ROOT_PASSWORD if provided (via {@link #withEnv(String, String)}
 * or similar method).
 *
 * <p>Use {@link #getS3Client()} or {@link #getS3ClientBuilder()} to get
 * an AWS S3 client or an AWS S3 client builder that is pre-configured for the garage container.
 *
 * @see GarageContainerAws
 */
abstract class AbstractGarageContainerAws<T extends AbstractGarageContainerAws<T>> extends AbstractGarageContainer<T> {
  /**
   * Construct a Garage container from the dockerImageName.
   *
   * @param dockerImageName the full image name to use
   */
  protected AbstractGarageContainerAws(final String dockerImageName) {
    super(dockerImageName);
  }

  /**
   * Construct a Garage container from the dockerImageName.
   *
   * Only ports 3900 (S3 API) and 3903 (Garage Admin API) get exposed by default.
   *
   * @param dockerImageName the full image name to use
   */
  protected AbstractGarageContainerAws(final DockerImageName dockerImageName) {
    super(dockerImageName);
  }

  /**
   * AWS SDK v2 S3 client builder pre-configured for this {@code GarageContainer}.
   *
   * @return {@link S3ClientBuilder}
   */
  public S3ClientBuilder getS3ClientBuilder() {
    return S3Client
        .builder()
        .endpointOverride(getEndpoint())
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(getAccessKey(), getSecretKey())))
        .region(Region.of(getRegion()))
        // https://github.com/aws/aws-sdk-java-v2/issues/6387
        .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build());
  }

  /**
   * AWS SDK v2 S3 client pre-configured for this {@code GarageContainer}.
   *
   * @return {@link S3Client}
   */
  public S3Client getS3Client() {
    return getS3ClientBuilder().build();
  }
}
